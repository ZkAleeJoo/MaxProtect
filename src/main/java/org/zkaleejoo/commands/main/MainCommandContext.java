package org.zkaleejoo.commands.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.permissions.MaxProtectPermissions;
import org.zkaleejoo.protection.storage.ProtectionStorage;
import org.zkaleejoo.utils.MessageUtils;

public class MainCommandContext {

    static final String ADMIN_PERMISSION = MaxProtectPermissions.ADMIN;
    static final String DEBUG_PERMISSION = MaxProtectPermissions.ADMIN_DEBUG;
    static final String LIST_PLACED_PERMISSION = MaxProtectPermissions.ADMIN_LIST_PLACED;
    static final String REPAIR_PERMISSION = MaxProtectPermissions.ADMIN_REPAIR;
    static final String REPORT_PERMISSION = MaxProtectPermissions.ADMIN_REPORT;
    static final String MIGRATE_PERMISSION = MaxProtectPermissions.ADMIN_MIGRATE;
    static final String LOGS_PERMISSION = MaxProtectPermissions.ADMIN_LOGS;

    private final MaxProtect plugin;

    public MainCommandContext(MaxProtect plugin) {
        this.plugin = plugin;
    }

    public MaxProtect plugin() {
        return plugin;
    }

    public boolean hasAdminPermission(CommandSender sender, String permission) {
        return sender.hasPermission(ADMIN_PERMISSION) || sender.hasPermission(permission);
    }

    public void sendNoPermission(CommandSender sender) {
        send(sender, plugin.getConfigManager().getMsgNoPermission());
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
    }

    public String formatLargestRegions(List<ProtectionStorage.RegionArea> regions) {
        if (regions.isEmpty()) {
            return plugin.getConfigManager().getAdminReportProtectionEmptyEntry();
        }
        List<String> lines = new ArrayList<>();
        int index = 1;
        for (ProtectionStorage.RegionArea region : regions) {
            String name = region.alias() == null || region.alias().isBlank() ? region.regionId() : region.alias();
            lines.add(plugin.getConfigManager().getAdminReportProtectionLargestEntry()
                    .replace("%index%", String.valueOf(index))
                    .replace("%name%", name)
                    .replace("%region%", region.regionId())
                    .replace("%blocks%", String.valueOf(region.area()))
                    .replace("%owner%", region.ownerName()));
            index++;
        }
        return String.join("\n", lines);
    }

    public String formatTopOwners(List<ProtectionStorage.OwnerCount> owners) {
        if (owners.isEmpty()) {
            return plugin.getConfigManager().getAdminReportProtectionEmptyEntry();
        }
        List<String> lines = new ArrayList<>();
        int index = 1;
        for (ProtectionStorage.OwnerCount owner : owners) {
            lines.add(plugin.getConfigManager().getAdminReportProtectionTopOwnerEntry()
                    .replace("%index%", String.valueOf(index))
                    .replace("%owner%", owner.ownerName())
                    .replace("%count%", String.valueOf(owner.count())));
            index++;
        }
        return String.join("\n", lines);
    }

    public List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                filtered.add(completion);
            }
        }
        return filtered;
    }

    public void play(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
    }
}
