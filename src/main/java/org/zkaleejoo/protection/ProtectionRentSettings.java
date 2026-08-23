package org.zkaleejoo.protection;

public record ProtectionRentSettings(boolean enabled, int periodHours, int checkIntervalMinutes) {

    private static final int MIN_PERIOD_HOURS = 1;
    private static final int MIN_CHECK_INTERVAL_MINUTES = 1;

    public ProtectionRentSettings {
        periodHours = Math.max(MIN_PERIOD_HOURS, periodHours);
        checkIntervalMinutes = Math.max(MIN_CHECK_INTERVAL_MINUTES, checkIntervalMinutes);
    }

    public long periodMillis() {
        return periodHours * 60L * 60L * 1000L;
    }

    public long checkIntervalTicks() {
        return checkIntervalMinutes * 60L * 20L;
    }

    public boolean isPaid(long paidUntilMillis, long nowMillis) {
        return paidUntilMillis > nowMillis;
    }
}
