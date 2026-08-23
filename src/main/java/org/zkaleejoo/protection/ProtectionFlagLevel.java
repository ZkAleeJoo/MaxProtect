package org.zkaleejoo.protection;

import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;

import java.util.Locale;

public enum ProtectionFlagLevel {
    NOBODY(StateFlag.State.DENY, RegionGroup.ALL, "&cNobody", "&cDisabled"),
    MEMBERS(StateFlag.State.ALLOW, RegionGroup.MEMBERS, "&eMembers+", "&eMembers+"),
    EVERYONE(StateFlag.State.ALLOW, RegionGroup.ALL, "&aEveryone", "&aEnabled");

    private final StateFlag.State state;
    private final RegionGroup group;
    private final String accessLabel;
    private final String toggleLabel;

    ProtectionFlagLevel(StateFlag.State state, RegionGroup group, String accessLabel, String toggleLabel) {
        this.state = state;
        this.group = group;
        this.accessLabel = accessLabel;
        this.toggleLabel = toggleLabel;
    }

    public ProtectionFlagLevel nextAccess() {
        return switch (this) {
            case NOBODY -> MEMBERS;
            case MEMBERS -> EVERYONE;
            case EVERYONE -> NOBODY;
        };
    }

    public ProtectionFlagLevel nextToggle() {
        return this == NOBODY ? EVERYONE : NOBODY;
    }

    public boolean allows(boolean owner, boolean member) {
        return switch (this) {
            case NOBODY -> false;
            case MEMBERS -> owner || member;
            case EVERYONE -> true;
        };
    }

    public StateFlag.State state() {
        return state;
    }

    public RegionGroup group() {
        return group;
    }

    public String accessLabel() {
        return accessLabel;
    }

    public String toggleLabel() {
        return toggleLabel;
    }

    public static ProtectionFlagLevel fromStorage(String value, ProtectionFlagLevel fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public static ProtectionFlagLevel fromWorldGuard(StateFlag.State state, RegionGroup group,
            ProtectionFlagLevel fallback) {
        if (state == null) {
            return fallback;
        }
        if (state == StateFlag.State.DENY) {
            return NOBODY;
        }
        if (group == RegionGroup.MEMBERS || group == RegionGroup.OWNERS) {
            return MEMBERS;
        }
        return EVERYONE;
    }
}
