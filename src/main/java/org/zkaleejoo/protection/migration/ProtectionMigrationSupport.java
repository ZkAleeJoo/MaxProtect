package org.zkaleejoo.protection.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProtectionMigrationSupport {

    public static final String LEGACY_PROTECTION_ID = "__legacy_protectionstones";
    public static final String LEGACY_SOURCE = "protectionstones";
    public static final String LEGACY_FLAG_SOURCE = "__legacy_source";
    public static final String LEGACY_FLAG_MATERIAL = "__legacy_material";

    private static final Pattern PROTECTIONSTONES_REGION_ID = Pattern.compile("^ps(-?\\d+)x(-?\\d+)y(-?\\d+)z$");

    private ProtectionMigrationSupport() {
    }

    public static Optional<ProtectionStoneLocation> parseProtectionStonesRegionId(String regionId) {
        if (regionId == null) {
            return Optional.empty();
        }
        Matcher matcher = PROTECTIONSTONES_REGION_ID.matcher(regionId.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ProtectionStoneLocation(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Map<String, String> legacyFlags(String material) {
        String normalizedMaterial = material == null || material.isBlank()
                ? ""
                : material.trim().toUpperCase(Locale.ROOT);
        return Map.of(
                LEGACY_FLAG_SOURCE, LEGACY_SOURCE,
                LEGACY_FLAG_MATERIAL, normalizedMaterial);
    }

    public static List<MigrationMember> resolveDomainPlayers(Set<UUID> uniqueIds, Set<String> playerNames,
            Function<String, UUID> uuidResolver, Function<UUID, String> uuidNameResolver) {
        Map<String, MigrationMember> resolved = new LinkedHashMap<>();
        for (UUID uuid : uniqueIds == null ? Set.<UUID>of() : uniqueIds) {
            if (uuid == null) {
                continue;
            }
            String name = uuidNameResolver == null ? "" : uuidNameResolver.apply(uuid);
            resolved.put(uuid.toString(), new MigrationMember(uuid.toString(),
                    name == null || name.isBlank() ? uuid.toString() : name));
        }

        for (String rawName : playerNames == null ? Set.<String>of() : playerNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim();
            UUID uuid = parseUuid(name).orElseGet(() -> uuidResolver == null ? null : uuidResolver.apply(name));
            if (uuid == null) {
                continue;
            }
            resolved.putIfAbsent(uuid.toString(), new MigrationMember(uuid.toString(), name));
        }

        return new ArrayList<>(resolved.values()).stream()
                .sorted(Comparator.comparing(member -> member.uuid()))
                .toList();
    }

    private static Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public enum MigrationStatus {
        IMPORTABLE,
        IMPORTED,
        SKIPPED,
        WARNING,
        ALREADY_IMPORTED,
        FAILED
    }

    public record ProtectionStoneLocation(int x, int y, int z) {
    }

    public record MigrationDecision(String regionId, String worldName, MigrationStatus status, String reason) {
    }

    public record MigrationCandidate(String regionId, String worldName, String material,
            ProtectionStoneLocation stoneLocation, String alias, boolean importedLegacy) {

        public String protectionId() {
            return LEGACY_PROTECTION_ID;
        }
    }

    public record ProtectionStonesCandidate(String regionId, String worldName, String material,
            ProtectionStoneLocation stoneLocation, String alias, String ownerUuid, String ownerName,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            boolean customHome, double homeX, double homeY, double homeZ,
            List<MigrationMember> members) {
    }

    public record MigrationMember(String uuid, String name) {
    }

    public record MigrationReport(int total, int importable, int imported, int skipped, int warnings,
            int alreadyImported, int failed, List<MigrationDecision> decisions) {

        public static MigrationReport from(List<MigrationDecision> decisions) {
            int importable = 0;
            int imported = 0;
            int skipped = 0;
            int warnings = 0;
            int alreadyImported = 0;
            int failed = 0;
            for (MigrationDecision decision : decisions) {
                switch (decision.status()) {
                    case IMPORTABLE -> importable++;
                    case IMPORTED -> imported++;
                    case SKIPPED -> skipped++;
                    case WARNING -> warnings++;
                    case ALREADY_IMPORTED -> alreadyImported++;
                    case FAILED -> failed++;
                }
            }
            return new MigrationReport(decisions.size(), importable, imported, skipped, warnings,
                    alreadyImported, failed, List.copyOf(decisions));
        }
    }
}
