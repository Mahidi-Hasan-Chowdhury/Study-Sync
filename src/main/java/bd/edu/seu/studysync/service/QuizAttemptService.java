package bd.edu.seu.studysync.service;

import bd.edu.seu.studysync.model.Question;
import bd.edu.seu.studysync.model.Quiz;
import bd.edu.seu.studysync.model.QuizAttempt;
import bd.edu.seu.studysync.model.UserAnswer;
import bd.edu.seu.studysync.repository.QuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRoomService quizRoomService;


    public QuizAttempt evaluateQuiz(Quiz quiz, Map<String, String> userAnswers, int timeTaken, String userId, String roomId) {
        List<UserAnswer> answers = new ArrayList<>();
        int correctCount = 0;

        // Loop through each question and check answer
        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Question question = quiz.getQuestions().get(i);
            int questionNum = i + 1;

            String selected = userAnswers.getOrDefault(String.valueOf(questionNum), "");
            String correct = question.getCorrectAnswer();

            boolean isCorrect = selected.equals(correct);
            if (isCorrect) {
                correctCount++;
            }

            UserAnswer userAnswer = new UserAnswer(
                    questionNum,
                    selected,
                    correct,
                    isCorrect,
                    question.getQuestion()
            );
            answers.add(userAnswer);
        }

        // Calculate score percentage
        double scorePercentage = (correctCount * 100.0) / quiz.getQuestions().size();

        // Create QuizAttempt object
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quiz.getId());
        attempt.setPdfFileName(quiz.getPdfFileName());
        attempt.setDifficulty(quiz.getDifficulty());
        attempt.setQuestionCount(quiz.getQuestionCount());
        attempt.setTimeLimitSeconds(quiz.getTimeLimitSeconds());
        attempt.setUserAnswers(answers);
        attempt.setTotalQuestions(quiz.getQuestions().size());
        attempt.setCorrectAnswers(correctCount);
        attempt.setScorePercentage(Math.round(scorePercentage * 100.0) / 100.0);
        attempt.setTimeTakenSeconds(timeTaken);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setUserId(userId);
        attempt.setRoomId(roomId);

        // Save to database
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        
        // Notify QuizRoomService if this validation is part of a room
        if (roomId != null && !roomId.isEmpty()) {
            quizRoomService.updateScore(roomId, userId, savedAttempt.getId(), savedAttempt.getScorePercentage());
        }
        
        return savedAttempt;
    }


    public List<QuizAttempt> getAttemptsByUserId(String userId) {
        return quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
    }


    public QuizAttempt getAttemptById(String attemptId) {
        return quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));
    }
}