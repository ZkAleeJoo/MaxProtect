package org.zkaleejoo.protection;

public record ProtectionLimitProfile(String id, String permission, int priority, int maxProtections,
        int maxRadius, double minPrice, double maxPrice) {

    public ProtectionLimitProfile {
        id = id == null || id.isBlank() ? "default" : id;
        permission = permission == null ? "" : permission;
        maxRadius = Math.max(1, maxRadius);
        minPrice = finiteOrDefault(minPrice, 0.0D);
        maxPrice = finiteOrDefault(maxPrice, ProtectionValidation.MAX_PRICE);
        if (maxPrice < minPrice) {
            maxPrice = minPrice;
        }
    }

    public boolean canCreateAnotherProtection(int currentProtections) {
        return maxProtections < 0 || currentProtections < maxProtections;
    }

    public boolean allowsRadius(int radius) {
        return radius <= maxRadius;
    }

    public boolean allowsPrice(double price) {
        return Double.isFinite(price) && price >= minPrice && price <= maxPrice;
    }

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}
