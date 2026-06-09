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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizAiService {

    private final OpenAiChatModel chatModel;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Quiz generateQuiz(String pdfText, String pdfFileName, String difficulty, int questionCount, String userId) {
        return generateQuiz(pdfText, pdfFileName, difficulty, questionCount, userId, "MCQ");
    }

    /**
     * Generate quiz with explicit MCQ and CQ counts
     */
    public Quiz generateQuiz(String pdfText, String pdfFileName, String difficulty, int mcqCount, int cqCount, String userId, String quizType) {
        try {
            // Parse difficulty
            QuizDifficulty quizDifficulty = QuizDifficulty.fromString(difficulty);
            int totalQuestions = mcqCount + cqCount;

            // Calculate time limit separately for MCQ and CQ
            // MCQ: 30/45/60 seconds per question based on difficulty
            // CQ: 2/2.5/3 minutes per question (writing takes much longer!)
            int mcqTimeSeconds = mcqCount * quizDifficulty.getSecondsPerQuestion();

            int cqSecondsPerQuestion;
            switch (difficulty.toUpperCase()) {
                case "EASY":
                    cqSecondsPerQuestion = 120; // 2 minutes for easy CQ
                    break;
                case "HARD":
                    cqSecondsPerQuestion = 180; // 3 minutes for hard CQ
                    break;
                default:
                    cqSecondsPerQuestion = 150; // 2.5 minutes for medium CQ
            }

            int cqTimeSeconds = cqCount * cqSecondsPerQuestion;
            int timeLimitSeconds = mcqTimeSeconds + cqTimeSeconds;

            List<Question> questions = new ArrayList<>();

            // Generate MCQ questions if count > 0
            if (mcqCount > 0) {
                questions.addAll(generateMcqQuestions(pdfText, difficulty, mcqCount));
            }

            // Generate CQ questions if count > 0
            if (cqCount > 0) {
                questions.addAll(generateCqQuestions(pdfText, difficulty, cqCount));
            }

            // Create Quiz object
            Quiz quiz = new Quiz();
            quiz.setPdfFileName(pdfFileName);
            quiz.setQuestions(questions);
            quiz.setQuizType(quizType.toUpperCase());
            quiz.setDifficulty(difficulty.toUpperCase());
            quiz.setQuestionCount(totalQuestions);
            quiz.setMcqCount(mcqCount);
            quiz.setCqCount(cqCount);
            quiz.setTimeLimitSeconds(timeLimitSeconds);
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setExtractedText(pdfText.substring(0, Math.min(500, pdfText.length())));
            quiz.setUserId(userId);

            return quizRepository.save(quiz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }

    public Quiz generateQuiz(String pdfText, String pdfFileName, String difficulty, int questionCount, String userId, String quizType) {
        try {
            // Parse difficulty
            QuizDifficulty quizDifficulty = QuizDifficulty.fromString(difficulty);

            List<Question> questions;
            int mcqCount = 0;
            int cqCount = 0;

            switch (quizType.toUpperCase()) {
                case "CQ":
                    questions = generateCqQuestions(pdfText, difficulty, questionCount);
                    cqCount = questionCount;
                    break;
                case "MIXED":
                    // Split questions evenly between MCQ and CQ
                    mcqCount = questionCount / 2;
                    cqCount = questionCount - mcqCount;
                    questions = new ArrayList<>();

                    // Generate MCQ questions
                    questions.addAll(generateMcqQuestions(pdfText, difficulty, mcqCount));

                    // Generate CQ questions
                    questions.addAll(generateCqQuestions(pdfText, difficulty, cqCount));
                    break;
                case "MCQ":
                default:
                    questions = generateMcqQuestions(pdfText, difficulty, questionCount);
                    mcqCount = questionCount;
                    break;
            }

            // Calculate time limit separately for MCQ and CQ
            int mcqTimeSeconds = mcqCount * quizDifficulty.getSecondsPerQuestion();

            int cqSecondsPerQuestion;
            switch (difficulty.toUpperCase()) {
                case "EASY":
                    cqSecondsPerQuestion = 120; // 2 minutes for easy CQ
                    break;
                case "HARD":
                    cqSecondsPerQuestion = 180; // 3 minutes for hard CQ
                    break;
                default:
                    cqSecondsPerQuestion = 150; // 2.5 minutes for medium CQ
            }

            int cqTimeSeconds = cqCount * cqSecondsPerQuestion;
            int timeLimitSeconds = mcqTimeSeconds + cqTimeSeconds;

            // Create Quiz object
            Quiz quiz = new Quiz();
            quiz.setPdfFileName(pdfFileName);
            quiz.setQuestions(questions);
            quiz.setQuizType(quizType.toUpperCase());
            quiz.setDifficulty(difficulty.toUpperCase());
            quiz.setQuestionCount(questionCount);
            quiz.setMcqCount(mcqCount);
            quiz.setCqCount(cqCount);
            quiz.setTimeLimitSeconds(timeLimitSeconds);
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setExtractedText(pdfText.substring(0, Math.min(500, pdfText.length())));
            quiz.setUserId(userId);

            return quizRepository.save(quiz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }

    /**
     * Generate MCQ questions using AI
     */
    private List<Question> generateMcqQuestions(String pdfText, String difficulty, int questionCount) {
        try {
            String prompt = buildMcqPrompt(pdfText, difficulty, questionCount);
            String aiResponse = chatModel.call(prompt);
            List<Question> questions = parseAiResponse(aiResponse);

            // Ensure question type is set
            questions.forEach(q -> q.setQuestionType("MCQ"));

            return questions;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate MCQ questions: " + e.getMessage(), e);
        }
    }

    /**
     * Generate CQ (Constructed Response) questions using AI
     */
    private List<Question> generateCqQuestions(String pdfText, String difficulty, int questionCount) {
        try {
            String prompt = buildCqPrompt(pdfText, difficulty, questionCount);
            String aiResponse = chatModel.call(prompt);
            List<Question> questions = parseCqResponse(aiResponse);

            // Ensure question type is set
            questions.forEach(q -> q.setQuestionType("CQ"));

            return questions;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CQ questions: " + e.getMessage(), e);
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

                CRITICAL CONSTRAINTS - READ CAREFULLY:
                - EVERY part of your questions (question, options, correct answer) MUST come from the PDF content below
                - Do NOT use external knowledge, business concepts, or common sense not found in the text
                - Do NOT introduce concepts, examples, or factors that are not explicitly mentioned in the PDF
                - If the PDF does not cover a topic, do NOT ask about it
                - If the PDF is brief/simple, your questions should also be brief/simple
                - The correct answer MUST be directly supported by the PDF text
                - Wrong options can be plausible but must not require outside knowledge to identify as wrong

                OUTPUT FORMAT (JSON):
                [
                  {
                    "question": "Question text here (must be answerable from PDF)?",
                    "optionA": "First option from PDF content",
                    "optionB": "Second option from PDF content",
                    "optionC": "Third option from PDF content",
                    "optionD": "Fourth option from PDF content",
                    "correctAnswer": "A"
                  }
                ]

                PDF CONTENT:
                %s

                Generate the %d MCQs now as JSON array:
                """.formatted(questionCount, difficulty, difficultyInstructions, pdfText, questionCount);
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

    /**
     * Builds the AI prompt for CQ (Constructed Response) generation
     */
    private String buildCqPrompt(String pdfText, String difficulty, int questionCount) {
        String difficultyInstructions = getDifficultyInstructions(difficulty);

        return """
                You are an expert quiz generator. Based ONLY on the following PDF content, generate EXACTLY %d constructed response questions (CQ) at %s difficulty level.

                DIFFICULTY GUIDELINES:
                %s

                CRITICAL CONSTRAINTS - READ CAREFULLY:
                - EVERY part of your questions and model answers MUST come from the PDF content below
                - Do NOT use external knowledge, business concepts, or common sense not found in the text
                - Do NOT introduce factors, considerations, or examples not explicitly mentioned in the PDF
                - If the PDF does not cover a topic, do NOT ask about it
                - If the PDF is brief, your questions and model answers should be brief
                - The model answer must ONLY contain information from the PDF
                - When creating "what if" or application questions, base them ONLY on concepts/examples in the PDF

                OUTPUT FORMAT (JSON):
                [
                  {
                    "question": "Question text here (must be answerable from PDF content)?",
                    "correctAnswer": "The complete model answer using ONLY information from the PDF. Do not add external knowledge.",
                    "answerExplanation": "Brief rubric: What earns 3 points (correct/good), 2 points (partial), 1 point (attempted), 0 points (wrong)."
                  }
                ]

                PDF CONTENT:
                %s

                Generate the %d CQs now as JSON array:
                """.formatted(questionCount, difficulty, difficultyInstructions, pdfText, questionCount);
    }

    /**
     * Parses AI CQ JSON response to List of Questions
     */
    private List<Question> parseCqResponse(String aiResponse) throws Exception {
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

        List<Question> questions = new ArrayList<>();

        // Use ObjectMapper to read as a list of maps
        List<Map<String, Object>> rawQuestions = objectMapper.readValue(
                cleanedResponse,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        for (Map<String, Object> rawQ : rawQuestions) {
            Question q = new Question();
            q.setQuestion((String) rawQ.get("question"));
            q.setCorrectAnswer((String) rawQ.get("correctAnswer")); // Expected key points
            q.setAnswerExplanation((String) rawQ.get("answerExplanation"));
            q.setQuestionType("CQ");
            questions.add(q);
        }

        return questions;
    }
}