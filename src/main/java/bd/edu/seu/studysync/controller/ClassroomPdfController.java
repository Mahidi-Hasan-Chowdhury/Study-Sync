package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.ClassroomPdf;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.ClassroomPdfService;
import bd.edu.seu.studysync.service.ClassroomService;
import bd.edu.seu.studysync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ClassroomPdfController {

    private final ClassroomPdfService classroomPdfService;
    private final ClassroomService classroomService;
    private final UserService userService;

    /**
     * Upload PDF to classroom
     */
    @PostMapping("/classroom/{id}/upload")
    public String uploadPdf(
            @PathVariable String id,
            @RequestParam("pdfFile") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<User> currentUser = userService.getCurrentUser();
            if (currentUser.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "You must be logged in");
                return "redirect:/login";
            }

            ClassroomPdf pdf = classroomPdfService.uploadPdf(id, file, currentUser.get().getId());
            redirectAttributes.addFlashAttribute("success", "PDF uploaded successfully!");
            return "redirect:/classroom/" + id;

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/classroom/" + id;
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
            return "redirect:/classroom/" + id;
        }
    }

    /**
     * Display materials page for a classroom
     */
    @GetMapping("/classroom/{id}/materials")
    public String viewMaterials(@PathVariable String id, Model model) {
        try {
            Optional<User> currentUser = userService.getCurrentUser();
            if (currentUser.isEmpty()) {
                return "redirect:/login";
            }

            // Check classroom access
            if (!classroomService.getClassroomById(id).isPresent()) {
                return "redirect:/classroom?error=Classroom not found";
            }

            boolean isOwner = classroomService.isTeacherOfClassroom(id, currentUser.get().getId());
            boolean isMember = classroomService.isStudentInClassroom(id, currentUser.get().getId());

            if (!isOwner && !isMember) {
                return "redirect:/classroom?error=You don't have access to this classroom";
            }

            List<ClassroomPdf> pdfs = classroomPdfService.getPdfsByClassroom(id);
            long totalStorage = classroomPdfService.getTotalStorageUsed(id);

            model.addAttribute("classroomId", id);
            model.addAttribute("pdfs", pdfs);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("isMember", isMember);
            model.addAttribute("totalStorage", totalStorage);
            model.addAttribute("contentPage", "classroom-materials");
            return "layout";

        } catch (Exception e) {
            return "redirect:/classroom?error=" + e.getMessage();
        }
    }

    /**
     * Download PDF
     */
    @GetMapping("/classroom/pdf/{pdfId}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String pdfId) {
        try {
            Optional<User> currentUser = userService.getCurrentUser();
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(403).build();
            }

            if (!classroomPdfService.canAccessPdf(pdfId, currentUser.get().getId())) {
                return ResponseEntity.status(403).build();
            }

            ClassroomPdf pdf = classroomPdfService.getPdfById(pdfId).orElseThrow();
            Path filePath = Paths.get("uploads/" + pdf.getFileName());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.getOriginalName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete PDF
     */
    @DeleteMapping("/classroom/pdf/{pdfId}")
    @ResponseBody
    public ResponseEntity<?> deletePdf(@PathVariable String pdfId) {
        try {
            Optional<User> currentUser = userService.getCurrentUser();
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(403).body("Not logged in");
            }

            classroomPdfService.deletePdf(pdfId, currentUser.get().getId());
            return ResponseEntity.ok().body("PDF deleted successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get PDF info (AJAX endpoint)
     */
    @GetMapping("/classroom/pdf/{pdfId}/info")
    @ResponseBody
    public ResponseEntity<?> getPdfInfo(@PathVariable String pdfId) {
        try {
            Optional<User> currentUser = userService.getCurrentUser();
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(403).build();
            }

            if (!classroomPdfService.canAccessPdf(pdfId, currentUser.get().getId())) {
                return ResponseEntity.status(403).build();
            }

            ClassroomPdf pdf = classroomPdfService.getPdfById(pdfId).orElseThrow();
            return ResponseEntity.ok(pdf);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
