package org.zkaleejoo.protection;

import org.bukkit.configuration.file.YamlConfiguration;

public final class ProtectionCommandSupport {

    private static final int MAX_ALIAS_LENGTH = 32;

    private ProtectionCommandSupport() {
    }

    public static boolean isValidAlias(String alias) {
        if (alias == null) {
            return false;
        }

        String trimmedAlias = alias.trim();
        if (trimmedAlias.isEmpty() || trimmedAlias.length() > MAX_ALIAS_LENGTH) {
            return false;
        }

        for (int index = 0; index < trimmedAlias.length(); index++) {
            if (Character.isISOControl(trimmedAlias.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public static String joinCommandTail(String[] args, int startIndex) {
        if (args == null || startIndex >= args.length) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString().trim();
    }

    public static boolean canChangeMember(boolean targetIsOwner, boolean targetIsSelf, boolean targetKnown) {
        return targetKnown && !targetIsOwner && !targetIsSelf;
    }

    public static boolean canLeaveProtection(boolean targetIsOwner, boolean targetIsMember) {
        return !targetIsOwner && targetIsMember;
    }

    public static boolean canTransferOwnership(boolean actorIsOwner, boolean targetIsCurrentOwner, boolean targetKnown) {
        return actorIsOwner && targetKnown && !targetIsCurrentOwner;
    }

    public static double loadProtectionPrice(YamlConfiguration yaml) {
        if (yaml == null) {
            return 0.0D;
        }

        String path = yaml.contains("price") ? "price" : "economy.price";
        return yaml.getDouble(path, 0.0D);
    }

    public static double loadProtectionRentPrice(YamlConfiguration yaml) {
        if (yaml == null) {
            return 0.0D;
        }
        if (yaml.contains("price-rent")) {
            return yaml.getDouble("price-rent", 0.0D);
        }
        return loadProtectionPrice(yaml);
    }
}
