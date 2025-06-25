package application;

import application.*;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the inbox feedback functionality.
 */
public class InboxFeedbackTest {

    private QuestionSystem questionSystem;
    private FakeDatabaseHelper fakeDb;

    /**
     * Sets up the test environment before each test method.
     */
    @BeforeEach
    public void setup() {
        fakeDb = new FakeDatabaseHelper();
        questionSystem = new QuestionSystem();
        questionSystem.databaseHelper = fakeDb;
    }

    /**
     * Tests the scenario where feedback is sent from a teacher to a student.
     */
    @Test
    public void testSendingFeedbackToStudent() {
        questionSystem.sendMessage("alice", "bob", 42, -1, "Nice job!", "review-feedback");

        assertEquals(1, fakeDb.sentMessages.size());
        Message msg = fakeDb.sentMessages.get(0);
        assertEquals("alice", msg.getSender());
        assertEquals("bob", msg.getRecipient());
        assertEquals("Nice job!", msg.getContent());
        assertEquals("review-feedback", msg.getMessageType());
    }

    /**
     * Tests that unread messages are correctly identified and can display an unread icon.
     * @throws Exception if an error occurs during the test.
     */
    @Test
    public void testUnreadMessagesDisplayUnreadIcon() throws Exception {
        Message unread = new Message(1, "bob", "alice", 10, -1, "New note",
                new Timestamp(System.currentTimeMillis()), false, "question");
        Message read = new Message(2, "bob", "alice", 10, -1, "Old note",
                new Timestamp(System.currentTimeMillis()), true, "question");

        fakeDb.fakeMessages.addAll(List.of(unread, read));

        List<Message> msgs = fakeDb.getAllMessagesForUser("alice");
        assertFalse(msgs.get(0).isRead());
        assertTrue(msgs.get(1).isRead());
    }

    /**
     * Tests that a student can reply to feedback received from a teacher.
     */
    @Test
    public void testStudentCanReplyToFeedback() {
        questionSystem.sendMessage("bob", "alice", 10, 5, "Thanks!", "review-feedback");

        assertEquals(1, fakeDb.sentMessages.size());
        Message msg = fakeDb.sentMessages.get(0);
        assertEquals("bob", msg.getSender());
        assertEquals("alice", msg.getRecipient());
        assertEquals("Thanks!", msg.getContent());
        assertEquals("review-feedback", msg.getMessageType());
    }

    /**
     * Tests that new feedback results in an unread notification for the recipient.
     */
    @Test
    public void testNewFeedbackResultsInUnreadNotification() {
        fakeDb.unreadCountMap.put("alice::10", 3);
        int unread = questionSystem.getUnreadMessageCount("alice", 10, -1);
        assertEquals(3, unread);
    }

    /**
     * Tests that multiple feedback messages related to the same question create a single thread.
     */
    @Test
    public void testMultipleFeedbacksCreateSingleThread() {
        String sender = "alice";
        String recipient = "bob";
        int questionId = 10;

        Message msg1 = new Message(1, sender, recipient, questionId, -1, "Hi",
                new Timestamp(System.currentTimeMillis()), false, "question");
        Message msg2 = new Message(2, recipient, sender, questionId, -1, "Hello",
                new Timestamp(System.currentTimeMillis()), false, "question");

        fakeDb.fakeMessages.addAll(List.of(msg1, msg2));

        Map<String, List<Message>> grouped = new HashMap<>();
        String threadKey = Stream.of(sender, recipient).sorted().collect(Collectors.joining("_")) + "::" + questionId;
        grouped.computeIfAbsent(threadKey, k -> new ArrayList<>()).addAll(List.of(msg1, msg2));

        assertEquals(1, grouped.size());
        assertEquals(2, grouped.get(threadKey).size());
    }

    /**
     * A fake implementation of the DatabaseHelper for testing purposes.
     */
    static class FakeDatabaseHelper extends DatabaseHelper {
        List<Message> sentMessages = new ArrayList<>();
        List<Message> fakeMessages = new ArrayList<>();
        Map<String, Integer> unreadCountMap = new HashMap<>();

        /**
         * Simulates sending a message and stores it in the sentMessages list.
         * @param sender The username of the sender.
         * @param recipient The username of the recipient.
         * @param questionId The ID of the question related to the message.
         * @param answerId The ID of the answer related to the message.
         * @param content The content of the message.
         * @param messageType The type of the message.
         */
        @Override
        public void sendMessage(String sender, String recipient, int questionId, int answerId, String content, String messageType) {
            sentMessages.add(new Message(-1, sender, recipient, questionId, answerId, content, new Timestamp(System.currentTimeMillis()), false, messageType));
        }

        /**
         * Simulates retrieving all messages for a given user.
         * @param username The username of the user.
         * @return A list of messages for the user.
         */
        @Override
        public List<Message> getAllMessagesForUser(String username) {
            return fakeMessages;
        }

        /**
         * Simulates getting the count of unread messages for a specific user and question.
         * @param recipient The username of the recipient.
         * @param questionId The ID of the question.
         * @param answerId The ID of the answer (not used in this fake implementation).
         * @return The count of unread messages.
         */
        @Override
        public int getUnreadMessageCount(String recipient, int questionId, int answerId) {
            return unreadCountMap.getOrDefault(recipient + "::" + questionId, 0);
        }

        /**
         * Simulates retrieving a question by its ID.
         * @param id The ID of the question.
         * @return The Question object.
         */
        @Override
        public Question getQuestionById(int id) {
            return new Question("Test Question", "Body", "alice", List.of());
        }
    }
}