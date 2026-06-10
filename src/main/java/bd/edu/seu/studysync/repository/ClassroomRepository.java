package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.Classroom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends MongoRepository<Classroom, String> {

    // Find by teacher ID
    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(String teacherId);

    // Find by access code (for joining)
    Optional<Classroom> findByAccessCode(String accessCode);

    // Find active classrooms by teacher
    List<Classroom> findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(String teacherId);

    // Check if access code exists
    boolean existsByAccessCode(String accessCode);

    // Find classrooms where student is enrolled
    List<Classroom> findByStudentIdsContaining(String studentId);
}
