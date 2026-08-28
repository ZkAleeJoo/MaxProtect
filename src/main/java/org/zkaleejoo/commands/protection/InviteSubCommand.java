package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.InviteResponseResult;

public class InviteSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public InviteSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("invite");
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
        if (args.length != 3 || (!args[1].equalsIgnoreCase("accept") && !args[1].equalsIgnoreCase("deny"))) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionInviteUsage());
            return;
        }

        long inviteId;
        try {
            inviteId = Long.parseLong(args[2]);
        } catch (NumberFormatException exception) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionInviteInvalid());
            return;
        }

        InviteResponseResult result = args[1].equalsIgnoreCase("accept")
                ? context.plugin().getProtectionRegionManager().acceptMemberInvite(player, inviteId)
                : context.plugin().getProtectionRegionManager().denyMemberInvite(player, inviteId);
        context.handleInviteResponse(player, args[1].equalsIgnoreCase("accept"), result);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(List.of("accept", "deny"), args[1]);
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
            List<String> pendingInvites = context.plugin().getProtectionRegionManager()
                    .getPendingInviteIdsForPlayer(player);
            return CommandDispatcher.filterCompletions(pendingInvites, args[2]);
        }

        return List.of();
    }
}
