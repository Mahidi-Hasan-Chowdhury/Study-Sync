package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.SystemConfig;
import bd.edu.seu.studysync.repository.ClassroomRepository;
import bd.edu.seu.studysync.repository.QuizAttemptRepository;
import bd.edu.seu.studysync.repository.QuizRepository;
import bd.edu.seu.studysync.repository.SystemConfigRepository;
import bd.edu.seu.studysync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocsService {

    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    private static final String DEFAULT_CONFIG_ID = "docs_config";

    /**
     * Check if documentation is currently accessible
     * Returns true if docsPublic is enabled OR current time is within the available window
     */
    public boolean isDocsAccessible() {
        SystemConfig config = getConfig();
        if (config.isDocsPublic()) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(config.getAvailableFrom()) && !now.isAfter(config.getAvailableTo());
    }

    /**
     * Get live system statistics
     */
    public Map<String, Long> getLiveStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalClassrooms", classroomRepository.count());
        stats.put("totalQuizzes", quizRepository.count());
        stats.put("totalAttempts", quizAttemptRepository.count());
        return stats;
    }

    /**
     * Get the system configuration
     * Creates default config if none exists
     */
    public SystemConfig getConfig() {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(DEFAULT_CONFIG_ID);
        if (configOpt.isEmpty()) {
            // Create default config
            SystemConfig defaultConfig = new SystemConfig(DEFAULT_CONFIG_ID);
            defaultConfig.setDocsPublic(true);
            defaultConfig.setAvailableFrom(LocalDateTime.of(2026, 6, 10, 0, 0, 0));
            defaultConfig.setAvailableTo(LocalDateTime.of(2026, 6, 14, 23, 59, 59));
            defaultConfig.setAdminPasscode("StudySyncDocs2026");
            return systemConfigRepository.save(defaultConfig);
        }
        return configOpt.get();
    }

    /**
     * Save system configuration
     */
    public void saveConfig(SystemConfig config) {
        config.setId(DEFAULT_CONFIG_ID);
        systemConfigRepository.save(config);
    }

    /**
     * Verify admin passcode
     */
    public boolean verifyAdminPasscode(String passcode) {
        SystemConfig config = getConfig();
        return config.getAdminPasscode() != null && config.getAdminPasscode().equals(passcode);
    }
}
