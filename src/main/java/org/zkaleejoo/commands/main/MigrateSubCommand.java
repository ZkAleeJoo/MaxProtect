package org.zkaleejoo.commands.main;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationDecision;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationReport;

public class MigrateSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public MigrateSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("migrate");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.MIGRATE_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length != 3 || !args[1].equalsIgnoreCase("protectionstones")
                || (!args[2].equalsIgnoreCase("preview") && !args[2].equalsIgnoreCase("apply"))) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminMigrateUsage());
            return;
        }

        boolean apply = args[2].equalsIgnoreCase("apply");
        if (!apply) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminMigrateBackupWarning());
        }
        MigrationReport report = apply
                ? context.plugin().getProtectionRegionManager().applyProtectionStonesMigration()
                : context.plugin().getProtectionRegionManager().previewProtectionStonesMigration();

        context.send(sender, context.plugin().getConfigManager().getMsgAdminMigrateSummary()
                .replace("%total%", String.valueOf(report.total()))
                .replace("%importable%", String.valueOf(report.importable()))
                .replace("%imported%", String.valueOf(report.imported()))
                .replace("%skipped%", String.valueOf(report.skipped()))
                .replace("%warnings%", String.valueOf(report.warnings()))
                .replace("%already%", String.valueOf(report.alreadyImported()))
                .replace("%failed%", String.valueOf(report.failed())));
        for (MigrationDecision decision : report.decisions()) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminMigrateEntry()
                    .replace("%region%", decision.regionId())
                    .replace("%world%", decision.worldName())
                    .replace("%status%", decision.status().name().toLowerCase(Locale.ROOT))
                    .replace("%reason%", decision.reason()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(List.of("protectionstones"), args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("protectionstones")) {
            return CommandDispatcher.filterCompletions(List.of("preview", "apply"), args[2]);
        }
        return List.of();
    }
}
