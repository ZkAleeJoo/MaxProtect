package org.zkaleejoo.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.zkaleejoo.protection.ProtectionFlagLevel;
import org.zkaleejoo.ArgosProtect;
import org.zkaleejoo.config.MainConfigManager.MenuItemConfig;
import org.zkaleejoo.permissions.ArgosProtectPermissions;
import org.zkaleejoo.protection.ProtectionFlagDefinition;
import org.zkaleejoo.utils.MessageUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ProtectionFlagListener implements Listener {

    private static final long DENIED_MESSAGE_COOLDOWN_MS = 1200L;

    private final ArgosProtect plugin;
    private final Map<UUID, Long> deniedMessageCooldowns = new HashMap<>();

    public ProtectionFlagListener(ArgosProtect plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Material material = block.getType();
        if (event.getAction() == Action.PHYSICAL && isPressurePlate(material)) {
            denyIfNeeded(event, block, ProtectionFlagDefinition.PRESSURE_PLATES);
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (isDoorControl(material)) {
            denyIfNeeded(event, block, ProtectionFlagDefinition.DOORS);
            return;
        }

        if (isButtonControl(material)) {
            denyIfNeeded(event, block, ProtectionFlagDefinition.BUTTONS);
            return;
        }

        if (isWorkstation(material)) {
            denyIfNeeded(event, block, ProtectionFlagDefinition.WORKSTATIONS);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Location location = holderLocation(event.getInventory().getHolder());
        if (location == null) {
            return;
        }

        if (!plugin.getProtectionRegionManager().canUseFlag(player, location, ProtectionFlagDefinition.STORAGE)) {
            event.setCancelled(true);
            sendDenied(player, ProtectionFlagDefinition.STORAGE);
            plugin.getProtectionRegionManager().notifyIntrusionAttempt(player, location,
                    actionName(ProtectionFlagDefinition.STORAGE));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission(ArgosProtectPermissions.PROTECTION_REMOVE_OTHERS)
                && plugin.getProtectionRegionManager().isProtectionStone(event.getBlock())) {
            return;
        }
        if (plugin.getProtectionRegionManager().canUseFlag(event.getPlayer(), event.getBlock().getLocation(),
                ProtectionFlagDefinition.BREAK_BLOCKS)) {
            return;
        }
        event.setCancelled(true);
        sendDenied(event.getPlayer(), ProtectionFlagDefinition.BREAK_BLOCKS);
        plugin.getProtectionRegionManager().notifyIntrusionAttempt(event.getPlayer(), event.getBlock().getLocation(),
                actionName(ProtectionFlagDefinition.BREAK_BLOCKS));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getProtectionRegionManager().isProtectionItem(event.getItemInHand())) {
            return;
        }
        if (plugin.getProtectionRegionManager().canUseFlag(event.getPlayer(), event.getBlockPlaced().getLocation(),
                ProtectionFlagDefinition.PLACE_BLOCKS)) {
            return;
        }
        event.setCancelled(true);
        sendDenied(event.getPlayer(), ProtectionFlagDefinition.PLACE_BLOCKS);
        plugin.getProtectionRegionManager().notifyIntrusionAttempt(event.getPlayer(),
                event.getBlockPlaced().getLocation(),
                actionName(ProtectionFlagDefinition.PLACE_BLOCKS));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.getProtectionRegionManager().canUseFlag(player, event.getTo(), ProtectionFlagDefinition.ENTRY)) {
            return;
        }
        event.setCancelled(true);
        sendDenied(player, ProtectionFlagDefinition.ENTRY);
        plugin.getProtectionRegionManager().notifyIntrusionAttempt(player, event.getTo(),
                actionName(ProtectionFlagDefinition.ENTRY));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();

        ProtectionFlagLevel keepInvLevel = plugin.getProtectionRegionManager()
                .getToggleFlagLevel(location, ProtectionFlagDefinition.KEEP_INVENTORY);
        if (keepInvLevel == ProtectionFlagLevel.EVERYONE) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }

        ProtectionFlagLevel keepExpLevel = plugin.getProtectionRegionManager()
                .getToggleFlagLevel(location, ProtectionFlagDefinition.KEEP_EXP);
        if (keepExpLevel == ProtectionFlagLevel.EVERYONE) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ProtectionFlagLevel level = plugin.getProtectionRegionManager()
                .getToggleFlagLevel(player.getLocation(), ProtectionFlagDefinition.FALL_DAMAGE);
        if (level == ProtectionFlagLevel.NOBODY) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (!(affected instanceof Player player)) {
                continue;
            }

            ProtectionFlagLevel level = plugin.getProtectionRegionManager()
                    .getToggleFlagLevel(player.getLocation(), ProtectionFlagDefinition.POTION_SPLASH);
            if (level == ProtectionFlagLevel.NOBODY) {
                event.setIntensity(affected, 0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }

        ProtectionFlagLevel level = plugin.getProtectionRegionManager()
                .getToggleFlagLevel(player.getLocation(), ProtectionFlagDefinition.HUNGER_DRAIN);
        if (level == ProtectionFlagLevel.NOBODY) {
            event.setCancelled(true);
        }
    }

    private void denyIfNeeded(PlayerInteractEvent event, Block block, ProtectionFlagDefinition definition) {
        Player player = event.getPlayer();
        if (plugin.getProtectionRegionManager().canUseFlag(player, block.getLocation(), definition)) {
            return;
        }

        event.setCancelled(true);
        sendDenied(player, definition);
        plugin.getProtectionRegionManager().notifyIntrusionAttempt(player, block.getLocation(), actionName(definition));
    }

    private Location holderLocation(InventoryHolder holder) {
        if (holder instanceof BlockState blockState) {
            return blockState.getLocation();
        }
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLocation();
        }
        return null;
    }

    private boolean isPressurePlate(Material material) {
        return material.name().endsWith("_PRESSURE_PLATE");
    }

    private boolean isDoorControl(Material material) {
        String name = material.name();
        return name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE");
    }

    private boolean isButtonControl(Material material) {
        String name = material.name();
        return name.endsWith("_BUTTON") || material == Material.LEVER;
    }

    private boolean isWorkstation(Material material) {
        return switch (material) {
            case CRAFTING_TABLE, ENCHANTING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
                    SMITHING_TABLE, CARTOGRAPHY_TABLE, LOOM, GRINDSTONE, STONECUTTER ->
                true;
            default -> false;
        };
    }

    private boolean sameBlock(Location first, Location second) {
        return first.getWorld() != null
                && second.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private String actionName(ProtectionFlagDefinition definition) {
        return plugin.getConfigManager().getProtectionIntrusionAction(definition.id());
    }

    private void sendDenied(Player player, ProtectionFlagDefinition definition) {
        long now = System.currentTimeMillis();
        long lastMessage = deniedMessageCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastMessage < DENIED_MESSAGE_COOLDOWN_MS) {
            return;
        }

        deniedMessageCooldowns.put(player.getUniqueId(), now);
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgProtectionFlagDenied()
                        .replace("%flag%", flagName(definition))));
        plugin.getConfigManager().getProtectionFeedbackConfig().errorSound().play(player);
    }

    @SuppressWarnings("null")
    private String flagName(ProtectionFlagDefinition definition) {
        return plugin.getConfigManager().getProtectionSettingsMenuItems().stream()
                .filter(item -> item.id().equalsIgnoreCase(definition.id()))
                .map(MenuItemConfig::name)
                .findFirst()
                .orElse(definition.id().toLowerCase(Locale.ROOT));
    }
}
