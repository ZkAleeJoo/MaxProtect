package org.zkaleejoo.protection.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zkaleejoo.protection.storage.ProtectionStorage.StoredMember;
import org.zkaleejoo.protection.storage.ProtectionStorage.StoredProtection;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SqliteProtectionStorageTest {

    @TempDir
    Path directory;

    @Test
    void savesOnlyChangedRegionsAndRemovesObsoleteFlagsAndMembers() throws Exception {
        StoredProtection kept = region("kept", "first", true);
        StoredProtection original = region("updated", "first", true);
        StoredProtection updated = region("updated", "second", false);
        try (var storage = openStorage()) {
            storage.replaceProtections(List.of(kept, original, region("removed", "first", true)));
        }
        try (var storage = openStorage()) {
            assertEquals(3, storage.loadProtections().size());
            execute("CREATE TRIGGER keep_existing BEFORE DELETE ON protections WHEN OLD.region_id = 'kept' "
                    + "BEGIN SELECT RAISE(ABORT, 'unchanged region was rewritten'); END");
            assertTrue(storage.replaceProtections(List.of(kept, updated)));
            assertEquals(List.of(kept, updated), storage.loadProtections());
            try (var connection = DriverManager.getConnection(url());
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(rows.next());
            }
        }
    }

    @Test
    void rollsBackFailedTransactionAndCanRetryIt() throws Exception {
        StoredProtection original = region("home", "before", true);
        StoredProtection updated = region("home", "after", false);
        try (var storage = openStorage()) {
            storage.replaceProtections(List.of(original));
            execute("CREATE TRIGGER reject_save BEFORE INSERT ON protections "
                    + "BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END");
            assertThrows(IllegalStateException.class, () -> storage.replaceProtections(List.of(updated)));
            try (var reader = openStorage()) {
                assertEquals(List.of(original), reader.loadProtections());
            }
            execute("DROP TRIGGER reject_save");
            assertTrue(storage.replaceProtections(List.of(updated)));
            assertEquals(List.of(updated), storage.loadProtections());
        }
    }

    @Test
    void retainsLastQueuedSnapshotAfterShutdownAndReopen() {
        StoredProtection latest = region("home", "latest", false);
        try (var storage = openStorage()) {
            for (int index = 0; index < 24; index++) {
                storage.replaceProtectionsAsync(List.of(region("home", Integer.toString(index), true)));
            }
            storage.replaceProtectionsAsync(List.of(latest));
        }
        try (var reader = openStorage()) {
            assertEquals(List.of(latest), reader.loadProtections());
        }
    }

    private SqliteProtectionStorage openStorage() {
        var storage = new SqliteProtectionStorage(directory.resolve("protections.db"), Logger.getAnonymousLogger());
        storage.initialize();
        return storage;
    }

    private String url() {
        return "jdbc:sqlite:" + directory.resolve("protections.db");
    }

    private void execute(String sql) throws Exception {
        try (var connection = DriverManager.getConnection(url()); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private StoredProtection region(String id, String alias, boolean withMember) {
        return new StoredProtection(id, "stone", alias, "owner", "Owner", "world",
                0, 64, 0, -4, 0, -4, 4, 255, 4, 1, "", "", false, 0, 0, 0, 0, 0,
                withMember ? Map.of("storage", "MEMBERS") : Map.of(),
                withMember ? Map.of("member", new StoredMember("member", "Member", "MEMBER")) : Map.of(),
                0, false);
    }
}
