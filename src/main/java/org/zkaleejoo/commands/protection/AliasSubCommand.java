package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class AliasSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public AliasSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("alias");
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
        if (args.length < 2) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionAliasUsage());
            return;
        }

        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findManageableProtectionAt(player);
        if (protection == null || !context.plugin().getProtectionRegionManager().canManageMembers(player, protection)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMenuNotInOwnProtection());
            return;
        }
        String newAlias = ProtectionCommandSupport.joinCommandTail(args, 1);
        if (!ProtectionCommandSupport.isValidAlias(newAlias)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionAliasInvalid());
            return;
        }
        if (!context.plugin().getProtectionRegionManager().isAliasAvailable(newAlias, protection.regionId())) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionAliasTaken());
            return;
        }
        if (!context.plugin().getProtectionRegionManager().setAlias(protection, newAlias)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionRegionSaveError());
            return;
        }
        context.send(player, context.plugin().getConfigManager().getMsgProtectionAliasUpdated().replace("%alias%", newAlias));
    }
}
