package application;
import application.InstructorHomePage;
import application.User;
import databasePart1.DatabaseHelper;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class InstructorHomePageTest {

    private DatabaseHelper db;
    private InstructorHomePage homePage;
    private User testInstructor;

    @BeforeAll
    public static void initToolkit() {
        new JFXPanel(); // Initializes JavaFX runtime
    }

    private <T> T findNodeByText(Pane root, Class<T> type, String text) {
        for (var node : root.getChildrenUnmodifiable()) {
            if (type.isInstance(node)) {
                if (node instanceof Label l && l.getText().contains(text)) return type.cast(l);
                if (node instanceof Button b && b.getText().equals(text)) return type.cast(b);
            }
            if (node instanceof Pane pane) {
                T found = findNodeByText(pane, type, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void runFXTest(FXTest test) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                test.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX thread did not complete in time");
    }

    @BeforeEach
    public void setupTest() {
        db = new DatabaseHelper();
        homePage = new InstructorHomePage();
        testInstructor = new User("test_instructor", "password", null);
    }

    @Test
    public void testMetricsDisplay() throws Exception {
        runFXTest(() -> {
            Stage stage = new Stage();
            homePage.show(stage, db, testInstructor);
            Scene scene = stage.getScene();

            Label title = findNodeByText((Pane) scene.getRoot(), Label.class, "Instructor Home");
            Label questionCount = findNodeByText((Pane) scene.getRoot(), Label.class, "Total questions");

            assertNotNull(title);
            assertNotNull(questionCount);
        });
    }

    @Test
    public void testQuestionSystemButtonExists() throws Exception {
        runFXTest(() -> {
            Stage stage = new Stage();
            homePage.show(stage, db, testInstructor);
            Button btn = findNodeByText((Pane) stage.getScene().getRoot(), Button.class, "Question System");
            assertNotNull(btn);
        });
    }

    @Test
    public void testReviewContentButtonExists() throws Exception {
        runFXTest(() -> {
            Stage stage = new Stage();
            homePage.show(stage, db, testInstructor);
            Button btn = findNodeByText((Pane) stage.getScene().getRoot(), Button.class, "Review Content");
            assertNotNull(btn);
        });
    }

    @Test
    public void testFlaggedItemsAlert() throws Exception {
        runFXTest(() -> {
            Stage stage = new Stage();
            homePage.show(stage, db, testInstructor);
            Button btn = findNodeByText((Pane) stage.getScene().getRoot(), Button.class, "Flagged Items");
            assertNotNull(btn);
            btn.fire();
        });
    }

    @Test
    public void testPendingReviewsAlert() throws Exception {
        runFXTest(() -> {
            Stage stage = new Stage();
            homePage.show(stage, db, testInstructor);
            Button btn = findNodeByText((Pane) stage.getScene().getRoot(), Button.class, "Pending Reviews");
            assertNotNull(btn);
            btn.fire();
        });
    }

    @FunctionalInterface
    interface FXTest {
        void run();
    }
}
