package bd.edu.seu.studysync.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    // Overall Statistics
    private int totalQuizzes;
    private int totalAttempts;
    private double averageScore;
    private int totalQuestionsAnswered;

    // Performance by Difficulty
    private int easyAttempts;
    private double easyAverageScore;

    private int mediumAttempts;
    private double mediumAverageScore;

    private int hardAttempts;
    private double hardAverageScore;

    // Best Performance
    private double bestScore;
    private String bestQuizName;

    // Recent Activity
    private int attemptsLast7Days;
    private int attemptsLast30Days;
}