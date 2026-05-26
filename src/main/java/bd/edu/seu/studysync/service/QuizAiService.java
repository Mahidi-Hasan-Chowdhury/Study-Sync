package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Question;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizDifficulty;
import bd.edu.seu.studysync.repository.QuizRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final OpenAiChatModel chatModel;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Quiz generateQuiz(String pdfText, String pdfFileName, String difficulty, int questionCount, String userId) {
        try {
            // Parse difficulty
            QuizDifficulty quizDifficulty = QuizDifficulty.fromString(difficulty);

            // Calculate time limit
            int timeLimitSeconds = quizDifficulty.calculateTimeLimit(questionCount);

            // Build AI prompt
            String prompt = buildMcqPrompt(pdfText, difficulty, questionCount);

            // Call AI using OpenAIChatModel
            String aiResponse = chatModel.call(prompt);

            // Parse JSON response
            List<Question> questions = parseAiResponse(aiResponse);

            // Create Quiz object
            Quiz quiz = new Quiz();
            quiz.setPdfFileName(pdfFileName);
            quiz.setQuestions(questions);
            quiz.setDifficulty(difficulty.toUpperCase());
            quiz.setQuestionCount(questionCount);
            quiz.setTimeLimitSeconds(timeLimitSeconds);
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setExtractedText(pdfText.substring(0, Math.min(500, pdfText.length())));
            quiz.setUserId(userId); // Set user ID

            return quizRepository.save(quiz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the AI prompt for MCQ generation with difficulty and count
     */
    private String buildMcqPrompt(String pdfText, String difficulty, int questionCount) {
        String difficultyInstructions = getDifficultyInstructions(difficulty);

        return """
                You are an expert quiz generator. Based ONLY on the following PDF content, generate EXACTLY %d multiple-choice questions (MCQs) at %s difficulty level.
                
                DIFFICULTY GUIDELINES:
                %s
                
                GENERAL RULES:
                - Use ONLY information from the PDF content below
                - Do NOT use external knowledge
                - Each question must have 4 options (A, B, C, D)
                - Each question must have exactly ONE correct answer
                - Questions should be interview-oriented and test understanding
                - Output MUST be valid JSON array
                - Do NOT include any explanation or preamble
                - Generate EXACTLY %d questions, no more, no less
                
                OUTPUT FORMAT (JSON):
                [
                  {
                    "question": "Question text here?",
                    "optionA": "First option",
                    "optionB": "Second option",
                    "optionC": "Third option",
                    "optionD": "Fourth option",
                    "correctAnswer": "A"
                  }
                ]
                
                PDF CONTENT:
                %s
                
                Generate the %d MCQs now as JSON array:
                """.formatted(questionCount, difficulty, difficultyInstructions, questionCount, pdfText, questionCount);
    }

    /**
     * Get difficulty-specific instructions for AI
     */
    private String getDifficultyInstructions(String difficulty) {
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> """
                EASY Level:
                - Questions should test basic recall and recognition
                - Answers should be directly stated in the text
                - Avoid complex analysis or interpretation
                - Use straightforward language
                - Focus on "what", "when", "where" questions
                """;
            case "HARD" -> """
                HARD Level:
                - Questions should require deep understanding and analysis
                - Test application of concepts to new scenarios
                - Include questions requiring synthesis of multiple concepts
                - Use "why", "how would", "what if" questions
                - Require critical thinking and inference
                """;
            default -> """
                MEDIUM Level:
                - Questions should test understanding of concepts
                - Require some interpretation of the text
                - Mix of recall and comprehension questions
                - Include "how", "why", "explain" questions
                - Balance between direct and inferential questions
                """;
        };
    }

    /**
     * Parses AI JSON response to List of Questions
     */
    private List<Question> parseAiResponse(String aiResponse) throws Exception {
        String cleanedResponse = aiResponse.trim();
        
        int startIndex = cleanedResponse.indexOf('[');
        int endIndex = cleanedResponse.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleanedResponse = cleanedResponse.substring(startIndex, endIndex + 1);
        } else {
            cleanedResponse = cleanedResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
        }

        return objectMapper.readValue(cleanedResponse, new TypeReference<List<Question>>() {});
    }


    
    /**
     * Get all quizzes for a specific user
     */
    public List<Quiz> getQuizzesByUserId(String userId) {
        return quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get quiz by ID
     */
    public Quiz getQuizById(String id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));
    }
}