package bd.edu.seu.studysync.model;

public enum QuizDifficulty {
    EASY("Easy", 30),      // 30 seconds per question
    MEDIUM("Medium", 45),  // 45 seconds per question
    HARD("Hard", 60);      // 60 seconds per question

    private final String displayName;
    private final int secondsPerQuestion;

    QuizDifficulty(String displayName, int secondsPerQuestion) {
        this.displayName = displayName;
        this.secondsPerQuestion = secondsPerQuestion;
    }



    /**
     * Calculate total time limit based on number of questions
     */
    public int calculateTimeLimit(int questionCount) {
        return questionCount * secondsPerQuestion;
    }

    /**
     * Get difficulty from string (case-insensitive)
     */
    public static QuizDifficulty fromString(String difficulty) {
        try {
            return QuizDifficulty.valueOf(difficulty.toUpperCase());
        } catch (Exception e) {
            return MEDIUM; // Default to medium
        }
    }
}