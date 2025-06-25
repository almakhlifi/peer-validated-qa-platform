/**
 * Start
 * 
 * Main entry point for the application. Handles launching the appropriate initial page based on the database state.
 */
package application;

import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.SQLException;

import databasePart1.DatabaseHelper;

public class Start extends Application {

    /**
     * Singleton instance of the DatabaseHelper used throughout the application.
     */
    private static final DatabaseHelper databaseHelper = new DatabaseHelper();

    /**
     * Main method to launch the application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes the application by determining whether the database is empty and showing the appropriate initial page.
     *
     * @param primaryStage The primary stage for the application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Attempt to connect to the database
            databaseHelper.connectToDatabase();

            // Check if the database is empty
            if (databaseHelper.isDatabaseEmpty()) {
                // Show the FirstPage if the database is empty
                new FirstPage(databaseHelper).show(primaryStage);
            } else {
                // Otherwise, show the SetupLoginSelectionPage
                new SetupLoginSelectionPage(databaseHelper).show(primaryStage);
            }
        } catch (SQLException e) {
            // Print the exception message if database connection fails
            System.out.println(e.getMessage());
        }
    }
}