package org.zkaleejoo.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.MaxProtections;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.main.CreateSubCommand;
import org.zkaleejoo.commands.main.DebugSubCommand;
import org.zkaleejoo.commands.main.LanguageSubCommand;
import org.zkaleejoo.commands.main.ListPlacedSubCommand;
import org.zkaleejoo.commands.main.LogsSubCommand;
import org.zkaleejoo.commands.main.MainCommandContext;
import org.zkaleejoo.commands.main.MigrateSubCommand;
import org.zkaleejoo.commands.main.ReloadSubCommand;
import org.zkaleejoo.commands.main.RepairSubCommand;
import org.zkaleejoo.commands.main.ReportSubCommand;
import org.zkaleejoo.utils.MessageUtils;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final MaxProtections plugin;
    private final CommandDispatcher dispatcher;

    public MainCommand(MaxProtections plugin) {
        this.plugin = plugin;
        MainCommandContext context = new MainCommandContext(plugin);
        this.dispatcher = new CommandDispatcher(List.of(
                new ReloadSubCommand(context),
                new CreateSubCommand(context),
                new LanguageSubCommand(context),
                new DebugSubCommand(context),
                new ListPlacedSubCommand(context),
                new ReportSubCommand(context),
                new LogsSubCommand(context),
                new MigrateSubCommand(context),
                new RepairSubCommand(context)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!dispatcher.dispatch(sender, args)) {
            sendUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return dispatcher.tabComplete(sender, args);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MessageUtils.getColoredMessage(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgUsageCommand()));
    }
}
