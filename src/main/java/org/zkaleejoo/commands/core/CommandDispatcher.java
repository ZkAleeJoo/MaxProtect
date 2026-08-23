package org.zkaleejoo.commands.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.command.CommandSender;

public class CommandDispatcher {

    private final List<PluginSubCommand> subCommands;

    public CommandDispatcher(List<PluginSubCommand> subCommands) {
        this.subCommands = List.copyOf(subCommands);
    }

    public boolean dispatch(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }
        Optional<PluginSubCommand> subCommand = find(args[0]);
        if (subCommand.isEmpty()) {
            return false;
        }
        subCommand.get().execute(sender, args);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (PluginSubCommand subCommand : subCommands) {
                if (!subCommand.canUse(sender)) {
                    continue;
                }
                completions.addAll(subCommand.completionAliases());
            }
            return filterCompletions(completions, args[0]);
        }
        return find(args[0])
                .filter(subCommand -> subCommand.canUse(sender))
                .map(subCommand -> subCommand.tabComplete(sender, args))
                .orElseGet(List::of);
    }

    public static List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                filtered.add(completion);
            }
        }
        return filtered;
    }

    private Optional<PluginSubCommand> find(String alias) {
        for (PluginSubCommand subCommand : subCommands) {
            for (String subAlias : subCommand.aliases()) {
                if (subAlias.equalsIgnoreCase(alias)) {
                    return Optional.of(subCommand);
                }
            }
        }
        return Optional.empty();
    }
}
