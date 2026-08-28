package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class ListSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public ListSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("list");
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
        if (args.length != 1) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionUsage());
            return;
        }
        List<ProtectionMenuContext> protections = context.plugin().getProtectionRegionManager().listOwnedProtections(player);
        if (protections.isEmpty()) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionListEmpty());
            return;
        }
        context.send(player, context.plugin().getConfigManager().getProtectionListHeader());
        for (ProtectionMenuContext protection : protections) {
            context.sendListLine(player, protection);
        }
    }
}
