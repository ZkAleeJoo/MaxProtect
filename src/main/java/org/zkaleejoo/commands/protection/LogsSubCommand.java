package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class LogsSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public LogsSubCommand(ProtectionCommandContext context) {
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
        return sender instanceof Player;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }
        if (args.length > 2) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionLogsUsage());
            return;
        }

        String lookup = ProtectionCommandSupport.joinCommandTail(args, 1);
        ProtectionMenuContext protection = lookup.isBlank()
                ? context.plugin().getProtectionRegionManager().findProtectionAt(player)
                : context.plugin().getProtectionRegionManager().findAccessibleProtection(player, lookup);
        if (protection == null) {
            context.send(player, lookup.isBlank()
                    ? context.plugin().getConfigManager().getMsgProtectionNotInProtection()
                    : context.plugin().getConfigManager().getMsgProtectionNotAccessible());
            return;
        }

        context.plugin().getProtectionMenuManager().openLogsMenu(player, protection, 0);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player && args.length == 2) {
            return CommandDispatcher.filterCompletions(context.listProtectionLookups(player), args[1]);
        }
        return List.of();
    }
}
