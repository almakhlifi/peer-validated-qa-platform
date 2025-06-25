package application;
import databasePart1.DatabaseHelper;
import application.Message;
import org.junit.jupiter.api.*;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InternalAlertViewerTest {

    private DatabaseHelper db;

    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        db.connectToDatabase();
    }

    @Test
    public void testInternalAlertAppears() throws Exception {
        db.sendMessage("staff", "staff1", -1, -1, "Alert content", "staff_alert");
        List<Message> alerts = db.getInternalStaffAlerts("staff1");

        assertFalse(alerts.isEmpty());
        assertEquals("Alert content", alerts.get(0).getContent());
    }
}
