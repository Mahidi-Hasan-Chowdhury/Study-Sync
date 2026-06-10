package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Model for PDF files uploaded to a classroom
 * Links classroom members to shared PDF materials
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "classroom_pdfs")
public class ClassroomPdf {

    @Id
    private String id;

    @Indexed
    private String classroomId;        // ID of the classroom this PDF belongs to

    @Indexed
    private String uploadedBy;         // User ID who uploaded this PDF

    private String uploadedByName;     // Username of uploader (for display)

    private String fileName;           // Stored file name (e.g., "1234567890_notes.pdf")
    private String originalName;       // Original upload name (e.g., "Lecture Notes.pdf")
    private Long fileSize;             // File size in bytes
    private String contentType;        // MIME type (e.g., "application/pdf")
    private LocalDateTime uploadedAt;

    /**
     * Constructor for creating new ClassroomPdf
     */
    public ClassroomPdf(String classroomId, String uploadedBy, String fileName, String originalName, Long fileSize, String contentType) {
        this.classroomId = classroomId;
        this.uploadedBy = uploadedBy;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = LocalDateTime.now();
    }

    /**
     * Get file size in human-readable format (MB, KB, etc.)
     */
    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "0 B";
        }

        long bytes = fileSize;
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Check if file size exceeds limit (10 MB)
     */
    public boolean exceedsLimit() {
        long maxSize = 10 * 1024 * 1024; // 10 MB
        return fileSize != null && fileSize > maxSize;
    }
}
