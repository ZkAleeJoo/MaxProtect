package org.zkaleejoo.commands.core;

import java.util.List;

import org.bukkit.command.CommandSender;

public interface PluginSubCommand {

    List<String> aliases();

    default List<String> completionAliases() {
        return List.of(aliases().get(0));
    }

    default boolean canUse(CommandSender sender) {
        return true;
    }

    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
