package application;

import java.util.ArrayList;
import java.util.List;

public class Question { 
    private static int nextId = 1;
    private int id; // ID is modifiable
    private String title;
    private String content;
    private final String author;
    private List<String> tags;
    private List<Answer> answers; 
    private Integer acceptedAnswerId; // Tracks accepted answer

    // Constructor for new questions
    public Question(String title, String content, String author, List<String> tags) { 
        this.id = nextId++;
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new ArrayList<>(tags);
        this.answers = new ArrayList<>();
        this.acceptedAnswerId = null; // Default to null
    }

    // Constructor for database-loaded questions
    public Question(int id, String title, String content, String author, List<String> tags, Integer acceptedAnswerId) { 
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new ArrayList<>(tags);
        this.answers = new ArrayList<>();
        this.acceptedAnswerId = acceptedAnswerId;
    }
    
    // Constructor for database-loaded questions (without acceptedAnswerId)
    public Question(int id, String title, String content, String author, List<String> tags) { 
        this(id, title, content, author, tags, null); // Calls the full constructor, setting acceptedAnswerId to null
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public List<Answer> getAnswers() { return answers; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    
    public void addAnswer(Answer answer) { 
        if (!answers.contains(answer)) {  // prevents duplicate answers
            answers.add(answer); 
        }
    }

    public Integer getAcceptedAnswerId() { return acceptedAnswerId; }
    public void setAcceptedAnswerId(Integer acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }
}