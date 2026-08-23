package org.zkaleejoo.protection.migration;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.protection.ProtectionValidation;
import org.zkaleejoo.protection.migration.ProtectionMigrationSupport.ProtectionStonesCandidate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class ProtectionStonesMigrationYamlSupport {

    private ProtectionStonesMigrationYamlSupport() {
    }

    public static YamlResolution plan(File protectionsFolder, ProtectionStonesCandidate candidate) {
        int radius = detectedRadius(candidate);
        Material material = Material.matchMaterial(candidate.material());
        if (isInvalidMaterial(material)) {
            throw new IllegalArgumentException("Invalid ProtectionStones material: " + candidate.material());
        }
        String existing = findMatchingDefinition(protectionsFolder, material, radius);
        if (existing != null) {
            return new YamlResolution(existing, false, radius);
        }
        return new YamlResolution(uniqueGeneratedId(protectionsFolder, material, radius), true, radius);
    }

    public static YamlResolution resolveOrCreate(File protectionsFolder, ProtectionStonesCandidate candidate,
            List<String> itemLore, String actionbarEnter, String actionbarExit) throws IOException {
        YamlResolution plan = plan(protectionsFolder, candidate);
        if (!plan.created()) {
            return plan;
        }
        if (!protectionsFolder.exists() && !protectionsFolder.mkdirs()) {
            throw new IOException("Could not create protections folder.");
        }

        Material material = Material.matchMaterial(candidate.material());
        if (isInvalidMaterial(material)) {
            throw new IOException("Invalid ProtectionStones material: " + candidate.material());
        }

        File file = new File(protectionsFolder, plan.protectionId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("protection.id", plan.protectionId());
        yaml.set("protection.display-name", "Imported " + material.name());
        yaml.set("protection.radius", plan.radius());
        yaml.set("protection.priority", 0);

        yaml.set("item.material", material.name());
        yaml.set("item.lore", itemLore == null ? List.of() : itemLore);

        yaml.set("price", 0.0D);
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

        yaml.set("actionbar.enter", actionbarEnter == null ? "" : actionbarEnter);
        yaml.set("actionbar.exit", actionbarExit == null ? "" : actionbarExit);
        yaml.save(file);
        return plan;
    }

    private static int detectedRadius(ProtectionStonesCandidate candidate) {
        int xRadius = Math.max(
                Math.abs(candidate.maxX() - candidate.stoneLocation().x()),
                Math.abs(candidate.stoneLocation().x() - candidate.minX()));
        int zRadius = Math.max(
                Math.abs(candidate.maxZ() - candidate.stoneLocation().z()),
                Math.abs(candidate.stoneLocation().z() - candidate.minZ()));
        int radius = Math.max(Math.max(xRadius, zRadius), ProtectionValidation.MIN_RADIUS);
        return Math.min(radius, ProtectionValidation.MAX_RADIUS);
    }

    private static String findMatchingDefinition(File protectionsFolder, Material material, int radius) {
        if (protectionsFolder == null || !protectionsFolder.isDirectory()) {
            return null;
        }
        File[] files = protectionsFolder.listFiles((ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return null;
        }
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yamlRadius(yaml) == radius && material == yamlMaterial(yaml)) {
                String id = yaml.getString("protection.id", file.getName().substring(0, file.getName().length() - 4));
                return ProtectionValidation.normalizeId(id).orElse(null);
            }
        }
        return null;
    }

    private static Material yamlMaterial(YamlConfiguration yaml) {
        String rawMaterial = yaml.getString("item.material",
                yaml.getString("block.material",
                        yaml.getString("item.block.type", yaml.getString("block.item.type", ""))));
        if (rawMaterial == null || rawMaterial.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(rawMaterial);
        return isInvalidMaterial(material) ? null : material;
    }

    private static boolean isInvalidMaterial(Material material) {
        return material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR;
    }

    private static int yamlRadius(YamlConfiguration yaml) {
        String path = yaml.contains("protection.radius") ? "protection.radius" : "radius";
        return yaml.contains(path) ? yaml.getInt(path) : -1;
    }

    private static String uniqueGeneratedId(File protectionsFolder, Material material, int radius) {
        String baseId = baseGeneratedId(material, radius);
        String candidate = baseId;
        int suffix = 2;
        while (new File(protectionsFolder, candidate + ".yml").exists()) {
            candidate = trimForSuffix(baseId, suffix);
            suffix++;
        }
        return candidate;
    }

    private static String baseGeneratedId(Material material, int radius) {
        String raw = "ps_" + material.name().toLowerCase(Locale.ROOT) + "_r" + radius;
        return raw.length() <= 48 ? raw : raw.substring(0, 48);
    }

    private static String trimForSuffix(String baseId, int suffix) {
        String suffixText = "_" + suffix;
        int maxBaseLength = 48 - suffixText.length();
        return (baseId.length() > maxBaseLength ? baseId.substring(0, maxBaseLength) : baseId) + suffixText;
    }

    public record YamlResolution(String protectionId, boolean created, int radius) {
    }
}
