package bd.edu.seu.studysync.service;
import bd.edu.seu.studysync.model.DashboardStats;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizAttempt;
import bd.edu.seu.studysync.repository.QuizAttemptRepository;
import bd.edu.seu.studysync.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public DashboardStats getStatistics(String userId) {
        DashboardStats stats = new DashboardStats();
        // Get all attempts for user
        List<QuizAttempt> userAttempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
        
        // Get user quizzes count
        List<Quiz> userQuizzes = quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Total counts
        stats.setTotalQuizzes(userQuizzes.size());
        stats.setTotalAttempts(userAttempts.size());
        if (userAttempts.isEmpty()) {
            return stats; // Return empty stats if no attempts
        }

        // Calculate overall average score
        double totalScore = userAttempts.stream()
                .mapToDouble(QuizAttempt::getScorePercentage)
                .sum();
        stats.setAverageScore(Math.round((totalScore / userAttempts.size()) * 100.0) / 100.0);


        // Total questions answered
        int totalQuestions = userAttempts.stream()
                .mapToInt(QuizAttempt::getTotalQuestions)
                .sum();
        stats.setTotalQuestionsAnswered(totalQuestions);


        // Performance by difficulty
        calculateDifficultyStats(stats, userAttempts, "EASY");
        calculateDifficultyStats(stats, userAttempts, "MEDIUM");
        calculateDifficultyStats(stats, userAttempts, "HARD");


        // Best performance
        QuizAttempt bestAttempt = userAttempts.stream()
                .max((a1, a2) -> Double.compare(a1.getScorePercentage(), a2.getScorePercentage()))
                .orElse(null);
        if (bestAttempt != null) {
            stats.setBestScore(bestAttempt.getScorePercentage());
            stats.setBestQuizName(bestAttempt.getPdfFileName());
        }


        // Recent activity
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        long last7Days = userAttempts.stream()
                .filter(a -> a.getAttemptedAt().isAfter(sevenDaysAgo))
                .count();
        stats.setAttemptsLast7Days((int) last7Days);

        long last30Days = userAttempts.stream()
                .filter(a -> a.getAttemptedAt().isAfter(thirtyDaysAgo))
                .count();
        stats.setAttemptsLast30Days((int) last30Days);
        return stats;
    }


    private void calculateDifficultyStats(DashboardStats stats, List<QuizAttempt> allAttempts, String difficulty) {
        List<QuizAttempt> difficultyAttempts = allAttempts.stream()
                .filter(a -> difficulty.equalsIgnoreCase(a.getDifficulty()))
                .toList();
        int count = difficultyAttempts.size();
        double average = 0.0;
        if (count > 0) {
            double total = difficultyAttempts.stream()
                    .mapToDouble(QuizAttempt::getScorePercentage)
                    .sum();
            average = Math.round((total / count) * 100.0) / 100.0;
        }
        switch (difficulty.toUpperCase()) {
            case "EASY":
                stats.setEasyAttempts(count);
                stats.setEasyAverageScore(average);
                break;
            case "MEDIUM":
                stats.setMediumAttempts(count);
                stats.setMediumAverageScore(average);
                break;
            case "HARD":
                stats.setHardAttempts(count);
                stats.setHardAverageScore(average);
                break;
        }
    }
}