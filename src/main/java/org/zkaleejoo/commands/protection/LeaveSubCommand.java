package org.zkaleejoo.commands.protection;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionMemberRank;
import org.zkaleejoo.protection.ProtectionRegionManager.MemberChangeResult;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class LeaveSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public LeaveSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("leave");
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
        if (protection == null) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionNotInProtection());
            return;
        }
        boolean owner = context.plugin().getProtectionRegionManager().isOwner(player, protection);
        ProtectionMemberRank rank = context.plugin().getProtectionRegionManager().rankFor(player, protection);
        if (!ProtectionCommandSupport.canLeaveProtection(owner, rank != null)) {
            if (owner) {
                context.send(player, context.plugin().getConfigManager().getMsgProtectionLeaveOwnDenied());
                return;
            }
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberNotMember());
            return;
        }

        MemberChangeResult result = context.plugin().getProtectionRegionManager()
                .removeMember(protection, player.getUniqueId(), false);
        switch (result) {
            case SUCCESS -> context.send(player, context.plugin().getConfigManager().getMsgProtectionLeaveSuccess()
                    .replace("%alias%", protection.displayAlias()));
            case TARGET_IS_OWNER -> context.send(player, context.plugin().getConfigManager().getMsgProtectionLeaveOwnDenied());
            case NOT_MEMBER -> context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberNotMember());
            case REGION_UNAVAILABLE -> context.send(player,
                    context.plugin().getConfigManager().getMsgProtectionWorldUnavailable());
            case SAVE_ERROR -> context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberSaveError());
            case ALREADY_MEMBER -> context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberAlready());
        }
    }
}
