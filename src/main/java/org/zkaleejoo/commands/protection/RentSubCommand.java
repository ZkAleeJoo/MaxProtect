package org.zkaleejoo.commands.protection;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionRegionManager.RentPaymentResult;

public class RentSubCommand implements PluginSubCommand {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ProtectionCommandContext context;

    public RentSubCommand(ProtectionCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("rent");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(ProtectionCommandContext.PROTECTION_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }
        if (!canUse(sender)) {
            context.sendError(player, context.plugin().getConfigManager().getMsgNoPermission());
            return;
        }
        if (args.length > 2) {
            context.sendError(player, context.plugin().getConfigManager().getMsgProtectionRentUsage());
            return;
        }

        String lookup = args.length == 2 ? ProtectionCommandSupport.joinCommandTail(args, 1) : "";
        RentPaymentResult result = context.plugin().getProtectionRegionManager().payRent(player, lookup);
        handleResult(player, result);
    }

    private void handleResult(Player player, RentPaymentResult result) {
        String alias = result.context() == null ? "" : result.context().displayAlias();
        String paidUntil = formatPaidUntil(result.paidUntilMillis());
        switch (result.status()) {
            case PAID -> context.send(player, context.plugin().getConfigManager().getMsgProtectionRentPaid()
                    .replace("%alias%", alias)
                    .replace("%paid_until%", paidUntil));
            case REACTIVATED -> context.send(player,
                    context.plugin().getConfigManager().getMsgProtectionRentReactivated()
                            .replace("%alias%", alias)
                            .replace("%paid_until%", paidUntil));
            case DISABLED ->
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionRentDisabled());
            case NOT_FOUND ->
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionNotAccessible());
            case NOT_OWNER ->
                context.sendError(player, context.plugin().getConfigManager().getMsgProtectionRemoveNotOwner());
            case ALREADY_PAID -> context.sendError(player,
                    context.plugin().getConfigManager().getMsgProtectionRentAlreadyPaid()
                            .replace("%alias%", alias)
                            .replace("%paid_until%", paidUntil));
            case ECONOMY_UNAVAILABLE -> context.sendError(player,
                    context.plugin().getConfigManager().getMsgProtectionEconomyUnavailable());
            case NOT_ENOUGH_MONEY -> context.sendError(player, context.replace(
                    context.plugin().getConfigManager().getMsgProtectionNotEnoughMoney(), "%price%", result.detail()));
            case PAYMENT_ERROR -> context.sendError(player, context.plugin().getConfigManager()
                    .getMsgProtectionRentPaymentFailed()
                    .replace("%alias%", alias)
                    .replace("%reason%", result.detail()));
            case REGION_UNAVAILABLE -> context.sendError(player,
                    context.plugin().getConfigManager().getMsgProtectionRegionUnavailable());
            case SAVE_ERROR -> context.sendError(player,
                    context.plugin().getConfigManager().getMsgProtectionRemoveSaveError());
        }
    }

    private String formatPaidUntil(long paidUntilMillis) {
        if (paidUntilMillis <= 0L) {
            return "-";
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(paidUntilMillis));
    }
}
