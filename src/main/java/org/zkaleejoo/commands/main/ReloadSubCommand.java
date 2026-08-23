package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.zkaleejoo.commands.core.PluginSubCommand;

public class ReloadSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public ReloadSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("reload");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.ADMIN_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        context.plugin().getConfigManager().reloadConfig();
        context.plugin().getProtectionRegionManager().reloadRentSettings();
        context.send(sender, context.plugin().getConfigManager().getMsgPluginReload());
    }
}
