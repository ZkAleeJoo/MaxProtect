package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class TrustSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;
    private final boolean add;

    public TrustSubCommand(ProtectionCommandContext context, boolean add) {
        this.context = context;
        this.add = add;
    }

    @Override
    public List<String> aliases() {
        return List.of(add ? "trust" : "untrust");
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
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMemberUsage());
            return;
        }
        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager().findManageableProtectionAt(player);
        if (protection == null || !context.plugin().getProtectionRegionManager().canManageMembers(player, protection)) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionMenuNotInOwnProtection());
            return;
        }
        context.changeMember(player, protection, args[1], add);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player && args.length == 2) {
            return CommandDispatcher.filterCompletions(Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .toList(), args[1]);
        }
        return List.of();
    }
}
