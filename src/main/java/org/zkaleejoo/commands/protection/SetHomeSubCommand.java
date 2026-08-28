package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.HomeUpdateResult;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class SetHomeSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public SetHomeSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("sethome", "set-home");
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

        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findProtectionAt(player);
        if (protection == null || !context.plugin().getProtectionRegionManager().isOwner(player, protection)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMenuNotInOwnProtection());
            return;
        }

        HomeUpdateResult result = context.plugin().getProtectionRegionManager().setHome(protection, player.getLocation());
        switch (result) {
            case SUCCESS -> context.send(player, context.plugin().getConfigManager().getMsgProtectionHomeSet()
                    .replace("%alias%", protection.displayAlias()));
            case NOT_INSIDE -> context.send(player, context.plugin().getConfigManager().getMsgProtectionHomeOutside());
            case REGION_UNAVAILABLE, SAVE_ERROR -> context.send(player,
                    context.plugin().getConfigManager().getMsgProtectionHomeSaveError());
        }
    }
}
