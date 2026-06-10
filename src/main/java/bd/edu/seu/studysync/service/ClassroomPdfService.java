package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Classroom;
import bd.edu.seu.studysync.model.ClassroomPdf;
import bd.edu.seu.studysync.repository.ClassroomPdfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassroomPdfService {

    private final ClassroomPdfRepository classroomPdfRepository;
    private final ClassroomService classroomService;
    private final DocumentService documentService;
    private final UserService userService;

    private static final String UPLOAD_DIR = "uploads/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /**
     * Upload a PDF to a classroom
     */
    public ClassroomPdf uploadPdf(String classroomId, MultipartFile file, String userId) throws IOException {
        // Validate classroom exists
        Optional<Classroom> classroomOpt = classroomService.getClassroomById(classroomId);
        if (classroomOpt.isEmpty()) {
            throw new RuntimeException("Classroom not found");
        }

        // Validate user is owner of classroom
        if (!classroomService.isTeacherOfClassroom(classroomId, userId)) {
            throw new RuntimeException("Only classroom owner can upload materials");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds 10 MB limit");
        }

        // Save file using existing DocumentService
        String savedFileName = documentService.saveFile(file);

        // Get username for display
        String username = userService.getUserById(userId)
                .map(user -> user.getUsername())
                .orElse("Unknown User");

        // Create ClassroomPdf record
        ClassroomPdf classroomPdf = new ClassroomPdf(
                classroomId,
                userId,
                savedFileName,
                originalName,
                file.getSize(),
                file.getContentType()
        );
        classroomPdf.setUploadedByName(username);

        return classroomPdfRepository.save(classroomPdf);
    }

    /**
     * Get all PDFs for a classroom (populates usernames for existing PDFs)
     */
    public List<ClassroomPdf> getPdfsByClassroom(String classroomId) {
        List<ClassroomPdf> pdfs = classroomPdfRepository.findByClassroomIdOrderByUploadedAtDesc(classroomId);

        // Populate username for PDFs that don't have it (backward compatibility)
        pdfs.forEach(pdf -> {
            if (pdf.getUploadedByName() == null || pdf.getUploadedByName().isEmpty()) {
                userService.getUserById(pdf.getUploadedBy())
                        .ifPresent(user -> pdf.setUploadedByName(user.getUsername()));
            }
        });

        return pdfs;
    }

    /**
     * Get PDF by ID
     */
    public Optional<ClassroomPdf> getPdfById(String pdfId) {
        return classroomPdfRepository.findById(pdfId);
    }

    /**
     * Delete a PDF from classroom
     */
    public void deletePdf(String pdfId, String userId) {
        Optional<ClassroomPdf> pdfOpt = classroomPdfRepository.findById(pdfId);
        if (pdfOpt.isEmpty()) {
            throw new RuntimeException("PDF not found");
        }

        ClassroomPdf pdf = pdfOpt.get();

        // Validate user is owner of classroom
        if (!classroomService.isTeacherOfClassroom(pdf.getClassroomId(), userId)) {
            throw new RuntimeException("Only classroom owner can delete materials");
        }

        // Delete file from filesystem
        try {
            Path filePath = Paths.get(UPLOAD_DIR + pdf.getFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue to delete database record
            System.err.println("Failed to delete file: " + e.getMessage());
        }

        // Delete database record
        classroomPdfRepository.deleteById(pdfId);
    }

    /**
     * Get the file path for a PDF
     */
    public File getPdfFile(String pdfId) {
        Optional<ClassroomPdf> pdfOpt = classroomPdfRepository.findById(pdfId);
        if (pdfOpt.isEmpty()) {
            throw new RuntimeException("PDF not found");
        }

        return new File(UPLOAD_DIR + pdfOpt.get().getFileName());
    }

    /**
     * Get total storage used by a classroom
     */
    public long getTotalStorageUsed(String classroomId) {
        List<ClassroomPdf> pdfs = classroomPdfRepository.findByClassroomIdOrderByUploadedAtDesc(classroomId);
        return pdfs.stream()
                .mapToLong(pdf -> pdf.getFileSize() != null ? pdf.getFileSize() : 0L)
                .sum();
    }

    /**
     * Check if user can access PDF (either owner or member of classroom)
     */
    public boolean canAccessPdf(String pdfId, String userId) {
        Optional<ClassroomPdf> pdfOpt = classroomPdfRepository.findById(pdfId);
        if (pdfOpt.isEmpty()) {
            return false;
        }

        ClassroomPdf pdf = pdfOpt.get();
        String classroomId = pdf.getClassroomId();

        // Owner can always access
        if (classroomService.isTeacherOfClassroom(classroomId, userId)) {
            return true;
        }

        // Members can also access
        return classroomService.isStudentInClassroom(classroomId, userId);
    }
}
