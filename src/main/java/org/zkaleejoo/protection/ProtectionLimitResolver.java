package org.zkaleejoo.protection;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class ProtectionLimitResolver {

    private ProtectionLimitResolver() {
    }

    public static ProtectionLimitProfile resolve(List<ProtectionLimitProfile> groups,
            ProtectionLimitProfile fallback, Predicate<String> hasPermission) {
        if (groups == null || groups.isEmpty()) {
            return fallback;
        }

        return groups.stream()
                .filter(profile -> profile.permission() != null && !profile.permission().isBlank())
                .filter(profile -> hasPermission.test(profile.permission()))
                .max(Comparator.comparingInt(p -> p.priority()))
                .orElse(fallback);
    }
}
