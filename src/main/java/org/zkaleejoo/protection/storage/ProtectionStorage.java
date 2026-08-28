package org.zkaleejoo.protection.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ProtectionStorage extends AutoCloseable {

        void initialize();

        List<StoredProtection> loadProtections();

        boolean replaceProtections(List<StoredProtection> protections);

        long createInvite(ProtectionInvite invite);

        List<ProtectionInvite> findPendingInvitesForPlayer(String invitedUuid, long nowMillis);

        Optional<ProtectionInvite> findInvite(long inviteId);

        Optional<ProtectionInvite> findPendingInvite(String regionId, String invitedUuid, long nowMillis);

        boolean deleteInvite(long inviteId);

        void recordEvent(ProtectionEvent event);

        List<ProtectionEventLog> recentEvents(String regionId, int limit);

        ReportSnapshot reportSnapshot();

        CompletableFuture<List<StoredProtection>> loadProtectionsAsync();

        CompletableFuture<Boolean> replaceProtectionsAsync(List<StoredProtection> protections);

        CompletableFuture<Long> createInviteAsync(ProtectionInvite invite);

        CompletableFuture<List<ProtectionInvite>> findPendingInvitesForPlayerAsync(String invitedUuid, long nowMillis);

        CompletableFuture<Optional<ProtectionInvite>> findInviteAsync(long inviteId);

        CompletableFuture<Optional<ProtectionInvite>> findPendingInviteAsync(String regionId, String invitedUuid,
                        long nowMillis);

        CompletableFuture<Boolean> deleteInviteAsync(long inviteId);

        CompletableFuture<Void> recordEventAsync(ProtectionEvent event);

        CompletableFuture<List<ProtectionEventLog>> recentEventsAsync(String regionId, int limit);

        CompletableFuture<ReportSnapshot> reportSnapshotAsync();

        @Override
        void close();

        record StoredProtection(String regionId, String protectionId, String alias, String ownerUuid, String ownerName,
                        String worldName, int stoneX, int stoneY, int stoneZ, int minX, int minY, int minZ,
                        int maxX, int maxY, int maxZ, long createdAtMillis, String actionbarEnter, String actionbarExit,
                        boolean customHome, double homeX, double homeY, double homeZ, float homeYaw, float homePitch,
                        Map<String, String> flags, Map<String, StoredMember> members,
                        long rentPaidUntilMillis, boolean rentSuspended) {
        }

        record StoredMember(String uuid, String name, String rank) {
        }

        record ProtectionInvite(long id, String regionId, String inviterUuid, String inviterName,
                        String invitedUuid, String invitedName, long createdAtMillis, long expiresAtMillis) {
        }

        record ProtectionEvent(String regionId, String actorUuid, String actorName, String targetUuid,
                        String targetName,
                        String eventType, String detail) {
        }

        record ProtectionEventLog(long id, String regionId, String actorUuid, String actorName, String targetUuid,
                        String targetName, String eventType, String detail, long createdAtMillis) {
        }

        record OwnerCount(String ownerUuid, String ownerName, int count) {
        }

        record RegionArea(String regionId, String alias, String ownerName, int area) {
        }

        record ReportSnapshot(int totalProtections, int eventCount, List<OwnerCount> topOwners,
                        List<RegionArea> largestProtections) {
        }
}
