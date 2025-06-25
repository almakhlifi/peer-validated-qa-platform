package application;

import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DatabaseHelperTest {

    private DatabaseHelper db;

    @BeforeEach
    public void setup() throws SQLException {
        db = new DatabaseHelper();
        db.clearAllFlags();
    }

    @Test
    public void testInsertAndCountUnresolved() {
        Flag flag = new Flag(Flag.FlagType.MESSAGE, "301", "user64", "flag this", LocalDateTime.now());
        DatabaseHelper.insertFlag(flag);

        int count = db.getUnresolvedFlagCountByUser("user64");
        assertEquals(1, count);
    }

    @Test
    public void testMarkFlagAsResolved() {
        Flag flag = new Flag(Flag.FlagType.MESSAGE, "302", "user64", "review later", LocalDateTime.now());
        DatabaseHelper.insertFlag(flag);

        db.markFlagAsResolved("302", Flag.FlagType.MESSAGE);
        assertTrue(db.isFlagResolved("302", Flag.FlagType.MESSAGE));

        assertTrue(db.isFlagResolved("302", Flag.FlagType.MESSAGE));
    }
}