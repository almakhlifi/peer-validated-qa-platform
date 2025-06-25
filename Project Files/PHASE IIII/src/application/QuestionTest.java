package application;

import application.Question;

import application.Answer;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionTest {

    @Test
    public void testIsAnsweredWhenNoAnswerAccepted() {
        Question q = new Question("Title", "Content", "alice", List.of("Exams"));
        assertFalse(q.isAnswered());
    }

    @Test
    public void testIsAnsweredWhenAnswerAccepted() {
        Question q = new Question("Title", "Content", "alice", List.of("Exams"));
        q.setAcceptedAnswerId(101);
        assertTrue(q.isAnswered());
    }
}
