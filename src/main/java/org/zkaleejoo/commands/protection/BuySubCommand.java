package org.zkaleejoo.commands.protection;

import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.CommandDispatcher;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.commands.protection.ProtectionCommandContext.ProtectionDefinition;

public class BuySubCommand implements PluginSubCommand {

    private final ProtectionCommandContext context;

    public BuySubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("buy");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(ProtectionCommandContext.BUY_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.send(sender, context.plugin().getConfigManager().getMsgNoPermission());
            if (sender instanceof Player player) {
                context.playError(player);
            }
            return;
        }

        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }

        if (args.length != 2) {
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionBuyUsage());
            return;
        }

        ProtectionDefinition protection = context.loadProtection(args[1]);
        if (protection == null) {
            context.sendProtectionLoadFailure(player, args[1]);
            context.playError(player);
            return;
        }
        String limitError = context.validatePurchaseLimits(player, protection);
        if (limitError != null) {
            context.sendError(player, limitError);
            return;
        }

        if (!context.canFit(player.getInventory(), context.buildProtectionItem(protection, 1))) {
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionNoInventorySpace());
            return;
        }

        double price = protection.price();
        if (price > 0) {
            if (!context.plugin().hasEconomy()) {
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionEconomyUnavailable());
                return;
            }

            if (!context.plugin().getEconomy().has(player, price)) {
                context.sendError(player, context.replace(
                        context.plugin().getConfigManager().getMsgProtectionNotEnoughMoney(),
                        "%price%", context.plugin().getEconomy().format(price)));
                return;
            }

            EconomyResponse response = context.plugin().getEconomy().withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                context.sendError(player,
                        context.replace(context.plugin().getConfigManager().getMsgProtectionBuyError(),
                                "%error%", response.errorMessage));
                return;
            }
        }

        player.getInventory().addItem(context.buildProtectionItem(protection, 1));
        if (price > 0 && context.plugin().hasEconomy()) {
            context.send(player, context.plugin().getConfigManager().getMsgProtectionBoughtWithPrice()
                    .replace("%protection%", protection.id())
                    .replace("%price%", context.plugin().getEconomy().format(price)));
        } else {
            context.send(player, context.replace(context.plugin().getConfigManager().getMsgProtectionBoughtFree(),
                    "%protection%", protection.id()));
        }
        context.plugin().getConfigManager().getProtectionFeedbackConfig().buySound().play(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandDispatcher.filterCompletions(context.listProtectionIds(), args[1]);
        }
        return List.of();
    }
}
