package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;

public class MenuSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public MenuSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("menu");
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

        context.plugin().getProtectionMenuManager().openMenu(player);
    }
}
