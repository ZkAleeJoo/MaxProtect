package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;
import org.zkaleejoo.protection.ProtectionRegionManager.ViewResult;

public class ViewSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public ViewSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("view");
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
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionUsage());
            return;
        }
        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findProtectionAt(player);
        if (protection == null) {
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionNotInProtection());
            return;
        }
        ViewResult result = context.plugin().getProtectionRegionManager().showBorder(player, protection);
        if (!result.success()) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionViewCooldown()
                    .replace("%seconds%", String.valueOf(result.remainingCooldownSeconds())));
            context.playError(player);
            return;
        }
        context.send(player, context.plugin().getConfigManager().getMsgProtectionViewShown()
                .replace("%alias%", protection.displayAlias()));
    }
}
