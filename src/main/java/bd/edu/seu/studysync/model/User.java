package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password; // BCrypt encrypted

    // Internal storage for role (String for DB compatibility)
    // @Field("role") maps this Java field to the "role" field in MongoDB
    @Field("role")
    private String roleStr = "STUDENT";

    private LocalDateTime createdAt;

    private boolean enabled = true;

    private boolean isPro = false;

    /**
     * Get the role as UserRole enum with backward compatibility
     * Converts stored string to enum, defaults to STUDENT for legacy "USER" role
     */
    public UserRole getRole() {
        if (roleStr == null || roleStr.isEmpty()) {
            return UserRole.STUDENT;
        }

        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Legacy role "USER" or any unknown -> default to STUDENT
            return UserRole.STUDENT;
        }
    }

    /**
     * Set the role using UserRole enum
     */
    public void setRole(UserRole role) {
        this.roleStr = role != null ? role.name() : "STUDENT";
    }

    // Helper methods for role checking
    public boolean isTeacher() {
        return getRole() == UserRole.TEACHER;
    }

    public boolean isStudent() {
        return getRole() == UserRole.STUDENT;
    }

    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN;
    }

    // Constructor for creating new user with role
    public User(String username, String email, String password, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.setRole(role);
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
        this.isPro = false;
    }
}
