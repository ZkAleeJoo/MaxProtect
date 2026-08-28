package org.zkaleejoo.protection.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class SqliteProtectionStorage implements ProtectionStorage {

    private final Path databasePath;
    private final Logger logger;
    private HikariDataSource dataSource;
    private StorageExecutor executor;

    public SqliteProtectionStorage(Path databasePath, Logger logger) {
        this.databasePath = databasePath;
        this.logger = logger;
    }

    @Override
    public void initialize() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
            config.setMaximumPoolSize(3);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(300000);
            config.setPoolName("ArgosProtect-SQLite");
            config.setConnectionInitSql("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;");
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");

            dataSource = new HikariDataSource(config);
            executor = new StorageExecutor(logger);

            try (Connection connection = dataSource.getConnection()) {
                createSchema(connection);
            }

            logger.info("Database connection pool initialized (HikariCP, pool=3, WAL mode).");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize protection database", exception);
        }
    }

    @Override
    public List<ProtectionInvite> findPendingInvitesForPlayer(String invitedUuid, long nowMillis) {
        return doFindPendingInvitesForPlayer(invitedUuid, nowMillis);
    }

    @Override
    public List<StoredProtection> loadProtections() {
        return doLoadProtections();
    }

    @Override
    public boolean replaceProtections(List<StoredProtection> protections) {
        return doReplaceProtections(protections);
    }

    @Override
    public long createInvite(ProtectionInvite invite) {
        return doCreateInvite(invite);
    }

    @Override
    public Optional<ProtectionInvite> findInvite(long inviteId) {
        return doFindInvite(inviteId);
    }

    @Override
    public Optional<ProtectionInvite> findPendingInvite(String regionId, String invitedUuid, long nowMillis) {
        return doFindPendingInvite(regionId, invitedUuid, nowMillis);
    }

    @Override
    public boolean deleteInvite(long inviteId) {
        return doDeleteInvite(inviteId);
    }

    @Override
    public void recordEvent(ProtectionEvent event) {
        doRecordEvent(event);
    }

    @Override
    public List<ProtectionEventLog> recentEvents(String regionId, int limit) {
        return doRecentEvents(regionId, limit);
    }

    @Override
    public ReportSnapshot reportSnapshot() {
        return doReportSnapshot();
    }

    @Override
    public CompletableFuture<List<StoredProtection>> loadProtectionsAsync() {
        return executor.supplyAsync(this::doLoadProtections);
    }

    @Override
    public CompletableFuture<Boolean> replaceProtectionsAsync(List<StoredProtection> protections) {
        return executor.supplyAsync(() -> doReplaceProtections(protections));
    }

    @Override
    public CompletableFuture<Long> createInviteAsync(ProtectionInvite invite) {
        return executor.supplyAsync(() -> doCreateInvite(invite));
    }

    @Override
    public CompletableFuture<List<ProtectionInvite>> findPendingInvitesForPlayerAsync(String invitedUuid,
            long nowMillis) {
        return executor.supplyAsync(() -> doFindPendingInvitesForPlayer(invitedUuid, nowMillis));
    }

    @Override
    public CompletableFuture<Optional<ProtectionInvite>> findInviteAsync(long inviteId) {
        return executor.supplyAsync(() -> doFindInvite(inviteId));
    }

    @Override
    public CompletableFuture<Optional<ProtectionInvite>> findPendingInviteAsync(String regionId, String invitedUuid,
            long nowMillis) {
        return executor.supplyAsync(() -> doFindPendingInvite(regionId, invitedUuid, nowMillis));
    }

    @Override
    public CompletableFuture<Boolean> deleteInviteAsync(long inviteId) {
        return executor.supplyAsync(() -> doDeleteInvite(inviteId));
    }

    @Override
    public CompletableFuture<Void> recordEventAsync(ProtectionEvent event) {
        return executor.runAsync(() -> doRecordEvent(event));
    }

    @Override
    public CompletableFuture<List<ProtectionEventLog>> recentEventsAsync(String regionId, int limit) {
        return executor.supplyAsync(() -> doRecentEvents(regionId, limit));
    }

    @Override
    public CompletableFuture<ReportSnapshot> reportSnapshotAsync() {
        return executor.supplyAsync(this::doReportSnapshot);
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }

    private List<ProtectionInvite> doFindPendingInvitesForPlayer(String invitedUuid, long nowMillis) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                                SELECT id, region_id, inviter_uuid, inviter_name, invited_uuid, invited_name, created_at, expires_at
                                FROM member_invites
                                WHERE invited_uuid = ? AND expires_at > ?
                                ORDER BY id DESC
                                """)) {
            statement.setString(1, invitedUuid);
            statement.setLong(2, nowMillis);
            List<ProtectionInvite> invites = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invites.add(readInvite(resultSet));
                }
            }
            return invites;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read pending protection invites for player", exception);
        }
    }

    private List<StoredProtection> doLoadProtections() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, StoredProtectionBuilder> builders = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT region_id, protection_id, alias, owner_uuid, owner_name, world_name,
                           stone_x, stone_y, stone_z, min_x, min_y, min_z, max_x, max_y, max_z,
                           created_at, actionbar_enter, actionbar_exit, custom_home,
                           home_x, home_y, home_z, home_yaw, home_pitch,
                           rent_paid_until, rent_suspended
                    FROM protections
                    ORDER BY region_id
                    """);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StoredProtectionBuilder builder = new StoredProtectionBuilder(
                            resultSet.getString("region_id"),
                            resultSet.getString("protection_id"),
                            resultSet.getString("alias"),
                            resultSet.getString("owner_uuid"),
                            resultSet.getString("owner_name"),
                            resultSet.getString("world_name"),
                            resultSet.getInt("stone_x"),
                            resultSet.getInt("stone_y"),
                            resultSet.getInt("stone_z"),
                            resultSet.getInt("min_x"),
                            resultSet.getInt("min_y"),
                            resultSet.getInt("min_z"),
                            resultSet.getInt("max_x"),
                            resultSet.getInt("max_y"),
                            resultSet.getInt("max_z"),
                            resultSet.getLong("created_at"),
                            resultSet.getString("actionbar_enter"),
                            resultSet.getString("actionbar_exit"),
                            resultSet.getInt("custom_home") == 1,
                            resultSet.getDouble("home_x"),
                            resultSet.getDouble("home_y"),
                            resultSet.getDouble("home_z"),
                            resultSet.getFloat("home_yaw"),
                            resultSet.getFloat("home_pitch"),
                            resultSet.getLong("rent_paid_until"),
                            resultSet.getInt("rent_suspended") == 1);
                    builders.put(builder.regionId, builder);
                }
            }

            loadFlags(connection, builders);
            loadMembers(connection, builders);
            return builders.values().stream().map(b -> b.build()).toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load protection database", exception);
        }
    }

    private boolean doReplaceProtections(List<StoredProtection> protections) {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                execute(connection, "DELETE FROM protection_flags");
                execute(connection, "DELETE FROM protection_members");
                execute(connection, "DELETE FROM protections");
                for (StoredProtection protection : protections) {
                    insertProtection(connection, protection);
                    insertFlags(connection, protection);
                    insertMembers(connection, protection);
                }
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save protection database", exception);
        }
    }

    private long doCreateInvite(ProtectionInvite invite) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO member_invites(region_id, inviter_uuid, inviter_name, invited_uuid, invited_name,
                                                   created_at, expires_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, invite.regionId());
            statement.setString(2, invite.inviterUuid());
            statement.setString(3, invite.inviterName());
            statement.setString(4, invite.invitedUuid());
            statement.setString(5, invite.invitedName());
            statement.setLong(6, invite.createdAtMillis());
            statement.setLong(7, invite.expiresAtMillis());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create protection invite", exception);
        }
    }

    private Optional<ProtectionInvite> doFindInvite(long inviteId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                                SELECT id, region_id, inviter_uuid, inviter_name, invited_uuid, invited_name, created_at, expires_at
                                FROM member_invites
                                WHERE id = ?
                                """)) {
            statement.setLong(1, inviteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readInvite(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read protection invite", exception);
        }
    }

    private Optional<ProtectionInvite> doFindPendingInvite(String regionId, String invitedUuid, long nowMillis) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        """
                                SELECT id, region_id, inviter_uuid, inviter_name, invited_uuid, invited_name, created_at, expires_at
                                FROM member_invites
                                WHERE region_id = ? AND invited_uuid = ? AND expires_at > ?
                                ORDER BY id DESC
                                LIMIT 1
                                """)) {
            statement.setString(1, regionId);
            statement.setString(2, invitedUuid);
            statement.setLong(3, nowMillis);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readInvite(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read pending protection invite", exception);
        }
    }

    private boolean doDeleteInvite(long inviteId) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("DELETE FROM member_invites WHERE id = ?")) {
            statement.setLong(1, inviteId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete protection invite", exception);
        }
    }

    private void doRecordEvent(ProtectionEvent event) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO protection_events(region_id, actor_uuid, actor_name, target_uuid, target_name,
                                                      event_type, detail, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
            statement.setString(1, event.regionId());
            statement.setString(2, event.actorUuid());
            statement.setString(3, event.actorName());
            statement.setString(4, event.targetUuid());
            statement.setString(5, event.targetName());
            statement.setString(6, event.eventType());
            statement.setString(7, event.detail());
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not record protection event", exception);
        }
    }

    private List<ProtectionEventLog> doRecentEvents(String regionId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(50, limit));
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, region_id, actor_uuid, actor_name, target_uuid, target_name,
                               event_type, detail, created_at
                        FROM protection_events
                        WHERE region_id = ?
                        ORDER BY id DESC
                        LIMIT ?
                        """)) {
            statement.setString(1, regionId);
            statement.setInt(2, normalizedLimit);
            List<ProtectionEventLog> events = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(new ProtectionEventLog(
                            resultSet.getLong("id"),
                            resultSet.getString("region_id"),
                            resultSet.getString("actor_uuid"),
                            resultSet.getString("actor_name"),
                            resultSet.getString("target_uuid"),
                            resultSet.getString("target_name"),
                            resultSet.getString("event_type"),
                            resultSet.getString("detail"),
                            resultSet.getLong("created_at")));
                }
            }
            return events;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read protection events", exception);
        }
    }

    private ReportSnapshot doReportSnapshot() {
        try (Connection connection = dataSource.getConnection()) {
            return new ReportSnapshot(
                    count(connection, "SELECT COUNT(*) FROM protections"),
                    count(connection, "SELECT COUNT(*) FROM protection_events"),
                    topOwners(connection),
                    largestProtections(connection));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not build protection report", exception);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS protections (
                    region_id TEXT PRIMARY KEY,
                    protection_id TEXT NOT NULL,
                    alias TEXT NOT NULL DEFAULT '',
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    stone_x INTEGER NOT NULL,
                    stone_y INTEGER NOT NULL,
                    stone_z INTEGER NOT NULL,
                    min_x INTEGER NOT NULL,
                    min_y INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    max_y INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    actionbar_enter TEXT NOT NULL DEFAULT '',
                    actionbar_exit TEXT NOT NULL DEFAULT '',
                    custom_home INTEGER NOT NULL DEFAULT 0,
                    home_x REAL NOT NULL DEFAULT 0,
                    home_y REAL NOT NULL DEFAULT 0,
                    home_z REAL NOT NULL DEFAULT 0,
                    home_yaw REAL NOT NULL DEFAULT 0,
                    home_pitch REAL NOT NULL DEFAULT 0,
                    rent_paid_until INTEGER NOT NULL DEFAULT 0,
                    rent_suspended INTEGER NOT NULL DEFAULT 0
                )
                """);
        addColumnIfMissing(connection, "protections", "rent_paid_until", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "protections", "rent_suspended", "INTEGER NOT NULL DEFAULT 0");
        execute(connection, """
                CREATE TABLE IF NOT EXISTS protection_members (
                    region_id TEXT NOT NULL,
                    member_uuid TEXT NOT NULL,
                    member_name TEXT NOT NULL,
                    rank TEXT NOT NULL,
                    PRIMARY KEY (region_id, member_uuid),
                    FOREIGN KEY (region_id) REFERENCES protections(region_id) ON DELETE CASCADE
                )
                """);
        execute(connection, """
                CREATE TABLE IF NOT EXISTS protection_flags (
                    region_id TEXT NOT NULL,
                    flag_id TEXT NOT NULL,
                    level TEXT NOT NULL,
                    PRIMARY KEY (region_id, flag_id),
                    FOREIGN KEY (region_id) REFERENCES protections(region_id) ON DELETE CASCADE
                )
                """);
        execute(connection, """
                CREATE TABLE IF NOT EXISTS member_invites (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    region_id TEXT NOT NULL,
                    inviter_uuid TEXT NOT NULL,
                    inviter_name TEXT NOT NULL,
                    invited_uuid TEXT NOT NULL,
                    invited_name TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
                """);
        execute(connection, """
                CREATE TABLE IF NOT EXISTS protection_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    region_id TEXT NOT NULL,
                    actor_uuid TEXT,
                    actor_name TEXT,
                    target_uuid TEXT,
                    target_name TEXT,
                    event_type TEXT NOT NULL,
                    detail TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL
                )
                """);
    }

    private void insertProtection(Connection connection, StoredProtection protection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO protections(region_id, protection_id, alias, owner_uuid, owner_name, world_name,
                        stone_x, stone_y, stone_z, min_x, min_y, min_z, max_x, max_y, max_z, created_at,
                        actionbar_enter, actionbar_exit, custom_home, home_x, home_y, home_z, home_yaw, home_pitch,
                        rent_paid_until, rent_suspended)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, protection.regionId());
            statement.setString(2, protection.protectionId());
            statement.setString(3, protection.alias());
            statement.setString(4, protection.ownerUuid());
            statement.setString(5, protection.ownerName());
            statement.setString(6, protection.worldName());
            statement.setInt(7, protection.stoneX());
            statement.setInt(8, protection.stoneY());
            statement.setInt(9, protection.stoneZ());
            statement.setInt(10, protection.minX());
            statement.setInt(11, protection.minY());
            statement.setInt(12, protection.minZ());
            statement.setInt(13, protection.maxX());
            statement.setInt(14, protection.maxY());
            statement.setInt(15, protection.maxZ());
            statement.setLong(16, protection.createdAtMillis());
            statement.setString(17, protection.actionbarEnter());
            statement.setString(18, protection.actionbarExit());
            statement.setInt(19, protection.customHome() ? 1 : 0);
            statement.setDouble(20, protection.homeX());
            statement.setDouble(21, protection.homeY());
            statement.setDouble(22, protection.homeZ());
            statement.setFloat(23, protection.homeYaw());
            statement.setFloat(24, protection.homePitch());
            statement.setLong(25, protection.rentPaidUntilMillis());
            statement.setInt(26, protection.rentSuspended() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private void insertFlags(Connection connection, StoredProtection protection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO protection_flags(region_id, flag_id, level)
                VALUES (?, ?, ?)
                """)) {
            for (Map.Entry<String, String> flag : protection.flags().entrySet()) {
                statement.setString(1, protection.regionId());
                statement.setString(2, flag.getKey());
                statement.setString(3, flag.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertMembers(Connection connection, StoredProtection protection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO protection_members(region_id, member_uuid, member_name, rank)
                VALUES (?, ?, ?, ?)
                """)) {
            for (StoredMember member : protection.members().values()) {
                statement.setString(1, protection.regionId());
                statement.setString(2, member.uuid());
                statement.setString(3, member.name());
                statement.setString(4, member.rank());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void loadFlags(Connection connection, Map<String, StoredProtectionBuilder> builders) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT region_id, flag_id, level
                FROM protection_flags
                ORDER BY region_id, flag_id
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                StoredProtectionBuilder builder = builders.get(resultSet.getString("region_id"));
                if (builder != null) {
                    builder.flags.put(resultSet.getString("flag_id"), resultSet.getString("level"));
                }
            }
        }
    }

    private void loadMembers(Connection connection, Map<String, StoredProtectionBuilder> builders) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT region_id, member_uuid, member_name, rank
                FROM protection_members
                ORDER BY region_id, member_name
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                StoredProtectionBuilder builder = builders.get(resultSet.getString("region_id"));
                if (builder != null) {
                    StoredMember member = new StoredMember(resultSet.getString("member_uuid"),
                            resultSet.getString("member_name"), resultSet.getString("rank"));
                    builder.members.put(member.uuid(), member);
                }
            }
        }
    }

    private ProtectionInvite readInvite(ResultSet resultSet) throws SQLException {
        return new ProtectionInvite(
                resultSet.getLong("id"),
                resultSet.getString("region_id"),
                resultSet.getString("inviter_uuid"),
                resultSet.getString("inviter_name"),
                resultSet.getString("invited_uuid"),
                resultSet.getString("invited_name"),
                resultSet.getLong("created_at"),
                resultSet.getLong("expires_at"));
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private List<OwnerCount> topOwners(Connection connection) throws SQLException {
        List<OwnerCount> owners = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT owner_uuid, owner_name, COUNT(*) AS total
                FROM protections
                GROUP BY owner_uuid, owner_name
                ORDER BY total DESC, owner_name COLLATE NOCASE
                LIMIT 5
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                owners.add(new OwnerCount(resultSet.getString("owner_uuid"), resultSet.getString("owner_name"),
                        resultSet.getInt("total")));
            }
        }
        return owners;
    }

    private List<RegionArea> largestProtections(Connection connection) throws SQLException {
        List<RegionArea> regions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT region_id, alias, owner_name,
                       ((max_x - min_x + 1) * (max_z - min_z + 1)) AS area
                FROM protections
                ORDER BY area DESC, region_id
                LIMIT 5
                """);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                regions.add(new RegionArea(resultSet.getString("region_id"), resultSet.getString("alias"),
                        resultSet.getString("owner_name"), resultSet.getInt("area")));
            }
        }
        return regions;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, table, column)) {
            if (columns.next()) {
                return;
            }
        }
        execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private static final class StoredProtectionBuilder {
        private final String regionId;
        private final String protectionId;
        private final String alias;
        private final String ownerUuid;
        private final String ownerName;
        private final String worldName;
        private final int stoneX;
        private final int stoneY;
        private final int stoneZ;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private final long createdAtMillis;
        private final String actionbarEnter;
        private final String actionbarExit;
        private final boolean customHome;
        private final double homeX;
        private final double homeY;
        private final double homeZ;
        private final float homeYaw;
        private final float homePitch;
        private final long rentPaidUntilMillis;
        private final boolean rentSuspended;
        private final Map<String, String> flags = new LinkedHashMap<>();
        private final Map<String, StoredMember> members = new LinkedHashMap<>();

        private StoredProtectionBuilder(String regionId, String protectionId, String alias, String ownerUuid,
                String ownerName, String worldName, int stoneX, int stoneY, int stoneZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, long createdAtMillis, String actionbarEnter, String actionbarExit,
                boolean customHome, double homeX, double homeY, double homeZ, float homeYaw, float homePitch,
                long rentPaidUntilMillis, boolean rentSuspended) {
            this.regionId = regionId;
            this.protectionId = protectionId;
            this.alias = alias;
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.worldName = worldName;
            this.stoneX = stoneX;
            this.stoneY = stoneY;
            this.stoneZ = stoneZ;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.createdAtMillis = createdAtMillis;
            this.actionbarEnter = actionbarEnter;
            this.actionbarExit = actionbarExit;
            this.customHome = customHome;
            this.homeX = homeX;
            this.homeY = homeY;
            this.homeZ = homeZ;
            this.homeYaw = homeYaw;
            this.homePitch = homePitch;
            this.rentPaidUntilMillis = rentPaidUntilMillis;
            this.rentSuspended = rentSuspended;
        }

        private StoredProtection build() {
            return new StoredProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName, stoneX,
                    stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ, createdAtMillis, actionbarEnter,
                    actionbarExit, customHome, homeX, homeY, homeZ, homeYaw, homePitch,
                    new LinkedHashMap<>(flags), new LinkedHashMap<>(members), rentPaidUntilMillis, rentSuspended);
        }
    }
}
