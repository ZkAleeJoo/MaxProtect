package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class MemberSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public MemberSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("member", "members");
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
        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findManageableProtectionAt(player);
        if (protection == null || !context.plugin().getProtectionRegionManager().canManageMembers(player, protection)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMenuNotInOwnProtection());
            return;
        }
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            context.plugin().getProtectionMenuManager().openMembersMenu(player);
            return;
        }
        if (args.length != 3 || (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove"))) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberUsage());
            return;
        }

        context.changeMember(player, protection, args[2], args[1].equalsIgnoreCase("add"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(List.of("add", "remove", "list"), args[1]);
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            return CommandDispatcher.filterCompletions(Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .toList(), args[2]);
        }
        return List.of();
    }
}
