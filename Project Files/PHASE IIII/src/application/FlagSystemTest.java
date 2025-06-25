package application;

import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FlagSystemTest {

    @BeforeEach
    public void clearFlagsBeforeTest() throws SQLException {
        FlagSystem.clearAllFlags();
    }

    @Test
    public void testAddAndRetrieveFlag() {
        Flag flag = new Flag(Flag.FlagType.QUESTION, "101", "staff", "Needs review", LocalDateTime.now());
        FlagSystem.addFlag(flag);

        List<Flag> result = FlagSystem.getFlagsByItemId("101");
        assertEquals(1, result.size());
        assertEquals("staff", result.get(0).getFlaggedBy());
    }

    @Test
    public void testFilterByType() {
        FlagSystem.addFlag(new Flag(Flag.FlagType.ANSWER, "202", "staff", "bad answer", LocalDateTime.now()));
        FlagSystem.addFlag(new Flag(Flag.FlagType.QUESTION, "203", "staff", "off-topic", LocalDateTime.now()));

        List<Flag> questionFlags = FlagSystem.getFlagsByType(Flag.FlagType.QUESTION);
        assertEquals(1, questionFlags.size());
        assertEquals("203", questionFlags.get(0).getItemId());
    }
}