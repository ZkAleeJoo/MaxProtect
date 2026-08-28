package org.zkaleejoo.protection.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ProtectionCache {

    private final Cache<String, List<ProtectionStorage.ProtectionInvite>> invitesByPlayer;

    private final Cache<Long, Optional<ProtectionStorage.ProtectionInvite>> inviteById;

    private final Cache<String, List<ProtectionStorage.ProtectionEventLog>> recentEvents;

    private final Cache<String, ProtectionStorage.ReportSnapshot> reportSnapshot;

    private static final String REPORT_KEY = "_report_";

    public ProtectionCache() {
        this.invitesByPlayer = Caffeine.newBuilder()
                .maximumSize(256)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();

        this.inviteById = Caffeine.newBuilder()
                .maximumSize(512)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();

        this.recentEvents = Caffeine.newBuilder()
                .maximumSize(128)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();

        this.reportSnapshot = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    public List<ProtectionStorage.ProtectionInvite> getInvitesByPlayer(String invitedUuid) {
        return invitesByPlayer.getIfPresent(invitedUuid);
    }

    public void putInvitesByPlayer(String invitedUuid, List<ProtectionStorage.ProtectionInvite> invites) {
        invitesByPlayer.put(invitedUuid, invites);
    }

    public void invalidateInvitesByPlayer(String invitedUuid) {
        invitesByPlayer.invalidate(invitedUuid);
    }

    public Optional<ProtectionStorage.ProtectionInvite> getInviteById(long inviteId) {
        return inviteById.getIfPresent(inviteId);
    }

    public void putInviteById(long inviteId, Optional<ProtectionStorage.ProtectionInvite> invite) {
        inviteById.put(inviteId, invite);
    }

    public void invalidateInviteById(long inviteId) {
        inviteById.invalidate(inviteId);
    }

    private static String eventsKey(String regionId, int limit) {
        return regionId + ":" + limit;
    }

    public List<ProtectionStorage.ProtectionEventLog> getRecentEvents(String regionId, int limit) {
        return recentEvents.getIfPresent(eventsKey(regionId, limit));
    }

    public void putRecentEvents(String regionId, int limit, List<ProtectionStorage.ProtectionEventLog> events) {
        recentEvents.put(eventsKey(regionId, limit), events);
    }

    public void invalidateRecentEvents(String regionId) {
        recentEvents.asMap().keySet().removeIf(key -> key.startsWith(regionId + ":"));
    }

    public ProtectionStorage.ReportSnapshot getReportSnapshot() {
        return reportSnapshot.getIfPresent(REPORT_KEY);
    }

    public void putReportSnapshot(ProtectionStorage.ReportSnapshot snapshot) {
        reportSnapshot.put(REPORT_KEY, snapshot);
    }

    public void invalidateReportSnapshot() {
        reportSnapshot.invalidate(REPORT_KEY);
    }

    public void invalidateAllInvites() {
        invitesByPlayer.invalidateAll();
        inviteById.invalidateAll();
    }

    public void invalidateAll() {
        invitesByPlayer.invalidateAll();
        inviteById.invalidateAll();
        recentEvents.invalidateAll();
        reportSnapshot.invalidateAll();
    }
}
