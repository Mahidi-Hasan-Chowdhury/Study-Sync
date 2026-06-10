package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "classrooms")
public class Classroom {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    @Indexed
    private String teacherId;

    @Indexed(unique = true)
    private String accessCode;  // 6-character code: "ABC123"

    private List<String> studentIds = new ArrayList<>();

    private boolean isActive = true;

    private LocalDateTime createdAt;

    private LocalDateTime archivedAt;

    // Constructor for creating new classroom
    public Classroom(String name, String description, String teacherId, String accessCode) {
        this.name = name;
        this.description = description;
        this.teacherId = teacherId;
        this.accessCode = accessCode;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.studentIds = new ArrayList<>();
    }

    // Add student to classroom
    public void addStudent(String studentId) {
        if (this.studentIds == null) {
            this.studentIds = new ArrayList<>();
        }
        if (!this.studentIds.contains(studentId)) {
            this.studentIds.add(studentId);
        }
    }

    // Remove student from classroom
    public void removeStudent(String studentId) {
        if (this.studentIds != null) {
            this.studentIds.remove(studentId);
        }
    }

    // Check if student is enrolled
    public boolean hasStudent(String studentId) {
        return this.studentIds != null && this.studentIds.contains(studentId);
    }

    // Get student count
    public int getStudentCount() {
        return this.studentIds != null ? this.studentIds.size() : 0;
    }

    // Archive classroom
    public void archive() {
        this.isActive = false;
        this.archivedAt = LocalDateTime.now();
    }

    // Reactivate classroom
    public void reactivate() {
        this.isActive = true;
        this.archivedAt = null;
    }
}
