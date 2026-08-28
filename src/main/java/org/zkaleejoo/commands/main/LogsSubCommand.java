package org.zkaleejoo.commands.main;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;
import org.zkaleejoo.protection.storage.ProtectionStorage;

public class LogsSubCommand implements PluginSubCommand {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final MainCommandContext context;

    public LogsSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("logs", "log");
    }

    @Override
    public List<String> completionAliases() {
        return List.of("logs");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.LOGS_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length != 2) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminLogsUsage());
            return;
        }

        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findPlacedProtection(args[1]);
        if (protection == null) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminDebugNotFound()
                    .replace("%lookup%", args[1]));
            return;
        }

        List<ProtectionStorage.ProtectionEventLog> events =
                context.plugin().getProtectionRegionManager().recentEvents(protection, 10);
        if (events.isEmpty()) {
            context.send(sender, applyContextPlaceholders(
                    context.plugin().getConfigManager().getMsgAdminLogsEmpty(), protection));
            return;
        }

        context.send(sender, applyContextPlaceholders(
                context.plugin().getConfigManager().getMsgAdminLogsHeader(), protection));
        for (ProtectionStorage.ProtectionEventLog event : events) {
            context.send(sender, applyLogPlaceholders(
                    context.plugin().getConfigManager().getMsgAdminLogsEntry(), protection, event));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(
                    context.plugin().getProtectionRegionManager().listPlacedRegionIds(), args[1]);
        }
        return List.of();
    }

    private String applyContextPlaceholders(String text, ProtectionMenuContext protection) {
        return text
                .replace("%alias%", protection.displayAlias())
                .replace("%region%", protection.regionId())
                .replace("%protection_id%", protection.protectionId())
                .replace("%owner%", protection.ownerName());
    }

    private String applyLogPlaceholders(String text, ProtectionMenuContext protection,
            ProtectionStorage.ProtectionEventLog event) {
        return applyContextPlaceholders(text, protection)
                .replace("%date%", formatDate(event.createdAtMillis()))
                .replace("%actor%", displayName(event.actorName(), event.actorUuid()))
                .replace("%target%", displayName(event.targetName(), event.targetUuid()))
                .replace("%type%", event.eventType())
                .replace("%detail%", event.detail() == null ? "" : event.detail());
    }

    private String formatDate(long createdAtMillis) {
        if (createdAtMillis <= 0) {
            return "-";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(createdAtMillis));
    }

    private String displayName(String name, String uuid) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return uuid == null || uuid.isBlank() ? "-" : uuid;
    }
}
