package org.zkaleejoo.protection;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ProtectionAdminSupport {

    private static final String REGION_PREFIX = "mp_";

    private ProtectionAdminSupport() {
    }

    public static Optional<String> inferProtectionId(String regionId, List<String> knownProtectionIds) {
        if (regionId == null || knownProtectionIds == null) {
            return Optional.empty();
        }

        String normalizedRegion = regionId.toLowerCase(Locale.ROOT);
        if (!normalizedRegion.startsWith(REGION_PREFIX)) {
            return Optional.empty();
        }

        return knownProtectionIds.stream()
                .map(id -> id.toLowerCase(Locale.ROOT))
                .sorted(Comparator.comparingInt((String s) -> s.length()).reversed())
                .filter(id -> normalizedRegion.startsWith(REGION_PREFIX + id + "_"))
                .findFirst();
    }
}
