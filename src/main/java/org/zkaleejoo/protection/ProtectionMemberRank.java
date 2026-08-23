package org.zkaleejoo.protection;

import java.util.Locale;

public enum ProtectionMemberRank {
    OWNER,
    ADMIN,
    MEMBER;

    public static ProtectionMemberRank fromStorage(String value) {
        if (value == null || value.isBlank()) {
            return MEMBER;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OWNER" -> OWNER;
            case "ADMIN", "VETERAN", "VETERANO" -> ADMIN;
            case "MEMBER" -> MEMBER;
            default -> MEMBER;
        };
    }

    public boolean canUseMemberLevelFlags() {
        return this == OWNER || this == ADMIN || this == MEMBER;
    }

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }

    public boolean canChangeFlags() {
        return this == OWNER || this == ADMIN;
    }

    public ProtectionMemberRank promote() {
        return this == MEMBER ? ADMIN : this;
    }

    public ProtectionMemberRank demote() {
        return this == ADMIN ? MEMBER : this;
    }
}
