package application;

import java.util.ArrayList;
import java.util.List;

public class Answer {
    private static int nextId = 1;
    private int id;
    private String content;
    private final String author;
    private final int questionId; // Link to Question
    private Integer parentAnswerId; // Null if it's a top-level answer
    private List<Answer> replies; // List of replies

    // Constructor for new top-level answers
    public Answer(String content, String author, int questionId) {
        this.id = nextId++;
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = null;
        this.replies = new ArrayList<>();
    }

    // Constructor for new replies
    public Answer(String content, String author, int questionId, int parentAnswerId) {
        this.id = nextId++;
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = parentAnswerId;
        this.replies = new ArrayList<>();
    }

    // Constructor for answers loaded from database
    public Answer(int id, String content, String author, int questionId, Integer parentAnswerId) {
        this.id = id; // Assign database ID
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = parentAnswerId;
        this.replies = new ArrayList<>();
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() { return this.id; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public int getQuestionId() { return questionId; }
    public Integer getParentAnswerId() { return parentAnswerId; }
    public List<Answer> getReplies() { return replies; }

    public void setContent(String content) { this.content = content; }

    // Add a reply to this answer
    public void addReply(Answer reply) {
        if (reply != null) {
            replies.add(reply);
        }
    }
}
