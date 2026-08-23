package org.zkaleejoo.protection;

import java.util.Locale;

public final class ProtectionRegionIdSupport {

    private ProtectionRegionIdSupport() {
    }

    public static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    public static String compactCoordinateId(int x, int y, int z) {
        return compact(x) + compact(y) + compact(z);
    }

    private static String compact(int value) {
        String encoded = Integer.toString(Math.abs(value), 36);
        return value < 0 ? "n" + encoded : encoded;
    }
}
