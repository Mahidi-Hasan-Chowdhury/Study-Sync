package bd.edu.seu.studysync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_configs")
public class SystemConfig {

    @Id
    private String id;

    private boolean docsPublic = true;

    private LocalDateTime availableFrom = LocalDateTime.of(2026, 6, 10, 0, 0, 0);

    private LocalDateTime availableTo = LocalDateTime.of(2026, 6, 14, 23, 59, 59);

    private String adminPasscode = "StudySyncDocs2026";

    /**
     * Default constructor for creating the initial config
     */
    public SystemConfig(String id) {
        this.id = id;
        this.docsPublic = true;
        this.availableFrom = LocalDateTime.of(2026, 6, 10, 0, 0, 0);
        this.availableTo = LocalDateTime.of(2026, 6, 14, 23, 59, 59);
        this.adminPasscode = "StudySyncDocs2026";
    }

    /**
     * Check if docs are currently accessible based on public flag and time window
     */
    public boolean isCurrentlyAccessible() {
        if (docsPublic) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(availableFrom) && !now.isAfter(availableTo);
    }
}
