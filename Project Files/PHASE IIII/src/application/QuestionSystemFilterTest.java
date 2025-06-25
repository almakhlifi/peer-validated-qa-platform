package application;

import application.Question;
import application.QuestionSystem;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionSystemFilterTest {

    private QuestionSystem system;

    @BeforeEach
    public void setUp() throws SQLException {
        system = new QuestionSystem();
        system.databaseHelper.connectToDatabase();
        system.databaseHelper.clearAllMessages();
    }

    @Test
    public void testFilterAnsweredAndUnanswered() throws SQLException {
        Question q1 = new Question("Q1", "Content", "bob", List.of("Classwork"));
        Question q2 = new Question("Q2", "Content", "bob", List.of("Classwork"));
        q2.setAcceptedAnswerId(10);

        system.databaseHelper.saveQuestion(q1);
        system.databaseHelper.saveQuestion(q2);

        List<Question> all = system.databaseHelper.loadQuestions();
        long answered = all.stream().filter(Question::isAnswered).count();
        long unanswered = all.stream().filter(q -> !q.isAnswered()).count();

        assertTrue(answered >= 1);
        assertTrue(unanswered >= 1);
    }
}
