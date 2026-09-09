package org.zkaleejoo.creation;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.config.MainConfigManager.MenuItemConfig;
import org.zkaleejoo.protection.ProtectionValidation;
import org.zkaleejoo.utils.ItemBuilder;
import org.zkaleejoo.utils.MaxProtectHolder;
import org.zkaleejoo.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ProtectionCreationManager {

    private final MaxProtect plugin;
    private final Map<UUID, ProtectionDraft> drafts = new HashMap<>();
    private final Map<UUID, String> pendingInputs = new HashMap<>();
    private final Set<UUID> menuRefreshes = new HashSet<>();

    public ProtectionCreationManager(MaxProtect plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player) {
        ProtectionDraft draft = drafts.computeIfAbsent(player.getUniqueId(), ignored -> new ProtectionDraft());
        Inventory inv = Bukkit.createInventory(new MaxProtectHolder("CREATION_MENU"),
                plugin.getConfigManager().getCreationMenuSize(),
                MessageUtils.getColoredMessage(plugin.getConfigManager().getCreationMenuTitle()));

        ItemStack filler = new ItemBuilder(plugin.getConfigManager().getCreationMenuFillerMaterial())
                .setName(plugin.getConfigManager().getCreationMenuFillerName())
                .build();

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        for (MenuItemConfig item : plugin.getConfigManager().getCreationMenuItems()) {
            for (int slot : item.slots()) {
                if (slot >= inv.getSize()) {
                    continue;
                }
                inv.setItem(slot, buildMenuItem(item, draft));
            }
        }

        if (isCreationMenuOpen(player)) {
            menuRefreshes.add(player.getUniqueId());
        }
        player.openInventory(inv);
        play(player, Sound.UI_BUTTON_CLICK);
    }

    public void handleMenuClick(Player player, int slot) {
        MenuItemConfig clicked = plugin.getConfigManager().getCreationMenuItems().stream()
                .filter(item -> item.hasSlot(slot))
                .findFirst()
                .orElse(null);

        if (clicked == null) {
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            return;
        }

        if (clicked.decorative()) {
            play(player, Sound.UI_BUTTON_CLICK);
            return;
        }

        switch (clicked.id().toLowerCase(Locale.ROOT)) {
            case "protection-id", "display-name", "radius", "price" ->
                requestInput(player, clicked.id().toLowerCase(Locale.ROOT));
            case "block-type" -> setBlockFromHand(player);
            case "confirm" -> confirm(player);
            default -> play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        }
    }

    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    public boolean consumeMenuRefresh(Player player) {
        return menuRefreshes.remove(player.getUniqueId());
    }

    public void reset(Player player) {
        UUID playerId = player.getUniqueId();
        drafts.remove(playerId);
        pendingInputs.remove(playerId);
        menuRefreshes.remove(playerId);
    }

    public void handleChatInput(Player player, String message) {
        String fieldId = pendingInputs.remove(player.getUniqueId());
        if (fieldId == null) {
            return;
        }

        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("cancelar")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationInputCancelled()));
            play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
            openMenu(player);
            return;
        }

        ProtectionDraft draft = drafts.computeIfAbsent(player.getUniqueId(), ignored -> new ProtectionDraft());
        String error = applyInput(draft, fieldId, message.trim());
        if (error != null) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + error));
            pendingInputs.put(player.getUniqueId(), fieldId);
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgCreationInputSaved()));
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        openMenu(player);
    }

    public String plainChat(net.kyori.adventure.text.Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    private ItemStack buildMenuItem(MenuItemConfig item, ProtectionDraft draft) {
        String placeholderId = item.statusItem() ? item.statusFor().toLowerCase(Locale.ROOT) : item.id();
        List<String> lore = item.statusItem()
                ? List.of()
                : item.lore().stream()
                        .map(line -> applyPlaceholders(line, placeholderId, draft))
                        .toList();

        String displayValue = draft.getDisplayValue(item.id(), plugin.getConfigManager().getCreationUnsetValue());
        if (!item.id().equalsIgnoreCase("confirm") && !item.statusItem() && !displayValue.isBlank()) {
            lore = new java.util.ArrayList<>(lore);
            lore.add("");
            lore.add(plugin.getConfigManager().getCreationCurrentValueLine()
                    .replace("%value%", displayValue));
        }

        ItemBuilder builder = new ItemBuilder(resolveMaterial(item, draft))
                .setName(applyPlaceholders(item.name(), placeholderId, draft))
                .setHeadTexture(item.headTexture());

        if (!lore.isEmpty()) {
            builder.setLore(lore.toArray(new String[0]));
        }

        return builder.build();
    }

    private Material resolveMaterial(MenuItemConfig item, ProtectionDraft draft) {
        if (!item.statusItem()) {
            return item.material();
        }

        return isFieldComplete(item.statusFor().toLowerCase(Locale.ROOT), draft)
                ? item.filledMaterial()
                : item.emptyMaterial();
    }

    private boolean isFieldComplete(String fieldId, ProtectionDraft draft) {
        return switch (fieldId) {
            case "protection-id" -> draft.getProtectionId() != null;
            case "display-name" -> draft.getDisplayName() != null;
            case "block-type" -> draft.getBlockType() != null;
            case "radius" -> draft.getRadius() != null;
            case "price" -> draft.getPrice() != null;
            default -> false;
        };
    }

    private String applyPlaceholders(String text, String itemId, ProtectionDraft draft) {
        return text
                .replace("%protection_id%", draft.getProtectionId() == null
                        ? plugin.getConfigManager().getCreationUnsetValue()
                        : draft.getProtectionId())
                .replace("%display_name%", draft.getDisplayName() == null
                        ? plugin.getConfigManager().getCreationUnsetValue()
                        : draft.getDisplayName())
                .replace("%block_type%", draft.getBlockType() == null
                        ? plugin.getConfigManager().getCreationUnsetValue()
                        : draft.getBlockType().name())
                .replace("%radius%", draft.getRadius() == null
                        ? plugin.getConfigManager().getCreationUnsetValue()
                        : String.valueOf(draft.getRadius()))
                .replace("%price%", draft.getPrice() == null
                        ? plugin.getConfigManager().getCreationUnsetValue()
                        : String.valueOf(draft.getPrice()))
                .replace("%value%", draft.getDisplayValue(itemId, plugin.getConfigManager().getCreationUnsetValue()));
    }

    private void requestInput(Player player, String fieldId) {
        pendingInputs.put(player.getUniqueId(), fieldId);
        player.closeInventory();
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getCreationPrompt(fieldId)));
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    private String applyInput(ProtectionDraft draft, String fieldId, String value) {
        return switch (fieldId) {
            case "protection-id" -> setProtectionId(draft, value);
            case "display-name" -> setDisplayName(draft, value);
            case "block-type" -> setBlockType(draft, value);
            case "radius" -> setRadius(draft, value);
            case "price" -> setPrice(draft, value);
            default -> plugin.getConfigManager().getMsgCreationUnknownField();
        };
    }

    private String setProtectionId(ProtectionDraft draft, String value) {
        Optional<String> normalized = ProtectionValidation.normalizeId(value);
        if (normalized.isEmpty()) {
            return plugin.getConfigManager().getMsgCreationInvalidId();
        }
        draft.setProtectionId(normalized.get());
        return null;
    }

    private String setDisplayName(ProtectionDraft draft, String value) {
        if (value.isBlank()) {
            return plugin.getConfigManager().getMsgCreationInvalidName();
        }
        draft.setDisplayName(value);
        return null;
    }

    private String setBlockType(ProtectionDraft draft, String value) {
        Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock() || material.isAir()) {
            return plugin.getConfigManager().getMsgCreationInvalidMaterial();
        }
        draft.setBlockType(material);
        return null;
    }

    private void setBlockFromHand(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir() || !handItem.getType().isBlock()) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationInvalidMaterial()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        ItemStack blockItem = handItem.clone();
        blockItem.setAmount(1);

        ProtectionDraft draft = drafts.computeIfAbsent(player.getUniqueId(), ignored -> new ProtectionDraft());
        draft.setBlockItem(blockItem);
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgCreationInputSaved()));
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        openMenu(player);
    }

    private String setRadius(ProtectionDraft draft, String value) {
        Optional<Integer> radius = ProtectionValidation.parseRadius(value);
        if (radius.isEmpty()) {
            return plugin.getConfigManager().getMsgCreationInvalidRadius();
        }
        draft.setRadius(radius.get());
        return null;
    }

    private String setPrice(ProtectionDraft draft, String value) {
        Optional<Double> price = ProtectionValidation.parsePrice(value);
        if (price.isEmpty()) {
            return plugin.getConfigManager().getMsgCreationInvalidPrice();
        }
        draft.setPrice(price.get());
        return null;
    }

    private void confirm(Player player) {
        ProtectionDraft draft = drafts.get(player.getUniqueId());
        if (draft == null || !draft.isComplete()) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationIncomplete()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        File protectionsFolder = new File(plugin.getDataFolder(), "protections");
        File protectionFile = new File(protectionsFolder, draft.getProtectionId() + ".yml");
        if (protectionFile.exists()) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationAlreadyExists()));
            play(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        try {
            if (!protectionsFolder.exists() && !protectionsFolder.mkdirs()) {
                throw new IOException("Could not create protections folder.");
            }
            saveProtectionFile(draft, protectionFile);
            drafts.remove(player.getUniqueId());
            pendingInputs.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationCreated().replace("%id%", draft.getProtectionId())));
            play(player, Sound.ENTITY_PLAYER_LEVELUP);
        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not create protection '" + draft.getProtectionId() + "': " + exception.getMessage());
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMsgCreationSaveError()));
            play(player, Sound.ENTITY_VILLAGER_NO);
        }
    }

    private void saveProtectionFile(ProtectionDraft draft, File file) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("protection.id", draft.getProtectionId());
        yaml.set("protection.display-name", draft.getDisplayName());
        yaml.set("protection.radius", draft.getRadius());
        yaml.set("protection.priority", 0);

        yaml.set("item.block", draft.getBlockItem());
        yaml.set("item.lore", plugin.getConfigManager().getProtectionItemLore());

        yaml.set("price", draft.getPrice());
        yaml.set("price-rent", 0.0D);

        yaml.set("worldguard.region-id-format", "ap_%id%_%compact%");
        yaml.set("worldguard.flags.pvp", "DENY");
        yaml.set("worldguard.flags.mob-damage", "ALLOW");
        yaml.set("worldguard.flags.damage-animals", "ALLOW");
        yaml.set("worldguard.flags.entry", "ALLOW");
        yaml.set("worldguard.flags.block-place", "ALLOW");
        yaml.set("worldguard.flags.block-break", "ALLOW");
        yaml.set("worldguard.flags.chest-access", "ALLOW");
        yaml.set("worldguard.flags.tnt", "DENY");
        yaml.set("worldguard.flags.creeper-explosion", "DENY");
        yaml.set("worldguard.flags.other-explosion", "DENY");
        yaml.set("worldguard.flags.leaf-decay", "ALLOW");
        yaml.set("worldguard.flags.fire-spread", "DENY");
        yaml.set("worldguard.flags.lava-fire", "DENY");
        yaml.set("worldguard.flags.lighter", "DENY");
        yaml.set("worldguard.flags.ender-build", "DENY");

        yaml.set("actionbar.enter", plugin.getConfigManager().getProtectionDefaultActionbarEnter());
        yaml.set("actionbar.exit", plugin.getConfigManager().getProtectionDefaultActionbarExit());
        yaml.save(file);
    }

    private void play(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
    }

    private boolean isCreationMenuOpen(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof MaxProtectHolder holder
                && holder.getMenuId().equals("CREATION_MENU");
    }
}
