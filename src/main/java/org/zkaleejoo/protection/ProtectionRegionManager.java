package org.zkaleejoo.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.EconomyResponse;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.zkaleejoo.utils.SchedulerUtils.TaskWrapper;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.config.ProtectionFeedbackConfig.CinematicPreview;
import org.zkaleejoo.config.ProtectionFeedbackConfig.VisualEffect;
import org.zkaleejoo.permissions.MaxProtectPermissions;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationDecision;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationMember;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationReport;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationStatus;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.ProtectionStonesCandidate;
import org.zkaleejoo.protection.migration.ProtectionStonesMigrationScanner;
import org.zkaleejoo.protection.migration.ProtectionStonesMigrationYamlSupport;
import org.zkaleejoo.protection.migration.ProtectionStonesMigrationYamlSupport.YamlResolution;
import org.zkaleejoo.protection.storage.ProtectionCache;
import org.zkaleejoo.protection.storage.ProtectionStorage;
import org.zkaleejoo.protection.storage.SqliteProtectionStorage;
import org.zkaleejoo.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProtectionRegionManager implements Listener {

    private static final DateTimeFormatter ADMIN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final long PREVIEW_FALL_DAMAGE_GRACE_MILLIS = 2500L;

    private final MaxProtect plugin;
    private final NamespacedKey protectionIdKey;
    private final NamespacedKey protectionRadiusKey;
    private final Map<UUID, String> currentRegionByPlayer = new HashMap<>();
    private final Map<UUID, Long> viewCooldowns = new HashMap<>();
    private final Map<UUID, TaskWrapper> viewTasks = new HashMap<>();
    private final Map<UUID, Long> previewCooldowns = new HashMap<>();
    private final Map<UUID, Long> cinematicPreviewCooldowns = new HashMap<>();
    private final Map<UUID, PreviewSession> previewSessions = new HashMap<>();
    private final Map<UUID, ProtectionFlightSession> protectionFlightSessions = new HashMap<>();
    private final Map<UUID, Long> previewFallProtectionUntil = new HashMap<>();
    private final Set<UUID> previewTeleports = new HashSet<>();
    private final Map<String, Long> intrusionAlertCooldowns = new HashMap<>();
    private final List<PlacedProtection> placedProtections = new ArrayList<>();
    private final SqliteProtectionStorage storage;
    private final ProtectionCache cache;
    private TaskWrapper rentTask;

    public ProtectionRegionManager(MaxProtect plugin) {
        this.plugin = plugin;
        this.protectionIdKey = new NamespacedKey(plugin, "protection_id");
        this.protectionRadiusKey = new NamespacedKey(plugin, "protection_radius");
        this.storage = new SqliteProtectionStorage(plugin.getDataFolder().toPath().resolve("protections.db"),
                plugin.getLogger());
        this.cache = new ProtectionCache();
        this.storage.initialize();
        loadPlacedProtections();
        ensureCompatibilityFlagsForPlacedProtections();
        initializeRentState();
        startRentTask();
    }

    @SuppressWarnings("null")
    public void shutdown() {
        if (rentTask != null) {
            rentTask.cancel();
            rentTask = null;
        }
        viewTasks.values().forEach(TaskWrapper::cancel);
        viewTasks.clear();
        previewSessions.keySet().stream().toList().forEach(uuid -> endCinematicPreview(uuid, false));
        protectionFlightSessions.keySet().stream().toList().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                disableProtectionFlight(player, false);
            } else {
                protectionFlightSessions.remove(uuid);
            }
        });
        viewCooldowns.clear();
        previewCooldowns.clear();
        cinematicPreviewCooldowns.clear();
        previewFallProtectionUntil.clear();
        previewTeleports.clear();
        intrusionAlertCooldowns.clear();
        cache.invalidateAll();
        storage.close();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (isCinematicPreviewing(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItemInHand();
        String protectionId = getProtectionId(item);
        if (protectionId == null) {
            return;
        }

        ProtectionDefinition definition = loadProtection(protectionId);
        if (definition == null) {
            event.setCancelled(true);
            sendError(player, protectionFileExists(protectionId)
                    ? plugin.getConfigManager().getMsgProtectionInvalidConfig().replace("%id%", protectionId)
                    : plugin.getConfigManager().getMsgProtectionNotFound().replace("%id%", protectionId));
            return;
        }
        String limitError = validatePlacementLimits(player, definition);
        if (limitError != null) {
            event.setCancelled(true);
            sendError(player, limitError);
            return;
        }

        CreationResult result = createWorldGuardRegion(player, event.getBlockPlaced(), definition);
        if (!result.success()) {
            event.setCancelled(true);
            sendError(player, result.message());
            return;
        }

        savePlacedProtection(player, event.getBlockPlaced(), definition, result.regionId());
        spawnVisualEffect(player, event.getBlockPlaced().getLocation(),
                plugin.getConfigManager().getProtectionFeedbackConfig().placeEffect());
        plugin.getConfigManager().getProtectionFeedbackConfig().placeSound().play(player);
        send(player, plugin.getConfigManager().getMsgProtectionPlaced()
                .replace("%protection%", protectionId)
                .replace("%region%", result.regionId())
                .replace("%radius%", String.valueOf(definition.radius())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isCinematicPreviewing(player)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        PlacedProtection placed = findProtectionStone(block);
        if (placed == null) {
            return;
        }

        event.setCancelled(true);
        if (!placed.ownerUuid().equals(player.getUniqueId().toString())
                && !player.hasPermission(MaxProtectPermissions.PROTECTION_REMOVE_OTHERS)) {
            sendError(player, plugin.getConfigManager().getMsgProtectionRemoveNotOwner());
            return;
        }
        plugin.getProtectionMenuManager().openRemoveConfirmMenu(player, toContext(placed));
        send(player, plugin.getConfigManager().getMsgProtectionRemoveConfirmOpen());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        blocks.removeIf(block -> {
            if (isProtectionStone(block)) {
                return true;
            }
            PlacedProtection placed = findProtectionAt(block.getLocation());
            if (placed != null) {
                ProtectionFlagLevel level = getProtectionFlagLevel(placed, ProtectionFlagDefinition.TNT);
                if (level == ProtectionFlagLevel.NOBODY) {
                    return true;
                }
            }
            return false;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectionStone(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectionStone(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (lockCinematicMovement(event)) {
            return;
        }

        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) {
            return;
        }

        Player player = event.getPlayer();
        refreshHeldProtectionPreview(player, false);
        PlacedProtection current = findProtectionAt(event.getTo());
        String nextRegionId = current == null ? null : current.regionId();
        String previousRegionId = currentRegionByPlayer.get(player.getUniqueId());
        boolean hasActiveProtectionFlight = protectionFlightSessions.containsKey(player.getUniqueId());
        boolean sameRegion = equals(previousRegionId, nextRegionId);

        if (!shouldProcessMovementRegionChange(previousRegionId, nextRegionId, hasActiveProtectionFlight)) {
            return;
        }

        if (!sameRegion) {
            PlacedProtection previous = previousRegionId == null ? null : findProtection(previousRegionId);
            if (previous != null) {
                String actionbarExit = resolveActionbar(previous, false);
                if (!actionbarExit.isBlank()) {
                    player.sendActionBar(formatActionbar(actionbarExit, previous, player));
                }
            }

            if (current != null) {
                currentRegionByPlayer.put(player.getUniqueId(), current.regionId());
                String actionbarEnter = resolveActionbar(current, true);
                if (!actionbarEnter.isBlank()) {
                    player.sendActionBar(formatActionbar(actionbarEnter, current, player));
                }
            } else {
                currentRegionByPlayer.remove(player.getUniqueId());
            }
        }

        validateProtectionFlight(player, event.getTo(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (isCinematicPreviewing(event.getPlayer())) {
            endCinematicPreview(event.getPlayer().getUniqueId(), true);
        }
        plugin.getSchedulerUtils().runAtEntity(event.getPlayer(), () -> refreshHeldProtectionPreview(event.getPlayer(), true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isCinematicPreviewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isCinematicPreviewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isCinematicPreviewing(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long protectedUntil = previewFallProtectionUntil.getOrDefault(playerId, 0L);
        if (shouldCancelPreviewFallDamage(isCinematicPreviewing(player), protectedUntil, System.currentTimeMillis())) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            return;
        }
        previewFallProtectionUntil.remove(playerId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (isCinematicPreviewing(player) && !previewTeleports.contains(player.getUniqueId())) {
            endCinematicPreview(player.getUniqueId(), false);
        }
        if (event.getTo() != null) {
            validateProtectionFlight(player, event.getTo(), true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        endCinematicPreview(event.getPlayer().getUniqueId(), false);
        disableProtectionFlight(event.getPlayer(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        endCinematicPreview(playerId, false);
        disableProtectionFlight(event.getPlayer(), false);
        previewFallProtectionUntil.remove(playerId);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        endCinematicPreview(playerId, false);
        disableProtectionFlight(event.getPlayer(), false);
        previewFallProtectionUntil.remove(playerId);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        endCinematicPreview(playerId, false);
        disableProtectionFlight(event.getPlayer(), false);
        previewFallProtectionUntil.remove(playerId);
    }

    private CreationResult createWorldGuardRegion(Player player, Block block, ProtectionDefinition definition) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(block.getWorld()));
        if (regionManager == null) {
            return CreationResult.fail(plugin.getConfigManager().getMsgProtectionRegionUnavailable());
        }

        String regionId = buildRegionId(player, block, definition);
        if (regionManager.hasRegion(regionId)) {
            return CreationResult.fail(plugin.getConfigManager().getMsgProtectionRegionExists());
        }

        int radius = definition.radius();
        BlockVector3 min = BlockVector3.at(block.getX() - radius, block.getWorld().getMinHeight(),
                block.getZ() - radius);
        BlockVector3 max = BlockVector3.at(block.getX() + radius, block.getWorld().getMaxHeight() - 1,
                block.getZ() + radius);
        if (overlapsExistingProtection(player.getUniqueId().toString(), block.getWorld().getName(),
                min.x(), min.y(), min.z(), max.x(), max.y(), max.z())) {
            return CreationResult.fail(plugin.getConfigManager().getMsgProtectionRegionOverlap());
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(definition.priority());

        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(player.getUniqueId());
        region.setOwners(owners);
        applyFlags(region, definition.yaml());
        applyMissingDefaultFlags(region);
        ProtectionWorldGuardFlagSupport.applyCompatibilityPassthrough(region);

        regionManager.addRegion(region);
        try {
            regionManager.saveChanges();
        } catch (StorageException exception) {
            regionManager.removeRegion(regionId);
            plugin.getLogger()
                    .warning("Could not save WorldGuard region '" + regionId + "': " + exception.getMessage());
            return CreationResult.fail(plugin.getConfigManager().getMsgProtectionRegionSaveError());
        }

        return CreationResult.ok(regionId);
    }

    private void applyFlags(ProtectedRegion region, YamlConfiguration yaml) {
        ConfigurationSection flags = yaml.getConfigurationSection("worldguard.flags");
        if (flags == null) {
            flags = yaml.getConfigurationSection("protection.flags.worldguard");
        }
        if (flags == null) {
            return;
        }

        for (String flagName : flags.getKeys(false)) {
            StateFlag flag = ProtectionWorldGuardFlagSupport.stateFlag(flagName);
            if (flag == null) {
                continue;
            }

            String rawState = flags.getString(flagName, "").toUpperCase(Locale.ROOT);
            StateFlag.State state = switch (rawState) {
                case "ALLOW" -> StateFlag.State.ALLOW;
                case "DENY" -> StateFlag.State.DENY;
                default -> null;
            };
            if (state != null) {
                region.setFlag(flag, state);
            }
        }
    }

    private boolean overlapsExistingProtection(String ownerUuid, String worldName, int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        return placedProtections.stream()
                .anyMatch(placed -> shouldBlockProtectionOverlap(placed.ownerUuid(), ownerUuid, placed.worldName(),
                        placed.minX(), placed.minY(), placed.minZ(),
                        placed.maxX(), placed.maxY(), placed.maxZ(),
                        worldName, minX, minY, minZ, maxX, maxY, maxZ));
    }

    static boolean shouldBlockProtectionOverlap(String existingOwnerUuid, String placingOwnerUuid,
            String firstWorld, int firstMinX, int firstMinY, int firstMinZ,
            int firstMaxX, int firstMaxY, int firstMaxZ,
            String secondWorld, int secondMinX, int secondMinY, int secondMinZ,
            int secondMaxX, int secondMaxY, int secondMaxZ) {
        return !existingOwnerUuid.equalsIgnoreCase(placingOwnerUuid)
                && regionsOverlap(firstWorld, firstMinX, firstMinY, firstMinZ, firstMaxX, firstMaxY, firstMaxZ,
                        secondWorld, secondMinX, secondMinY, secondMinZ, secondMaxX, secondMaxY, secondMaxZ);
    }

    static boolean regionsOverlap(String firstWorld, int firstMinX, int firstMinY, int firstMinZ,
            int firstMaxX, int firstMaxY, int firstMaxZ,
            String secondWorld, int secondMinX, int secondMinY, int secondMinZ,
            int secondMaxX, int secondMaxY, int secondMaxZ) {
        return firstWorld.equals(secondWorld)
                && firstMinX <= secondMaxX
                && firstMaxX >= secondMinX
                && firstMinY <= secondMaxY
                && firstMaxY >= secondMinY
                && firstMinZ <= secondMaxZ
                && firstMaxZ >= secondMinZ;
    }

    private void applyMissingDefaultFlags(ProtectedRegion region) {
        for (ProtectionFlagDefinition definition : ProtectionFlagDefinition.values()) {
            if (!definition.hasWorldGuardFlags()) {
                continue;
            }

            ProtectionFlagLevel level = definition.defaultLevel();
            for (StateFlag flag : definition.worldGuardFlags()) {
                if (region.getFlag(flag) == null) {
                    region.setFlag(flag, level.state());
                }
                if (region.getFlag(flag.getRegionGroupFlag()) == null) {
                    region.setFlag(flag.getRegionGroupFlag(), level.group());
                }
            }
        }
    }

    @SuppressWarnings("null")
    private void savePlacedProtection(Player player, Block block, ProtectionDefinition definition, String regionId) {
        ProtectionRentSettings rentSettings = plugin.getConfigManager().getProtectionRentSettings();
        long rentPaidUntil = rentSettings.enabled() ? System.currentTimeMillis() + rentSettings.periodMillis() : 0L;
        PlacedProtection placed = PlacedProtection.from(player, block, definition, regionId, rentPaidUntil, false);
        placedProtections.removeIf(existing -> existing.regionId().equalsIgnoreCase(regionId));
        placedProtections.add(placed);
        placedProtections.sort(Comparator.comparing(PlacedProtection::regionId));
        savePlacedProtections();
    }

    private void loadPlacedProtections() {
        placedProtections.clear();
        placedProtections.addAll(storage.loadProtections().stream()
                .map(this::fromStoredProtection)
                .toList());
    }

    private void ensureCompatibilityFlagsForPlacedProtections() {
        Map<RegionManager, Boolean> dirtyManagers = new LinkedHashMap<>();
        for (PlacedProtection placed : placedProtections) {
            RegionManager regionManager = getRegionManager(placed.worldName());
            if (regionManager == null) {
                continue;
            }
            ProtectedRegion region = regionManager.getRegion(placed.regionId());
            if (region == null) {
                continue;
            }
            if (ProtectionWorldGuardFlagSupport.applyCompatibilityPassthrough(region)) {
                dirtyManagers.put(regionManager, true);
            }
        }

        dirtyManagers.keySet().forEach(regionManager -> {
            try {
                regionManager.saveChanges();
            } catch (StorageException exception) {
                plugin.getLogger().warning("Could not save WorldGuard compatibility flags: "
                        + exception.getMessage());
            }
        });
    }

    private boolean savePlacedProtections() {
        try {
            List<ProtectionStorage.StoredProtection> snapshot = placedProtections.stream()
                    .map(this::toStoredProtection)
                    .toList();
            storage.replaceProtectionsAsync(snapshot).exceptionally(throwable -> {
                plugin.getLogger().warning("Async protection save failed: " + throwable.getMessage());
                return false;
            });
            cache.invalidateAll();
            return true;
        } catch (IllegalStateException exception) {
            plugin.getLogger().warning("Could not save protection database: " + exception.getMessage());
            return false;
        }
    }

    public void reloadRentSettings() {
        initializeRentState();
        startRentTask();
    }

    private void initializeRentState() {
        ProtectionRentSettings settings = plugin.getConfigManager().getProtectionRentSettings();
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < placedProtections.size(); i++) {
            PlacedProtection placed = placedProtections.get(i);
            if (!settings.enabled()) {
                if (placed.rentSuspended()) {
                    PlacedProtection updated = placed.withRentState(placed.rentPaidUntilMillis(), false);
                    placedProtections.set(i, updated);
                    restoreWorldGuardRegion(updated);
                    changed = true;
                }
                continue;
            }
            if (placed.isLegacyImported() && loadProtection(placed.protectionId()) == null) {
                continue;
            }
            if (placed.rentPaidUntilMillis() <= 0L) {
                placedProtections.set(i, placed.withRentState(now + settings.periodMillis(), false));
                changed = true;
            }
        }
        if (changed) {
            savePlacedProtections();
        }
    }

    private void startRentTask() {
        if (rentTask != null) {
            rentTask.cancel();
            rentTask = null;
        }
        ProtectionRentSettings settings = plugin.getConfigManager().getProtectionRentSettings();
        if (!settings.enabled()) {
            return;
        }
        rentTask = plugin.getSchedulerUtils().runTaskTimer(this::checkProtectionRent,
                settings.checkIntervalTicks(), settings.checkIntervalTicks());
    }

    private void checkProtectionRent() {
        ProtectionRentSettings settings = plugin.getConfigManager().getProtectionRentSettings();
        if (!settings.enabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (PlacedProtection placed : new ArrayList<>(placedProtections)) {
            if (placed.isLegacyImported() && loadProtection(placed.protectionId()) == null) {
                continue;
            }
            if (placed.rentSuspended() || settings.isPaid(placed.rentPaidUntilMillis(), now)) {
                continue;
            }
            processAutomaticRentPayment(placed, settings, now);
        }
    }

    private void processAutomaticRentPayment(PlacedProtection placed, ProtectionRentSettings settings, long now) {
        OptionalDouble rentPrice = resolveRentPrice(placed);
        if (rentPrice.isEmpty()) {
            suspendForUnpaidRent(placed);
            return;
        }
        double price = rentPrice.getAsDouble();
        if (price <= 0.0D) {
            updateRentState(placed, now + settings.periodMillis(), false);
            restoreWorldGuardRegion(findProtection(placed.regionId()));
            return;
        }
        if (!plugin.hasEconomy()) {
            suspendForUnpaidRent(placed);
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(parseUuid(placed.ownerUuid()));
        if (!plugin.getEconomy().has(owner, price)) {
            suspendForUnpaidRent(placed);
            return;
        }

        EconomyResponse response = plugin.getEconomy().withdrawPlayer(owner, price);
        if (!response.transactionSuccess()) {
            suspendForUnpaidRent(placed);
            return;
        }
        updateRentState(placed, now + settings.periodMillis(), false);
    }

    private void suspendForUnpaidRent(PlacedProtection placed) {
        if (placed.rentSuspended()) {
            return;
        }
        if (!updateRentState(placed, placed.rentPaidUntilMillis(), true)) {
            return;
        }
        World world = plugin.getServer().getWorld(placed.worldName());
        if (world != null) {
            removeWorldGuardRegion(world, placed.regionId());
        }
        disableProtectionFlightsInRegion(placed.regionId(), true);
        notifyOwnerRentSuspended(placed);
    }

    private void notifyOwnerRentSuspended(PlacedProtection placed) {
        Player owner = Bukkit.getPlayer(parseUuid(placed.ownerUuid()));
        if (owner == null || !owner.isOnline()) {
            return;
        }
        send(owner, plugin.getConfigManager().getMsgProtectionRentSuspended()
                .replace("%alias%", displayNameForLookup(placed))
                .replace("%region%", placed.regionId()));
    }

    public RentPaymentResult payRent(Player player, String lookup) {
        ProtectionRentSettings settings = plugin.getConfigManager().getProtectionRentSettings();
        if (!settings.enabled()) {
            return new RentPaymentResult(RentPaymentStatus.DISABLED, null, 0L, "");
        }

        PlacedProtection placed = findRentPaymentTarget(player, lookup);
        if (placed == null) {
            return new RentPaymentResult(RentPaymentStatus.NOT_FOUND, null, 0L, "");
        }
        if (!placed.ownerUuid().equals(player.getUniqueId().toString())
                && !player.hasPermission(MaxProtectPermissions.ADMIN)) {
            return new RentPaymentResult(RentPaymentStatus.NOT_OWNER, toContext(placed), placed.rentPaidUntilMillis(),
                    "");
        }

        long now = System.currentTimeMillis();
        if (!placed.rentSuspended() && settings.isPaid(placed.rentPaidUntilMillis(), now)) {
            return new RentPaymentResult(RentPaymentStatus.ALREADY_PAID, toContext(placed),
                    placed.rentPaidUntilMillis(), "");
        }
        OptionalDouble rentPrice = resolveRentPrice(placed);
        if (rentPrice.isEmpty()) {
            return new RentPaymentResult(RentPaymentStatus.REGION_UNAVAILABLE, toContext(placed),
                    placed.rentPaidUntilMillis(), "");
        }
        double price = rentPrice.getAsDouble();
        if (price > 0.0D) {
            if (!plugin.hasEconomy()) {
                return new RentPaymentResult(RentPaymentStatus.ECONOMY_UNAVAILABLE, toContext(placed),
                        placed.rentPaidUntilMillis(), plugin.getConfigManager().getMsgProtectionEconomyUnavailable());
            }
            if (!plugin.getEconomy().has(player, price)) {
                return new RentPaymentResult(RentPaymentStatus.NOT_ENOUGH_MONEY, toContext(placed),
                        placed.rentPaidUntilMillis(), plugin.getEconomy().format(price));
            }
            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, price);
            if (!response.transactionSuccess()) {
                return new RentPaymentResult(RentPaymentStatus.PAYMENT_ERROR, toContext(placed),
                        placed.rentPaidUntilMillis(), response.errorMessage);
            }
        }

        long paidUntil = now + settings.periodMillis();
        if (!updateRentState(placed, paidUntil, false)) {
            return new RentPaymentResult(RentPaymentStatus.SAVE_ERROR, toContext(placed), placed.rentPaidUntilMillis(),
                    "");
        }

        PlacedProtection updated = findProtection(placed.regionId());
        if (updated != null && !restoreWorldGuardRegion(updated)) {
            updateRentState(updated, paidUntil, true);
            return new RentPaymentResult(RentPaymentStatus.REGION_UNAVAILABLE, toContext(updated), paidUntil, "");
        }
        return new RentPaymentResult(placed.rentSuspended()
                ? RentPaymentStatus.REACTIVATED
                : RentPaymentStatus.PAID, updated == null ? null : toContext(updated), paidUntil, "");
    }

    private OptionalDouble resolveRentPrice(PlacedProtection placed) {
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        if (definition == null) {
            plugin.getLogger().warning("Could not read rent price for protection '" + placed.regionId()
                    + "' because protection YAML '" + placed.protectionId() + ".yml' is missing or invalid.");
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(definition.rentPrice());
    }

    private PlacedProtection findRentPaymentTarget(Player player, String lookup) {
        if (lookup == null || lookup.isBlank()) {
            PlacedProtection current = findProtectionAt(player.getLocation());
            return current != null && current.ownerUuid().equals(player.getUniqueId().toString()) ? current : null;
        }
        String normalized = lookup.toLowerCase(Locale.ROOT);
        return placedProtections.stream()
                .filter(placed -> placed.ownerUuid().equals(player.getUniqueId().toString())
                        || player.hasPermission(MaxProtectPermissions.ADMIN))
                .filter(placed -> matchesLookup(placed, normalized))
                .findFirst()
                .orElse(null);
    }

    private boolean updateRentState(PlacedProtection placed, long paidUntilMillis, boolean suspended) {
        if (placed == null) {
            return false;
        }
        int index = placedProtections.indexOf(placed);
        if (index < 0) {
            return false;
        }
        PlacedProtection updated = placed.withRentState(paidUntilMillis, suspended);
        placedProtections.set(index, updated);
        if (savePlacedProtections()) {
            return true;
        }
        placedProtections.set(index, placed);
        return false;
    }

    private PlacedProtection fromStoredProtection(ProtectionStorage.StoredProtection stored) {
        Map<String, ProtectionFlagLevel> flags = new LinkedHashMap<>();
        String legacySource = "";
        String legacyMaterial = "";
        stored.flags().forEach((flagId, level) -> {
            if (ProtectionMigrationSupport.LEGACY_FLAG_SOURCE.equals(flagId)) {
                return;
            }
            if (ProtectionMigrationSupport.LEGACY_FLAG_MATERIAL.equals(flagId)) {
                return;
            }
            ProtectionFlagLevel parsed = ProtectionFlagLevel.fromStorage(level, null);
            if (parsed != null) {
                flags.put(flagId, parsed);
            }
        });
        legacySource = stored.flags().getOrDefault(ProtectionMigrationSupport.LEGACY_FLAG_SOURCE, "");
        legacyMaterial = stored.flags().getOrDefault(ProtectionMigrationSupport.LEGACY_FLAG_MATERIAL, "");
        Map<String, ProtectionMemberEntry> members = new LinkedHashMap<>();
        stored.members().forEach((uuid, member) -> members.put(uuid,
                new ProtectionMemberEntry(member.uuid(), member.name(),
                        ProtectionMemberRank.fromStorage(member.rank()))));
        return new PlacedProtection(stored.regionId(), stored.protectionId(), stored.alias(), stored.ownerUuid(),
                stored.ownerName(), stored.worldName(), stored.stoneX(), stored.stoneY(), stored.stoneZ(),
                stored.minX(), stored.minY(), stored.minZ(), stored.maxX(), stored.maxY(), stored.maxZ(),
                stored.createdAtMillis(), stored.actionbarEnter(), stored.actionbarExit(), stored.customHome(),
                stored.homeX(), stored.homeY(), stored.homeZ(), stored.homeYaw(), stored.homePitch(), flags,
                legacySource, legacyMaterial, members, stored.rentPaidUntilMillis(), stored.rentSuspended());
    }

    private ProtectionStorage.StoredProtection toStoredProtection(PlacedProtection placed) {
        Map<String, String> flags = new LinkedHashMap<>();
        placed.flags().forEach((flagId, level) -> flags.put(flagId, level.name()));
        if (placed.isLegacyImported()) {
            flags.putAll(ProtectionMigrationSupport.legacyFlags(placed.legacyMaterial()));
        }
        Map<String, ProtectionStorage.StoredMember> members = new LinkedHashMap<>();
        placed.members().forEach((uuid, member) -> members.put(uuid,
                new ProtectionStorage.StoredMember(member.uuid(), member.name(), member.rank().name())));
        return new ProtectionStorage.StoredProtection(placed.regionId(), placed.protectionId(), placed.alias(),
                placed.ownerUuid(), placed.ownerName(), placed.worldName(), placed.stoneX(), placed.stoneY(),
                placed.stoneZ(), placed.minX(), placed.minY(), placed.minZ(), placed.maxX(), placed.maxY(),
                placed.maxZ(), placed.createdAtMillis(), placed.actionbarEnter(), placed.actionbarExit(),
                placed.customHome(), placed.homeX(), placed.homeY(), placed.homeZ(), placed.homeYaw(),
                placed.homePitch(), flags, members, placed.rentPaidUntilMillis(), placed.rentSuspended());
    }

    private ProtectionDefinition loadProtection(String rawId) {
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
        Integer radius = loadRadius(yaml, id);
        if (radius == null) {
            return null;
        }
        int priority = yaml.getInt("protection.priority", 0);
        String regionIdFormat = yaml.getString("worldguard.region-id-format",
                "maxprotect_%player%_%id%_%world%_%x%_%y%_%z%");
        String actionbarEnter = yaml.getString("actionbar.enter", yaml.getString("protection.flags.actionbar.enter",
                plugin.getConfigManager().getProtectionDefaultActionbarEnter()));
        String actionbarExit = yaml.getString("actionbar.exit", yaml.getString("protection.flags.actionbar.exit",
                plugin.getConfigManager().getProtectionDefaultActionbarExit()));
        if (!yaml.contains("protection.display-name") && !yaml.contains("display-name")) {
            warnInvalidProtection(id, "missing protection.display-name");
            return null;
        }
        String displayName = yaml.getString("protection.display-name", yaml.getString("display-name"));
        if (displayName == null || displayName.isBlank()) {
            warnInvalidProtection(id, "missing protection.display-name");
            return null;
        }
        List<String> lore = yaml.getStringList("item.lore");
        if (lore.isEmpty()) {
            lore = plugin.getConfigManager().getProtectionItemLore();
        }
        ItemStack blockItem = loadBlockItem(yaml);
        if (blockItem == null) {
            warnInvalidProtection(id, "missing or invalid item.block/item.material");
            return null;
        }
        Double price = loadPrice(yaml, id);
        if (price == null) {
            return null;
        }
        Double rentPrice = loadRentPrice(yaml, id);
        if (rentPrice == null) {
            return null;
        }
        return new ProtectionDefinition(id, displayName, blockItem, radius, priority, price, rentPrice, lore,
                regionIdFormat, actionbarEnter, actionbarExit, yaml);
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
        Material material = Material.matchMaterial(
                rawMaterial);
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

    private Double loadRentPrice(YamlConfiguration yaml, String id) {
        double price = ProtectionCommandSupport.loadProtectionRentPrice(yaml);
        if (!ProtectionValidation.isValidPrice(price)) {
            warnInvalidProtection(id, "price-rent must be finite and between " + ProtectionValidation.MIN_PRICE
                    + " and " + ProtectionValidation.MAX_PRICE);
            return null;
        }
        return price;
    }

    private String validatePlacementLimits(Player player, ProtectionDefinition definition) {
        ProtectionLimitProfile profile = resolveLimitProfile(player);
        int ownedProtections = countOwnedProtections(player);
        if (!profile.canCreateAnotherProtection(ownedProtections)) {
            return plugin.getConfigManager().getMsgProtectionLimitMaxProtect()
                    .replace("%current%", String.valueOf(ownedProtections))
                    .replace("%max%", String.valueOf(profile.maxProtections()))
                    .replace("%group%", profile.id());
        }
        if (!profile.allowsRadius(definition.radius())) {
            return plugin.getConfigManager().getMsgProtectionLimitRadius()
                    .replace("%radius%", String.valueOf(definition.radius()))
                    .replace("%max_radius%", String.valueOf(profile.maxRadius()))
                    .replace("%group%", profile.id());
        }
        if (!profile.allowsPrice(definition.price())) {
            return plugin.getConfigManager().getMsgProtectionLimitPrice()
                    .replace("%price%", String.valueOf(definition.price()))
                    .replace("%min_price%", String.valueOf(profile.minPrice()))
                    .replace("%max_price%", String.valueOf(profile.maxPrice()))
                    .replace("%group%", profile.id());
        }
        return null;
    }

    public ProtectionLimitProfile resolveLimitProfile(Player player) {
        return ProtectionLimitResolver.resolve(
                plugin.getConfigManager().getProtectionLimitGroups(),
                plugin.getConfigManager().getDefaultProtectionLimitProfile(),
                player::hasPermission);
    }

    private void warnInvalidProtection(String id, String reason) {
        plugin.getLogger().warning("Protection '" + id + "' has invalid YAML: " + reason + ".");
    }

    private boolean protectionFileExists(String rawId) {
        Optional<String> normalizedId = ProtectionValidation.normalizeId(rawId);
        return normalizedId
                .map(id -> new File(new File(plugin.getDataFolder(), "protections"), id + ".yml").isFile())
                .orElse(false);
    }

    private ItemStack buildProtectionItem(ProtectionDefinition definition, int amount) {
        ItemStack item = definition.blockItem().clone();
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(definition.displayName()));
            meta.lore(definition.lore().stream()
                    .map(line -> line
                            .replace("%id%", definition.id())
                            .replace("%display_name%", definition.displayName())
                            .replace("%material%", definition.blockItem().getType().name())
                            .replace("%price%", String.valueOf(definition.price()))
                            .replace("%radius%", String.valueOf(definition.radius())))
                    .map(MessageUtils::getColoredMessage)
                    .toList());
            meta.getPersistentDataContainer().set(protectionIdKey, PersistentDataType.STRING, definition.id());
            meta.getPersistentDataContainer().set(protectionRadiusKey, PersistentDataType.INTEGER, definition.radius());
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean canFit(PlayerInventory inventory, ItemStack item) {
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

    private boolean removeWorldGuardRegion(World world, String regionId) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            return false;
        }

        ProtectedRegion removedRegion = regionManager.getRegion(regionId);
        if (removedRegion == null) {
            return true;
        }

        regionManager.removeRegion(regionId);
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException exception) {
            regionManager.addRegion(removedRegion);
            plugin.getLogger()
                    .warning("Could not remove WorldGuard region '" + regionId + "': " + exception.getMessage());
            return false;
        }
    }

    private boolean restoreWorldGuardRegion(PlacedProtection placed) {
        if (placed == null) {
            return false;
        }
        World world = plugin.getServer().getWorld(placed.worldName());
        RegionManager regionManager = world == null ? null : getRegionManager(placed.worldName());
        if (world == null || regionManager == null) {
            return false;
        }
        if (regionManager.getRegion(placed.regionId()) != null) {
            return true;
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(placed.regionId(),
                BlockVector3.at(placed.minX(), placed.minY(), placed.minZ()),
                BlockVector3.at(placed.maxX(), placed.maxY(), placed.maxZ()));
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        region.setPriority(definition == null ? 0 : definition.priority());

        DefaultDomain owners = new DefaultDomain();
        owners.addPlayer(parseUuid(placed.ownerUuid()));
        region.setOwners(owners);
        placed.members().values().forEach(member -> region.getMembers().addPlayer(parseUuid(member.uuid())));
        if (definition != null) {
            applyFlags(region, definition.yaml());
            applyMissingDefaultFlags(region);
        }
        applyStoredWorldGuardFlags(region, placed);
        ProtectionWorldGuardFlagSupport.applyCompatibilityPassthrough(region);

        regionManager.addRegion(region);
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException exception) {
            regionManager.removeRegion(placed.regionId());
            plugin.getLogger().warning("Could not restore WorldGuard region '" + placed.regionId()
                    + "' after rent payment: " + exception.getMessage());
            return false;
        }
    }

    @SuppressWarnings("null")
    private void applyStoredWorldGuardFlags(ProtectedRegion region, PlacedProtection placed) {
        placed.flags().forEach((flagId, level) -> ProtectionFlagDefinition.fromId(flagId)
                .filter(ProtectionFlagDefinition::hasWorldGuardFlags)
                .ifPresent(definition -> {
                    for (StateFlag flag : definition.worldGuardFlags()) {
                        region.setFlag(flag, level.state());
                        region.setFlag(flag.getRegionGroupFlag(), level.group());
                    }
                }));
    }

    public FlagSnapshot getStateFlag(ProtectionMenuContext context, StateFlag flag) {
        ProtectedRegion region = getWorldGuardRegion(context);
        if (region == null) {
            return new FlagSnapshot(null, null);
        }
        return new FlagSnapshot(region.getFlag(flag), region.getFlag(flag.getRegionGroupFlag()));
    }

    public ProtectionFlagLevel getProtectionFlagLevel(ProtectionMenuContext context,
            ProtectionFlagDefinition definition) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed != null) {
            return getProtectionFlagLevel(placed, definition);
        }

        if (definition.hasWorldGuardFlags()) {
            FlagSnapshot snapshot = getStateFlag(context, definition.worldGuardFlags().get(0));
            return ProtectionFlagLevel.fromWorldGuard(snapshot.state(), snapshot.group(), definition.defaultLevel());
        }

        return definition.defaultLevel();
    }

    public boolean setProtectionFlag(ProtectionMenuContext context, ProtectionFlagDefinition definition,
            ProtectionFlagLevel level) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null) {
            return false;
        }

        if (definition.hasWorldGuardFlags()) {
            if (!saveWorldGuardFlags(context, definition, level)) {
                return false;
            }
        } else if (!saveWorldGuardCompatibilityFlags(context)) {
            return false;
        }

        int index = placedProtections.indexOf(placed);
        placedProtections.set(index, placed.withFlag(definition, level));
        return savePlacedProtections();
    }

    public boolean canUseFlag(Player player, Location location, ProtectionFlagDefinition definition) {
        if (player.hasPermission(MaxProtectPermissions.ADMIN)) {
            return true;
        }

        PlacedProtection placed = findProtectionAt(location);
        if (placed == null) {
            return true;
        }

        ProtectionFlagLevel level = getProtectionFlagLevel(placed, definition);
        ProtectionMemberRank rank = rankFor(player, placed).orElse(null);
        return level.allows(rank == ProtectionMemberRank.OWNER,
                rank != null && rank.canUseMemberLevelFlags());
    }

    public ProtectionFlagLevel getToggleFlagLevel(Location location, ProtectionFlagDefinition definition) {
        PlacedProtection placed = findProtectionAt(location);
        if (placed == null) {
            return definition.defaultLevel();
        }
        return getProtectionFlagLevel(placed, definition);
    }

    public ProtectionFlightResult toggleProtectionFlight(Player player) {
        UUID playerId = player.getUniqueId();
        ProtectionFlightSession active = protectionFlightSessions.get(playerId);
        if (active != null) {
            disableProtectionFlight(player, false);
            return new ProtectionFlightResult(ProtectionFlightStatus.DISABLED, null);
        }

        PlacedProtection placed = findProtectionAt(player.getLocation());
        if (placed == null) {
            return new ProtectionFlightResult(ProtectionFlightStatus.NOT_IN_PROTECTION, null);
        }
        if (!canAccess(player, placed)) {
            return new ProtectionFlightResult(ProtectionFlightStatus.NOT_ACCESSIBLE, toContext(placed));
        }

        protectionFlightSessions.put(playerId, new ProtectionFlightSession(
                placed.regionId(), player.getAllowFlight(), player.isFlying()));
        currentRegionByPlayer.put(playerId, placed.regionId());
        player.setAllowFlight(true);
        player.setFlying(true);
        return new ProtectionFlightResult(ProtectionFlightStatus.ENABLED, toContext(placed));
    }

    private void validateProtectionFlight(Player player, Location location, boolean notify) {
        ProtectionFlightSession session = protectionFlightSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        PlacedProtection placed = findProtectionAt(location);
        if (placed != null && placed.regionId().equalsIgnoreCase(session.regionId()) && canAccess(player, placed)) {
            return;
        }

        disableProtectionFlight(player, notify);
    }

    private void disableProtectionFlight(Player player, boolean notify) {
        ProtectionFlightSession session = protectionFlightSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        player.setAllowFlight(session.allowFlight());
        player.setFlying(session.allowFlight() && session.flying());
        player.setFallDistance(0.0F);
        if (notify && player.isOnline()) {
            send(player, plugin.getConfigManager().getMsgProtectionFlyLeftRegion());
        }
    }

    private void disableProtectionFlight(UUID playerId, String regionId, boolean notify) {
        ProtectionFlightSession session = protectionFlightSessions.get(playerId);
        if (session == null || !session.regionId().equalsIgnoreCase(regionId)) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            disableProtectionFlight(player, notify);
        } else {
            protectionFlightSessions.remove(playerId);
        }
    }

    @SuppressWarnings("null")
    private void disableProtectionFlightsInRegion(String regionId, boolean notify) {
        protectionFlightSessions.entrySet().stream()
                .filter(entry -> entry.getValue().regionId().equalsIgnoreCase(regionId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(playerId -> disableProtectionFlight(playerId, regionId, notify));
    }

    public void notifyIntrusionAttempt(Player intruder, Location location, String action) {
        PlacedProtection placed = findProtectionAt(location);
        if (placed == null || placed.ownerUuid().equals(intruder.getUniqueId().toString())) {
            return;
        }

        Player owner = Bukkit.getPlayer(parseUuid(placed.ownerUuid()));
        if (owner == null || !owner.isOnline()) {
            return;
        }

        String key = placed.regionId() + ":" + intruder.getUniqueId() + ":" + action.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        long nextAlert = intrusionAlertCooldowns.getOrDefault(key, 0L);
        if (nextAlert > now) {
            return;
        }
        intrusionAlertCooldowns.put(key, now + 10_000L);
        owner.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMsgProtectionIntrusionAlert()
                        .replace("%player%", intruder.getName())
                        .replace("%action%", action)
                        .replace("%alias%", displayNameForLookup(placed))
                        .replace("%region%", placed.regionId())));
        recordProtectionEvent(placed.regionId(), intruder.getUniqueId().toString(), intruder.getName(),
                placed.ownerUuid(), placed.ownerName(), "INTRUSION_ALERT", action);
    }

    public ProtectionReport protectionReport() {
        ProtectionStorage.ReportSnapshot cached = cache.getReportSnapshot();
        ProtectionStorage.ReportSnapshot snapshot = cached != null ? cached : storage.reportSnapshot();
        if (cached == null) {
            cache.putReportSnapshot(snapshot);
        }
        int active = 0;
        for (PlacedProtection placed : placedProtections) {
            if (getWorldGuardRegion(placed) != null) {
                active++;
            }
        }
        return new ProtectionReport(snapshot.totalProtections(), active,
                Math.max(0, snapshot.totalProtections() - active),
                snapshot.topOwners(), snapshot.largestProtections(), snapshot.eventCount());
    }

    public MigrationReport previewProtectionStonesMigration() {
        return scanProtectionStonesMigration(false);
    }

    public MigrationReport applyProtectionStonesMigration() {
        return scanProtectionStonesMigration(true);
    }

    @SuppressWarnings("null")
    private MigrationReport scanProtectionStonesMigration(boolean apply) {
        ProtectionStonesMigrationScanner.ScanResult scan = new ProtectionStonesMigrationScanner(plugin).scan();
        List<MigrationDecision> decisions = new ArrayList<>(scan.rejected());
        if (scan.candidates().isEmpty() && decisions.isEmpty()) {
            decisions.add(new MigrationDecision("*", "*", MigrationStatus.WARNING,
                    "no ProtectionStones WorldGuard regions were found; check that ProtectionStones is enabled and regions are loaded"));
        }
        List<PlacedProtection> original = new ArrayList<>(placedProtections);
        int imported = 0;
        File protectionsFolder = new File(plugin.getDataFolder(), "protections");

        for (ProtectionStonesCandidate candidate : scan.candidates()) {
            PlacedProtection existing = findProtection(candidate.regionId());
            if (existing != null && !canAttachMigrationYaml(existing)) {
                decisions.add(new MigrationDecision(candidate.regionId(), candidate.worldName(),
                        MigrationStatus.ALREADY_IMPORTED, "already tracked by MaxProtect"));
                continue;
            }
            YamlResolution yamlResolution = ProtectionStonesMigrationYamlSupport.plan(protectionsFolder, candidate);
            if (!apply) {
                decisions.add(new MigrationDecision(candidate.regionId(), candidate.worldName(),
                        previewMigrationStatus(scan.protectionStonesEnabled(), existing),
                        previewMigrationReason(scan.protectionStonesEnabled(), existing, yamlResolution)));
                continue;
            }
            try {
                yamlResolution = ProtectionStonesMigrationYamlSupport.resolveOrCreate(
                        protectionsFolder,
                        candidate,
                        plugin.getConfigManager().getProtectionItemLore(),
                        plugin.getConfigManager().getProtectionDefaultActionbarEnter(),
                        plugin.getConfigManager().getProtectionDefaultActionbarExit());
            } catch (IOException exception) {
                decisions.add(new MigrationDecision(candidate.regionId(), candidate.worldName(),
                        MigrationStatus.FAILED, "could not create migration YAML: " + exception.getMessage()));
                continue;
            }
            if (existing == null) {
                placedProtections.add(fromProtectionStonesCandidate(candidate, yamlResolution.protectionId()));
            } else {
                placedProtections.set(placedProtections.indexOf(existing),
                        existing.withProtectionId(yamlResolution.protectionId(), candidate.material()));
            }
            imported++;
            decisions.add(new MigrationDecision(candidate.regionId(), candidate.worldName(),
                    MigrationStatus.IMPORTED,
                    (existing == null ? "imported using " : "attached migration YAML using ")
                            + yamlReason(yamlResolution)));
        }

        if (apply && imported > 0) {
            placedProtections.sort(Comparator.comparing(PlacedProtection::regionId));
            if (!savePlacedProtections()) {
                placedProtections.clear();
                placedProtections.addAll(original);
                decisions.add(new MigrationDecision("*", "*", MigrationStatus.FAILED,
                        "could not save protection database; rolled back imported protections"));
            } else {
                ensureCompatibilityFlagsForPlacedProtections();
            }
        }

        return MigrationReport.from(decisions);
    }

    private boolean canAttachMigrationYaml(PlacedProtection placed) {
        return placed != null
                && placed.isLegacyImported()
                && !protectionFileExists(placed.protectionId());
    }

    private MigrationStatus previewMigrationStatus(boolean protectionStonesEnabled, PlacedProtection existing) {
        return protectionStonesEnabled && existing == null ? MigrationStatus.IMPORTABLE : MigrationStatus.WARNING;
    }

    private String previewMigrationReason(boolean protectionStonesEnabled, PlacedProtection existing,
            YamlResolution yamlResolution) {
        String action = existing == null ? "ready to import using " : "already tracked; apply will attach ";
        if (protectionStonesEnabled) {
            return action + yamlReason(yamlResolution);
        }
        return "ProtectionStones is not enabled; using WorldGuard data only with " + yamlReason(yamlResolution);
    }

    private String yamlReason(YamlResolution yamlResolution) {
        return (yamlResolution.created() ? "created YAML " : "existing YAML ")
                + yamlResolution.protectionId() + ".yml";
    }

    private PlacedProtection fromProtectionStonesCandidate(ProtectionStonesCandidate candidate, String protectionId) {
        Map<String, ProtectionMemberEntry> members = new LinkedHashMap<>();
        for (MigrationMember member : candidate.members()) {
            members.put(member.uuid(), new ProtectionMemberEntry(member.uuid(), member.name(),
                    ProtectionMemberRank.MEMBER));
        }
        return new PlacedProtection(
                candidate.regionId(),
                protectionId,
                candidate.alias() == null ? "" : candidate.alias(),
                candidate.ownerUuid(),
                candidate.ownerName(),
                candidate.worldName(),
                candidate.stoneLocation().x(),
                candidate.stoneLocation().y(),
                candidate.stoneLocation().z(),
                candidate.minX(),
                candidate.minY(),
                candidate.minZ(),
                candidate.maxX(),
                candidate.maxY(),
                candidate.maxZ(),
                System.currentTimeMillis(),
                "",
                "",
                candidate.customHome(),
                candidate.homeX(),
                candidate.homeY(),
                candidate.homeZ(),
                0.0F,
                0.0F,
                new LinkedHashMap<>(),
                ProtectionMigrationSupport.LEGACY_SOURCE,
                candidate.material(),
                members,
                0L,
                false);
    }

    public String flagLabel(ProtectionMenuContext context, ProtectionFlagDefinition definition) {
        return plugin.getConfigManager().getProtectionFlagLabel(getProtectionFlagLevel(context, definition),
                definition.mode() == ProtectionFlagMode.TOGGLE);
    }

    public Optional<ItemStack> displayBlockItem(String protectionId) {
        ProtectionDefinition definition = loadProtection(protectionId);
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = definition.blockItem().clone();
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(protectionIdKey);
            meta.getPersistentDataContainer().remove(protectionRadiusKey);
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    public Optional<ItemStack> displayBlockItem(ProtectionMenuContext context) {
        if (context == null) {
            return Optional.empty();
        }

        Optional<ItemStack> configuredItem = displayBlockItem(context.protectionId());
        if (configuredItem.isPresent()) {
            return configuredItem;
        }

        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null || !placed.isLegacyImported()) {
            return Optional.empty();
        }

        Material material = Material.matchMaterial(placed.legacyMaterial());
        if (material == null || !material.isBlock() || material.isAir()) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(material));
    }

    private ProtectionFlagLevel getProtectionFlagLevel(PlacedProtection placed, ProtectionFlagDefinition definition) {
        ProtectionFlagLevel storedLevel = placed.flags().get(definition.id());
        if (storedLevel != null) {
            return storedLevel;
        }

        if (definition.hasWorldGuardFlags()) {
            RegionManager regionManager = getRegionManager(placed.worldName());
            if (regionManager != null) {
                ProtectedRegion region = regionManager.getRegion(placed.regionId());
                if (region != null) {
                    StateFlag flag = definition.worldGuardFlags().get(0);
                    return ProtectionFlagLevel.fromWorldGuard(region.getFlag(flag),
                            region.getFlag(flag.getRegionGroupFlag()), definition.defaultLevel());
                }
            }
        }

        return definition.defaultLevel();
    }

    private boolean saveWorldGuardFlags(ProtectionMenuContext context, ProtectionFlagDefinition definition,
            ProtectionFlagLevel level) {
        RegionManager regionManager = getRegionManager(context.worldName());
        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = regionManager.getRegion(context.regionId());
        if (region == null) {
            return false;
        }

        for (StateFlag flag : definition.worldGuardFlags()) {
            region.setFlag(flag, level.state());
            region.setFlag(flag.getRegionGroupFlag(), level.group());
        }
        ProtectionWorldGuardFlagSupport.applyCompatibilityPassthrough(region);

        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException exception) {
            plugin.getLogger()
                    .warning("Could not save flags for WorldGuard region '" + context.regionId() + "': "
                            + exception.getMessage());
            return false;
        }
    }

    private boolean saveWorldGuardCompatibilityFlags(ProtectionMenuContext context) {
        RegionManager regionManager = getRegionManager(context.worldName());
        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = regionManager.getRegion(context.regionId());
        if (region == null) {
            return false;
        }

        if (!ProtectionWorldGuardFlagSupport.applyCompatibilityPassthrough(region)) {
            return true;
        }

        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException exception) {
            plugin.getLogger()
                    .warning("Could not save compatibility flags for WorldGuard region '" + context.regionId() + "': "
                            + exception.getMessage());
            return false;
        }
    }

    private boolean isOwner(Player player, PlacedProtection placed) {
        return placed.ownerUuid().equals(player.getUniqueId().toString());
    }

    private boolean canAccess(Player player, PlacedProtection placed) {
        return player.hasPermission(MaxProtectPermissions.ADMIN) || rankFor(player, placed).isPresent();
    }

    private boolean canManageProtection(Player player, PlacedProtection placed) {
        if (player.hasPermission(MaxProtectPermissions.ADMIN)) {
            return true;
        }
        return rankFor(player, placed)
                .map(rank -> rank.canManageMembers() || rank.canChangeFlags())
                .orElse(false);
    }

    private Optional<ProtectionMemberRank> rankFor(Player player, PlacedProtection placed) {
        if (isOwner(player, placed)) {
            return Optional.of(ProtectionMemberRank.OWNER);
        }

        String playerId = player.getUniqueId().toString();
        ProtectionMemberEntry entry = placed.members().get(playerId);
        if (entry != null) {
            return Optional.of(entry.rank());
        }

        ProtectedRegion region = getWorldGuardRegion(placed);
        if (region != null && region.getMembers().contains(player.getUniqueId())) {
            return Optional.of(ProtectionMemberRank.MEMBER);
        }
        return Optional.empty();
    }

    public ProtectionMenuContext findProtectionAt(Player player) {
        PlacedProtection placed = findProtectionAt(player.getLocation());
        return placed == null ? null : toContext(placed);
    }

    public boolean isProtectionStone(Block block) {
        return findProtectionStone(block) != null;
    }

    public ProtectionMenuContext findManageableProtectionAt(Player player) {
        PlacedProtection placed = findProtectionAt(player.getLocation());
        return placed != null && canManageProtection(player, placed) ? toContext(placed) : null;
    }

    @SuppressWarnings("null")
    public ProtectionMenuContext findAccessibleProtection(Player player, String lookup) {
        if (lookup == null || lookup.isBlank()) {
            return null;
        }

        String normalized = lookup.toLowerCase(Locale.ROOT);
        return placedProtections.stream()
                .filter(placed -> canAccess(player, placed))
                .filter(placed -> matchesLookup(placed, normalized))
                .sorted(Comparator.comparing(PlacedProtection::regionId))
                .map(this::toContext)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("null")
    public ProtectionMenuContext findPlacedProtection(String lookup) {
        if (lookup == null || lookup.isBlank()) {
            return null;
        }

        String normalized = lookup.toLowerCase(Locale.ROOT);
        return placedProtections.stream()
                .filter(placed -> matchesLookup(placed, normalized))
                .sorted(Comparator.comparing(PlacedProtection::regionId))
                .map(this::toContext)
                .findFirst()
                .orElse(null);
    }

    public List<ProtectionMenuContext> listOwnedProtections(Player player) {
        return placedProtections.stream()
                .filter(placed -> isOwner(player, placed))
                .sorted(Comparator.comparing(placed -> displayNameForLookup(placed).toLowerCase(Locale.ROOT)))
                .map(this::toContext)
                .toList();
    }

    public int countOwnedProtections(Player player) {
        return (int) placedProtections.stream()
                .filter(placed -> isOwner(player, placed))
                .count();
    }

    public List<ProtectionMenuContext> listAccessibleProtections(Player player) {
        return placedProtections.stream()
                .filter(placed -> canAccess(player, placed))
                .sorted(Comparator.comparing(placed -> displayNameForLookup(placed).toLowerCase(Locale.ROOT)))
                .map(this::toContext)
                .toList();
    }

    @SuppressWarnings("null")
    public List<ProtectionMenuContext> listPlacedProtections() {
        return placedProtections.stream()
                .sorted(Comparator.comparing(PlacedProtection::regionId))
                .map(this::toContext)
                .toList();
    }

    @SuppressWarnings("null")
    public List<String> listPlacedRegionIds() {
        return placedProtections.stream()
                .map(PlacedProtection::regionId)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<ProtectionStorage.ProtectionEventLog> recentEvents(ProtectionMenuContext context, int limit) {
        if (context == null) {
            return List.of();
        }
        List<ProtectionStorage.ProtectionEventLog> cached = cache.getRecentEvents(context.regionId(), limit);
        if (cached != null) {
            return cached;
        }
        List<ProtectionStorage.ProtectionEventLog> events = storage.recentEvents(context.regionId(), limit);
        cache.putRecentEvents(context.regionId(), limit, events);
        return events;
    }

    public ProtectionAdminState adminState(ProtectionMenuContext context) {
        if (context == null) {
            return new ProtectionAdminState(false, false, false);
        }
        PlacedProtection placed = findProtection(context.regionId());
        return new ProtectionAdminState(
                getWorldGuardRegion(context) != null,
                placed != null && (protectionFileExists(context.protectionId()) || placed.isLegacyImported()),
                placed != null && hasMatchingStoneBlock(placed));
    }

    @SuppressWarnings("null")
    public List<String> debugProtection(String lookup) {
        if (lookup == null || lookup.isBlank()) {
            return List.of(plugin.getConfigManager().getMsgAdminDebugUsage());
        }

        String normalized = lookup.toLowerCase(Locale.ROOT);
        PlacedProtection placed = placedProtections.stream()
                .filter(candidate -> matchesLookup(candidate, normalized))
                .findFirst()
                .orElse(null);
        WorldGuardLookup worldGuardLookup = placed == null
                ? findWorldGuardRegion(lookup)
                : findWorldGuardRegion(placed.worldName(), placed.regionId());

        if (placed == null && worldGuardLookup == null) {
            return List.of(applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminDebugNotFound(),
                    Map.of("%lookup%", lookup)));
        }

        List<String> lines = new ArrayList<>();
        String regionId = placed == null ? worldGuardLookup.region().getId() : placed.regionId();
        lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugHeader(),
                Map.of("%region%", regionId)));

        if (placed == null) {
            lines.add(plugin.getConfigManager().getAdminDebugTrackingMissing());
        } else {
            lines.add(plugin.getConfigManager().getAdminDebugTrackingPresent());
            Map<String, String> placeholders = placedDebugPlaceholders(placed);
            placeholders.put("%state%", status(protectionFileExists(placed.protectionId())));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugProtectionYaml(), placeholders));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugOwner(), placeholders));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugMembers(), placeholders));
            for (ProtectionMemberEntry member : memberEntries(placed)) {
                Map<String, String> memberPlaceholders = new LinkedHashMap<>(placeholders);
                memberPlaceholders.put("%member%", member.name());
                memberPlaceholders.put("%member_uuid%", member.uuid().toString());
                memberPlaceholders.put("%rank%", member.rank().name().toLowerCase(Locale.ROOT));
                lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugMemberEntry(),
                        memberPlaceholders));
            }
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorld(), placeholders));
            placeholders.put("%state%", status(hasMatchingStoneBlock(placed)));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugStone(), placeholders));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugBounds(), placeholders));
            lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugCreated(), placeholders));
        }

        if (worldGuardLookup == null) {
            lines.add(plugin.getConfigManager().getAdminDebugWorldGuardMissing());
            return lines;
        }

        ProtectedRegion region = worldGuardLookup.region();
        Map<String, String> worldGuardPlaceholders = worldGuardDebugPlaceholders(worldGuardLookup);
        lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorldGuardPresent(),
                worldGuardPlaceholders));
        lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorldGuardOwners(),
                worldGuardPlaceholders));
        region.getOwners().getUniqueIds().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(owner -> {
                    Map<String, String> ownerPlaceholders = new LinkedHashMap<>(worldGuardPlaceholders);
                    ownerPlaceholders.put("%owner%", displayName(owner));
                    ownerPlaceholders.put("%owner_uuid%", owner.toString());
                    lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorldGuardOwnerEntry(),
                            ownerPlaceholders));
                });
        lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorldGuardMembers(),
                worldGuardPlaceholders));
        lines.add(applyDebugPlaceholders(plugin.getConfigManager().getAdminDebugWorldGuardBounds(),
                worldGuardPlaceholders));
        return lines;
    }

    @SuppressWarnings("null")
    public RepairReport repairPlacedProtections(boolean cleanup) {
        List<PlacedProtection> original = new ArrayList<>(placedProtections);
        List<String> lines = new ArrayList<>();
        int repaired = 0;
        int cleaned = 0;
        int skipped = 0;

        for (PlacedProtection placed : new ArrayList<>(placedProtections)) {
            World world = plugin.getServer().getWorld(placed.worldName());
            RegionManager regionManager = world == null ? null : getRegionManager(placed.worldName());
            if (world == null || regionManager == null) {
                skipped++;
                lines.add(applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminRepairSkippedWorldUnavailable(),
                        Map.of("%region%", placed.regionId())));
                continue;
            }
            if (regionManager.getRegion(placed.regionId()) == null) {
                if (cleanup) {
                    placedProtections.remove(placed);
                    currentRegionByPlayer.values().removeIf(regionId -> regionId.equalsIgnoreCase(placed.regionId()));
                    cleaned++;
                    lines.add(applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminRepairCleanedStaleTracker(),
                            Map.of("%region%", placed.regionId())));
                } else {
                    skipped++;
                    lines.add(applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminRepairOrphanTracker(),
                            Map.of("%region%", placed.regionId())));
                }
            }
        }

        List<String> knownProtectionIds = listProtectionIds();
        for (World world : plugin.getServer().getWorlds()) {
            RegionManager regionManager = getRegionManager(world.getName());
            if (regionManager == null) {
                continue;
            }
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                if (findProtection(region.getId()) != null) {
                    continue;
                }
                if (ProtectionAdminSupport.inferProtectionId(region.getId(), knownProtectionIds).isEmpty()) {
                    continue;
                }
                RepairCandidate candidate = buildRepairCandidate(world, region, knownProtectionIds);
                if (candidate.placed() == null) {
                    skipped++;
                    lines.add(
                            applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminRepairSkippedWorldGuardRegion(),
                                    Map.of("%region%", region.getId(), "%reason%", candidate.reason())));
                    continue;
                }
                placedProtections.add(candidate.placed());
                repaired++;
                lines.add(applyDebugPlaceholders(plugin.getConfigManager().getMsgAdminRepairRepairedWorldGuardRegion(),
                        Map.of("%region%", region.getId())));
            }
        }

        if (repaired > 0 || cleaned > 0) {
            placedProtections.sort(Comparator.comparing(PlacedProtection::regionId));
            if (!savePlacedProtections()) {
                placedProtections.clear();
                placedProtections.addAll(original);
                return new RepairReport(repaired, cleaned, skipped, false,
                        List.of(plugin.getConfigManager().getMsgAdminRepairSaveRollback()));
            }
        }

        return new RepairReport(repaired, cleaned, skipped, true, lines);
    }

    public boolean isOwner(Player player, ProtectionMenuContext context) {
        return context != null && context.ownerUuid().equals(player.getUniqueId().toString());
    }

    public ProtectionMemberRank rankFor(Player player, ProtectionMenuContext context) {
        if (context == null) {
            return null;
        }
        PlacedProtection placed = findProtection(context.regionId());
        return placed == null ? null : rankFor(player, placed).orElse(null);
    }

    @SuppressWarnings("null")
    public boolean canManageMembers(Player player, ProtectionMenuContext context) {
        if (context == null) {
            return false;
        }
        if (player.hasPermission(MaxProtectPermissions.ADMIN)) {
            return true;
        }
        PlacedProtection placed = findProtection(context.regionId());
        return placed != null && rankFor(player, placed)
                .map(ProtectionMemberRank::canManageMembers)
                .orElse(false);
    }

    public boolean canChangeFlags(Player player, ProtectionMenuContext context) {
        if (context == null) {
            return false;
        }
        PlacedProtection placed = findProtection(context.regionId());
        ProtectionMemberRank rank = placed == null ? null : rankFor(player, placed).orElse(null);
        return canChangeFlags(
                player.hasPermission(MaxProtectPermissions.ADMIN),
                player.hasPermission(MaxProtectPermissions.FLAGS),
                rank);
    }

    static boolean canChangeFlags(boolean hasAdminPermission, boolean hasFlagsPermission, ProtectionMemberRank rank) {
        if (hasAdminPermission) {
            return true;
        }
        return hasFlagsPermission && rank != null && rank.canChangeFlags();
    }

    public boolean isAliasAvailable(String alias, String currentRegionId) {
        return placedProtections.stream()
                .noneMatch(placed -> !placed.regionId().equalsIgnoreCase(currentRegionId)
                        && !placed.alias().isBlank()
                        && placed.alias().equalsIgnoreCase(alias));
    }

    public boolean setAlias(ProtectionMenuContext context, String alias) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null) {
            return false;
        }

        int index = placedProtections.indexOf(placed);
        placedProtections.set(index, placed.withAlias(alias));
        return savePlacedProtections();
    }

    @SuppressWarnings("null")
    public OwnershipTransferResult transferOwnership(ProtectionMenuContext context, Player actor,
            OfflinePlayer target) {
        PlacedProtection placed = findProtection(context.regionId());
        ProtectedRegion region = getWorldGuardRegion(context);
        RegionManager regionManager = getRegionManager(context.worldName());
        if (placed == null || region == null || regionManager == null) {
            return new OwnershipTransferResult(OwnershipTransferStatus.REGION_UNAVAILABLE, null);
        }
        UUID targetId = target == null ? null : target.getUniqueId();
        if (!ProtectionCommandSupport.canTransferOwnership(
                placed.ownerUuid().equals(actor.getUniqueId().toString()),
                targetId != null && placed.ownerUuid().equals(targetId.toString()),
                targetId != null)) {
            if (!placed.ownerUuid().equals(actor.getUniqueId().toString())) {
                return new OwnershipTransferResult(OwnershipTransferStatus.NOT_OWNER, toContext(placed));
            }
            if (targetId == null) {
                return new OwnershipTransferResult(OwnershipTransferStatus.TARGET_UNKNOWN, toContext(placed));
            }
            return new OwnershipTransferResult(OwnershipTransferStatus.TARGET_IS_OWNER, toContext(placed));
        }

        String targetName = target.getName() == null ? targetId.toString() : target.getName();
        Set<UUID> previousOwners = new HashSet<>(region.getOwners().getUniqueIds());
        Set<UUID> previousMembers = new HashSet<>(region.getMembers().getUniqueIds());
        PlacedProtection previousPlaced = placed;
        int index = placedProtections.indexOf(placed);

        DefaultDomain newOwners = new DefaultDomain();
        newOwners.addPlayer(targetId);
        region.setOwners(newOwners);
        region.getMembers().removePlayer(targetId);
        region.getMembers().addPlayer(actor.getUniqueId());

        try {
            regionManager.saveChanges();
        } catch (StorageException exception) {
            restoreDomain(region, previousOwners, previousMembers);
            plugin.getLogger().warning("Could not save owner transfer for WorldGuard region '" + context.regionId()
                    + "': " + exception.getMessage());
            return new OwnershipTransferResult(OwnershipTransferStatus.SAVE_ERROR, toContext(placed));
        }

        PlacedProtection updated = placed.withOwner(targetId, targetName, actor.getUniqueId(), actor.getName());
        placedProtections.set(index, updated);
        if (savePlacedProtections()) {
            recordProtectionEvent(updated.regionId(), actor.getUniqueId().toString(), actor.getName(),
                    targetId.toString(), targetName, "OWNER_TRANSFERRED",
                    plugin.getConfigManager().getProtectionEventOwnerTransferred()
                            .replace("%old_owner%", actor.getName())
                            .replace("%new_owner%", targetName));
            return new OwnershipTransferResult(OwnershipTransferStatus.SUCCESS, toContext(updated));
        }

        placedProtections.set(index, previousPlaced);
        restoreDomain(region, previousOwners, previousMembers);
        try {
            regionManager.saveChanges();
        } catch (StorageException exception) {
            plugin.getLogger().warning("Could not roll back owner transfer for WorldGuard region '"
                    + context.regionId() + "': " + exception.getMessage());
        }
        return new OwnershipTransferResult(OwnershipTransferStatus.SAVE_ERROR, toContext(previousPlaced));
    }

    @SuppressWarnings("null")
    public RemoveResult removeProtection(Player player, ProtectionMenuContext context) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null) {
            return RemoveResult.REGION_UNAVAILABLE;
        }
        if (!placed.ownerUuid().equals(player.getUniqueId().toString())
                && !player.hasPermission(MaxProtectPermissions.PROTECTION_REMOVE_OTHERS)) {
            return RemoveResult.NOT_OWNER;
        }

        ItemStack returnItem = buildReturnItem(placed);
        if (returnItem == null) {
            return RemoveResult.INVALID_CONFIG;
        }
        if (!canFit(player.getInventory(), returnItem)) {
            return RemoveResult.INVENTORY_FULL;
        }

        World world = plugin.getServer().getWorld(placed.worldName());
        if (world == null) {
            return RemoveResult.WORLD_UNAVAILABLE;
        }

        int index = placedProtections.indexOf(placed);
        placedProtections.remove(index);
        if (!savePlacedProtections()) {
            placedProtections.add(index, placed);
            placedProtections.sort(Comparator.comparing(PlacedProtection::regionId));
            return RemoveResult.SAVE_ERROR;
        }
        if (!removeWorldGuardRegion(world, placed.regionId())) {
            placedProtections.add(index, placed);
            placedProtections.sort(Comparator.comparing(PlacedProtection::regionId));
            savePlacedProtections();
            return RemoveResult.SAVE_ERROR;
        }

        currentRegionByPlayer.values().removeIf(regionId -> regionId.equalsIgnoreCase(placed.regionId()));
        disableProtectionFlightsInRegion(placed.regionId(), true);
        if (placed.hasStoneLocation()) {
            Block stone = world.getBlockAt(placed.stoneX(), placed.stoneY(), placed.stoneZ());
            spawnVisualEffect(player, stone.getLocation(),
                    plugin.getConfigManager().getProtectionFeedbackConfig().removeEffect());
            stone.setType(Material.AIR);
        }
        player.getInventory().addItem(returnItem);
        return RemoveResult.SUCCESS;
    }

    private ItemStack buildReturnItem(PlacedProtection placed) {
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        if (definition != null) {
            return buildProtectionItem(definition, 1);
        }
        if (!placed.isLegacyImported()) {
            return null;
        }
        Material material = Material.matchMaterial(placed.legacyMaterial());
        if (material == null || !material.isBlock() || material.isAir()) {
            return null;
        }
        return new ItemStack(material);
    }

    public HomeUpdateResult setHome(ProtectionMenuContext context, Location location) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null) {
            return HomeUpdateResult.REGION_UNAVAILABLE;
        }
        if (!placed.contains(location)) {
            return HomeUpdateResult.NOT_INSIDE;
        }

        int index = placedProtections.indexOf(placed);
        placedProtections.set(index, placed.withHome(location));
        return savePlacedProtections() ? HomeUpdateResult.SUCCESS : HomeUpdateResult.SAVE_ERROR;
    }

    public Optional<Location> homeLocation(ProtectionMenuContext context) {
        World world = plugin.getServer().getWorld(context.worldName());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, context.homeX(), context.homeY(), context.homeZ(),
                context.homeYaw(), context.homePitch()));
    }

    public Optional<Location> exitLocation(Player player, ProtectionMenuContext context) {
        World world = player.getWorld();
        if (!world.getName().equals(context.worldName())) {
            return Optional.empty();
        }

        Location current = player.getLocation();
        int distanceToMinX = Math.abs(current.getBlockX() - context.minX());
        int distanceToMaxX = Math.abs(context.maxX() - current.getBlockX());
        int distanceToMinZ = Math.abs(current.getBlockZ() - context.minZ());
        int distanceToMaxZ = Math.abs(context.maxZ() - current.getBlockZ());

        int x = current.getBlockX();
        int z = current.getBlockZ();
        int shortest = Math.min(Math.min(distanceToMinX, distanceToMaxX), Math.min(distanceToMinZ, distanceToMaxZ));
        if (shortest == distanceToMinX) {
            x = context.minX() - 1;
        } else if (shortest == distanceToMaxX) {
            x = context.maxX() + 1;
        } else if (shortest == distanceToMinZ) {
            z = context.minZ() - 1;
        } else {
            z = context.maxZ() + 1;
        }

        int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, current.getBlockY()));
        return Optional.of(new Location(world, x + 0.5, y, z + 0.5, current.getYaw(), current.getPitch()));
    }

    public ViewResult showBorder(Player player, ProtectionMenuContext context) {
        VisualEffect effect = plugin.getConfigManager().getProtectionFeedbackConfig().viewEffect();
        if (!effect.enabled()) {
            return ViewResult.SUCCESS;
        }

        long now = System.currentTimeMillis();
        long nextUse = viewCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (effect.cooldownSeconds() > 0 && nextUse > now) {
            return ViewResult.cooldown(Math.max(1L, (nextUse - now + 999L) / 1000L));
        }

        TaskWrapper previousTask = viewTasks.remove(player.getUniqueId());
        if (previousTask != null) {
            previousTask.cancel();
        }

        if (effect.cooldownSeconds() > 0) {
            viewCooldowns.put(player.getUniqueId(), now + effect.cooldownSeconds() * 1000L);
        }

        drawBorder(player, context, effect);
        if (effect.durationSeconds() <= 0) {
            return ViewResult.SUCCESS;
        }

        long durationTicks = effect.durationSeconds() * 20L;
        final long[] elapsedTicks = { 0L };
        TaskWrapper task = plugin.getSchedulerUtils().runAtEntityTimer(player, () -> {
            elapsedTicks[0] += effect.intervalTicks();
            if (!player.isOnline()
                    || elapsedTicks[0] >= durationTicks
                    || !player.getWorld().getName().equals(context.worldName())) {
                TaskWrapper finished = viewTasks.remove(player.getUniqueId());
                if (finished != null) {
                    finished.cancel();
                }
                return;
            }
            drawBorder(player, context, effect);
        }, effect.intervalTicks(), effect.intervalTicks());
        viewTasks.put(player.getUniqueId(), task);
        return ViewResult.SUCCESS;
    }

    private void refreshHeldProtectionPreview(Player player, boolean force) {
        ItemStack item = player.getInventory().getItemInMainHand();
        String protectionId = getProtectionId(item);
        if (protectionId == null) {
            previewCooldowns.remove(player.getUniqueId());
            endCinematicPreview(player.getUniqueId(), true);
            return;
        }

        long now = System.currentTimeMillis();
        long nextUse = previewCooldowns.getOrDefault(player.getUniqueId(), 0L);
        CinematicPreview cinematic = plugin.getConfigManager().getProtectionFeedbackConfig().cinematicPreview();
        if (!cinematic.enabled() && !force && nextUse > now) {
            return;
        }

        ProtectionDefinition definition = loadProtection(protectionId);
        if (definition == null) {
            return;
        }

        Location previewCenter = previewCenter(player);
        if (previewCenter == null) {
            return;
        }

        if (cinematic.enabled()) {
            startCinematicPreview(player, previewCenter, definition.radius(), cinematic);
            return;
        }

        VisualEffect effect = plugin.getConfigManager().getProtectionFeedbackConfig().viewEffect();
        if (!effect.enabled()) {
            return;
        }
        previewCooldowns.put(player.getUniqueId(), now + 1500L);
        drawBorder(player, previewCenter, definition.radius(), effect);
    }

    private Location previewCenter(Player player) {
        RayTraceResult result = player.rayTraceBlocks(6.0D);
        if (result == null || result.getHitBlock() == null) {
            return null;
        }

        Block block = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();
        Block target = face == null ? block : block.getRelative(face);
        return target.getLocation();
    }

    private void startCinematicPreview(Player player, Location center, int radius, CinematicPreview config) {
        UUID playerId = player.getUniqueId();
        if (previewSessions.containsKey(playerId)) {
            return;
        }

        long now = System.currentTimeMillis();
        long nextUse = cinematicPreviewCooldowns.getOrDefault(playerId, 0L);
        if (config.cooldownSeconds() > 0 && nextUse > now) {
            return;
        }

        World world = center.getWorld();
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        List<Location> border = borderLocations(world, center, radius);
        if (border.isEmpty()) {
            return;
        }

        Location camera = cinematicCameraLocation(player, center, border, config.heightOffset());
        PreviewSession session = new PreviewSession(
                player.getLocation().clone(),
                camera.clone(),
                border,
                player.getAllowFlight(),
                player.isFlying(),
                player.hasGravity(),
                player.getWalkSpeed(),
                player.getFlySpeed(),
                null);
        previewSessions.put(playerId, session);

        VisualEffect viewEffect = plugin.getConfigManager().getProtectionFeedbackConfig().viewEffect();
        if (config.useFakeBlocks()) {
            sendFakeBorder(player, border, config.borderMaterial().createBlockData());
        } else if (viewEffect.enabled()) {
            drawBorder(player, center, radius, viewEffect);
        } else {
            previewSessions.remove(playerId);
            return;
        }

        previewFallProtectionUntil.remove(playerId);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setGravity(false);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setWalkSpeed(0.0F);
        player.setFlySpeed(0.0F);
        player.setFallDistance(0.0F);
        teleportForPreview(player, camera);
        if (config.cooldownSeconds() > 0) {
            cinematicPreviewCooldowns.put(playerId, now + config.cooldownSeconds() * 1000L);
        }

        TaskWrapper task = config.useFakeBlocks()
                ? plugin.getSchedulerUtils().runAtEntityLater(player,
                        () -> endCinematicPreview(playerId, true), config.durationSeconds() * 20L)
                : scheduleParticlePreview(player, playerId, center.clone(), radius, viewEffect,
                        config.durationSeconds());
        previewSessions.put(playerId, session.withTask(task));
    }

    private TaskWrapper scheduleParticlePreview(Player player, UUID playerId, Location center, int radius,
            VisualEffect effect, int durationSeconds) {
        long durationTicks = durationSeconds * 20L;
        final long[] elapsedTicks = { 0L };
        return plugin.getSchedulerUtils().runAtEntityTimer(player, () -> {
            elapsedTicks[0] += effect.intervalTicks();
            if (!player.isOnline() || elapsedTicks[0] >= durationTicks) {
                endCinematicPreview(playerId, true);
                return;
            }
            drawBorder(player, center, radius, effect);
        }, effect.intervalTicks(), effect.intervalTicks());
    }

    private List<Location> borderLocations(World world, Location center, int radius) {
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;
        List<Location> border = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            addSurfaceBorderLocation(border, world, x, minZ);
            addSurfaceBorderLocation(border, world, x, maxZ);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            addSurfaceBorderLocation(border, world, minX, z);
            addSurfaceBorderLocation(border, world, maxX, z);
        }
        return border;
    }

    private void addSurfaceBorderLocation(List<Location> border, World world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(x, z) + 1));
        border.add(new Location(world, x, y, z));
    }

    private Location cinematicCameraLocation(Player player, Location center, List<Location> border,
            double heightOffset) {
        World world = center.getWorld();
        @SuppressWarnings("null")
        int highestY = border.stream()
                .mapToInt(Location::getBlockY)
                .max()
                .orElse(player.getLocation().getBlockY());
        double y = Math.max(world.getMinHeight() + 1.0D,
                Math.min(world.getMaxHeight() - 2.0D, highestY + heightOffset));
        Location camera = new Location(world, center.getBlockX() + 0.5D, y, center.getBlockZ() + 0.5D);
        camera.setYaw(player.getLocation().getYaw());
        camera.setPitch(75.0F);
        return camera;
    }

    private void sendFakeBorder(Player player, List<Location> border, BlockData blockData) {
        for (Location location : border) {
            player.sendBlockChange(location, blockData);
        }
    }

    private boolean lockCinematicMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PreviewSession session = previewSessions.get(player.getUniqueId());
        if (session == null || event.getTo() == null) {
            return false;
        }

        Location locked = session.cameraLocation().clone();
        locked.setYaw(event.getTo().getYaw());
        locked.setPitch(event.getTo().getPitch());
        if (sameLocation(event.getTo(), locked)) {
            return false;
        }
        event.setTo(locked);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFallDistance(0.0F);
        return true;
    }

    private boolean sameLocation(Location first, Location second) {
        return first.getWorld() != null
                && first.getWorld().equals(second.getWorld())
                && first.distanceSquared(second) < 0.0001D;
    }

    private boolean isCinematicPreviewing(Player player) {
        return player != null && previewSessions.containsKey(player.getUniqueId());
    }

    static boolean shouldCancelPreviewFallDamage(boolean previewing, long protectedUntilMillis, long nowMillis) {
        return previewing || protectedUntilMillis > nowMillis;
    }

    private void endCinematicPreview(UUID playerId, boolean restoreLocation) {
        PreviewSession session = previewSessions.remove(playerId);
        if (session == null) {
            return;
        }
        if (session.task() != null) {
            session.task().cancel();
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        for (Location location : session.borderLocations()) {
            if (location.getWorld() != null && player.getWorld().equals(location.getWorld())) {
                player.sendBlockChange(location, location.getBlock().getBlockData());
            }
        }

        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFallDistance(0.0F);

        if (restoreLocation
                && player.isOnline()
                && session.originalLocation().getWorld() != null
                && player.getWorld().equals(session.originalLocation().getWorld())) {
            teleportForPreview(player, session.originalLocation());
        }

        previewFallProtectionUntil.put(playerId, System.currentTimeMillis() + PREVIEW_FALL_DAMAGE_GRACE_MILLIS);
        player.setWalkSpeed(session.walkSpeed());
        player.setFlySpeed(session.flySpeed());
        player.setGravity(session.gravity());
        player.setAllowFlight(session.allowFlight());
        player.setFlying(session.flying() && session.allowFlight());
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setFallDistance(0.0F);
    }

    private void teleportForPreview(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        previewTeleports.add(playerId);
        try {
            plugin.getSchedulerUtils().teleportEntity(player, location);
        } finally {
            previewTeleports.remove(playerId);
        }
    }

    private void drawBorder(Player player, ProtectionMenuContext context, VisualEffect effect) {
        World world = player.getWorld();
        if (!world.getName().equals(context.worldName())) {
            return;
        }

        drawBorder(player, world, context.minX(), context.maxX(), context.minZ(), context.maxZ(), effect);
    }

    private void drawBorder(Player player, Location center, int radius, VisualEffect effect) {
        World world = center.getWorld();
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }
        drawBorder(player, world, center.getBlockX() - radius, center.getBlockX() + radius,
                center.getBlockZ() - radius, center.getBlockZ() + radius, effect);
    }

    private void drawBorder(Player player, World world, int minX, int maxX, int minZ, int maxZ, VisualEffect effect) {
        int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, player.getLocation().getBlockY()));
        for (int x = minX; x <= maxX; x++) {
            spawnBorderParticle(player, world, x, y, minZ, effect);
            spawnBorderParticle(player, world, x, y, maxZ, effect);
        }
        for (int z = minZ; z <= maxZ; z++) {
            spawnBorderParticle(player, world, minX, y, z, effect);
            spawnBorderParticle(player, world, maxX, y, z, effect);
        }
    }

    public MemberChangeResult addMember(ProtectionMenuContext context, UUID memberId) {
        return changeMember(context, memberId, displayName(memberId), ProtectionMemberRank.MEMBER, false, true);
    }

    public MemberChangeResult addMember(ProtectionMenuContext context, UUID memberId, String memberName) {
        return changeMember(context, memberId, memberName, ProtectionMemberRank.MEMBER, false, true);
    }

    public MemberChangeResult removeMember(ProtectionMenuContext context, UUID memberId) {
        return removeMember(context, memberId, true);
    }

    public MemberChangeResult removeMember(ProtectionMenuContext context, UUID memberId, boolean notifyTeleported) {
        PlacedProtection placed = findProtection(context.regionId());
        String memberName = placed == null || !placed.members().containsKey(memberId.toString())
                ? displayName(memberId)
                : placed.members().get(memberId.toString()).name();
        MemberChangeResult result = changeMember(context, memberId, memberName, null, true, false);
        if (result == MemberChangeResult.SUCCESS) {
            teleportRemovedMember(context, memberId, notifyTeleported);
            recordProtectionEvent(context.regionId(), null, "", memberId.toString(), memberName,
                    "MEMBER_REMOVED", plugin.getConfigManager().getProtectionEventMemberRemoved());
        }
        return result;
    }

    public InviteCreateResult createMemberInvite(ProtectionMenuContext context, Player inviter, Player invited) {
        PlacedProtection placed = findProtection(context.regionId());
        ProtectedRegion region = getWorldGuardRegion(context);
        if (placed == null || region == null) {
            return new InviteCreateResult(MemberInviteStatus.REGION_UNAVAILABLE, 0L);
        }
        if (placed.ownerUuid().equals(invited.getUniqueId().toString())
                || region.getOwners().contains(invited.getUniqueId())) {
            return new InviteCreateResult(MemberInviteStatus.TARGET_IS_OWNER, 0L);
        }
        if (placed.members().containsKey(invited.getUniqueId().toString())
                || region.getMembers().contains(invited.getUniqueId())) {
            return new InviteCreateResult(MemberInviteStatus.ALREADY_MEMBER, 0L);
        }

        long now = System.currentTimeMillis();
        Optional<ProtectionStorage.ProtectionInvite> existing = storage.findPendingInvite(
                placed.regionId(), invited.getUniqueId().toString(), now);
        if (existing.isPresent()) {
            return new InviteCreateResult(MemberInviteStatus.INVITE_EXISTS, existing.get().id());
        }

        try {
            long inviteId = storage.createInvite(new ProtectionStorage.ProtectionInvite(0L, placed.regionId(),
                    inviter.getUniqueId().toString(), inviter.getName(), invited.getUniqueId().toString(),
                    invited.getName(), now, now + 300_000L));
            cache.invalidateInvitesByPlayer(invited.getUniqueId().toString());
            recordProtectionEvent(placed.regionId(), inviter.getUniqueId().toString(), inviter.getName(),
                    invited.getUniqueId().toString(), invited.getName(), "INVITE_CREATED",
                    plugin.getConfigManager().getProtectionEventInviteCreated());
            return new InviteCreateResult(MemberInviteStatus.SUCCESS, inviteId);
        } catch (IllegalStateException exception) {
            plugin.getLogger().warning("Could not create protection invite: " + exception.getMessage());
            return new InviteCreateResult(MemberInviteStatus.SAVE_ERROR, 0L);
        }
    }

    public InviteResponseResult acceptMemberInvite(Player player, long inviteId) {
        Optional<ProtectionStorage.ProtectionInvite> invite = storage.findInvite(inviteId);
        if (invite.isEmpty()) {
            return new InviteResponseResult(InviteResponseStatus.INVALID, null, "");
        }
        ProtectionStorage.ProtectionInvite pending = invite.get();
        if (!pending.invitedUuid().equals(player.getUniqueId().toString())) {
            return new InviteResponseResult(InviteResponseStatus.NOT_FOR_YOU, null, pending.inviterName());
        }
        if (pending.expiresAtMillis() <= System.currentTimeMillis()) {
            storage.deleteInvite(inviteId);
            cache.invalidateInviteById(inviteId);
            cache.invalidateInvitesByPlayer(pending.invitedUuid());
            return new InviteResponseResult(InviteResponseStatus.EXPIRED, null, pending.inviterName());
        }
        PlacedProtection placed = findProtection(pending.regionId());
        if (placed == null) {
            storage.deleteInvite(inviteId);
            cache.invalidateInviteById(inviteId);
            cache.invalidateInvitesByPlayer(pending.invitedUuid());
            return new InviteResponseResult(InviteResponseStatus.INVALID, null, pending.inviterName());
        }

        ProtectionMenuContext context = toContext(placed);
        MemberChangeResult change = addMember(context, player.getUniqueId(), player.getName());
        if (change == MemberChangeResult.SUCCESS || change == MemberChangeResult.ALREADY_MEMBER) {
            storage.deleteInvite(inviteId);
            cache.invalidateInviteById(inviteId);
            cache.invalidateInvitesByPlayer(pending.invitedUuid());
            recordProtectionEvent(placed.regionId(), pending.inviterUuid(), pending.inviterName(),
                    player.getUniqueId().toString(), player.getName(), "INVITE_ACCEPTED",
                    plugin.getConfigManager().getProtectionEventInviteAccepted());
            return new InviteResponseResult(change == MemberChangeResult.SUCCESS
                    ? InviteResponseStatus.SUCCESS
                    : InviteResponseStatus.ALREADY_MEMBER, context, pending.inviterName());
        }
        return new InviteResponseResult(change == MemberChangeResult.REGION_UNAVAILABLE
                ? InviteResponseStatus.REGION_UNAVAILABLE
                : InviteResponseStatus.SAVE_ERROR, context, pending.inviterName());
    }

    public InviteResponseResult denyMemberInvite(Player player, long inviteId) {
        Optional<ProtectionStorage.ProtectionInvite> invite = storage.findInvite(inviteId);
        if (invite.isEmpty()) {
            return new InviteResponseResult(InviteResponseStatus.INVALID, null, "");
        }
        ProtectionStorage.ProtectionInvite pending = invite.get();
        if (!pending.invitedUuid().equals(player.getUniqueId().toString())) {
            return new InviteResponseResult(InviteResponseStatus.NOT_FOR_YOU, null, pending.inviterName());
        }
        storage.deleteInvite(inviteId);
        cache.invalidateInviteById(inviteId);
        cache.invalidateInvitesByPlayer(pending.invitedUuid());
        PlacedProtection placed = findProtection(pending.regionId());
        ProtectionMenuContext context = placed == null ? null : toContext(placed);
        recordProtectionEvent(pending.regionId(), pending.inviterUuid(), pending.inviterName(),
                player.getUniqueId().toString(), player.getName(), "INVITE_DENIED",
                plugin.getConfigManager().getProtectionEventInviteDenied());
        return new InviteResponseResult(InviteResponseStatus.SUCCESS, context, pending.inviterName());
    }

    public List<String> getPendingInviteIdsForPlayer(Player player) {
        long now = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        List<ProtectionStorage.ProtectionInvite> cached = cache.getInvitesByPlayer(uuid);
        List<ProtectionStorage.ProtectionInvite> invites = cached != null ? cached
                : storage.findPendingInvitesForPlayer(uuid, now);
        if (cached == null) {
            cache.putInvitesByPlayer(uuid, invites);
        }
        return invites.stream()
                .filter(invite -> invite.expiresAtMillis() > now)
                .map(invite -> String.valueOf(invite.id()))
                .toList();
    }

    public MemberChangeResult setMemberRank(ProtectionMenuContext context, UUID memberId,
            ProtectionMemberRank rank) {
        if (rank == null || rank == ProtectionMemberRank.OWNER) {
            return MemberChangeResult.TARGET_IS_OWNER;
        }
        PlacedProtection placed = findProtection(context.regionId());
        ProtectionMemberEntry existing = placed == null ? null : placed.members().get(memberId.toString());
        String memberName = existing == null ? displayName(memberId) : existing.name();
        return changeMember(context, memberId, memberName, rank, false, false);
    }

    @SuppressWarnings("null")
    public List<String> listMemberNames(ProtectionMenuContext context) {
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null) {
            return List.of();
        }
        return memberEntries(placed).stream()
                .map(ProtectionMemberEntry::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @SuppressWarnings("null")
    public List<ProtectionMemberInfo> listProtectionMembers(ProtectionMenuContext context) {
        List<ProtectionMemberInfo> members = new ArrayList<>();
        members.add(new ProtectionMemberInfo(parseUuid(context.ownerUuid()), context.ownerName(),
                ProtectionMemberRank.OWNER));
        PlacedProtection placed = findProtection(context.regionId());
        if (placed != null) {
            memberEntries(placed).stream()
                    .filter(entry -> !entry.uuid().equalsIgnoreCase(context.ownerUuid()))
                    .map(entry -> new ProtectionMemberInfo(parseUuid(entry.uuid()), entry.name(), entry.rank()))
                    .forEach(members::add);
        }

        return members.stream()
                .sorted(Comparator.comparing(ProtectionMemberInfo::owner).reversed()
                        .thenComparing(member -> member.rank() == ProtectionMemberRank.ADMIN ? 0 : 1)
                        .thenComparing(ProtectionMemberInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<ProtectionMemberEntry> memberEntries(PlacedProtection placed) {
        Map<String, ProtectionMemberEntry> members = new LinkedHashMap<>(placed.members());
        ProtectedRegion region = getWorldGuardRegion(placed);
        if (region != null) {
            region.getMembers().getUniqueIds().forEach(memberId -> members.putIfAbsent(memberId.toString(),
                    new ProtectionMemberEntry(memberId.toString(), displayName(memberId),
                            ProtectionMemberRank.MEMBER)));
        }
        return new ArrayList<>(members.values());
    }

    private boolean matchesLookup(PlacedProtection placed, String normalized) {
        return placed.regionId().equalsIgnoreCase(normalized)
                || placed.protectionId().equalsIgnoreCase(normalized)
                || (!placed.alias().isBlank() && placed.alias().equalsIgnoreCase(normalized));
    }

    private String displayNameForLookup(PlacedProtection placed) {
        return placed.alias().isBlank() ? placed.regionId() : placed.alias();
    }

    @SuppressWarnings("null")
    private RepairCandidate buildRepairCandidate(World world, ProtectedRegion region, List<String> knownProtectionIds) {
        Optional<String> protectionId = ProtectionAdminSupport.inferProtectionId(region.getId(), knownProtectionIds);
        if (protectionId.isEmpty()) {
            return RepairCandidate.skip(plugin.getConfigManager().getMsgAdminRepairReasonRegionIdMismatch());
        }

        ProtectionDefinition definition = loadProtection(protectionId.get());
        if (definition == null) {
            return RepairCandidate.skip(plugin.getConfigManager().getMsgAdminRepairReasonProtectionYamlInvalid());
        }

        @SuppressWarnings("null")
        List<UUID> owners = region.getOwners().getUniqueIds().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        if (owners.isEmpty()) {
            return RepairCandidate.skip(plugin.getConfigManager().getMsgAdminRepairReasonNoOwner());
        }

        int centerX = (region.getMinimumPoint().x() + region.getMaximumPoint().x()) / 2;
        int centerZ = (region.getMinimumPoint().z() + region.getMaximumPoint().z()) / 2;
        int stoneY = findRepairStoneY(world, centerX, centerZ, definition.blockItem().getType());
        if (stoneY == Integer.MIN_VALUE) {
            return RepairCandidate.skip(plugin.getConfigManager().getMsgAdminRepairReasonStoneNotFound());
        }

        UUID ownerId = owners.get(0);
        Map<String, ProtectionMemberEntry> members = new LinkedHashMap<>();
        region.getMembers().getUniqueIds().stream()
                .filter(memberId -> !memberId.equals(ownerId))
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(memberId -> members.put(memberId.toString(),
                        new ProtectionMemberEntry(memberId.toString(), displayName(memberId),
                                ProtectionMemberRank.MEMBER)));

        return RepairCandidate.repaired(PlacedProtection.fromWorldGuard(
                region,
                definition,
                ownerId,
                displayName(ownerId),
                world.getName(),
                centerX,
                stoneY,
                centerZ,
                members));
    }

    private int findRepairStoneY(World world, int x, int z, Material material) {
        int foundY = Integer.MIN_VALUE;
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            if (world.getBlockAt(x, y, z).getType() != material) {
                continue;
            }
            if (foundY != Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            foundY = y;
        }
        return foundY;
    }

    private boolean hasMatchingStoneBlock(PlacedProtection placed) {
        if (!placed.hasStoneLocation()) {
            return false;
        }
        World world = plugin.getServer().getWorld(placed.worldName());
        if (world == null) {
            return false;
        }
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        if (definition == null) {
            if (!placed.isLegacyImported()) {
                return false;
            }
            Material material = Material.matchMaterial(placed.legacyMaterial());
            return material != null
                    && material.isBlock()
                    && !material.isAir()
                    && world.getBlockAt(placed.stoneX(), placed.stoneY(), placed.stoneZ()).getType() == material;
        }
        return world.getBlockAt(placed.stoneX(), placed.stoneY(), placed.stoneZ()).getType() == definition.blockItem()
                .getType();
    }

    private WorldGuardLookup findWorldGuardRegion(String regionId) {
        for (World world : plugin.getServer().getWorlds()) {
            WorldGuardLookup lookup = findWorldGuardRegion(world.getName(), regionId);
            if (lookup != null) {
                return lookup;
            }
        }
        return null;
    }

    private WorldGuardLookup findWorldGuardRegion(String worldName, String regionId) {
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) {
            return null;
        }
        ProtectedRegion region = regionManager.getRegion(regionId);
        return region == null ? null : new WorldGuardLookup(worldName, regionManager, region);
    }

    @SuppressWarnings("null")
    private List<String> listProtectionIds() {
        File protectionsFolder = new File(plugin.getDataFolder(), "protections");
        File[] files = protectionsFolder.listFiles((ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .map(File::getName)
                .map(name -> name.substring(0, name.length() - 4))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Map<String, String> placedDebugPlaceholders(PlacedProtection placed) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%region%", placed.regionId());
        placeholders.put("%protection_id%", placed.protectionId());
        placeholders.put("%alias%", displayNameForLookup(placed));
        placeholders.put("%owner%", placed.ownerName());
        placeholders.put("%owner_uuid%", placed.ownerUuid());
        placeholders.put("%members%", String.valueOf(memberEntries(placed).size()));
        placeholders.put("%world%", placed.worldName());
        placeholders.put("%stone_x%", String.valueOf(placed.stoneX()));
        placeholders.put("%stone_y%", String.valueOf(placed.stoneY()));
        placeholders.put("%stone_z%", String.valueOf(placed.stoneZ()));
        placeholders.put("%min_x%", String.valueOf(placed.minX()));
        placeholders.put("%min_y%", String.valueOf(placed.minY()));
        placeholders.put("%min_z%", String.valueOf(placed.minZ()));
        placeholders.put("%max_x%", String.valueOf(placed.maxX()));
        placeholders.put("%max_y%", String.valueOf(placed.maxY()));
        placeholders.put("%max_z%", String.valueOf(placed.maxZ()));
        placeholders.put("%created_at%", formatAdminDate(placed.createdAtMillis()));
        return placeholders;
    }

    private Map<String, String> worldGuardDebugPlaceholders(WorldGuardLookup lookup) {
        ProtectedRegion region = lookup.region();
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("%region%", region.getId());
        placeholders.put("%world%", lookup.worldName());
        placeholders.put("%owners%", String.valueOf(region.getOwners().size()));
        placeholders.put("%members%", String.valueOf(region.getMembers().size()));
        placeholders.put("%min%", region.getMinimumPoint().toString());
        placeholders.put("%max%", region.getMaximumPoint().toString());
        return placeholders;
    }

    private String applyDebugPlaceholders(String line, Map<String, String> placeholders) {
        String formatted = line;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace(entry.getKey(), entry.getValue());
        }
        return formatted;
    }

    private String status(boolean value) {
        return value ? plugin.getConfigManager().getAdminDebugStatusOk()
                : plugin.getConfigManager().getAdminDebugStatusMissing();
    }

    private String formatAdminDate(long createdAtMillis) {
        if (createdAtMillis <= 0) {
            return plugin.getConfigManager().getAdminDebugUnknownDate();
        }
        return ADMIN_DATE_FORMATTER.format(Instant.ofEpochMilli(createdAtMillis));
    }

    private void spawnVisualEffect(Player player, Location location, VisualEffect effect) {
        if (!effect.enabled() || location.getWorld() == null) {
            return;
        }
        Location center = location.clone().add(0.5D, 0.6D, 0.5D);
        if (effect.particle() == Particle.DUST) {
            player.getWorld().spawnParticle(effect.particle(), center, effect.count(),
                    effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.speed(), effect.dustOptions());
            return;
        }
        player.getWorld().spawnParticle(effect.particle(), center, effect.count(),
                effect.offsetX(), effect.offsetY(), effect.offsetZ(), effect.speed());
    }

    private void spawnBorderParticle(Player player, World world, int x, int y, int z, VisualEffect effect) {
        Location location = new Location(world, x + 0.5, y + 0.15, z + 0.5);
        if (effect.particle() == Particle.DUST) {
            player.spawnParticle(effect.particle(), location, effect.count(), 0, 0, 0, 0, effect.dustOptions());
            return;
        }
        player.spawnParticle(effect.particle(), location, effect.count(), 0, 0, 0, effect.speed());
    }

    private MemberChangeResult changeMember(ProtectionMenuContext context, UUID memberId, String memberName,
            ProtectionMemberRank rank, boolean remove, boolean requireNewMember) {
        ProtectedRegion region = getWorldGuardRegion(context);
        RegionManager regionManager = getRegionManager(context.worldName());
        PlacedProtection placed = findProtection(context.regionId());
        if (region == null || regionManager == null) {
            return MemberChangeResult.REGION_UNAVAILABLE;
        }
        if (region.getOwners().contains(memberId)) {
            return MemberChangeResult.TARGET_IS_OWNER;
        }
        boolean storedMember = placed != null && placed.members().containsKey(memberId.toString());
        boolean worldGuardMember = region.getMembers().contains(memberId);
        if (!remove && requireNewMember && (storedMember || worldGuardMember)) {
            return MemberChangeResult.ALREADY_MEMBER;
        }
        if (!remove && !requireNewMember && !storedMember && !worldGuardMember) {
            return MemberChangeResult.NOT_MEMBER;
        }
        if (remove && !storedMember && !worldGuardMember) {
            return MemberChangeResult.NOT_MEMBER;
        }

        if (remove) {
            region.getMembers().removePlayer(memberId);
        } else {
            region.getMembers().addPlayer(memberId);
        }

        try {
            regionManager.saveChanges();
        } catch (StorageException exception) {
            if (remove) {
                region.getMembers().addPlayer(memberId);
            } else {
                region.getMembers().removePlayer(memberId);
            }
            plugin.getLogger().warning("Could not save members for WorldGuard region '" + context.regionId()
                    + "': " + exception.getMessage());
            return MemberChangeResult.SAVE_ERROR;
        }

        if (placed == null) {
            return MemberChangeResult.SUCCESS;
        }

        int index = placedProtections.indexOf(placed);
        PlacedProtection updated = remove
                ? placed.withoutMember(memberId)
                : placed.withMember(memberId, memberName, rank);
        placedProtections.set(index, updated);
        if (savePlacedProtections()) {
            if (remove) {
                disableProtectionFlight(memberId, context.regionId(), true);
            }
            return MemberChangeResult.SUCCESS;
        }

        placedProtections.set(index, placed);
        if (remove) {
            region.getMembers().addPlayer(memberId);
        } else {
            region.getMembers().removePlayer(memberId);
        }
        try {
            regionManager.saveChanges();
        } catch (StorageException exception) {
            plugin.getLogger().warning("Could not save members for WorldGuard region '" + context.regionId()
                    + "': " + exception.getMessage());
        }
        return MemberChangeResult.SAVE_ERROR;
    }

    private String displayName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() == null ? playerId.toString() : offlinePlayer.getName();
    }

    private void teleportRemovedMember(ProtectionMenuContext context, UUID memberId, boolean notify) {
        Player target = Bukkit.getPlayer(memberId);
        if (target == null || !target.isOnline()) {
            return;
        }
        PlacedProtection placed = findProtection(context.regionId());
        if (placed == null || !placed.contains(target.getLocation())) {
            return;
        }
        exitLocation(target, context).ifPresent(location -> {
            plugin.getSchedulerUtils().teleportEntity(target, location);
            if (notify) {
                target.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgProtectionMemberRemovedTeleported()
                                .replace("%alias%", context.displayAlias())));
            }
        });
    }

    private void recordProtectionEvent(String regionId, String actorUuid, String actorName, String targetUuid,
            String targetName, String eventType, String detail) {
        cache.invalidateRecentEvents(regionId);
        cache.invalidateReportSnapshot();
        storage.recordEventAsync(new ProtectionStorage.ProtectionEvent(regionId, actorUuid, actorName, targetUuid,
                targetName, eventType, detail)).exceptionally(throwable -> {
            plugin.getLogger().warning("Could not write protection event: " + throwable.getMessage());
            return null;
        });
    }

    private void restoreDomain(ProtectedRegion region, Set<UUID> owners, Set<UUID> members) {
        DefaultDomain ownerDomain = new DefaultDomain();
        owners.forEach(ownerDomain::addPlayer);
        region.setOwners(ownerDomain);

        DefaultDomain memberDomain = new DefaultDomain();
        members.forEach(memberDomain::addPlayer);
        region.setMembers(memberDomain);
    }

    private UUID parseUuid(String rawUuid) {
        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException exception) {
            return new UUID(0L, 0L);
        }
    }

    private ProtectionMenuContext toContext(PlacedProtection placed) {
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        String displayName = definition == null
                ? legacyDisplayName(placed)
                : definition.displayName();
        int radius = definition == null ? Math.max(0, (placed.maxX() - placed.minX()) / 2) : definition.radius();
        ProtectedRegion region = getWorldGuardRegion(placed);
        int ownerCount = region == null ? 1 : Math.max(1, region.getOwners().size());
        int memberCount = Math.max(placed.members().size(), region == null ? 0 : region.getMembers().size());
        return new ProtectionMenuContext(
                placed.regionId(),
                placed.protectionId(),
                placed.alias(),
                displayName,
                placed.ownerUuid(),
                placed.ownerName(),
                placed.worldName(),
                placed.stoneX(),
                placed.stoneY(),
                placed.stoneZ(),
                placed.minX(),
                placed.minY(),
                placed.minZ(),
                placed.maxX(),
                placed.maxY(),
                placed.maxZ(),
                radius,
                placed.customHome(),
                placed.customHome() ? placed.homeX() : placed.stoneX() + 0.5D,
                placed.customHome() ? placed.homeY() : placed.stoneY() + 1.0D,
                placed.customHome() ? placed.homeZ() : placed.stoneZ() + 0.5D,
                placed.customHome() ? placed.homeYaw() : 0.0F,
                placed.customHome() ? placed.homePitch() : 0.0F,
                placed.createdAtMillis(),
                ownerCount,
                memberCount,
                placed.rentPaidUntilMillis(),
                placed.rentSuspended());
    }

    private String legacyDisplayName(PlacedProtection placed) {
        if (placed.alias() != null && !placed.alias().isBlank()) {
            return placed.alias();
        }
        if (placed.isLegacyImported()) {
            return placed.legacyMaterial().isBlank() ? "Imported Protection" : placed.legacyMaterial();
        }
        return placed.protectionId();
    }

    private ProtectedRegion getWorldGuardRegion(PlacedProtection placed) {
        RegionManager regionManager = getRegionManager(placed.worldName());
        if (regionManager == null) {
            return null;
        }
        return regionManager.getRegion(placed.regionId());
    }

    private ProtectedRegion getWorldGuardRegion(ProtectionMenuContext context) {
        RegionManager regionManager = getRegionManager(context.worldName());
        if (regionManager == null) {
            return null;
        }
        return regionManager.getRegion(context.regionId());
    }

    private RegionManager getRegionManager(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
    }

    private String buildRegionId(Player player, Block block, ProtectionDefinition definition) {
        return definition.regionIdFormat()
                .replace("%player%", ProtectionRegionIdSupport.sanitize(player.getName()))
                .replace("%uuid%", player.getUniqueId().toString().replace("-", ""))
                .replace("%id%", ProtectionRegionIdSupport.sanitize(definition.id()))
                .replace("%world%", ProtectionRegionIdSupport.sanitize(block.getWorld().getName()))
                .replace("%x%", String.valueOf(block.getX()))
                .replace("%y%", String.valueOf(block.getY()))
                .replace("%z%", String.valueOf(block.getZ()))
                .replace("%compact%",
                        ProtectionRegionIdSupport.compactCoordinateId(block.getX(), block.getY(), block.getZ()))
                .toLowerCase(Locale.ROOT);
    }

    private String getProtectionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(protectionIdKey, PersistentDataType.STRING);
    }

    public boolean isProtectionItem(ItemStack item) {
        return getProtectionId(item) != null;
    }

    private PlacedProtection findProtectionAt(Location location) {
        return placedProtections.stream()
                .filter(placed -> !placed.rentSuspended())
                .filter(placed -> placed.contains(location))
                .findFirst()
                .orElse(null);
    }

    public ProtectionMenuContext findOwnedProtectionAt(Player player) {
        PlacedProtection placed = findProtectionAt(player.getLocation());
        if (placed == null || !placed.ownerUuid().equals(player.getUniqueId().toString())) {
            return null;
        }

        return toContext(placed);
    }

    private PlacedProtection findProtectionStone(Block block) {
        return placedProtections.stream()
                .filter(placed -> placed.isStoneBlock(block))
                .findFirst()
                .orElse(null);
    }

    private PlacedProtection findProtection(String regionId) {
        return placedProtections.stream()
                .filter(placed -> placed.regionId().equalsIgnoreCase(regionId))
                .findFirst()
                .orElse(null);
    }

    private Component formatActionbar(String message, PlacedProtection placed, Player player) {
        return MessageUtils.getColoredMessage(message
                .replace("%player%", player.getName())
                .replace("%owner%", placed.ownerName())
                .replace("%protection%", placed.protectionId())
                .replace("%region%", placed.regionId()));
    }

    private String resolveActionbar(PlacedProtection placed, boolean enter) {
        ProtectionDefinition definition = loadProtection(placed.protectionId());
        if (definition != null) {
            return enter ? definition.actionbarEnter() : definition.actionbarExit();
        }
        return enter ? placed.actionbarEnter() : placed.actionbarExit();
    }

    static boolean shouldProcessMovementRegionChange(String previousRegionId, String nextRegionId,
            boolean hasActiveProtectionFlight) {
        return hasActiveProtectionFlight || !equalsRegionId(previousRegionId, nextRegionId);
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    private boolean equals(String first, String second) {
        return equalsRegionId(first, second);
    }

    private static boolean equalsRegionId(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private void send(Player player, String message) {
        player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix() + message));
    }

    private void sendError(Player player, String message) {
        send(player, message);
        plugin.getConfigManager().getProtectionFeedbackConfig().errorSound().play(player);
    }

    private record CreationResult(boolean success, String regionId, String message) {
        private static CreationResult ok(String regionId) {
            return new CreationResult(true, regionId, "");
        }

        private static CreationResult fail(String message) {
            return new CreationResult(false, "", message);
        }
    }

    private record ProtectionDefinition(String id, String displayName, ItemStack blockItem, int radius, int priority,
            double price, double rentPrice, List<String> lore, String regionIdFormat,
            String actionbarEnter, String actionbarExit, YamlConfiguration yaml) {
    }

    private record PreviewSession(
            Location originalLocation,
            Location cameraLocation,
            List<Location> borderLocations,
            boolean allowFlight,
            boolean flying,
            boolean gravity,
            float walkSpeed,
            float flySpeed,
            TaskWrapper task) {

        private PreviewSession withTask(TaskWrapper task) {
            return new PreviewSession(originalLocation, cameraLocation, borderLocations, allowFlight, flying, gravity,
                    walkSpeed, flySpeed, task);
        }
    }

    private record ProtectionFlightSession(String regionId, boolean allowFlight, boolean flying) {
    }

    private record PlacedProtection(String regionId, String protectionId, String alias, String ownerUuid,
            String ownerName,
            String worldName, int stoneX, int stoneY, int stoneZ, int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ, long createdAtMillis,
            String actionbarEnter, String actionbarExit, boolean customHome,
            double homeX, double homeY, double homeZ, float homeYaw, float homePitch,
            Map<String, ProtectionFlagLevel> flags,
            String legacySource, String legacyMaterial,
            Map<String, ProtectionMemberEntry> members,
            long rentPaidUntilMillis, boolean rentSuspended) {

        private static PlacedProtection from(Player player, Block block, ProtectionDefinition definition,
                String regionId, long rentPaidUntilMillis, boolean rentSuspended) {
            int radius = definition.radius();
            World world = block.getWorld();
            return new PlacedProtection(
                    regionId,
                    definition.id(),
                    "",
                    player.getUniqueId().toString(),
                    player.getName(),
                    world.getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    block.getX() - radius,
                    world.getMinHeight(),
                    block.getZ() - radius,
                    block.getX() + radius,
                    world.getMaxHeight() - 1,
                    block.getZ() + radius,
                    System.currentTimeMillis(),
                    definition.actionbarEnter(),
                    definition.actionbarExit(),
                    false,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0F,
                    0.0F,
                    new LinkedHashMap<>(),
                    "",
                    "",
                    new LinkedHashMap<>(),
                    rentPaidUntilMillis,
                    rentSuspended);
        }

        private static PlacedProtection fromWorldGuard(ProtectedRegion region, ProtectionDefinition definition,
                UUID ownerId, String ownerName, String worldName, int stoneX, int stoneY, int stoneZ,
                Map<String, ProtectionMemberEntry> members) {
            return new PlacedProtection(
                    region.getId(),
                    definition.id(),
                    "",
                    ownerId.toString(),
                    ownerName,
                    worldName,
                    stoneX,
                    stoneY,
                    stoneZ,
                    region.getMinimumPoint().x(),
                    region.getMinimumPoint().y(),
                    region.getMinimumPoint().z(),
                    region.getMaximumPoint().x(),
                    region.getMaximumPoint().y(),
                    region.getMaximumPoint().z(),
                    System.currentTimeMillis(),
                    definition.actionbarEnter(),
                    definition.actionbarExit(),
                    false,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0F,
                    0.0F,
                    new LinkedHashMap<>(),
                    "",
                    "",
                    members,
                    0L,
                    false);
        }

        private PlacedProtection withFlag(ProtectionFlagDefinition definition, ProtectionFlagLevel level) {
            Map<String, ProtectionFlagLevel> updatedFlags = new LinkedHashMap<>(flags);
            updatedFlags.put(definition.id(), level);
            return new PlacedProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, updatedFlags, legacySource, legacyMaterial, members, rentPaidUntilMillis,
                    rentSuspended);
        }

        private PlacedProtection withAlias(String newAlias) {
            return new PlacedProtection(regionId, protectionId, newAlias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, legacySource, legacyMaterial, members, rentPaidUntilMillis,
                    rentSuspended);
        }

        private PlacedProtection withHome(Location location) {
            return new PlacedProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, true,
                    location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), flags,
                    legacySource, legacyMaterial, members, rentPaidUntilMillis, rentSuspended);
        }

        private PlacedProtection withMember(UUID memberId, String memberName, ProtectionMemberRank rank) {
            Map<String, ProtectionMemberEntry> updatedMembers = new LinkedHashMap<>(members);
            updatedMembers.put(memberId.toString(),
                    new ProtectionMemberEntry(memberId.toString(), memberName, rank));
            return new PlacedProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, legacySource, legacyMaterial, updatedMembers, rentPaidUntilMillis,
                    rentSuspended);
        }

        private PlacedProtection withoutMember(UUID memberId) {
            Map<String, ProtectionMemberEntry> updatedMembers = new LinkedHashMap<>(members);
            updatedMembers.remove(memberId.toString());
            return new PlacedProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, legacySource, legacyMaterial, updatedMembers, rentPaidUntilMillis,
                    rentSuspended);
        }

        private PlacedProtection withOwner(UUID newOwnerId, String newOwnerName, UUID previousOwnerId,
                String previousOwnerName) {
            Map<String, ProtectionMemberEntry> updatedMembers = new LinkedHashMap<>(members);
            updatedMembers.remove(newOwnerId.toString());
            updatedMembers.put(previousOwnerId.toString(),
                    new ProtectionMemberEntry(previousOwnerId.toString(), previousOwnerName,
                            ProtectionMemberRank.ADMIN));
            return new PlacedProtection(regionId, protectionId, alias, newOwnerId.toString(), newOwnerName,
                    worldName, stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, legacySource, legacyMaterial, updatedMembers, rentPaidUntilMillis,
                    rentSuspended);
        }

        private PlacedProtection withRentState(long paidUntilMillis, boolean suspended) {
            return new PlacedProtection(regionId, protectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, legacySource, legacyMaterial, members, paidUntilMillis, suspended);
        }

        private PlacedProtection withProtectionId(String newProtectionId, String newLegacyMaterial) {
            return new PlacedProtection(regionId, newProtectionId, alias, ownerUuid, ownerName, worldName,
                    stoneX, stoneY, stoneZ, minX, minY, minZ, maxX, maxY, maxZ,
                    createdAtMillis, actionbarEnter, actionbarExit, customHome, homeX, homeY, homeZ,
                    homeYaw, homePitch, flags, ProtectionMigrationSupport.LEGACY_SOURCE, newLegacyMaterial, members,
                    rentPaidUntilMillis, rentSuspended);
        }

        private boolean isLegacyImported() {
            return ProtectionMigrationSupport.LEGACY_SOURCE.equalsIgnoreCase(legacySource)
                    && legacyMaterial != null
                    && !legacyMaterial.isBlank();
        }

        private boolean contains(Location location) {
            return location.getWorld() != null
                    && location.getWorld().getName().equals(worldName)
                    && location.getBlockX() >= minX
                    && location.getBlockX() <= maxX
                    && location.getBlockY() >= minY
                    && location.getBlockY() <= maxY
                    && location.getBlockZ() >= minZ
                    && location.getBlockZ() <= maxZ;
        }

        private boolean isStoneBlock(Block block) {
            if (!hasStoneLocation()) {
                return false;
            }
            return block.getWorld().getName().equals(worldName)
                    && block.getX() == stoneX
                    && block.getY() == stoneY
                    && block.getZ() == stoneZ;
        }

        private boolean hasStoneLocation() {
            return stoneX != Integer.MIN_VALUE
                    && stoneY != Integer.MIN_VALUE
                    && stoneZ != Integer.MIN_VALUE;
        }
    }

    public record ProtectionMenuContext(String regionId, String protectionId, String alias, String displayName,
            String ownerUuid,
            String ownerName, String worldName, int stoneX, int stoneY, int stoneZ,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int radius,
            boolean customHome, double homeX, double homeY, double homeZ, float homeYaw, float homePitch,
            long createdAtMillis, int ownerCount, int memberCount, long rentPaidUntilMillis,
            boolean rentSuspended) {

        public int displaySize() {
            return radius;
        }

        public String displayAlias() {
            return alias == null || alias.isBlank() ? regionId : alias;
        }
    }

    public record ProtectionAdminState(boolean worldGuardRegion, boolean protectionYaml, boolean stoneBlock) {
    }

    public record RepairReport(int repaired, int cleaned, int skipped, boolean saved, List<String> lines) {
    }

    public record FlagSnapshot(StateFlag.State state, RegionGroup group) {
    }

    private record ProtectionMemberEntry(String uuid, String name, ProtectionMemberRank rank) {
    }

    private record RepairCandidate(PlacedProtection placed, String reason) {
        private static RepairCandidate repaired(PlacedProtection placed) {
            return new RepairCandidate(placed, "");
        }

        private static RepairCandidate skip(String reason) {
            return new RepairCandidate(null, reason);
        }
    }

    private record WorldGuardLookup(String worldName, RegionManager regionManager, ProtectedRegion region) {
    }

    public record ProtectionMemberInfo(UUID uuid, String name, ProtectionMemberRank rank) {
        public boolean owner() {
            return rank == ProtectionMemberRank.OWNER;
        }
    }

    public enum MemberChangeResult {
        SUCCESS,
        REGION_UNAVAILABLE,
        TARGET_IS_OWNER,
        ALREADY_MEMBER,
        NOT_MEMBER,
        SAVE_ERROR
    }

    public enum MemberInviteStatus {
        SUCCESS,
        REGION_UNAVAILABLE,
        TARGET_IS_OWNER,
        ALREADY_MEMBER,
        INVITE_EXISTS,
        SAVE_ERROR
    }

    public enum InviteResponseStatus {
        SUCCESS,
        INVALID,
        EXPIRED,
        NOT_FOR_YOU,
        ALREADY_MEMBER,
        REGION_UNAVAILABLE,
        SAVE_ERROR
    }

    public record InviteCreateResult(MemberInviteStatus status, long inviteId) {
    }

    public record InviteResponseResult(InviteResponseStatus status, ProtectionMenuContext context, String inviterName) {
    }

    public enum OwnershipTransferStatus {
        SUCCESS,
        NOT_OWNER,
        TARGET_UNKNOWN,
        TARGET_IS_OWNER,
        REGION_UNAVAILABLE,
        SAVE_ERROR
    }

    public record OwnershipTransferResult(OwnershipTransferStatus status, ProtectionMenuContext context) {
    }

    public record ProtectionReport(int total, int active, int orphaned,
            List<ProtectionStorage.OwnerCount> topOwners,
            List<ProtectionStorage.RegionArea> largestProtections,
            int eventCount) {
    }

    public enum RentPaymentStatus {
        PAID,
        REACTIVATED,
        DISABLED,
        NOT_FOUND,
        NOT_OWNER,
        ALREADY_PAID,
        ECONOMY_UNAVAILABLE,
        NOT_ENOUGH_MONEY,
        PAYMENT_ERROR,
        REGION_UNAVAILABLE,
        SAVE_ERROR
    }

    public record RentPaymentResult(RentPaymentStatus status, ProtectionMenuContext context, long paidUntilMillis,
            String detail) {
    }

    public enum ProtectionFlightStatus {
        ENABLED,
        DISABLED,
        NOT_IN_PROTECTION,
        NOT_ACCESSIBLE
    }

    public record ProtectionFlightResult(ProtectionFlightStatus status, ProtectionMenuContext context) {
    }

    public enum RemoveResult {
        SUCCESS,
        NOT_OWNER,
        INVALID_CONFIG,
        INVENTORY_FULL,
        WORLD_UNAVAILABLE,
        REGION_UNAVAILABLE,
        SAVE_ERROR
    }

    public record ViewResult(boolean success, long remainingCooldownSeconds) {
        public static final ViewResult SUCCESS = new ViewResult(true, 0L);

        private static ViewResult cooldown(long seconds) {
            return new ViewResult(false, seconds);
        }
    }

    public enum HomeUpdateResult {
        SUCCESS,
        NOT_INSIDE,
        REGION_UNAVAILABLE,
        SAVE_ERROR
    }
}
