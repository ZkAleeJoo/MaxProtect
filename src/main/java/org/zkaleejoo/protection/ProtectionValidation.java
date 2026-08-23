package org.zkaleejoo.protection;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ProtectionValidation {

    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 256;
    public static final double MIN_PRICE = 0.0D;
    public static final double MAX_PRICE = 1_000_000_000.0D;

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_-]{2,48}");

    private ProtectionValidation() {
    }

    public static Optional<String> normalizeId(String rawId) {
        if (rawId == null) {
            return Optional.empty();
        }

        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        if (!VALID_ID.matcher(normalized).matches()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    public static Optional<Integer> parseRadius(String rawRadius) {
        if (rawRadius == null) {
            return Optional.empty();
        }

        try {
            int radius = Integer.parseInt(rawRadius.trim());
            return isValidRadius(radius) ? Optional.of(radius) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static boolean isValidRadius(int radius) {
        return radius >= MIN_RADIUS && radius <= MAX_RADIUS;
    }

    public static Optional<Double> parsePrice(String rawPrice) {
        if (rawPrice == null) {
            return Optional.empty();
        }

        try {
            double price = Double.parseDouble(rawPrice.trim());
            return isValidPrice(price) ? Optional.of(price) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static boolean isValidPrice(double price) {
        return Double.isFinite(price) && price >= MIN_PRICE && price <= MAX_PRICE;
    }
}
