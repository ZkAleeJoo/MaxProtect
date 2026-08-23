package org.zkaleejoo.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.zkaleejoo.ArgosProtect;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.protection.AliasSubCommand;
import org.zkaleejoo.commands.protection.BuySubCommand;
import org.zkaleejoo.commands.protection.FlySubCommand;
import org.zkaleejoo.commands.protection.GiveSubCommand;
import org.zkaleejoo.commands.protection.HomeSubCommand;
import org.zkaleejoo.commands.protection.InfoSubCommand;
import org.zkaleejoo.commands.protection.InviteSubCommand;
import org.zkaleejoo.commands.protection.LeaveSubCommand;
import org.zkaleejoo.commands.protection.ListSubCommand;
import org.zkaleejoo.commands.protection.LogsSubCommand;
import org.zkaleejoo.commands.protection.MemberSubCommand;
import org.zkaleejoo.commands.protection.MenuSubCommand;
import org.zkaleejoo.commands.protection.ProtectionCommandContext;
import org.zkaleejoo.commands.protection.RentSubCommand;
import org.zkaleejoo.commands.protection.SetHomeSubCommand;
import org.zkaleejoo.commands.protection.SettingsSubCommand;
import org.zkaleejoo.commands.protection.TeleportSubCommand;
import org.zkaleejoo.commands.protection.TrustSubCommand;
import org.zkaleejoo.commands.protection.TransferSubCommand;
import org.zkaleejoo.commands.protection.ViewSubCommand;
import org.zkaleejoo.utils.MessageUtils;

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private final ArgosProtect plugin;
    private final CommandDispatcher dispatcher;

    public ProtectionCommand(ArgosProtect plugin) {
        this.plugin = plugin;
        ProtectionCommandContext context = new ProtectionCommandContext(plugin);
        this.dispatcher = new CommandDispatcher(List.of(
                new GiveSubCommand(context),
                new BuySubCommand(context),
                new RentSubCommand(context),
                new FlySubCommand(context),
                new MenuSubCommand(context),
                new TeleportSubCommand(context),
                new AliasSubCommand(context),
                new InfoSubCommand(context),
                new ViewSubCommand(context),
                new LeaveSubCommand(context),
                new InviteSubCommand(context),
                new MemberSubCommand(context),
                new TrustSubCommand(context, true),
                new TrustSubCommand(context, false),
                new TransferSubCommand(context),
                new LogsSubCommand(context),
                new SettingsSubCommand(context),
                new ListSubCommand(context),
                new HomeSubCommand(context),
                new SetHomeSubCommand(context)));
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
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMsgProtectionUsage()));
    }
}
