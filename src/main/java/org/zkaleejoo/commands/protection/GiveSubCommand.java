package org.zkaleejoo.commands.protection;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.commands.protection.ProtectionCommandContext.ProtectionDefinition;

public class GiveSubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public GiveSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("give");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(ProtectionCommandContext.GIVE_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.send(sender, context.plugin().getConfigManager().getMsgNoPermission());
            return;
        }

        if (args.length != 4) {
            context.send(sender, context.plugin().getConfigManager().getMsgProtectionGiveUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            context.send(sender, context.replace(context.plugin().getConfigManager().getMsgProtectionPlayerOffline(),
                    "%player%", args[1]));
            return;
        }

        ProtectionDefinition protection = context.loadProtection(args[2]);
        if (protection == null) {
            context.sendProtectionLoadFailure(sender, args[2]);
            return;
        }

        int amount = context.parseAmount(args[3]);
        if (amount < 1) {
            context.send(sender, context.plugin().getConfigManager().getMsgProtectionInvalidAmount());
            return;
        }

        int delivered = context.giveProtectionItems(target, protection, amount);
        if (delivered < amount) {
            context.send(sender, context.plugin().getConfigManager().getMsgProtectionInventoryFull()
                    .replace("%delivered%", String.valueOf(delivered))
                    .replace("%amount%", String.valueOf(amount)));
            return;
        }

        context.send(sender, context.plugin().getConfigManager().getMsgProtectionGiveSender()
                .replace("%amount%", String.valueOf(amount))
                .replace("%protection%", protection.id())
                .replace("%player%", target.getName()));
        context.send(target, context.plugin().getConfigManager().getMsgProtectionGiveTarget()
                .replace("%amount%", String.valueOf(amount))
                .replace("%protection%", protection.id()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .toList(), args[1]);
        }
        if (args.length == 3) {
            return CommandDispatcher.filterCompletions(context.listProtectionIds(), args[2]);
        }
        if (args.length == 4) {
            return CommandDispatcher.filterCompletions(List.of("1", "8", "16", "32", "64"), args[3]);
        }
        return List.of();
    }
}
