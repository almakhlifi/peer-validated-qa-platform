package application;

import databasePart1.DatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionSystem {
    private final List<Question> questions = new ArrayList<>();
    DatabaseHelper databaseHelper = new DatabaseHelper();

    public Question createQuestion(String title, String content, String author, List<String> tags) {
        Question question = new Question(title, content, author, tags);
        try {
            databaseHelper.saveQuestion(question); // Save to database
        } catch (SQLException e) {
            e.printStackTrace();
        }
        questions.add(question);
        return question;
    }

    public List<Question> loadQuestions() throws SQLException {
        List<Question> loadedQuestions = databaseHelper.loadQuestions();
        questions.clear();
        questions.addAll(loadedQuestions);
        return new ArrayList<>(loadedQuestions);
    }

    // Update question
    public boolean updateQuestion(int questionId, String newTitle, String newContent) {
        boolean success = databaseHelper.updateQuestion(questionId, newTitle, newContent);
        if (success) {
            questions.stream()
                    .filter(q -> q.getId() == questionId)
                    .findFirst()
                    .ifPresent(q -> {
                        q.setTitle(newTitle);
                        q.setContent(newContent);
                    });
        }
        return success;
    }
    
    // method for updating answers 
    
    public boolean updateAnswer(int answerId, String newContent) throws SQLException {
        return databaseHelper.updateAnswer(answerId, newContent);
    }



    public boolean deleteQuestion(int questionId, String author) {
        try {
            boolean success = databaseHelper.deleteQuestion(questionId);
            if (success) {
                questions.removeIf(q -> q.getId() == questionId && q.getAuthor().equals(author));
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Filter questions by tag(s)
    public List<Question> filterQuestionsByTag(String tag) {
        try {
            return databaseHelper.getQuestionsByTag(tag); // Query DB for filtered results
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }


    public Answer createAnswer(String content, String author, int questionId) {
        Answer answer = new Answer(content, author, questionId);

        try {
            databaseHelper.saveAnswer(answer);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Question question = questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);

        if (question != null) {
            question.addAnswer(answer);
        }

        return answer;
    }

    public List<Answer> getAllAnswersForQuestion(int questionId) { // New method to get answers for a specific question
        return questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .map(Question::getAnswers)
                .orElse(new ArrayList<>());
    }

    // New method to create a threaded answer (either top-level or reply)
    public Answer createThreadedAnswer(String content, String author, int questionId, Integer parentAnswerId) {
        // Generate a new unique ID for this answer
        int newAnswerId = generateUniqueAnswerId();
        
        Answer answer = new Answer(newAnswerId, content, author, questionId, parentAnswerId);
        try {
            databaseHelper.saveThreadedAnswer(answer);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        Question question = questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);

        if (question != null) {
            if (parentAnswerId == null) {
                question.addAnswer(answer);
            } else {
                Answer parentAnswer = findAnswerById(question, parentAnswerId);
                if (parentAnswer != null) {
                    parentAnswer.addReply(answer);
                }
            }
        }
        return answer;
    }
    
    
    public List<Answer> loadThreadedAnswersForQuestion(int questionId) {
        try {
            return databaseHelper.loadThreadedAnswersForQuestion(questionId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private int generateUniqueAnswerId() {
        // Implement a method to generate a unique ID for each answer
        // This could be based on the current timestamp, a database sequence, or any other method
        // For simplicity, let's use the current timestamp in milliseconds
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
    
    private Answer findAnswerById(Question question, int answerId) {
        return findAnswerRecursive(question.getAnswers(), answerId);
    }

    
    private Answer findAnswerRecursive(List<Answer> answers, int answerId) {
        for (Answer answer : answers) {
            if (answer.getId() == answerId) {
                return answer;
            }
            Answer found = findAnswerRecursive(answer.getReplies(), answerId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public Answer replyToAnswer(String content, String author, int questionId, int answerId) {
        Answer reply = new Answer(content, author, questionId);

        try {
            databaseHelper.saveAnswer(reply); // Save reply to database
        } catch (SQLException e) {
            e.printStackTrace();
        }

        questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .ifPresent(question -> {
                    question.getAnswers().stream()
                            .filter(a -> a.getId() == answerId)
                            .findFirst()
                            .ifPresent(answer -> answer.addReply(reply));
                });

        return reply;
    }

    @SuppressWarnings("unchecked")
    public List<Answer> getRepliesForAnswer(int questionId, int answerId) {
        return (List<Answer>) questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .flatMap(question -> question.getAnswers().stream()
                        .filter(a -> a.getId() == answerId)
                        .findFirst())
                .map(Answer::getReplies)
                .orElse(new ArrayList<>());
    }


}