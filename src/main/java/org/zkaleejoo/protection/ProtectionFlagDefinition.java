package org.zkaleejoo.protection;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ProtectionFlagDefinition {
    PVP("pvp", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.NOBODY, CustomRule.NONE, Flags.PVP),
    PVP_MOBS("pvp-mobs", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.EVERYONE, CustomRule.NONE,
            Flags.MOB_DAMAGE, Flags.DAMAGE_ANIMALS),
    ENTRY("entry", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.EVERYONE, CustomRule.NONE, Flags.ENTRY),
    PRESSURE_PLATES("pressure-plates", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS,
            CustomRule.PRESSURE_PLATE),
    DOORS("doors", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.DOOR),
    BUTTONS("buttons", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.BUTTON),
    PLACE_BLOCKS("place-blocks", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.NONE),
    BREAK_BLOCKS("break-blocks", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.NONE),
    STORAGE("storage", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.STORAGE),
    WORKSTATIONS("workstations", ProtectionFlagMode.ACCESS, ProtectionFlagLevel.MEMBERS, CustomRule.WORKSTATION),
    TNT("tnt", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.NOBODY, CustomRule.NONE,
            Flags.TNT, Flags.CREEPER_EXPLOSION, Flags.OTHER_EXPLOSION),
    LEAF_DECAY("leaf-decay", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.EVERYONE, CustomRule.NONE,
            Flags.LEAF_DECAY),
    KEEP_INVENTORY("keep-inventory", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.NOBODY, CustomRule.NONE),
    KEEP_EXP("keep-exp", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.NOBODY, CustomRule.NONE),
    FALL_DAMAGE("fall-damage", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.EVERYONE, CustomRule.NONE),
    POTION_SPLASH("potion-splash", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.EVERYONE, CustomRule.NONE),
    HUNGER_DRAIN("hunger-drain", ProtectionFlagMode.TOGGLE, ProtectionFlagLevel.EVERYONE, CustomRule.NONE);

    private final String id;
    private final ProtectionFlagMode mode;
    private final ProtectionFlagLevel defaultLevel;
    private final CustomRule customRule;
    private final List<StateFlag> worldGuardFlags;

    ProtectionFlagDefinition(String id, ProtectionFlagMode mode, ProtectionFlagLevel defaultLevel,
            CustomRule customRule, StateFlag... worldGuardFlags) {
        this.id = id;
        this.mode = mode;
        this.defaultLevel = defaultLevel;
        this.customRule = customRule;
        this.worldGuardFlags = List.of(worldGuardFlags);
    }

    public String id() {
        return id;
    }

    public ProtectionFlagMode mode() {
        return mode;
    }

    public ProtectionFlagLevel defaultLevel() {
        return defaultLevel;
    }

    public CustomRule customRule() {
        return customRule;
    }

    public List<StateFlag> worldGuardFlags() {
        return worldGuardFlags;
    }

    public ProtectionFlagLevel next(ProtectionFlagLevel current) {
        return mode == ProtectionFlagMode.TOGGLE ? current.nextToggle() : current.nextAccess();
    }

    public boolean hasWorldGuardFlags() {
        return !worldGuardFlags.isEmpty();
    }

    public boolean hasCustomRule() {
        return customRule != CustomRule.NONE;
    }

    public static Optional<ProtectionFlagDefinition> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(flag -> flag.id.equals(normalized))
                .findFirst();
    }

    public enum CustomRule {
        NONE,
        PRESSURE_PLATE,
        DOOR,
        BUTTON,
        STORAGE,
        WORKSTATION
    }
}
