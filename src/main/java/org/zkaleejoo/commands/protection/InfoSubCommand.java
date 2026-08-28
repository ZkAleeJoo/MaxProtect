package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class InfoSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public InfoSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("info");
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

        String lookup = ProtectionCommandSupport.joinCommandTail(args, 1);
        ProtectionMenuContext protection = !lookup.isBlank()
                ? context.plugin().getProtectionRegionManager().findAccessibleProtection(player, lookup)
                : context.plugin().getProtectionRegionManager().findProtectionAt(player);
        if (protection == null) {
            context.send(player, !lookup.isBlank()
                    ? context.plugin().getConfigManager().getMsgProtectionNotAccessible()
                    : context.plugin().getConfigManager().getMsgProtectionNotInProtection());
            return;
        }

        context.sendProtectionInfo(player, protection);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player && args.length == 2) {
            return CommandDispatcher.filterCompletions(context.listProtectionLookups(player), args[1]);
        }
        return List.of();
    }
}
