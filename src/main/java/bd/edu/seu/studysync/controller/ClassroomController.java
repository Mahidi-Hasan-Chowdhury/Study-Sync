package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.Classroom;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.model.UserRole;
import bd.edu.seu.studysync.service.ClassroomService;
import bd.edu.seu.studysync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;
    private final UserService userService;

    /**
     * Display classroom creation page
     */
    @GetMapping("/classroom/create")
    public String createClassroomPage(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();

        // Check if user is logged in
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("classroom", new Classroom());
        model.addAttribute("contentPage", "classroom-create");
        return "layout";
    }

    /**
     * Process classroom creation
     */
    @PostMapping("/classroom/create")
    public String createClassroom(
            @RequestParam String name,
            @RequestParam String description,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to create a classroom");
            return "redirect:/login";
        }

        try {
            Classroom classroom = classroomService.createClassroom(name, description, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success",
                    "Classroom created successfully! Access code: " + classroom.getAccessCode());
            return "redirect:/classroom/" + classroom.getId();

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/create";
        }
    }

    /**
     * Display classroom join page (students only)
     */
    @GetMapping("/classroom/join")
    public String joinClassroomPage(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();

        // Check if user is logged in
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("accessCode", "");
        model.addAttribute("contentPage", "classroom-join");
        return "layout";
    }

    /**
     * Process classroom join
     */
    @PostMapping("/classroom/join")
    public String joinClassroom(
            @RequestParam String accessCode,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to join a classroom");
            return "redirect:/login";
        }

        try {
            Classroom classroom = classroomService.joinClassroom(accessCode, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "Successfully joined classroom: " + classroom.getName());
            return "redirect:/classroom/" + classroom.getId();

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/join";
        }
    }

    /**
     * Leave a classroom
     */
    @PostMapping("/classroom/{id}/leave")
    public String leaveClassroom(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in");
            return "redirect:/login";
        }

        try {
            classroomService.leaveClassroom(id, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "You have left the classroom");
            return "redirect:/classroom";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/" + id;
        }
    }

    /**
     * Display all classrooms for current user
     */
    @GetMapping("/classroom")
    public String listClassrooms(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUser.get();

        // Get both classrooms created by user and classrooms user has joined
        List<Classroom> createdClassrooms = classroomService.getTeacherClassrooms(user.getId());
        List<Classroom> joinedClassrooms = classroomService.getStudentClassrooms(user.getId());

        model.addAttribute("createdClassrooms", createdClassrooms);
        model.addAttribute("joinedClassrooms", joinedClassrooms);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "classrooms");
        return "layout";
    }

    /**
     * Display classroom details
     */
    @GetMapping("/classroom/{id}")
    public String classroomDetails(@PathVariable String id, Model model) {
        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }

        Optional<Classroom> classroomOpt = classroomService.getClassroomById(id);
        if (classroomOpt.isEmpty()) {
            return "redirect:/classroom?error=Classroom+not+found";
        }

        Classroom classroom = classroomOpt.get();
        User user = currentUser.get();

        // Check permissions
        boolean isTeacher = classroomService.isTeacherOfClassroom(id, user.getId());
        boolean isStudent = classroomService.isStudentInClassroom(id, user.getId());

        if (!isTeacher && !isStudent) {
            return "redirect:/classroom?error=You+don't+have+access+to+this+classroom";
        }

        model.addAttribute("classroom", classroom);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("currentUser", user);
        model.addAttribute("contentPage", "classroom-details");
        return "layout";
    }

    /**
     * Update classroom details (teacher only)
     */
    @PostMapping("/classroom/{id}/update")
    public String updateClassroom(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam String description,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in");
            return "redirect:/login";
        }

        // Verify user is the teacher
        if (!classroomService.isTeacherOfClassroom(id, currentUser.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Only the teacher can update classroom details");
            return "redirect:/classroom/" + id;
        }

        try {
            classroomService.updateClassroom(id, name, description);
            redirectAttributes.addFlashAttribute("success", "Classroom updated successfully");
            return "redirect:/classroom/" + id;

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/" + id;
        }
    }

    /**
     * Archive a classroom (teacher only)
     */
    @PostMapping("/classroom/{id}/archive")
    public String archiveClassroom(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUser = userService.getCurrentUser();

        if (currentUser.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in");
            return "redirect:/login";
        }

        // Verify user is the teacher
        if (!classroomService.isTeacherOfClassroom(id, currentUser.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Only the teacher can archive the classroom");
            return "redirect:/classroom/" + id;
        }

        try {
            classroomService.archiveClassroom(id);
            redirectAttributes.addFlashAttribute("success", "Classroom archived successfully");
            return "redirect:/classroom";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/" + id;
        }
    }
}
