package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.OwnershipTransferResult;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class TransferSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public TransferSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("transfer");
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
        if (args.length != 2) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionTransferUsage());
            return;
        }

        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findProtectionAt(player);
        if (protection == null || !context.plugin().getProtectionRegionManager().isOwner(player, protection)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionRemoveNotOwner());
            return;
        }

        OfflinePlayer target = context.resolveOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionPlayerOffline()
                    .replace("%player%", args[1]));
            return;
        }

        OwnershipTransferResult result = context.plugin().getProtectionRegionManager()
                .transferOwnership(protection, player, target);
        switch (result.status()) {
            case SUCCESS -> {
                context.send(player, context.plugin().getConfigManager().getMsgProtectionTransferSuccess()
                        .replace("%alias%", result.context().displayAlias())
                        .replace("%player%", target.getName() == null ? args[1] : target.getName()));
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    context.send(onlineTarget, context.plugin().getConfigManager().getMsgProtectionTransferReceived()
                            .replace("%alias%", result.context().displayAlias())
                            .replace("%player%", player.getName()));
                }
            }
            case TARGET_IS_OWNER -> context.send(player,
                    context.plugin().getConfigManager().getMsgProtectionTransferTargetOwner());
            case NOT_OWNER -> context.send(player, context.plugin().getConfigManager().getMsgProtectionRemoveNotOwner());
            case TARGET_UNKNOWN -> context.send(player, context.plugin().getConfigManager().getMsgProtectionPlayerOffline()
                    .replace("%player%", args[1]));
            case REGION_UNAVAILABLE, SAVE_ERROR -> context.send(player,
                    context.plugin().getConfigManager().getMsgProtectionRegionSaveError());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .toList(), args[1]);
        }
        return List.of();
    }
}
