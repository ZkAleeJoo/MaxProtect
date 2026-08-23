package org.zkaleejoo.commands.protection;

import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;

public class TeleportSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public TeleportSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("teleport", "tp");
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
            context.send(player, context.plugin().getConfigManager().getMsgProtectionTeleportUsage());
            return;
        }

        String lookup = ProtectionCommandSupport.joinCommandTail(args, 1);
        ProtectionMenuContext protection = context.plugin().getProtectionRegionManager()
                .findAccessibleProtection(player, lookup);
        if (protection == null) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionNotAccessible());
            return;
        }

        Optional<Location> home = context.plugin().getProtectionRegionManager().homeLocation(protection);
        if (home.isEmpty()) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionWorldUnavailable());
            return;
        }
        context.plugin().getSchedulerUtils().teleportEntity(player, home.get());
        context.send(player, context.plugin().getConfigManager().getMsgProtectionTeleportSuccess()
                .replace("%alias%", protection.displayAlias()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player && args.length == 2) {
            return CommandDispatcher.filterCompletions(context.listProtectionLookups(player), args[1]);
        }
        return List.of();
    }
}
