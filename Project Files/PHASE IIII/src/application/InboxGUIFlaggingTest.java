package application;
import application.Flag;
import application.FlagSystem;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class InboxGUIFlaggingTest {

    private DatabaseHelper db;

    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        FlagSystem.clearAllFlags();
    }

    @Test
    public void testFlagMessageAppears() {
        Flag flag = new Flag(Flag.FlagType.MESSAGE, "321", "staff", "Spam content", LocalDateTime.now());
        db.insertFlag(flag);

        assertTrue(FlagSystem.isFlagged("321", Flag.FlagType.MESSAGE));
    }
}
