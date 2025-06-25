package application;
import application.Flag;
import application.FlagSystem;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class FlaggedContentViewerTest {

    private DatabaseHelper db;

    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        FlagSystem.clearAllFlags();
    }

    @Test
    public void testMarkFlagAsResolved() {
        Flag flag = new Flag(Flag.FlagType.QUESTION, "999", "staff", "Incorrect data", LocalDateTime.now());
        db.insertFlag(flag);

        assertFalse(db.isFlagResolved("999", Flag.FlagType.QUESTION));
        db.markFlagAsResolved("999", Flag.FlagType.QUESTION);
        assertTrue(db.isFlagResolved("999", Flag.FlagType.QUESTION));
    }
}
