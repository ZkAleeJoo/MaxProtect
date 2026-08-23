package org.zkaleejoo.protection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.ClickType;
import org.zkaleejoo.ArgosProtect;
import org.zkaleejoo.config.MainConfigManager.MenuDisplayConfig;
import org.zkaleejoo.config.MainConfigManager.MenuItemConfig;
import org.zkaleejoo.config.MainConfigManager.MenuNavigationConfig;
import org.zkaleejoo.permissions.ArgosProtectPermissions;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMemberInfo;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionMenuContext;
import org.zkaleejoo.protection.ProtectionRegionManager.MemberChangeResult;
import org.zkaleejoo.protection.ProtectionRegionManager.ProtectionAdminState;
import org.zkaleejoo.protection.ProtectionRegionManager.RemoveResult;
import org.zkaleejoo.utils.ItemBuilder;
import org.zkaleejoo.utils.ArgosProtectHolder;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.protection.storage.ProtectionStorage.ProtectionEventLog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProtectionMenuManager {

    private static final String HOME_MENU_ID = "PROTECTION_HOME_MENU";
    private static final String MEMBERS_MENU_ID = "PROTECTION_MEMBERS_MENU";
    private static final String SETTINGS_MENU_ID = "PROTECTION_SETTINGS_MENU";
    private static final String REMOVE_CONFIRM_MENU_ID = "PROTECTION_REMOVE_CONFIRM";
    private static final String ADMIN_PLACED_MENU_ID = "PROTECTION_ADMIN_PLACED";
    private static final String LOGS_MENU_ID = "PROTECTION_LOGS_MENU";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final ArgosProtect plugin;
    private final NamespacedKey menuRegionKey;
    private final NamespacedKey menuMemberKey;
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();

    public ProtectionMenuManager(ArgosProtect plugin) {
        this.plugin = plugin;
        this.menuRegionKey = new NamespacedKey(plugin, "menu_region");
        this.menuMemberKey = new NamespacedKey(plugin, "menu_member");
    }

    public void openMenu(Player player) {
        ProtectionMenuContext context = plugin.getProtectionRegionManager().findManageableProtectionAt(player);
        if (context == null) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder("PROTECTION_MENU"),
                plugin.getConfigManager().getProtectionMenuSize(),
                MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionMenuTitle()));

        ItemStack filler = new ItemBuilder(plugin.getConfigManager().getProtectionMenuFillerMaterial())
                .setName(plugin.getConfigManager().getProtectionMenuFillerName())
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        for (MenuItemConfig item : protectionMenuItems()) {
            for (int slot : item.slots()) {
                if (slot >= inv.getSize()) {
                    continue;
                }
                inv.setItem(slot, buildMenuItem(item, context, player));
            }
        }

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void openInfoMenu(Player player, ProtectionMenuContext context) {
        if (context == null) {
            return;
        }

        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder("INFO_MENU"),
                plugin.getConfigManager().getProtectionInfoMenuSize(),
                MessageUtils.getColoredMessage(applyPlaceholders(plugin.getConfigManager().getProtectionInfoMenuTitle(), context, player)));

        ItemStack filler = new ItemBuilder(plugin.getConfigManager().getProtectionInfoMenuFillerMaterial())
                .setName(plugin.getConfigManager().getProtectionInfoMenuFillerName())
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        for (MenuItemConfig item : plugin.getConfigManager().getProtectionInfoMenuItems()) {
            for (int slot : item.slots()) {
                if (slot >= inv.getSize()) {
                    continue;
                }
                inv.setItem(slot, buildMenuItem(item, context, player));
            }
        }

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleInfoMenuClick(Player player, int slot) {
        handleStaticMenuClick(player, slot, plugin.getConfigManager().getProtectionInfoMenuItems());
    }

    public void handleMenuClick(Player player, int slot) {
        MenuItemConfig clicked = protectionMenuItems().stream()
                .filter(item -> item.hasSlot(slot))
                .filter(item -> !item.decorative())
                .findFirst()
                .orElse(null);

        if (clicked == null) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (clicked.id().equalsIgnoreCase("settings")) {
            openSettingsMenu(player);
            return;
        }

        if (clicked.id().equalsIgnoreCase("members")) {
            openMembersMenu(player);
            return;
        }

        ProtectionMenuContext context = plugin.getProtectionRegionManager().findManageableProtectionAt(player);
        if (context == null) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        if (clicked.id().equalsIgnoreCase("remove")) {
            openRemoveConfirmMenu(player, context);
            return;
        }

        play(player, Sound.UI_BUTTON_CLICK);
    }

    private List<MenuItemConfig> protectionMenuItems() {
        return plugin.getConfigManager().getProtectionMenuItems().stream()
                .filter(item -> !item.id().equalsIgnoreCase("set-home"))
                .toList();
    }

    public void openRemoveConfirmMenu(Player player, ProtectionMenuContext context) {
        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(REMOVE_CONFIRM_MENU_ID, context.regionId()),
                plugin.getConfigManager().getProtectionRemoveConfirmMenuSize(),
                MessageUtils.getColoredMessage(applyPlaceholders(
                        plugin.getConfigManager().getProtectionRemoveConfirmMenuTitle(), context, player)));

        fill(inv, plugin.getConfigManager().getProtectionRemoveConfirmMenuFillerMaterial(),
                plugin.getConfigManager().getProtectionRemoveConfirmMenuFillerName());
        setStaticItems(inv, plugin.getConfigManager().getProtectionRemoveConfirmMenuItems(), context, player);

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleRemoveConfirmClick(Player player, int slot, ArgosProtectHolder holder) {
        ProtectionMenuContext context = plugin.getProtectionRegionManager()
                .findAccessibleProtection(player, holder.getContextId());
        if (context == null) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionNotAccessible()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        MenuItemConfig item = findMenuItem(plugin.getConfigManager().getProtectionRemoveConfirmMenuItems(), slot);
        if (item == null || item.decorative()) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (item.id().equalsIgnoreCase("cancel")) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionRemoveCancelled()));
            play(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        if (!item.id().equalsIgnoreCase("confirm")) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        RemoveResult result = plugin.getProtectionRegionManager().removeProtection(player, context);
        player.closeInventory();
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionRemoved()
                                .replace("%protection%", context.protectionId())
                                .replace("%region%", context.regionId())));
                plugin.getConfigManager().getProtectionFeedbackConfig().removeSound().play(player);
            }
            case NOT_OWNER -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionRemoveNotOwner()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case INVENTORY_FULL -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionNoInventorySpace()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case WORLD_UNAVAILABLE -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionWorldUnavailable()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case INVALID_CONFIG -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionInvalidConfig()
                                .replace("%id%", context.protectionId())));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case REGION_UNAVAILABLE, SAVE_ERROR -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionRemoveSaveError()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
        }
    }

    public void openHomeMenu(Player player) {
        openHomeMenu(player, 0);
    }

    public void openHomeMenu(Player player, int page) {
        List<ProtectionMenuContext> protections = plugin.getProtectionRegionManager().listAccessibleProtections(player);
        if (protections.isEmpty()) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionHomeEmpty()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        List<Integer> contentSlots = plugin.getConfigManager().getProtectionHomeMenuContentSlots();
        int maxPage = maxPage(protections.size(), contentSlots.size());
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(HOME_MENU_ID, String.valueOf(safePage)),
                plugin.getConfigManager().getProtectionHomeMenuSize(),
                MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionHomeMenuTitle()));

        fill(inv, plugin.getConfigManager().getProtectionHomeMenuFillerMaterial(),
                plugin.getConfigManager().getProtectionHomeMenuFillerName());
        setStaticItems(inv, plugin.getConfigManager().getProtectionHomeMenuItems(), null, player);
        int start = safePage * contentSlots.size();
        int end = Math.min(protections.size(), start + contentSlots.size());
        for (int index = start; index < end; index++) {
            inv.setItem(contentSlots.get(index - start), buildHomeItem(player, protections.get(index)));
        }
        setNavigation(inv, safePage, maxPage,
                plugin.getConfigManager().getProtectionHomeMenuPreviousItem(),
                plugin.getConfigManager().getProtectionHomeMenuNextItem());

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleHomeMenuClick(Player player, int slot, ItemStack clicked, ArgosProtectHolder holder) {
        int page = parsePage(holder.getContextId());
        if (slot == plugin.getConfigManager().getProtectionHomeMenuPreviousItem().slot()) {
            openHomeMenu(player, page - 1);
            return;
        }
        if (slot == plugin.getConfigManager().getProtectionHomeMenuNextItem().slot()) {
            openHomeMenu(player, page + 1);
            return;
        }

        String regionId = readString(clicked, menuRegionKey);
        if (regionId == null) {
            handleStaticMenuClick(player, slot, plugin.getConfigManager().getProtectionHomeMenuItems());
            return;
        }

        ProtectionMenuContext context = plugin.getProtectionRegionManager().findAccessibleProtection(player, regionId);
        if (context == null) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionNotAccessible()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        int cooldownSeconds = plugin.getConfigManager().getProtectionHomeCooldownSeconds();
        long now = System.currentTimeMillis();
        long nextUse = homeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (cooldownSeconds > 0 && nextUse > now) {
            long remainingSeconds = Math.max(1L, (nextUse - now + 999L) / 1000L);
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionHomeCooldown()
                            .replace("%seconds%", String.valueOf(remainingSeconds))));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        Optional<Location> home = plugin.getProtectionRegionManager().homeLocation(context);
        if (home.isEmpty()) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionWorldUnavailable()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        if (cooldownSeconds > 0) {
            homeCooldowns.put(player.getUniqueId(), now + cooldownSeconds * 1000L);
        }
        player.closeInventory();
        plugin.getSchedulerUtils().teleportEntity(player, home.get());
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgProtectionTeleportSuccess()
                        .replace("%alias%", context.displayAlias())));
        play(player, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    public void openAdminPlacedMenu(Player player) {
        openAdminPlacedMenu(player, 0);
    }

    public void openAdminPlacedMenu(Player player, int page) {
        List<ProtectionMenuContext> protections = plugin.getProtectionRegionManager().listPlacedProtections();
        List<Integer> contentSlots = plugin.getConfigManager().getAdminPlacedMenuContentSlots();
        int maxPage = maxPage(protections.size(), contentSlots.size());
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(ADMIN_PLACED_MENU_ID, String.valueOf(safePage)),
                plugin.getConfigManager().getAdminPlacedMenuSize(),
                MessageUtils.getColoredMessage(applyAdminPlacedPlaceholders(
                        plugin.getConfigManager().getAdminPlacedMenuTitle(), safePage, maxPage, protections.size())));

        fill(inv, plugin.getConfigManager().getAdminPlacedMenuFillerMaterial(),
                plugin.getConfigManager().getAdminPlacedMenuFillerName());
        setAdminPlacedStaticItems(inv, safePage, maxPage, protections.size());

        int start = safePage * contentSlots.size();
        int end = Math.min(protections.size(), start + contentSlots.size());
        for (int index = start; index < end; index++) {
            inv.setItem(contentSlots.get(index - start), buildAdminPlacedItem(player, protections.get(index)));
        }

        setNavigation(inv, safePage, maxPage,
                plugin.getConfigManager().getAdminPlacedMenuPreviousItem(),
                plugin.getConfigManager().getAdminPlacedMenuNextItem());

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleAdminPlacedMenuClick(Player player, int slot, ItemStack clicked, ArgosProtectHolder holder) {
        int page = parsePage(holder.getContextId());
        if (slot == plugin.getConfigManager().getAdminPlacedMenuPreviousItem().slot()) {
            openAdminPlacedMenu(player, page - 1);
            return;
        }
        if (slot == plugin.getConfigManager().getAdminPlacedMenuNextItem().slot()) {
            openAdminPlacedMenu(player, page + 1);
            return;
        }

        String regionId = readString(clicked, menuRegionKey);
        if (regionId == null) {
            handleStaticMenuClick(player, slot, plugin.getConfigManager().getAdminPlacedMenuItems());
            return;
        }

        sendAdminDebug(player, regionId);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    private ItemStack buildAdminPlacedItem(Player player, ProtectionMenuContext context) {
        ProtectionAdminState state = plugin.getProtectionRegionManager().adminState(context);
        Material fallbackMaterial = state.worldGuardRegion() && state.protectionYaml() && state.stoneBlock()
                ? plugin.getConfigManager().getAdminPlacedHealthyMaterial()
                : plugin.getConfigManager().getAdminPlacedBrokenMaterial();
        MenuDisplayConfig display = plugin.getConfigManager().getAdminPlacedMenuItem();
        ItemStack item = plugin.getConfigManager().isAdminPlacedUseProtectionBlock()
                ? plugin.getProtectionRegionManager().displayBlockItem(context)
                        .orElseGet(() -> new ItemBuilder(fallbackMaterial)
                                .setHeadTexture(display.headTexture())
                                .build())
                : new ItemBuilder(fallbackMaterial)
                        .setHeadTexture(display.headTexture())
                        .build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(
                    applyAdminPlacedItemPlaceholders(display.name(), context, player, state)));
            if (!display.lore().isEmpty()) {
                meta.lore(display.lore().stream()
                        .map(line -> applyAdminPlacedItemPlaceholders(line, context, player, state))
                        .map(MessageUtils::getColoredMessage)
                        .toList());
            }
            item.setItemMeta(meta);
        }
        tag(item, menuRegionKey, context.regionId());
        return item;
    }

    private void sendAdminDebug(Player player, String regionId) {
        for (String line : plugin.getProtectionRegionManager().debugProtection(regionId)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + line));
        }
    }

    public void openMembersMenu(Player player) {
        ProtectionMenuContext context = plugin.getProtectionRegionManager().findManageableProtectionAt(player);
        if (context == null || !plugin.getProtectionRegionManager().canManageMembers(player, context)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }
        openMembersMenu(player, context, 0);
    }

    public void openMembersMenu(Player player, ProtectionMenuContext context, int page) {
        List<ProtectionMemberInfo> members = plugin.getProtectionRegionManager().listProtectionMembers(context);
        List<Integer> contentSlots = plugin.getConfigManager().getProtectionMembersMenuContentSlots();
        int maxPage = maxPage(members.size(), contentSlots.size());
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(MEMBERS_MENU_ID,
                context.regionId() + "|" + safePage), plugin.getConfigManager().getProtectionMembersMenuSize(),
                MessageUtils.getColoredMessage(applyPlaceholders(plugin.getConfigManager().getProtectionMembersMenuTitle(),
                        context, player)));

        fill(inv, plugin.getConfigManager().getProtectionMembersMenuFillerMaterial(),
                plugin.getConfigManager().getProtectionMembersMenuFillerName());
        setStaticItems(inv, plugin.getConfigManager().getProtectionMembersMenuItems(), context, player);
        int start = safePage * contentSlots.size();
        int end = Math.min(members.size(), start + contentSlots.size());
        for (int index = start; index < end; index++) {
            inv.setItem(contentSlots.get(index - start), buildMemberItem(player, members.get(index), context));
        }
        setNavigation(inv, safePage, maxPage,
                plugin.getConfigManager().getProtectionMembersMenuPreviousItem(),
                plugin.getConfigManager().getProtectionMembersMenuNextItem());

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleMembersMenuClick(Player player, int slot, ItemStack clicked, ClickType click,
            ArgosProtectHolder holder) {
        String[] contextParts = holder.getContextId().split("\\|", 2);
        if (contextParts.length != 2) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        ProtectionMenuContext context = plugin.getProtectionRegionManager()
                .findAccessibleProtection(player, contextParts[0]);
        if (context == null || !plugin.getProtectionRegionManager().canManageMembers(player, context)) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        int page = parsePage(contextParts[1]);
        if (slot == plugin.getConfigManager().getProtectionMembersMenuPreviousItem().slot()) {
            openMembersMenu(player, context, page - 1);
            return;
        }
        if (slot == plugin.getConfigManager().getProtectionMembersMenuNextItem().slot()) {
            openMembersMenu(player, context, page + 1);
            return;
        }

        String memberId = readString(clicked, menuMemberKey);
        if (memberId == null) {
            handleStaticMenuClick(player, slot, plugin.getConfigManager().getProtectionMembersMenuItems());
            return;
        }
        handleMemberItemClick(player, context, memberId, click);
    }

    private void handleMemberItemClick(Player player, ProtectionMenuContext context, String rawMemberId,
            ClickType click) {
        UUID memberId = parseUuid(rawMemberId);
        if (memberId == null || memberId.equals(player.getUniqueId())) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        ProtectionMemberInfo member = plugin.getProtectionRegionManager().listProtectionMembers(context).stream()
                .filter(candidate -> candidate.uuid().equals(memberId))
                .findFirst() 
                .orElse(null);
        if (member == null || member.owner()) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (click.isShiftClick() && click.isRightClick()) {
            MemberChangeResult result = plugin.getProtectionRegionManager().removeMember(context, memberId);
            handleMemberResult(player, context, member.name(), result,
                    plugin.getConfigManager().getMsgProtectionMemberRemoved());
            return;
        }

        if (click.isLeftClick() && member.rank() == ProtectionMemberRank.MEMBER) {
            MemberChangeResult result = plugin.getProtectionRegionManager()
                    .setMemberRank(context, memberId, ProtectionMemberRank.ADMIN);
            handleMemberResult(player, context, member.name(), result,
                    plugin.getConfigManager().getMsgProtectionMemberPromoted());
            return;
        }

        if (click.isRightClick() && member.rank() == ProtectionMemberRank.ADMIN) {
            MemberChangeResult result = plugin.getProtectionRegionManager()
                    .setMemberRank(context, memberId, ProtectionMemberRank.MEMBER);
            handleMemberResult(player, context, member.name(), result,
                    plugin.getConfigManager().getMsgProtectionMemberDemoted());
            return;
        }

        play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    private void handleMemberResult(Player player, ProtectionMenuContext context, String memberName,
            MemberChangeResult result, String successMessage) {
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + successMessage
                                .replace("%player%", memberName)
                                .replace("%alias%", context.displayAlias())));
                openMembersMenu(player, context, 0);
                play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            }
            case TARGET_IS_OWNER -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionMemberTargetOwner()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case ALREADY_MEMBER -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionMemberAlready()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case NOT_MEMBER -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionMemberNotMember()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
            case REGION_UNAVAILABLE, SAVE_ERROR -> {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionMemberSaveError()));
                play(player, Sound.ENTITY_VILLAGER_NO);
            }
        }
    }

    public void openSettingsMenu(Player player) {
        openSettingsMenu(player, 0, true);
    }

    public void openSettingsMenu(Player player, int page) {
        openSettingsMenu(player, page, true);
    }

    private void openSettingsMenu(Player player, int page, boolean playOpenSound) {
        ProtectionMenuContext context = plugin.getProtectionRegionManager().findManageableProtectionAt(player);
        if (context == null || !plugin.getProtectionRegionManager().canChangeFlags(player, context)) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        int maxPage = plugin.getConfigManager().getProtectionSettingsMenuItems().stream()
                .mapToInt(item -> item.page())
                .max()
                .orElse(0);
        int safePage = Math.max(0, Math.min(page, maxPage));

        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(SETTINGS_MENU_ID, String.valueOf(safePage)),
                plugin.getConfigManager().getProtectionSettingsMenuSize(),
                MessageUtils.getColoredMessage(plugin.getConfigManager().getProtectionSettingsMenuTitle()
                        .replace("%page%", String.valueOf(safePage + 1))
                        .replace("%max_page%", String.valueOf(maxPage + 1))));

        ItemStack filler = new ItemBuilder(plugin.getConfigManager().getProtectionSettingsMenuFillerMaterial())
                .setName(plugin.getConfigManager().getProtectionSettingsMenuFillerName())
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        for (MenuItemConfig item : plugin.getConfigManager().getProtectionSettingsMenuItems()) {
            if (item.page() != -1 && item.page() != safePage) {
                continue;
            }

            if (item.id().equalsIgnoreCase("back")) {
                for (int slot : item.slots()) {
                    if (slot < inv.getSize()) {
                        inv.setItem(slot, buildConfiguredItem(item, null, context, player));
                    }
                }
                continue;
            }

            ProtectionFlagDefinition definition = ProtectionFlagDefinition.fromId(item.id()).orElse(null);
            if (definition == null) {
                continue;
            }
            for (int slot : item.slots()) {
                if (slot < inv.getSize()) {
                    inv.setItem(slot, buildConfiguredItem(item,
                            plugin.getProtectionRegionManager().flagLabel(context, definition), context, player));
                }
            }
        }

        setNavigation(inv, safePage, maxPage,
                plugin.getConfigManager().getProtectionSettingsMenuPreviousItem(),
                plugin.getConfigManager().getProtectionSettingsMenuNextItem());

        player.openInventory(inv);
        if (playOpenSound) {
            play(player, Sound.UI_BUTTON_CLICK);
        }
    }

    public void handleSettingsMenuClick(Player player, int slot, ArgosProtectHolder holder) {
        int page = parsePage(holder.getContextId());
        
        if (slot == plugin.getConfigManager().getProtectionSettingsMenuPreviousItem().slot()) {
            openSettingsMenu(player, page - 1);
            return;
        }
        if (slot == plugin.getConfigManager().getProtectionSettingsMenuNextItem().slot()) {
            openSettingsMenu(player, page + 1);
            return;
        }

        MenuItemConfig clicked = plugin.getConfigManager().getProtectionSettingsMenuItems().stream()
                .filter(item -> item.hasSlot(slot))
                .filter(item -> item.page() == -1 || item.page() == page)
                .filter(item -> !item.decorative())
                .findFirst()
                .orElse(null);
        if (clicked == null) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (clicked.id().equalsIgnoreCase("back")) {
            openMenu(player);
            return;
        }

        ProtectionFlagDefinition definition = ProtectionFlagDefinition.fromId(clicked.id()).orElse(null);
        if (definition == null) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        ProtectionMenuContext context = plugin.getProtectionRegionManager().findManageableProtectionAt(player);
        if (context == null || !plugin.getProtectionRegionManager().canChangeFlags(player, context)) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        ProtectionFlagLevel current = plugin.getProtectionRegionManager().getProtectionFlagLevel(context, definition);
        ProtectionFlagLevel next = definition.next(current);
        boolean saved = plugin.getProtectionRegionManager().setProtectionFlag(context, definition, next);
        if (!saved) {
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgProtectionFlagUpdated()
                        .replace("%flag%", clicked.name())
                        .replace("%state%", plugin.getConfigManager().getProtectionFlagLabel(next,
                                definition.mode() == ProtectionFlagMode.TOGGLE))));
        openSettingsMenu(player, page, false);
        plugin.getConfigManager().getProtectionFeedbackConfig().flagChangeSound().play(player);
    }

    private ItemStack buildMenuItem(MenuItemConfig item, ProtectionMenuContext context, Player player) {
        List<String> lore = item.lore().stream()
                .map(line -> applyPlaceholders(line, context, player))
                .toList();

        ItemBuilder builder = new ItemBuilder(resolveMaterial(item))
                .setName(applyPlaceholders(item.name(), context, player));

        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }

        if (item.id().equalsIgnoreCase("owners")) {
            builder.setHeadOwner(parseUuid(context.ownerUuid()));
        } else {
            builder.setHeadTexture(item.headTexture());
        }

        return builder.build();
    }

    private ItemStack buildHomeItem(Player player, ProtectionMenuContext context) {
        ProtectionMemberRank playerRank = plugin.getProtectionRegionManager().rankFor(player, context);
        String rank = switch (playerRank == null ? ProtectionMemberRank.ADMIN : playerRank) {
            case OWNER -> plugin.getConfigManager().getProtectionHomeRoleOwner();
            case ADMIN -> playerRank == null && player.hasPermission(ArgosProtectPermissions.ADMIN)
                    ? plugin.getConfigManager().getProtectionHomeRoleAdmin()
                    : plugin.getConfigManager().getProtectionMembersRoleAdmin();
            case MEMBER -> plugin.getConfigManager().getProtectionHomeRoleMember();
        };
        MenuDisplayConfig display = plugin.getConfigManager().getProtectionHomeMenuItem();
        List<String> lore = display.lore().stream()
                .map(line -> applyHomePlaceholders(line, context, player, rank))
                .toList();
        ItemStack item = plugin.getProtectionRegionManager().displayBlockItem(context)
                .orElseGet(() -> new ItemBuilder(display.material())
                        .setHeadTexture(display.headTexture())
                        .build());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(
                    applyHomePlaceholders(display.name(), context, player, rank)));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(MessageUtils::getColoredMessage)
                        .toList());
            }
            item.setItemMeta(meta);
        }
        tag(item, menuRegionKey, context.regionId());
        return item;
    }

    private ItemStack buildMemberItem(Player player, ProtectionMemberInfo member, ProtectionMenuContext context) {
        String rank = switch (member.rank()) {
            case OWNER -> plugin.getConfigManager().getProtectionMembersRoleOwner();
            case ADMIN -> plugin.getConfigManager().getProtectionMembersRoleAdmin();
            case MEMBER -> plugin.getConfigManager().getProtectionMembersRoleMember();
        };
        MenuDisplayConfig display = plugin.getConfigManager().getProtectionMembersMenuItem();
        List<String> lore = display.lore().stream()
                .map(line -> applyMemberPlaceholders(line, context, player, member, rank))
                .toList();
        ItemBuilder builder = new ItemBuilder(display.material())
                .setName(applyMemberPlaceholders(display.name(), context, player, member, rank));
        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }
        ItemStack item = builder
                .setHeadTexture(display.headTexture())
                .setHeadOwner(member.uuid())
                .build();
        tag(item, menuMemberKey, member.uuid().toString());
        return item;
    }

    private void fill(Inventory inv, Material material, String name) {
        ItemStack filler = new ItemBuilder(material)
                .setName(name)
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private void setNavigation(Inventory inv, int page, int maxPage, MenuNavigationConfig previousItem,
            MenuNavigationConfig nextItem) {
        if (page > 0) {
            setNavigationItem(inv, previousItem, page);
        }
        if (page < maxPage) {
            setNavigationItem(inv, nextItem, page + 2);
        }
    }

    private void setNavigationItem(Inventory inv, MenuNavigationConfig item, int displayPage) {
        if (item.slot() < 0 || item.slot() >= inv.getSize()) {
            return;
        }
        List<String> lore = item.lore().stream()
                .map(line -> line.replace("%page%", String.valueOf(displayPage)))
                .toList();
        ItemBuilder builder = new ItemBuilder(item.material())
                .setName(item.name().replace("%page%", String.valueOf(displayPage)));
        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }
        inv.setItem(item.slot(), builder
                .setHeadTexture(item.headTexture())
                .build());
    }

    private void setStaticItems(Inventory inv, List<MenuItemConfig> items, ProtectionMenuContext context,
            Player player) {
        for (MenuItemConfig item : items) {
            ItemStack stack = buildStaticItem(item, context, player);
            for (int slot : item.slots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, stack.clone());
                }
            }
        }
    }

    private void setAdminPlacedStaticItems(Inventory inv, int page, int maxPage, int total) {
        for (MenuItemConfig item : plugin.getConfigManager().getAdminPlacedMenuItems()) {
            List<String> lore = item.lore().stream()
                    .map(line -> applyAdminPlacedPlaceholders(line, page, maxPage, total))
                    .toList();
            ItemBuilder builder = new ItemBuilder(item.material())
                    .setName(applyAdminPlacedPlaceholders(item.name(), page, maxPage, total));
            if (!lore.isEmpty()) {
                builder.setLore(lore.toArray(new String[0]));
            }
            ItemStack stack = builder
                    .hideAttributes()
                    .setHeadTexture(item.headTexture())
                    .build();
            for (int slot : item.slots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, stack.clone());
                }
            }
        }
    }

    private ItemStack buildStaticItem(MenuItemConfig item, ProtectionMenuContext context, Player player) {
        List<String> lore = item.lore().stream()
                .map(line -> applyOptionalPlaceholders(line, context, player))
                .toList();

        ItemBuilder builder = new ItemBuilder(item.material())
                .setName(applyOptionalPlaceholders(item.name(), context, player));
        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }
        return builder
                .hideAttributes()
                .setHeadTexture(item.headTexture())
                .build();
    }

    private void handleStaticMenuClick(Player player, int slot, List<MenuItemConfig> items) {
        MenuItemConfig item = findMenuItem(items, slot);
        if (item == null) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (item.decorative()) {
            return;
        }

        if (item.id().equalsIgnoreCase("back") || item.id().equalsIgnoreCase("return")) {
            openMenu(player);
            return;
        }

        play(player, Sound.UI_BUTTON_CLICK);
    }

    private MenuItemConfig findMenuItem(List<MenuItemConfig> items, int slot) {
        return items.stream()
                .filter(item -> item.hasSlot(slot))
                .findFirst()
                .orElse(null);
    }

    private void tag(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    private String readString(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private int parsePage(String rawPage) {
        try {
            return Integer.parseInt(rawPage);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private int maxPage(int itemCount, int contentSlotCount) {
        if (itemCount <= 0 || contentSlotCount <= 0) {
            return 0;
        }
        return Math.max(0, (itemCount - 1) / contentSlotCount);
    }

    private String formatDate(long createdAtMillis) {
        if (createdAtMillis <= 0) {
            return plugin.getConfigManager().getProtectionMembersUnknownDate();
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(createdAtMillis));
    }

    private Material resolveMaterial(MenuItemConfig item) {
        if (item.id().equalsIgnoreCase("members")) {
            return Material.PLAYER_HEAD;
        }
        return item.material();
    }

    private String applyPlaceholders(String text, ProtectionMenuContext context, Player player) {
        return text
                .replace("%protection%", context.protectionId())
                .replace("%protection_id%", context.protectionId())
                .replace("%alias%", context.displayAlias())
                .replace("%display_name%", context.displayName())
                .replace("%region%", context.regionId())
                .replace("%owner%", context.ownerName())
                .replace("%owners%", String.valueOf(context.ownerCount()))
                .replace("%members%", String.valueOf(context.memberCount()))
                .replace("%size%", context.displaySize() + "x" + context.displaySize())
                .replace("%radius%", String.valueOf(context.radius()))
                .replace("%created_at%", formatDate(context.createdAtMillis()))
                .replace("%world%", context.worldName())
                .replace("%x%", String.valueOf(player.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(player.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(player.getLocation().getBlockZ()))
                .replace("%stone_x%", String.valueOf(context.stoneX()))
                .replace("%stone_y%", String.valueOf(context.stoneY()))
                .replace("%stone_z%", String.valueOf(context.stoneZ()))
                .replace("%home_custom%", String.valueOf(context.customHome()))
                .replace("%home_x%", formatCoordinate(context.homeX()))
                .replace("%home_y%", formatCoordinate(context.homeY()))
                .replace("%home_z%", formatCoordinate(context.homeZ()))
                .replace("%min_x%", String.valueOf(context.minX()))
                .replace("%min_y%", String.valueOf(context.minY()))
                .replace("%min_z%", String.valueOf(context.minZ()))
                .replace("%max_x%", String.valueOf(context.maxX()))
                .replace("%max_y%", String.valueOf(context.maxY()))
                .replace("%max_z%", String.valueOf(context.maxZ()));
    }

    private String formatCoordinate(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String statusLabel(boolean value) {
        return value ? plugin.getConfigManager().getAdminPlacedStatusOk()
                : plugin.getConfigManager().getAdminPlacedStatusMissing();
    }

    private String applyAdminPlacedPlaceholders(String text, int page, int maxPage, int total) {
        return text
                .replace("%page%", String.valueOf(page + 1))
                .replace("%max_page%", String.valueOf(maxPage + 1))
                .replace("%total%", String.valueOf(total));
    }

    private String applyAdminPlacedItemPlaceholders(String text, ProtectionMenuContext context, Player player,
            ProtectionAdminState state) {
        return applyPlaceholders(text, context, player)
                .replace("%state_worldguard%", statusLabel(state.worldGuardRegion()))
                .replace("%state_yaml%", statusLabel(state.protectionYaml()))
                .replace("%state_stone%", statusLabel(state.stoneBlock()));
    }

    private String applyOptionalPlaceholders(String text, ProtectionMenuContext context, Player player) {
        if (context != null) {
            return applyPlaceholders(text, context, player);
        }
        return text
                .replace("%x%", String.valueOf(player.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(player.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
    }

    private String applyHomePlaceholders(String text, ProtectionMenuContext context, Player player, String role) {
        return applyPlaceholders(text, context, player)
                .replace("%role%", role);
    }

    private String applyMemberPlaceholders(String text, ProtectionMenuContext context, Player player,
            ProtectionMemberInfo member, String role) {
        return applyPlaceholders(text, context, player)
                .replace("%member%", member.name())
                .replace("%member_uuid%", member.uuid().toString())
                .replace("%role%", role)
                .replace("%created_date%", formatDate(context.createdAtMillis()));
    }

    private UUID parseUuid(String rawUuid) {
        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void play(Player player, Sound sound) {
        if (sound == Sound.ENTITY_VILLAGER_NO) {
            plugin.getConfigManager().getProtectionFeedbackConfig().errorSound().play(player);
            return;
        }
        player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
    }

    private ItemStack buildConfiguredItem(MenuItemConfig item, String state, ProtectionMenuContext context,
            Player player) {
        List<String> lore = item.lore().stream()
                .map(line -> applyPlaceholders(line, context, player)
                        .replace("%state%", state == null ? "" : state))
                .toList();

        ItemBuilder builder = new ItemBuilder(item.material())
                .setName(applyPlaceholders(item.name(), context, player).replace("%state%", state == null ? "" : state));
        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }
        return builder
                .hideAttributes()
                .setHeadTexture(item.headTexture())
                .build();
    }




    public void openLogsMenu(Player player, ProtectionMenuContext context, int page) {
        List<ProtectionEventLog> events = plugin.getProtectionRegionManager().recentEvents(context, 100);
        List<Integer> contentSlots = plugin.getConfigManager().getProtectionLogsMenuContentSlots();
        int maxPage = maxPage(events.size(), contentSlots.size());
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder(LOGS_MENU_ID,
                context.regionId() + "|" + safePage), plugin.getConfigManager().getProtectionLogsMenuSize(),
                MessageUtils.getColoredMessage(applyPlaceholders(plugin.getConfigManager().getProtectionLogsMenuTitle(),
                        context, player)));

        fill(inv, plugin.getConfigManager().getProtectionLogsMenuFillerMaterial(),
                plugin.getConfigManager().getProtectionLogsMenuFillerName());
        setStaticItems(inv, plugin.getConfigManager().getProtectionLogsMenuItems(), context, player);
        int start = safePage * contentSlots.size();
        int end = Math.min(events.size(), start + contentSlots.size());
        for (int index = start; index < end; index++) {
            inv.setItem(contentSlots.get(index - start), buildLogItem(player, events.get(index), context));
        }
        setNavigation(inv, safePage, maxPage,
                plugin.getConfigManager().getProtectionLogsMenuPreviousItem(),
                plugin.getConfigManager().getProtectionLogsMenuNextItem());

        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleLogsMenuClick(Player player, int slot, ItemStack clicked, ClickType click,
            ArgosProtectHolder holder) {
        String[] contextParts = holder.getContextId().split("\\|", 2);
        if (contextParts.length != 2) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        ProtectionMenuContext context = plugin.getProtectionRegionManager()
                .findAccessibleProtection(player, contextParts[0]);
        if (context == null) {
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgProtectionMenuNotInOwnProtection()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        int page = parsePage(contextParts[1]);
        if (slot == plugin.getConfigManager().getProtectionLogsMenuPreviousItem().slot()) {
            openLogsMenu(player, context, page - 1);
            return;
        }
        if (slot == plugin.getConfigManager().getProtectionLogsMenuNextItem().slot()) {
            openLogsMenu(player, context, page + 1);
            return;
        }

        handleStaticMenuClick(player, slot, plugin.getConfigManager().getProtectionLogsMenuItems());
    }

    private ItemStack buildLogItem(Player player, ProtectionEventLog log, ProtectionMenuContext context) {
        MenuDisplayConfig display = plugin.getConfigManager().getProtectionLogsMenuItem();
        String texture = plugin.getConfigManager().getProtectionLogsMenuTexture(log.eventType());
        ItemStack item = texture.isEmpty() ? new ItemBuilder(Material.PLAYER_HEAD).build() : new ItemBuilder(Material.PLAYER_HEAD).setHeadTexture(texture).build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(applyLogPlaceholders(display.name(), context, player, log)));
            List<net.kyori.adventure.text.Component> lore = display.lore().stream()
                    .map(line -> applyLogPlaceholders(line, context, player, log))
                    .map(MessageUtils::getColoredMessage)
                    .toList();
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyLogPlaceholders(String text, ProtectionMenuContext context, Player player, ProtectionEventLog log) {
        String dateStr = log.createdAtMillis() <= 0 ? plugin.getConfigManager().getProtectionMembersUnknownDate() : DATE_FORMATTER.format(Instant.ofEpochMilli(log.createdAtMillis()));
        String actor = log.actorName() != null ? log.actorName() : (log.actorUuid() != null ? log.actorUuid() : "Unknown");
        String target = log.targetName() != null ? log.targetName() : (log.targetUuid() != null ? log.targetUuid() : "None");
        String detail = log.detail() != null ? log.detail() : "None";
        return applyPlaceholders(text, context, player)
                .replace("%date%", dateStr)
                .replace("%actor%", actor)
                .replace("%target%", target)
                .replace("%type%", log.eventType())
                .replace("%detail%", detail);
    }
}