package org.zkaleejoo.config;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;

public record ProtectionFeedbackConfig(
        VisualEffect placeEffect,
        VisualEffect removeEffect,
        VisualEffect viewEffect,
        CinematicPreview cinematicPreview,
        SoundEffect buySound,
        SoundEffect placeSound,
        SoundEffect removeSound,
        SoundEffect errorSound,
        SoundEffect flagChangeSound) {

    public static ProtectionFeedbackConfig from(FileConfiguration config) {
        VisualEffect defaultPlace = VisualEffect.simple(true, Particle.DUST, 24, 0.45D, 0.55D, 0.45D, 0.02D)
                .withColor(124, 255, 107, 1.2F);
        VisualEffect defaultRemove = VisualEffect.simple(true, Particle.DUST, 28, 0.55D, 0.65D, 0.55D, 0.03D)
                .withColor(255, 77, 77, 1.35F);
        VisualEffect defaultView = VisualEffect.simple(true, Particle.DUST, 1, 0.0D, 0.0D, 0.0D, 0.0D)
                .withColor(39, 245, 97, 1.2F)
                .withTiming(5, 10, 10);

        return new ProtectionFeedbackConfig(
                VisualEffect.from(config.getConfigurationSection("protection.visual-effects.place"), defaultPlace),
                VisualEffect.from(config.getConfigurationSection("protection.visual-effects.remove"), defaultRemove),
                VisualEffect.from(config.getConfigurationSection("protection.visual-effects.view"), defaultView),
                CinematicPreview.from(config.getConfigurationSection("protection.preview.cinematic"),
                        new CinematicPreview(false, 5, 15, 18.0D, Material.RED_STAINED_GLASS, true)),
                SoundEffect.from(config.getConfigurationSection("protection.sounds.buy"),
                        new SoundEffect(true, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F)),
                SoundEffect.from(config.getConfigurationSection("protection.sounds.place"),
                        new SoundEffect(true, Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.0F)),
                SoundEffect.from(config.getConfigurationSection("protection.sounds.remove"),
                        new SoundEffect(true, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 0.8F)),
                SoundEffect.from(config.getConfigurationSection("protection.sounds.error"),
                        new SoundEffect(true, Sound.ENTITY_VILLAGER_NO, 0.8F, 1.2F)),
                SoundEffect.from(config.getConfigurationSection("protection.sounds.flag-change"),
                        new SoundEffect(true, Sound.UI_BUTTON_CLICK, 0.8F, 1.4F)));
    }

    public record SoundEffect(boolean enabled, Sound sound, float volume, float pitch) {

        public static SoundEffect from(ConfigurationSection section, SoundEffect fallback) {
            if (section == null) {
                return fallback;
            }
            return new SoundEffect(
                    section.getBoolean("enabled", fallback.enabled()),
                    parseSound(section.getString("sound"), fallback.sound()),
                    clampFloat(section.getDouble("volume", fallback.volume()), 0.0F, 10.0F),
                    clampFloat(section.getDouble("pitch", fallback.pitch()), 0.1F, 2.0F));
        }

        public void play(Player player) {
            if (!enabled || player == null) {
                return;
            }
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

        @SuppressWarnings("removal")
        private static Sound parseSound(String rawSound, Sound fallback) {
            if (rawSound == null || rawSound.isBlank()) {
                return fallback;
            }
            try {
                return Sound.valueOf(rawSound.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | ExceptionInInitializerError ignored) {
                return fallback;
            }
        }
    }

    public record CinematicPreview(
            boolean enabled,
            int durationSeconds,
            int cooldownSeconds,
            double heightOffset,
            Material borderMaterial,
            boolean useFakeBlocks) {

        public static CinematicPreview from(ConfigurationSection section, CinematicPreview fallback) {
            if (section == null) {
                return fallback;
            }
            return new CinematicPreview(
                    section.getBoolean("enabled", fallback.enabled()),
                    clampInt(section.getInt("duration-seconds", fallback.durationSeconds()), 1, 10),
                    clampInt(section.getInt("cooldown-seconds", fallback.cooldownSeconds()), 0, 3600),
                    clampDouble(section.getDouble("height-offset", fallback.heightOffset()), 4.0D, 64.0D),
                    parseBorderMaterial(section.getString("border-material"), fallback.borderMaterial()),
                    section.getBoolean("use-fake-blocks", fallback.useFakeBlocks()));
        }

        private static Material parseBorderMaterial(String rawMaterial, Material fallback) {
            if (rawMaterial == null || rawMaterial.isBlank()) {
                return fallback;
            }
            try {
                Material material = Material.valueOf(rawMaterial.trim().toUpperCase(Locale.ROOT));
                if (!isPreviewBorderMaterial(material)) {
                    return fallback;
                }
                return material;
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }

        private static boolean isPreviewBorderMaterial(Material material) {
            return switch (material) {
                case GLASS,
                        WHITE_STAINED_GLASS,
                        ORANGE_STAINED_GLASS,
                        MAGENTA_STAINED_GLASS,
                        LIGHT_BLUE_STAINED_GLASS,
                        YELLOW_STAINED_GLASS,
                        LIME_STAINED_GLASS,
                        PINK_STAINED_GLASS,
                        GRAY_STAINED_GLASS,
                        LIGHT_GRAY_STAINED_GLASS,
                        CYAN_STAINED_GLASS,
                        PURPLE_STAINED_GLASS,
                        BLUE_STAINED_GLASS,
                        BROWN_STAINED_GLASS,
                        GREEN_STAINED_GLASS,
                        RED_STAINED_GLASS,
                        BLACK_STAINED_GLASS -> true;
                default -> false;
            };
        }
    }

    public record VisualEffect(
            boolean enabled,
            Particle particle,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed,
            int red,
            int green,
            int blue,
            float dustSize,
            int durationSeconds,
            int intervalTicks,
            int cooldownSeconds) {

        public static VisualEffect simple(boolean enabled, Particle particle, int count,
                double offsetX, double offsetY, double offsetZ, double speed) {
            return new VisualEffect(enabled, particle, count, offsetX, offsetY, offsetZ, speed,
                    255, 255, 255, 1.0F, 0, 1, 0);
        }

        public static VisualEffect from(ConfigurationSection section, VisualEffect fallback) {
            if (section == null) {
                return fallback;
            }
            return new VisualEffect(
                    section.getBoolean("enabled", fallback.enabled()),
                    parseParticle(section.getString("particle"), fallback.particle()),
                    clampInt(section.getInt("count", fallback.count()), 1, 200),
                    clampDouble(section.getDouble("offset-x", fallback.offsetX()), 0.0D, 16.0D),
                    clampDouble(section.getDouble("offset-y", fallback.offsetY()), 0.0D, 16.0D),
                    clampDouble(section.getDouble("offset-z", fallback.offsetZ()), 0.0D, 16.0D),
                    clampDouble(section.getDouble("speed", fallback.speed()), 0.0D, 5.0D),
                    clampInt(section.getInt("color.red", fallback.red()), 0, 255),
                    clampInt(section.getInt("color.green", fallback.green()), 0, 255),
                    clampInt(section.getInt("color.blue", fallback.blue()), 0, 255),
                    clampFloat(section.getDouble("dust-size", fallback.dustSize()), 0.1F, 4.0F),
                    clampInt(section.getInt("duration-seconds", fallback.durationSeconds()), 0, 60),
                    clampInt(section.getInt("interval-ticks", fallback.intervalTicks()), 1, 200),
                    clampInt(section.getInt("cooldown-seconds", fallback.cooldownSeconds()), 0, 3600));
        }

        public VisualEffect withColor(int red, int green, int blue, float dustSize) {
            return new VisualEffect(enabled, particle, count, offsetX, offsetY, offsetZ, speed,
                    red, green, blue, dustSize, durationSeconds, intervalTicks, cooldownSeconds);
        }

        public VisualEffect withTiming(int durationSeconds, int intervalTicks, int cooldownSeconds) {
            return new VisualEffect(enabled, particle, count, offsetX, offsetY, offsetZ, speed,
                    red, green, blue, dustSize, durationSeconds, intervalTicks, cooldownSeconds);
        }

        public Particle.DustOptions dustOptions() {
            return new Particle.DustOptions(Color.fromRGB(red, green, blue), dustSize);
        }

        private static Particle parseParticle(String rawParticle, Particle fallback) {
            if (rawParticle == null || rawParticle.isBlank()) {
                return fallback;
            }
            try {
                Particle particle = Particle.valueOf(rawParticle.trim().toUpperCase(Locale.ROOT));
                if (particle != Particle.DUST && particle.getDataType() != Void.class) {
                    return fallback;
                }
                return particle;
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(double value, float min, float max) {
        return (float) Math.max(min, Math.min(max, value));
    }
}
