package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.RepairReport;

public class RepairSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public RepairSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("repair");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.REPAIR_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length > 2 || (args.length == 2 && !args[1].equalsIgnoreCase("cleanup"))) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminRepairUsage());
            return;
        }

        RepairReport report = context.plugin().getProtectionRegionManager().repairPlacedProtections(args.length == 2);
        context.send(sender, context.plugin().getConfigManager().getMsgAdminRepairSummary()
                .replace("%repaired%", String.valueOf(report.repaired()))
                .replace("%cleaned%", String.valueOf(report.cleaned()))
                .replace("%skipped%", String.valueOf(report.skipped())));
        for (String line : report.lines()) {
            context.send(sender, line);
        }
        if (!report.saved()) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminRepairSaveError());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(List.of("cleanup"), args[1]);
        }
        return List.of();
    }
}
