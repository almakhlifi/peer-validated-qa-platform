package application;

import application.User;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class StaffHomePageTest {

    private DatabaseHelper db;

    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.clearAllFlags();
    }

    @Test
    public void testUnresolvedFlagCount() throws Exception {
        db.insertFlag(new application.Flag(application.Flag.FlagType.QUESTION, "321", "staff1", "Wrong info", java.time.LocalDateTime.now()));
        int count = db.getUnresolvedFlagCountByUser("staff1");
        assertEquals(1, count);
    }
}
