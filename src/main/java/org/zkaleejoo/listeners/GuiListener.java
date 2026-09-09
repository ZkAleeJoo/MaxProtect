package org.zkaleejoo.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.utils.MaxProtectHolder;
import org.zkaleejoo.utils.MessageUtils;

import io.papermc.paper.event.player.AsyncChatEvent;

public class GuiListener implements Listener {
    private final MaxProtect plugin;

    public GuiListener(MaxProtect plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;
        if (event.getInventory().getHolder() == null)
            return;

        if (event.getInventory().getHolder() instanceof MaxProtectHolder) {

            event.setCancelled(true);

            MaxProtectHolder holder = (MaxProtectHolder) event.getInventory().getHolder();
            String menuId = holder.getMenuId();

            if (menuId.equals("CREATION_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionCreationManager().handleMenuClick(player, event.getSlot());
                return;
            }

            if (menuId.equals("INFO_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleInfoMenuClick(player, event.getSlot());
                return;
            }

            if (menuId.equals("PROTECTION_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleMenuClick(player, event.getSlot());
                return;
            }

            if (menuId.equals("PROTECTION_SETTINGS_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleSettingsMenuClick(player, event.getSlot(), holder);
                return;
            }

            if (menuId.equals("PROTECTION_HOME_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleHomeMenuClick(player, event.getSlot(),
                        event.getCurrentItem(), holder);
                return;
            }

            if (menuId.equals("PROTECTION_MEMBERS_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleMembersMenuClick(player, event.getSlot(),
                        event.getCurrentItem(), event.getClick(), holder);
                return;
            }

            if (menuId.equals("PROTECTION_LOGS_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleLogsMenuClick(player, event.getSlot(),
                        event.getCurrentItem(), event.getClick(), holder);
                return;
            }

            if (menuId.equals("PROTECTION_REMOVE_CONFIRM")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleRemoveConfirmClick(player, event.getSlot(), holder);
                return;
            }

            if (menuId.equals("PROTECTION_ADMIN_PLACED")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                plugin.getProtectionMenuManager().handleAdminPlacedMenuClick(player, event.getSlot(),
                        event.getCurrentItem(), holder);
                return;
            }

            if (menuId.equals("LANG_MENU")) {
                if (!(event.getWhoClicked() instanceof Player player)) {
                    return;
                }
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) {
                    play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                    return;
                }

                String languageCode = plugin.getConfigManager().getLanguageMenuItems().stream()
                        .filter(item -> item.hasSlot(event.getSlot()))
                        .filter(item -> !item.decorative())
                        .map(item -> item.id().toLowerCase())
                        .findFirst()
                        .orElse(null);
                if (languageCode == null) {
                    play(player, Sound.BLOCK_NOTE_BLOCK_BASS);
                    return;
                }

                boolean changed = plugin.getConfigManager().applyLanguageConfig(languageCode);
                if (changed) {
                    String message = plugin.getConfigManager().getMsgLanguageChanged()
                            .replace("%language%", plugin.getConfigManager().getLanguageDisplayName(languageCode));
                    player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
                    play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                } else {
                    String message = plugin.getConfigManager().getMsgLanguageFileMissing()
                            .replace("%file%", "config_" + languageCode + ".yml");
                    player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
                    play(player, Sound.ENTITY_VILLAGER_NO);
                }
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MaxProtectHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MaxProtectHolder holder)) {
            return;
        }
        if (!holder.getMenuId().equals("CREATION_MENU")) {
            return;
        }
        if (plugin.getProtectionCreationManager().hasPendingInput(player)) {
            return;
        }
        if (plugin.getProtectionCreationManager().consumeMenuRefresh(player)) {
            return;
        }

        plugin.getProtectionCreationManager().reset(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getProtectionCreationManager().reset(event.getPlayer());
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getProtectionCreationManager().hasPendingInput(player)) {
            return;
        }

        event.setCancelled(true);
        String message = plugin.getProtectionCreationManager().plainChat(event.message());
        plugin.getSchedulerUtils().runAtEntity(player,
                () -> plugin.getProtectionCreationManager().handleChatInput(player, message));
    }

    private void play(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 0.8f, 1.2f);
    }
}
