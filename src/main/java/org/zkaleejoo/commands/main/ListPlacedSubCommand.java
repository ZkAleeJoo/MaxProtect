package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;

public class ListPlacedSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public ListPlacedSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("listplaced");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.LIST_PLACED_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (args.length != 1) {
            context.send(sender, context.plugin().getConfigManager().getMsgAdminListPlacedUsage());
            return;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }

        context.plugin().getProtectionMenuManager().openAdminPlacedMenu(player);
    }
}
