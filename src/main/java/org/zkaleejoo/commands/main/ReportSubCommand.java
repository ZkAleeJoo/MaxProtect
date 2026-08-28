package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionReport;

public class ReportSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public ReportSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("report");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.REPORT_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length != 2 || !args[1].equalsIgnoreCase("protections")) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminReportUsage());
            return;
        }

        ProtectionReport report = context.plugin().getProtectionRegionManager().protectionReport();
        String largest = context.formatLargestRegions(report.largestProtections());
        String topOwners = context.formatTopOwners(report.topOwners());
        for (String line : context.plugin().getConfigManager().getAdminReportProtectionLines()) {
            context.send(sender, line
                    .replace("%total%", String.valueOf(report.total()))
                    .replace("%active%", String.valueOf(report.active()))
                    .replace("%orphaned%", String.valueOf(report.orphaned()))
                    .replace("%events%", String.valueOf(report.eventCount()))
                    .replace("%largest_regions%", largest)
                    .replace("%top_owners%", topOwners));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(List.of("protections"), args[1]);
        }
        return List.of();
    }
}
