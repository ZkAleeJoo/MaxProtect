package org.zkaleejoo.commands.protection;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.zkaleejoo.ArgosProtect;
import org.zkaleejoo.permissions.ArgosProtectPermissions;
import org.zkaleejoo.protection.ProtectionCommandSupport;
import org.zkaleejoo.protection.ProtectionLimitProfile;
import org.zkaleejoo.protection.ProtectionRegionManager.InviteCreateResult;
import org.zkaleejoo.protection.ProtectionRegionManager.InviteResponseResult;
import org.zkaleejoo.protection.ProtectionRegionManager.MemberChangeResult;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;
import org.zkaleejoo.protection.ProtectionValidation;
import org.zkaleejoo.protection.storage.ProtectionStorage;
import org.zkaleejoo.utils.MessageUtils;

public class ProtectionCommandContext {

    static final String PROTECTION_PERMISSION = ArgosProtectPermissions.PROTECTION;
    static final String GIVE_PERMISSION = ArgosProtectPermissions.PROTECTION_GIVE;
    static final String BUY_PERMISSION = ArgosProtectPermissions.PROTECTION_BUY;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ArgosProtect plugin;
    private final NamespacedKey protectionIdKey;
    private final NamespacedKey protectionRadiusKey;

    public ProtectionCommandContext(ArgosProtect plugin) {
        this.plugin = plugin;
        this.protectionIdKey = new NamespacedKey(plugin, "protection_id");
        this.protectionRadiusKey = new NamespacedKey(plugin, "protection_radius");
    }

    public ArgosProtect plugin() {
        return plugin;
    }

    String validatePurchaseLimits(Player player, ProtectionDefinition protection) {
        ProtectionLimitProfile profile = plugin.getProtectionRegionManager().resolveLimitProfile(player);
        if (!profile.allowsRadius(protection.radius())) {
            return plugin.getConfigManager().getMsgProtectionLimitRadius()
                    .replace("%radius%", String.valueOf(protection.radius()))
                    .replace("%max_radius%", String.valueOf(profile.maxRadius()))
                    .replace("%group%", profile.id());
        }
        if (!profile.allowsPrice(protection.price())) {
            return plugin.getConfigManager().getMsgProtectionLimitPrice()
                    .replace("%price%", String.valueOf(protection.price()))
                    .replace("%min_price%", String.valueOf(profile.minPrice()))
                    .replace("%max_price%", String.valueOf(profile.maxPrice()))
                    .replace("%group%", profile.id());
        }
        return null;
    }

    void sendProtectionInfo(Player player, ProtectionMenuContext context) {
        plugin.getProtectionMenuManager().openInfoMenu(player, context);
    }

    void sendListLine(Player player, ProtectionMenuContext context) {
        String command = "/p info " + context.displayAlias();
        Component line = MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + applyContextPlaceholders(plugin.getConfigManager().getProtectionListEntry(), context))
                .append(MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionListInfoLabel())
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(MessageUtils.getColoredMessage(
                                applyContextPlaceholders(plugin.getConfigManager().getProtectionListInfoHover(), context)
                        .replace("%command%", command)))));
        player.sendMessage(line);
    }

    void sendProtectionLogs(Player player, ProtectionMenuContext context) {
        List<ProtectionStorage.ProtectionEventLog> events =
                plugin.getProtectionRegionManager().recentEvents(context, 10);
        if (events.isEmpty()) {
            send(player, applyContextPlaceholders(plugin.getConfigManager().getMsgProtectionLogsEmpty(), context));
            return;
        }
        send(player, applyContextPlaceholders(plugin.getConfigManager().getMsgProtectionLogsHeader(), context));
        for (ProtectionStorage.ProtectionEventLog event : events) {
            send(player, applyLogPlaceholders(plugin.getConfigManager().getMsgProtectionLogsEntry(), context, event));
        }
    }

    String applyContextPlaceholders(String text, ProtectionMenuContext context) {
        ProtectionDefinition definition = loadProtection(context.protectionId());
        String price = definition == null ? "0" : String.valueOf(definition.price());
        return text
                .replace("%alias%", context.displayAlias())
                .replace("%region%", context.regionId())
                .replace("%protection%", context.displayName())
                .replace("%protection_id%", context.protectionId())
                .replace("%price%", price)
                .replace("%size%", String.valueOf(context.displaySize()))
                .replace("%created_at%", formatCreatedAt(context.createdAtMillis()))
                .replace("%rent_paid_until%", formatCreatedAt(context.rentPaidUntilMillis()))
                .replace("%rent_status%", context.rentSuspended() ? "SUSPENDED" : "ACTIVE")
                .replace("%owners%", String.valueOf(context.ownerCount()))
                .replace("%owner%", context.ownerName())
                .replace("%members%", String.valueOf(context.memberCount()))
                .replace("%world%", context.worldName())
                .replace("%stone_x%", String.valueOf(context.stoneX()))
                .replace("%stone_y%", String.valueOf(context.stoneY()))
                .replace("%stone_z%", String.valueOf(context.stoneZ()))
                .replace("%home_custom%", String.valueOf(context.customHome()))
                .replace("%home_x%", formatCoord(context.homeX()))
                .replace("%home_y%", formatCoord(context.homeY()))
                .replace("%home_z%", formatCoord(context.homeZ()))
                .replace("%min_x%", String.valueOf(context.minX()))
                .replace("%min_y%", String.valueOf(context.minY()))
                .replace("%min_z%", String.valueOf(context.minZ()))
                .replace("%max_x%", String.valueOf(context.maxX()))
                .replace("%max_y%", String.valueOf(context.maxY()))
                .replace("%max_z%", String.valueOf(context.maxZ()));
    }

    String applyLogPlaceholders(String text, ProtectionMenuContext context,
            ProtectionStorage.ProtectionEventLog event) {
        return applyContextPlaceholders(text, context)
                .replace("%date%", formatCreatedAt(event.createdAtMillis()))
                .replace("%actor%", displayName(event.actorName(), event.actorUuid()))
                .replace("%target%", displayName(event.targetName(), event.targetUuid()))
                .replace("%type%", event.eventType())
                .replace("%detail%", event.detail() == null ? "" : event.detail());
    }

    OfflinePlayer resolveOfflinePlayer(String playerName) {
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return online;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            return null;
        }
        return offlinePlayer;
    }

    void changeMember(Player player, ProtectionMenuContext context, String targetName, boolean add) {
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = add ? onlineTarget : resolveOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            send(player, plugin.getConfigManager().getMsgProtectionPlayerOffline().replace("%player%", targetName));
            return;
        }
        if (!ProtectionCommandSupport.canChangeMember(
                target.getUniqueId().toString().equals(context.ownerUuid()),
                target.getUniqueId().equals(player.getUniqueId()),
                true)) {
            send(player, plugin.getConfigManager().getMsgProtectionMemberTargetOwner());
            return;
        }

        if (add) {
            InviteCreateResult result = plugin.getProtectionRegionManager().createMemberInvite(context, player,
                    onlineTarget);
            handleInviteCreateResult(player, onlineTarget, context, result);
            return;
        }

        MemberChangeResult result = plugin.getProtectionRegionManager().removeMember(context, target.getUniqueId());
        handleMemberResult(player, context, target.getName() == null ? targetName : target.getName(), false, result);
    }

    void handleInviteResponse(Player player, boolean accept, InviteResponseResult result) {
        ProtectionMenuContext context = result.context();
        String alias = context == null ? "Unknown" : context.displayAlias();
        switch (result.status()) {
            case SUCCESS -> {
                send(player, (accept
                        ? plugin.getConfigManager().getMsgProtectionInviteAccepted()
                        : plugin.getConfigManager().getMsgProtectionInviteDenied())
                        .replace("%alias%", alias)
                        .replace("%inviter%", result.inviterName()));
                notifyInviter(result, player, accept);
            }
            case ALREADY_MEMBER -> send(player, plugin.getConfigManager().getMsgProtectionMemberAlready());
            case EXPIRED -> send(player, plugin.getConfigManager().getMsgProtectionInviteExpired());
            case NOT_FOR_YOU -> send(player, plugin.getConfigManager().getMsgProtectionInviteNotForYou());
            case INVALID -> send(player, plugin.getConfigManager().getMsgProtectionInviteInvalid());
            case REGION_UNAVAILABLE, SAVE_ERROR -> send(player, plugin.getConfigManager().getMsgProtectionMemberSaveError());
        }
    }

    ProtectionDefinition loadProtection(String rawId) {
        Optional<String> normalizedId = ProtectionValidation.normalizeId(rawId);
        if (normalizedId.isEmpty()) {
            return null;
        }
        String id = normalizedId.get();
        File protectionFile = new File(new File(plugin.getDataFolder(), "protections"), id + ".yml");
        if (!protectionFile.isFile()) {
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(protectionFile);
        ItemStack blockItem = loadBlockItem(yaml);
        if (blockItem == null) {
            warnInvalidProtection(id, "missing or invalid item.block/item.material");
            return null;
        }

        if (!yaml.contains("protection.display-name") && !yaml.contains("display-name")) {
            warnInvalidProtection(id, "missing protection.display-name");
            return null;
        }
        String displayName = yaml.getString("protection.display-name", yaml.getString("display-name"));
        if (displayName == null || displayName.isBlank()) {
            warnInvalidProtection(id, "missing protection.display-name");
            return null;
        }
        Integer radius = loadRadius(yaml, id);
        if (radius == null) {
            return null;
        }
        Double price = loadPrice(yaml, id);
        if (price == null) {
            return null;
        }
        List<String> lore = yaml.getStringList("item.lore");
        if (lore.isEmpty()) {
            lore = plugin.getConfigManager().getProtectionItemLore();
        }
        return new ProtectionDefinition(id, displayName, blockItem, radius, price, lore);
    }

    void sendProtectionLoadFailure(CommandSender sender, String rawId) {
        Optional<String> normalizedId = ProtectionValidation.normalizeId(rawId);
        if (normalizedId.isPresent()) {
            File protectionFile = new File(new File(plugin.getDataFolder(), "protections"), normalizedId.get() + ".yml");
            if (protectionFile.isFile()) {
                send(sender, plugin.getConfigManager().getMsgProtectionInvalidConfig()
                        .replace("%id%", normalizedId.get()));
                return;
            }
        }
        send(sender, replace(plugin.getConfigManager().getMsgProtectionNotFound(), "%id%", rawId));
    }

    ItemStack buildProtectionItem(ProtectionDefinition protection, int amount) {
        ItemStack item = protection.blockItem().clone();
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(protection.displayName()));
            List<Component> lore = protection.lore().stream()
                    .map(line -> line
                            .replace("%id%", protection.id())
                            .replace("%display_name%", protection.displayName())
                            .replace("%material%", protection.material().name())
                            .replace("%price%", String.valueOf(protection.price()))
                            .replace("%radius%", String.valueOf(protection.radius())))
                    .map(MessageUtils::getColoredMessage)
                    .toList();
            meta.lore(lore);
            meta.getPersistentDataContainer().set(protectionIdKey, PersistentDataType.STRING, protection.id());
            meta.getPersistentDataContainer().set(protectionRadiusKey, PersistentDataType.INTEGER, protection.radius());
            item.setItemMeta(meta);
        }
        return item;
    }

    int giveProtectionItems(Player player, ProtectionDefinition protection, int amount) {
        int remaining = amount;
        int delivered = 0;
        int maxStack = protection.blockItem().getMaxStackSize();

        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            Map<Integer, ItemStack> leftovers = player.getInventory()
                    .addItem(buildProtectionItem(protection, stackAmount));
            int leftoverAmount = 0;
            for (ItemStack leftover : leftovers.values()) {
                if (leftover != null) {
                    leftoverAmount += leftover.getAmount();
                }
            }
            delivered += stackAmount - leftoverAmount;

            if (leftoverAmount > 0) {
                break;
            }
            remaining -= stackAmount;
        }

        return delivered;
    }

    boolean canFit(PlayerInventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        int maxStack = item.getMaxStackSize();

        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= maxStack;
            } else if (content.isSimilar(item)) {
                remaining -= maxStack - content.getAmount();
            }

            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    int parseAmount(String rawAmount) {
        try {
            return Integer.parseInt(rawAmount);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    @SuppressWarnings("null")
    List<String> listProtectionIds() {
        File protectionsFolder = new File(plugin.getDataFolder(), "protections");
        File[] files = protectionsFolder.listFiles((ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return List.of();
        }

        return Arrays.stream(files)
                .map(File::getName)
                .map(name -> name.substring(0, name.length() - 4))
                .sorted()
                .toList();
    }

    @SuppressWarnings("null")
    List<String> listProtectionLookups(Player player) {
        return plugin.getProtectionRegionManager().listAccessibleProtections(player).stream()
                .map(ProtectionMenuContext::displayAlias)
                .toList();
    }

    void send(CommandSender sender, String message) {
        sender.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
    }

    void sendError(Player player, String message) {
        send(player, message);
        playError(player);
    }

    void playError(Player player) {
        plugin.getConfigManager().getProtectionFeedbackConfig().errorSound().play(player);
    }

    String replace(String message, String placeholder, String value) {
        return message.replace(placeholder, value);
    }

    private void handleMemberResult(Player player, ProtectionMenuContext context, String targetName, boolean add,
            MemberChangeResult result) {
        switch (result) {
            case SUCCESS -> send(player, (add
                    ? plugin.getConfigManager().getMsgProtectionMemberAdded()
                    : plugin.getConfigManager().getMsgProtectionMemberRemoved())
                    .replace("%player%", targetName)
                    .replace("%alias%", context.displayAlias()));
            case TARGET_IS_OWNER -> send(player, plugin.getConfigManager().getMsgProtectionMemberTargetOwner());
            case ALREADY_MEMBER -> send(player, plugin.getConfigManager().getMsgProtectionMemberAlready());
            case NOT_MEMBER -> send(player, plugin.getConfigManager().getMsgProtectionMemberNotMember());
            case REGION_UNAVAILABLE, SAVE_ERROR -> send(player, plugin.getConfigManager().getMsgProtectionMemberSaveError());
        }
    }

    private void handleInviteCreateResult(Player player, Player target, ProtectionMenuContext context,
            InviteCreateResult result) {
        switch (result.status()) {
            case SUCCESS -> {
                send(player, plugin.getConfigManager().getMsgProtectionInviteSent()
                        .replace("%player%", target.getName())
                        .replace("%alias%", context.displayAlias()));
                sendInviteMessage(target, player.getName(), context, result.inviteId());
            }
            case INVITE_EXISTS -> send(player, plugin.getConfigManager().getMsgProtectionInviteExists()
                    .replace("%player%", target.getName())
                    .replace("%alias%", context.displayAlias()));
            case TARGET_IS_OWNER -> send(player, plugin.getConfigManager().getMsgProtectionMemberTargetOwner());
            case ALREADY_MEMBER -> send(player, plugin.getConfigManager().getMsgProtectionMemberAlready());
            case REGION_UNAVAILABLE, SAVE_ERROR -> send(player, plugin.getConfigManager().getMsgProtectionMemberSaveError());
        }
    }

    private void notifyInviter(InviteResponseResult result, Player invited, boolean accepted) {
        ProtectionMenuContext context = result.context();
        if (context == null) {
            return;
        }
        Player owner;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(context.ownerUuid()));
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (owner == null || !owner.isOnline()) {
            return;
        }
        send(owner, (accepted
                ? plugin.getConfigManager().getMsgProtectionInviteAcceptedOwner()
                : plugin.getConfigManager().getMsgProtectionInviteDeniedOwner())
                .replace("%player%", invited.getName())
                .replace("%alias%", context.displayAlias()));
    }

    private void sendInviteMessage(Player target, String inviterName, ProtectionMenuContext context, long inviteId) {
        String acceptCommand = "/p invite accept " + inviteId;
        String denyCommand = "/p invite deny " + inviteId;
        for (String rawLine : plugin.getConfigManager().getProtectionInviteLines()) {
            String line = rawLine
                    .replace("%inviter%", inviterName)
                    .replace("%alias%", context.displayAlias())
                    .replace("%region%", context.regionId())
                    .replace("%invite_id%", String.valueOf(inviteId));
            if (line.contains("%actions%") || containsConfiguredInviteActions(line)) {
                target.sendMessage(inviteActionComponent(
                        line.contains("%actions%") ? line : "%actions%",
                        acceptCommand, denyCommand));
            } else {
                target.sendMessage(MessageUtils.getColoredMessage(centerLine(line)));
            }
        }
    }

    private Component inviteActionComponent(String template, String acceptCommand, String denyCommand) {
        String acceptLabel = plugin.getConfigManager().getProtectionInviteAcceptLabel();
        String denyLabel = plugin.getConfigManager().getProtectionInviteDenyLabel();
        String separator = plugin.getConfigManager().getProtectionInviteActionSeparator();
        String actionLine = acceptLabel + separator + denyLabel;
        String renderedLine = template.replace("%actions%", actionLine);
        String[] parts = template.split("%actions%", 2);
        String prefix = parts.length > 0 ? parts[0] : "";
        String suffix = parts.length > 1 ? parts[1] : "";
        Component accept = MessageUtils.getColoredMessage(acceptLabel)
                .clickEvent(ClickEvent.runCommand(acceptCommand))
                .hoverEvent(HoverEvent.showText(
                        MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionInviteAcceptHover())));
        Component deny = MessageUtils.getColoredMessage(denyLabel)
                .clickEvent(ClickEvent.runCommand(denyCommand))
                .hoverEvent(HoverEvent.showText(
                        MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionInviteDenyHover())));
        return Component.text(centerPrefix(renderedLine))
                .append(MessageUtils.getColoredMessage(prefix))
                .append(accept)
                .append(MessageUtils.getColoredMessage(separator))
                .append(deny)
                .append(MessageUtils.getColoredMessage(suffix));
    }

    private boolean containsConfiguredInviteActions(String line) {
        String plainLine = stripColors(line);
        return plainLine.contains(stripColors(plugin.getConfigManager().getProtectionInviteAcceptLabel()))
                && plainLine.contains(stripColors(plugin.getConfigManager().getProtectionInviteDenyLabel()));
    }

    private String centerLine(String line) {
        return centerPrefix(line) + line;
    }

    private String centerPrefix(String line) {
        String plain = stripColors(line);
        int spaces = Math.max(0, (58 - plain.length()) / 2);
        return " ".repeat(spaces);
    }

    private String stripColors(String line) {
        return line.replaceAll("(?i)&#[0-9a-f]{6}", "").replaceAll("&.", "");
    }

    private String formatCreatedAt(long createdAtMillis) {
        if (createdAtMillis <= 0) {
            return plugin.getConfigManager().getMsgProtectionInfoUnknownDate();
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(createdAtMillis));
    }

    private String displayName(String name, String uuid) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return uuid == null || uuid.isBlank() ? "-" : uuid;
    }

    private String formatCoord(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private ItemStack loadBlockItem(YamlConfiguration yaml) {
        ItemStack blockItem = yaml.getItemStack("item.block");
        if (blockItem == null) {
            blockItem = yaml.getItemStack("block.item");
        }
        if (blockItem != null && blockItem.getType().isBlock() && !blockItem.getType().isAir()) {
            blockItem = blockItem.clone();
            blockItem.setAmount(1);
            return blockItem;
        }

        String rawMaterial = yaml.getString("item.material", yaml.getString("block.material", ""));
        if (rawMaterial.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(rawMaterial);
        if (material == null || !material.isBlock() || material.isAir()) {
            return null;
        }
        return new ItemStack(material);
    }

    private Integer loadRadius(YamlConfiguration yaml, String id) {
        String path = yaml.contains("protection.radius") ? "protection.radius" : "radius";
        if (!yaml.contains(path)) {
            warnInvalidProtection(id, "missing protection.radius");
            return null;
        }

        int radius = yaml.getInt(path);
        if (!ProtectionValidation.isValidRadius(radius)) {
            warnInvalidProtection(id, "radius must be between " + ProtectionValidation.MIN_RADIUS
                    + " and " + ProtectionValidation.MAX_RADIUS);
            return null;
        }
        return radius;
    }

    private Double loadPrice(YamlConfiguration yaml, String id) {
        double price = ProtectionCommandSupport.loadProtectionPrice(yaml);
        if (!ProtectionValidation.isValidPrice(price)) {
            warnInvalidProtection(id, "price must be finite and between " + ProtectionValidation.MIN_PRICE
                    + " and " + ProtectionValidation.MAX_PRICE);
            return null;
        }
        return price;
    }

    private void warnInvalidProtection(String id, String reason) {
        plugin.getLogger().warning("Protection '" + id + "' has invalid YAML: " + reason + ".");
    }

    record ProtectionDefinition(String id, String displayName, ItemStack blockItem, int radius, double price,
            List<String> lore) {
        private Material material() {
            return blockItem.getType();
        }
    }
}
