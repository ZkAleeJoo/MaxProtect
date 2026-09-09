package org.zkaleejoo.protection.migration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationDecision;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationMember;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.MigrationStatus;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.ProtectionStoneLocation;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.ProtectionStonesCandidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProtectionStonesMigrationScanner {

    private static final String FLAG_BLOCK_MATERIAL = "ps-block-material";
    private static final String FLAG_HOME = "ps-home";
    private static final String FLAG_NAME = "ps-name";
    private static final String FLAG_MERGED_REGIONS = "ps-merged-regions";

    private final MaxProtect plugin;

    public ProtectionStonesMigrationScanner(MaxProtect plugin) {
        this.plugin = plugin;
    }

    public ScanResult scan() {
        List<ProtectionStonesCandidate> candidates = new ArrayList<>();
        List<MigrationDecision> rejected = new ArrayList<>();
        boolean protectionStonesEnabled = isProtectionStonesEnabled();

        for (World world : plugin.getServer().getWorlds()) {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                continue;
            }
            for (ProtectedRegion region : regionManager.getRegions().values()) {
                CandidateResult result = buildCandidate(world, region, protectionStonesEnabled);
                if (result.candidate() != null) {
                    candidates.add(result.candidate());
                } else if (result.decision() != null) {
                    rejected.add(result.decision());
                }
            }
        }

        candidates.sort(Comparator.comparing((ProtectionStonesCandidate c) -> c.worldName())
                .thenComparing(c -> c.regionId()));
        return new ScanResult(candidates, rejected, protectionStonesEnabled);
    }

    private CandidateResult buildCandidate(World world, ProtectedRegion region, boolean protectionStonesEnabled) {
        String materialName = stringFlag(region, FLAG_BLOCK_MATERIAL).orElse("");
        Optional<ProtectionStoneLocation> parsedLocation = ProtectionMigrationSupport
                .parseProtectionStonesRegionId(region.getId());
        boolean hasProtectionStonesMarker = !materialName.isBlank()
                || stringFlag(region, FLAG_MERGED_REGIONS).isPresent()
                || parsedLocation.isPresent();
        if (!hasProtectionStonesMarker) {
            return CandidateResult.empty();
        }
        if (stringFlag(region, FLAG_MERGED_REGIONS).isPresent() && parsedLocation.isEmpty()) {
            return CandidateResult.rejected(region, world,
                    "merged ProtectionStones region without a single stone ID is not importable yet");
        }
        if (parsedLocation.isEmpty()) {
            return CandidateResult.rejected(region, world, "could not resolve ProtectionStones stone location");
        }

        ProtectionStoneLocation stone = parsedLocation.get();
        Material material = materialFromFlagOrBlock(world, stone, materialName);
        if (material == null || !material.isBlock() || material.isAir()) {
            return CandidateResult.rejected(region, world, "stone block material is missing or invalid");
        }
        List<MigrationMember> owners = ProtectionMigrationSupport.resolveDomainPlayers(
                region.getOwners().getUniqueIds(),
                region.getOwners().getPlayers(),
                this::uuidFromName,
                this::displayName);
        if (owners.isEmpty()) {
            return CandidateResult.rejected(region, world, "owner is missing");
        }

        MigrationMember owner = owners.stream()
                .sorted(Comparator.comparing(member -> member.uuid()))
                .findFirst()
                .orElseThrow();
        String alias = stringFlag(region, FLAG_NAME).orElse("");
        Optional<HomeLocation> home = parseHome(stringFlag(region, FLAG_HOME).orElse(""));
        List<MigrationMember> members = ProtectionMigrationSupport.resolveDomainPlayers(
                region.getMembers().getUniqueIds(),
                region.getMembers().getPlayers(),
                this::uuidFromName,
                this::displayName).stream()
                .filter(member -> !member.uuid().equals(owner.uuid()))
                .toList();

        MigrationStatus status = protectionStonesEnabled ? MigrationStatus.IMPORTABLE : MigrationStatus.WARNING;
        ProtectionStonesCandidate candidate = new ProtectionStonesCandidate(
                region.getId(),
                world.getName(),
                material.name(),
                stone,
                alias,
                owner.uuid(),
                owner.name(),
                region.getMinimumPoint().x(),
                region.getMinimumPoint().y(),
                region.getMinimumPoint().z(),
                region.getMaximumPoint().x(),
                region.getMaximumPoint().y(),
                region.getMaximumPoint().z(),
                home.isPresent(),
                home.map(h -> h.x()).orElse(0.0D),
                home.map(h -> h.y()).orElse(0.0D),
                home.map(h -> h.z()).orElse(0.0D),
                members);
        return new CandidateResult(candidate, new MigrationDecision(region.getId(), world.getName(), status,
                protectionStonesEnabled ? "ready" : "ProtectionStones is not enabled; using WorldGuard data only"));
    }

    private boolean isProtectionStonesEnabled() {
        Plugin source = Bukkit.getPluginManager().getPlugin("ProtectionStones");
        return source != null && source.isEnabled();
    }

    private Material materialFromFlagOrBlock(World world, ProtectionStoneLocation stone, String materialName) {
        Material material = materialName.isBlank() ? null : Material.matchMaterial(materialName);
        if (material != null) {
            return material;
        }
        Material blockType = world.getBlockAt(stone.x(), stone.y(), stone.z()).getType();
        return blockType.isBlock() && !blockType.isAir() ? blockType : null;
    }

    private Optional<HomeLocation> parseHome(String rawHome) {
        if (rawHome == null || rawHome.isBlank()) {
            return Optional.empty();
        }
        String[] parts = rawHome.trim().split("\\s+");
        if (parts.length < 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new HomeLocation(
                    Double.parseDouble(parts[0]) + 0.5D,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]) + 0.5D));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String displayName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    private UUID uuidFromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Optional<String> stringFlag(ProtectedRegion region, String flagName) {
        Flag flag = WorldGuard.getInstance().getFlagRegistry().get(flagName);
        if (flag == null) {
            return Optional.empty();
        }
        Object value = region.getFlag(flag);
        if (value == null) {
            return Optional.empty();
        }
        String text = value.toString();
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private record HomeLocation(double x, double y, double z) {
    }

    private record CandidateResult(ProtectionStonesCandidate candidate, MigrationDecision decision) {
        private static CandidateResult empty() {
            return new CandidateResult(null, null);
        }

        private static CandidateResult rejected(ProtectedRegion region, World world, String reason) {
            return new CandidateResult(null,
                    new MigrationDecision(region.getId(), world.getName(), MigrationStatus.SKIPPED, reason));
        }
    }

    public record ScanResult(List<ProtectionStonesCandidate> candidates,
            List<MigrationDecision> rejected, boolean protectionStonesEnabled) {
    }
}
