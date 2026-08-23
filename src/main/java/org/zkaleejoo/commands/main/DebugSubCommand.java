package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;

public class DebugSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public DebugSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("debug");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.DEBUG_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length < 2) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminDebugUsage());
            return;
        }

        String lookup = ProtectionCommandSupport.joinCommandTail(args, 1);
        for (String line : context.plugin().getProtectionRegionManager().debugProtection(lookup)) {
            context.send(sender, line);
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
}
