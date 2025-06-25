package application;

import databasePart1.DatabaseHelper;
import java.util.List;
import java.sql.SQLException;

/**
 * A utility class that manages all flags created in the system.
 * Stores and retrieves flags using the database instead of memory.
 */
public class FlagSystem {

    /**
     * Adds a new flag to the system by saving it to the database.
     *
     * @param flag the flag object to be added
     */
    public static void addFlag(Flag flag) {
        DatabaseHelper.insertFlag(flag);
    }

    /**
     * Retrieves all flags from the database.
     *
     * @return a list of all Flag objects
     */
    public static List<Flag> getAllFlags() {
        DatabaseHelper db = new DatabaseHelper();
        return db.getAllFlags();
    }

    /**
     * Retrieves all flags of a specific type (e.g., QUESTION, ANSWER, FEEDBACK).
     *
     * @param type the type of flag to filter by
     * @return a list of Flag objects of the specified type
     */
    public static List<Flag> getFlagsByType(Flag.FlagType type) {
        return getAllFlags().stream()
                .filter(flag -> flag.getType() == type)
                .toList();
    }

    /**
     * Retrieves all flags associated with a specific item ID.
     *
     * @param itemId the ID of the item to find flags for
     * @return a list of Flag objects that match the given item ID
     */
    public static List<Flag> getFlagsByItemId(String itemId) {
        return getAllFlags().stream()
                .filter(flag -> flag.getItemId().equals(itemId))
                .toList();
    }
    
    /**
     * Checks whether a specific item has already been flagged.
     *
     * @param itemId the ID of the item to check (e.g., question ID, answer ID, or message ID)
     * @param type the type of the item (QUESTION, ANSWER, or MESSAGE)
     * @return true if the item has already been flagged, false otherwise
     */
    public static boolean isFlagged(String itemId, Flag.FlagType type) {
        return getAllFlags().stream()
                .anyMatch(flag -> flag.getItemId().equals(itemId) && flag.getType() == type);
    }
    
    /**
     * Clears all flags from the database. (For testing/debugging only)
     */
    public static void clearAllFlags() throws SQLException {
        DatabaseHelper db = new DatabaseHelper();
        db.clearAllFlags();
    }
}