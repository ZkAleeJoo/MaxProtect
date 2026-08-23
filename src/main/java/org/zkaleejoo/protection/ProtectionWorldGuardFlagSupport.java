package org.zkaleejoo.protection;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.List;
import java.util.Locale;

final class ProtectionWorldGuardFlagSupport {

    private static final List<StateFlag> COMPATIBILITY_PASSTHROUGH_FLAGS = List.of(
            Flags.INTERACT,
            Flags.USE,
            Flags.USE_ANVIL,
            Flags.BLOCK_BREAK,
            Flags.BLOCK_PLACE,
            Flags.CHEST_ACCESS);

    private ProtectionWorldGuardFlagSupport() {
    }

    static List<StateFlag> compatibilityPassthroughFlags() {
        return COMPATIBILITY_PASSTHROUGH_FLAGS;
    }

    static boolean applyCompatibilityPassthrough(ProtectedRegion region) {
        boolean changed = false;
        for (StateFlag flag : COMPATIBILITY_PASSTHROUGH_FLAGS) {
            if (region.getFlag(flag) != StateFlag.State.ALLOW) {
                region.setFlag(flag, StateFlag.State.ALLOW);
                changed = true;
            }
            if (region.getFlag(flag.getRegionGroupFlag()) != RegionGroup.ALL) {
                region.setFlag(flag.getRegionGroupFlag(), RegionGroup.ALL);
                changed = true;
            }
        }
        return changed;
    }

    static StateFlag stateFlag(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "pvp" -> Flags.PVP;
            case "mob-damage" -> Flags.MOB_DAMAGE;
            case "damage-animals" -> Flags.DAMAGE_ANIMALS;
            case "entry" -> Flags.ENTRY;
            case "block-place" -> Flags.BLOCK_PLACE;
            case "block-break" -> Flags.BLOCK_BREAK;
            case "chest-access" -> Flags.CHEST_ACCESS;
            case "use" -> Flags.USE;
            case "interact" -> Flags.INTERACT;
            case "use-anvil" -> Flags.USE_ANVIL;
            case "block-trampling" -> Flags.TRAMPLE_BLOCKS;
            case "tnt" -> Flags.TNT;
            case "creeper-explosion" -> Flags.CREEPER_EXPLOSION;
            case "other-explosion" -> Flags.OTHER_EXPLOSION;
            case "leaf-decay" -> Flags.LEAF_DECAY;
            case "fire-spread" -> Flags.FIRE_SPREAD;
            case "lava-fire" -> Flags.LAVA_FIRE;
            case "lighter" -> Flags.LIGHTER;
            case "withering", "wither-damage" -> Flags.WITHER_DAMAGE;
            case "ender-build" -> Flags.ENDER_BUILD;
            case "mob-spawning" -> Flags.MOB_SPAWNING;
            default -> null;
        };
    }
}
