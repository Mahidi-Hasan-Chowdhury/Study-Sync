package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Classroom;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.model.UserRole;
import bd.edu.seu.studysync.repository.ClassroomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final UserService userService;

    /**
     * Generate a unique 6-character access code
     * Format: 3 letters + 3 digits (e.g., "ABC123")
     */
    public String generateAccessCode() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed confusing chars: I, O, 0, 1
        StringBuilder code;

        int maxAttempts = 100;
        int attempts = 0;

        do {
            code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = (int) (Math.random() * characters.length());
                code.append(characters.charAt(index));
            }

            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique access code after " + maxAttempts + " attempts");
            }
        } while (classroomRepository.existsByAccessCode(code.toString()));

        return code.toString();
    }

    /**
     * Create a new classroom (any logged-in user can create)
     */
    public Classroom createClassroom(String name, String description, String ownerId) {
        // Verify user exists
        Optional<User> userOpt = userService.getUserById(ownerId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Generate unique access code
        String accessCode = generateAccessCode();

        // Create classroom with user as owner/teacher
        Classroom classroom = new Classroom(name, description, ownerId, accessCode);
        return classroomRepository.save(classroom);
    }

    /**
     * Join a classroom using access code (any logged-in user can join)
     */
    public Classroom joinClassroom(String accessCode, String userId) {
        // Verify user exists
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Find classroom by access code
        Optional<Classroom> classroomOpt = classroomRepository.findByAccessCode(accessCode);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Invalid access code");
        }

        Classroom classroom = classroomOpt.get();

        // Check if classroom is active
        if (!classroom.isActive()) {
            throw new RuntimeException("This classroom is not active");
        }

        // Check if user is already enrolled
        if (classroom.hasStudent(userId)) {
            throw new RuntimeException("You are already enrolled in this classroom");
        }

        // Prevent classroom owner from joining their own classroom as a student
        if (classroom.getTeacherId().equals(userId)) {
            throw new RuntimeException("You cannot join your own classroom");
        }

        // Add user to classroom
        classroom.addStudent(userId);
        return classroomRepository.save(classroom);
    }

    /**
     * Leave a classroom
     */
    public void leaveClassroom(String classroomId, String studentId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();
        classroom.removeStudent(studentId);
        classroomRepository.save(classroom);
    }

    /**
     * Get all classrooms for a teacher
     */
    public List<Classroom> getTeacherClassrooms(String teacherId) {
        return classroomRepository.findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(teacherId);
    }

    /**
     * Get all classrooms for a student
     */
    public List<Classroom> getStudentClassrooms(String studentId) {
        return classroomRepository.findByStudentIdsContaining(studentId);
    }

    /**
     * Get classroom by ID
     */
    public Optional<Classroom> getClassroomById(String classroomId) {
        return classroomRepository.findById(classroomId);
    }

    /**
     * Get classroom by access code
     */
    public Optional<Classroom> getClassroomByAccessCode(String accessCode) {
        return classroomRepository.findByAccessCode(accessCode);
    }

    /**
     * Update classroom details
     */
    public Classroom updateClassroom(String classroomId, String name, String description) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();
        classroom.setName(name);
        classroom.setDescription(description);
        return classroomRepository.save(classroom);
    }

    /**
     * Archive a classroom
     */
    public void archiveClassroom(String classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();
        classroom.archive();
        classroomRepository.save(classroom);
    }

    /**
     * Reactivate a classroom
     */
    public void reactivateClassroom(String classroomId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();
        classroom.reactivate();
        classroomRepository.save(classroom);
    }

    /**
     * Check if user is teacher of classroom
     */
    public boolean isTeacherOfClassroom(String classroomId, String userId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        return classroomOpt.isPresent() && classroomOpt.get().getTeacherId().equals(userId);
    }

    /**
     * Check if user is student in classroom
     */
    public boolean isStudentInClassroom(String classroomId, String userId) {
        Optional<Classroom> classroomOpt = classroomRepository.findById(classroomId);
        return classroomOpt.isPresent() && classroomOpt.get().hasStudent(userId);
    }
}
