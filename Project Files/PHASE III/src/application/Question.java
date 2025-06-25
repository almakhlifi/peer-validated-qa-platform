package application;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a question posted by a user in the application.
 */
public class Question {
    private static int nextId = 1;
    private int id; // ID is modifiable
    private String title;
    private String content;
    private final String author;
    private List<String> tags;
    private List<Answer> answers;
    private Integer acceptedAnswerId; // Tracks accepted answer

    /**
     * Constructs a new Question object for a new question being created.
     * The ID will be automatically assigned.
     * @param title The title of the question.
     * @param content The detailed content of the question.
     * @param author The username of the user who posted the question.
     * @param tags A list of tags associated with the question.
     */
    public Question(String title, String content, String author, List<String> tags) {
        this.id = nextId++;
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new ArrayList<>(tags);
        this.answers = new ArrayList<>();
        this.acceptedAnswerId = null; // Default to null
    }

    /**
     * Constructs a new Question object for a question loaded from the database.
     * @param id The unique identifier of the question.
     * @param title The title of the question.
     * @param content The detailed content of the question.
     * @param author The username of the user who posted the question.
     * @param tags A list of tags associated with the question.
     * @param acceptedAnswerId The ID of the accepted answer for this question, or null if none.
     */
    public Question(int id, String title, String content, String author, List<String> tags, Integer acceptedAnswerId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new ArrayList<>(tags);
        this.answers = new ArrayList<>();
        this.acceptedAnswerId = acceptedAnswerId;
    }

    /**
     * Constructs a new Question object for a question loaded from the database
     * without a specified accepted answer.
     * @param id The unique identifier of the question.
     * @param title The title of the question.
     * @param content The detailed content of the question.
     * @param author The username of the user who posted the question.
     * @param tags A list of tags associated with the question.
     */
    public Question(int id, String title, String content, String author, List<String> tags) {
        this(id, title, content, author, tags, null); // Calls the full constructor, setting acceptedAnswerId to null
    }

    /**
     * Returns the unique identifier of the question.
     * @return The question ID.
     */
    public int getId() { return id; }

    /**
     * Sets the unique identifier of the question.
     * @param id The new question ID.
     */
    public void setId(int id) { this.id = id; }

    /**
     * Returns the title of the question.
     * @return The question title.
     */
    public String getTitle() { return title; }

    /**
     * Returns the detailed content of the question.
     * @return The question content.
     */
    public String getContent() { return content; }

    /**
     * Returns the username of the author who posted the question.
     * @return The author's username.
     */
    public String getAuthor() { return author; }

    /**
     * Returns a list of tags associated with the question.
     * @return A new list containing the question's tags.
     */
    public List<String> getTags() { return new ArrayList<>(tags); }

    /**
     * Returns a list of answers posted for this question.
     * @return The list of answers.
     */
    public List<Answer> getAnswers() { return answers; }

    /**
     * Sets the title of the question.
     * @param title The new question title.
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Sets the detailed content of the question.
     * @param content The new question content.
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Adds a new answer to the question's list of answers.
     * Ignores duplicate answers.
     * @param answer The answer to add.
     */
    public void addAnswer(Answer answer) {
    	  if (!answers.contains(answer)) {  // prevents duplicate answers
              answers.add(answer); 
          }
      }

      public Integer getAcceptedAnswerId() { return acceptedAnswerId; }
      public void setAcceptedAnswerId(Integer acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }
  }