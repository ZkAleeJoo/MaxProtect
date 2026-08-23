package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.permissions.MaxProtectionsPermissions;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionFlightResult;

public class FlySubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public FlySubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("fly");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(MaxProtectionsPermissions.PROTECTION_FLY);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }
        if (!canUse(sender)) {
            context.sendError(player, context.plugin().getConfigManager().getMsgNoPermission());
            return;
        }
        if (args.length != 1) {
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionFlyUsage());
            return;
        }

        ProtectionFlightResult result = context.plugin().getProtectionRegionManager().toggleProtectionFlight(player);
        String alias = result.context() == null ? "" : result.context().displayAlias();
        switch (result.status()) {
            case ENABLED -> context.send(player, context.plugin().getConfigManager().getMsgProtectionFlyEnabled()
                    .replace("%alias%", alias));
            case DISABLED -> context.send(player, context.plugin().getConfigManager().getMsgProtectionFlyDisabled());
            case NOT_IN_PROTECTION ->
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionNotInProtection());
            case NOT_ACCESSIBLE ->
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionFlyNotAccessible());
        }
    }
}
