package org.zkaleejoo.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.zkaleejoo.ArgosProtect;
import org.zkaleejoo.protection.ProtectionFlagLevel;
import org.zkaleejoo.protection.ProtectionLimitProfile;
import org.zkaleejoo.protection.ProtectionRentSettings;
import org.zkaleejoo.protection.ProtectionValidation;

public class MainConfigManager {

    private final CustomConfig configFile;
    private CustomConfig langFile;
    private final ArgosProtect plugin;
    private final Set<String> registeredLimitPermissions = new LinkedHashSet<>();

    // CONFIG
    private String prefix;
    private String creationMenuTitle;
    private int creationMenuSize;
    private Material creationMenuFillerMaterial;
    private String creationMenuFillerName;
    private List<MenuItemConfig> creationMenuItems;
    private String languageMenuTitle;
    private int languageMenuSize;
    private Material languageMenuFillerMaterial;
    private String languageMenuFillerName;
    private List<MenuItemConfig> languageMenuItems;
    private String protectionMenuTitle;
    private int protectionMenuSize;
    private Material protectionMenuFillerMaterial;
    private String protectionMenuFillerName;
    private List<MenuItemConfig> protectionMenuItems;
    private String protectionInfoMenuTitle;
    private int protectionInfoMenuSize;
    private Material protectionInfoMenuFillerMaterial;
    private String protectionInfoMenuFillerName;
    private List<MenuItemConfig> protectionInfoMenuItems;
    private String protectionSettingsMenuTitle;
    private int protectionSettingsMenuSize;
    private Material protectionSettingsMenuFillerMaterial;
    private String protectionSettingsMenuFillerName;
    private MenuNavigationConfig protectionSettingsMenuPreviousItem;
    private MenuNavigationConfig protectionSettingsMenuNextItem;
    private List<MenuItemConfig> protectionSettingsMenuItems;
    private String protectionSettingsAccessNobody;
    private String protectionSettingsAccessMembers;
    private String protectionSettingsAccessEveryone;
    private String protectionSettingsToggleDisabled;
    private String protectionSettingsToggleEnabled;
    private String protectionHomeMenuTitle;
    private int protectionHomeMenuSize;
    private Material protectionHomeMenuFillerMaterial;
    private String protectionHomeMenuFillerName;
    private List<Integer> protectionHomeMenuContentSlots;
    private List<MenuItemConfig> protectionHomeMenuItems;
    private MenuDisplayConfig protectionHomeMenuItem;
    private MenuNavigationConfig protectionHomeMenuPreviousItem;
    private MenuNavigationConfig protectionHomeMenuNextItem;
    private String protectionHomeRoleOwner;
    private String protectionHomeRoleAdmin;
    private String protectionHomeRoleMember;
    private String protectionMembersMenuTitle;
    private int protectionMembersMenuSize;
    private Material protectionMembersMenuFillerMaterial;
    private String protectionMembersMenuFillerName;
    private List<Integer> protectionMembersMenuContentSlots;
    private List<MenuItemConfig> protectionMembersMenuItems;
    private MenuDisplayConfig protectionMembersMenuItem;
    private MenuNavigationConfig protectionMembersMenuPreviousItem;
    private MenuNavigationConfig protectionMembersMenuNextItem;
    private String protectionMembersRoleOwner;
    private String protectionMembersRoleAdmin;
    private String protectionMembersRoleMember;
    private String protectionMembersUnknownDate;
    private String adminPlacedMenuTitle;
    private int adminPlacedMenuSize;
    private Material adminPlacedMenuFillerMaterial;
    private String adminPlacedMenuFillerName;

    private String protectionLogsMenuTitle;
    private int protectionLogsMenuSize;
    private Material protectionLogsMenuFillerMaterial;
    private String protectionLogsMenuFillerName;
    private List<Integer> protectionLogsMenuContentSlots;
    private MenuDisplayConfig protectionLogsMenuItem;
    private Map<String, String> protectionLogsMenuTextures;
    private String protectionLogsMenuDefaultTexture;
    private List<MenuItemConfig> protectionLogsMenuItems;
    private MenuNavigationConfig protectionLogsMenuPreviousItem;
    private MenuNavigationConfig protectionLogsMenuNextItem;
    private List<Integer> adminPlacedMenuContentSlots;
    private List<MenuItemConfig> adminPlacedMenuItems;
    private MenuDisplayConfig adminPlacedMenuItem;
    private MenuNavigationConfig adminPlacedMenuPreviousItem;
    private MenuNavigationConfig adminPlacedMenuNextItem;
    private String adminPlacedStatusOk;
    private String adminPlacedStatusMissing;
    private boolean adminPlacedUseProtectionBlock;
    private Material adminPlacedHealthyMaterial;
    private Material adminPlacedBrokenMaterial;
    private String protectionRemoveConfirmMenuTitle;
    private int protectionRemoveConfirmMenuSize;
    private Material protectionRemoveConfirmMenuFillerMaterial;
    private String protectionRemoveConfirmMenuFillerName;
    private List<MenuItemConfig> protectionRemoveConfirmMenuItems;
    private int protectionHomeCooldownSeconds;
    private ProtectionFeedbackConfig protectionFeedbackConfig;
    private ProtectionRentSettings protectionRentSettings;
    private ProtectionLimitProfile defaultProtectionLimitProfile;
    private List<ProtectionLimitProfile> protectionLimitGroups;
    private boolean updateCheckEnabled;
    private boolean bStatsEnabled;

    // MESSAGES
    private String msgUpdateAvailable;
    private String msgUpdateCurrent;
    private String msgUpdateDownload;
    private String msgNoPermission;
    private String msgPluginReload;
    private String msgUsageCommand;
    private String msgPlayerOnly;
    private String msgLanguageChanged;
    private String msgLanguageFileMissing;
    private String msgLanguageNameEnglish;
    private String msgLanguageNameSpanish;
    private String msgAdminDebugUsage;
    private String msgAdminDebugNotFound;
    private String msgAdminListPlacedUsage;
    private String msgAdminReportUsage;
    private String msgAdminLogsUsage;
    private String msgAdminLogsEmpty;
    private String msgAdminLogsHeader;
    private String msgAdminLogsEntry;
    private List<String> adminReportProtectionLines;
    private String adminReportProtectionEmptyEntry;
    private String adminReportProtectionLargestEntry;
    private String adminReportProtectionTopOwnerEntry;
    private String msgAdminRepairUsage;
    private String msgAdminRepairSummary;
    private String msgAdminRepairSaveError;
    private String msgAdminRepairSkippedWorldUnavailable;
    private String msgAdminRepairCleanedStaleTracker;
    private String msgAdminRepairOrphanTracker;
    private String msgAdminRepairSkippedWorldGuardRegion;
    private String msgAdminRepairRepairedWorldGuardRegion;
    private String msgAdminRepairSaveRollback;
    private String msgAdminRepairReasonRegionIdMismatch;
    private String msgAdminRepairReasonProtectionYamlInvalid;
    private String msgAdminRepairReasonNoOwner;
    private String msgAdminRepairReasonStoneNotFound;
    private String msgAdminMigrateUsage;
    private String msgAdminMigrateSummary;
    private String msgAdminMigrateEntry;
    private String msgAdminMigrateBackupWarning;
    private String adminDebugHeader;
    private String adminDebugTrackingMissing;
    private String adminDebugTrackingPresent;
    private String adminDebugProtectionYaml;
    private String adminDebugOwner;
    private String adminDebugMembers;
    private String adminDebugMemberEntry;
    private String adminDebugWorld;
    private String adminDebugStone;
    private String adminDebugBounds;
    private String adminDebugCreated;
    private String adminDebugWorldGuardMissing;
    private String adminDebugWorldGuardPresent;
    private String adminDebugWorldGuardOwners;
    private String adminDebugWorldGuardOwnerEntry;
    private String adminDebugWorldGuardMembers;
    private String adminDebugWorldGuardBounds;
    private String adminDebugStatusOk;
    private String adminDebugStatusMissing;
    private String adminDebugUnknownDate;
    private String creationCurrentValueLine;
    private String creationUnsetValue;
    private String creationPromptProtectionId;
    private String creationPromptDisplayName;
    private String creationPromptBlockType;
    private String creationPromptRadius;
    private String creationPromptPrice;
    private String creationPromptDefault;
    private String msgCreationInputCancelled;
    private String msgCreationInputSaved;
    private String msgCreationIncomplete;
    private String msgCreationAlreadyExists;
    private String msgCreationCreated;
    private String msgCreationSaveError;
    private String msgCreationInvalidId;
    private String msgCreationInvalidName;
    private String msgCreationInvalidMaterial;
    private String msgCreationInvalidRadius;
    private String msgCreationInvalidPrice;
    private String msgCreationUnknownField;
    private String msgProtectionUsage;
    private String msgProtectionGiveUsage;
    private String msgProtectionBuyUsage;
    private String msgProtectionPlayerOffline;
    private String msgProtectionNotFound;
    private String msgProtectionInvalidAmount;
    private String msgProtectionInventoryFull;
    private String msgProtectionGiveSender;
    private String msgProtectionGiveTarget;
    private String msgProtectionNoInventorySpace;
    private String msgProtectionEconomyUnavailable;
    private String msgProtectionNotEnoughMoney;
    private String msgProtectionBuyError;
    private String msgProtectionBoughtWithPrice;
    private String msgProtectionBoughtFree;
    private String msgProtectionRentUsage;
    private String msgProtectionRentDisabled;
    private String msgProtectionRentAlreadyPaid;
    private String msgProtectionRentPaid;
    private String msgProtectionRentSuspended;
    private String msgProtectionRentReactivated;
    private String msgProtectionRentPaymentFailed;
    private String msgProtectionFlyUsage;
    private String msgProtectionFlyEnabled;
    private String msgProtectionFlyDisabled;
    private String msgProtectionFlyLeftRegion;
    private String msgProtectionFlyNotAccessible;
    private String msgProtectionPlaced;
    private String msgProtectionRegionUnavailable;
    private String msgProtectionRegionExists;
    private String msgProtectionRegionOverlap;
    private String msgProtectionRegionSaveError;
    private String msgProtectionRemoved;
    private String msgProtectionRemoveNotOwner;
    private String msgProtectionRemoveSaveError;
    private String msgProtectionRemoveConfirmOpen;
    private String msgProtectionRemoveCancelled;
    private String msgProtectionMenuNotInOwnProtection;
    private String msgProtectionTeleportUsage;
    private String msgProtectionAliasUsage;
    private String msgProtectionMemberUsage;
    private String msgProtectionInviteUsage;
    private String msgProtectionTransferUsage;
    private String msgProtectionLogsUsage;
    private String msgProtectionNotInProtection;
    private String msgProtectionNotAccessible;
    private String msgProtectionWorldUnavailable;
    private String msgProtectionTeleportSuccess;
    private String msgProtectionAliasInvalid;
    private String msgProtectionAliasTaken;
    private String msgProtectionAliasUpdated;
    private String msgProtectionTransferSuccess;
    private String msgProtectionTransferReceived;
    private String msgProtectionTransferTargetOwner;
    private String msgProtectionLogsEmpty;
    private String msgProtectionLogsHeader;
    private String msgProtectionLogsEntry;
    private String msgProtectionLeaveOwnDenied;
    private String msgProtectionLeaveSuccess;
    private String msgProtectionViewShown;
    private String msgProtectionViewCooldown;
    private String msgProtectionListEmpty;
    private String msgProtectionInvalidConfig;
    private String msgProtectionInfoUnknownDate;

    private String protectionListHeader;
    private String protectionListEntry;
    private String protectionListInfoLabel;
    private String protectionListInfoHover;
    private String msgProtectionHomeEmpty;
    private String msgProtectionHomeCooldown;
    private String msgProtectionMemberAdded;
    private String msgProtectionMemberRemoved;
    private String msgProtectionMemberPromoted;
    private String msgProtectionMemberDemoted;
    private String msgProtectionMemberListEmpty;
    private String msgProtectionMemberAlready;
    private String msgProtectionMemberNotMember;
    private String msgProtectionMemberTargetOwner;
    private String msgProtectionMemberSaveError;
    private String msgProtectionInviteSent;
    private String msgProtectionInviteExists;
    private String msgProtectionInviteInvalid;
    private String msgProtectionInviteExpired;
    private String msgProtectionInviteNotForYou;
    private String msgProtectionInviteAccepted;
    private String msgProtectionInviteAcceptedOwner;
    private String msgProtectionInviteDenied;
    private String msgProtectionInviteDeniedOwner;
    private List<String> protectionInviteLines;
    private String protectionInviteAcceptLabel;
    private String protectionInviteAcceptHover;
    private String protectionInviteDenyLabel;
    private String protectionInviteDenyHover;
    private String protectionInviteActionSeparator;
    private String msgProtectionIntrusionAlert;
    private String protectionIntrusionActionEntry;
    private String protectionIntrusionActionStorage;
    private String protectionIntrusionActionBreakBlocks;
    private String protectionIntrusionActionPlaceBlocks;
    private String protectionIntrusionActionDoors;
    private String protectionIntrusionActionButtons;
    private String protectionIntrusionActionPressurePlates;
    private String protectionIntrusionActionWorkstations;
    private String protectionIntrusionActionInteract;
    private String protectionEventMemberRemoved;
    private String protectionEventInviteCreated;
    private String protectionEventInviteAccepted;
    private String protectionEventInviteDenied;
    private String protectionEventOwnerTransferred;
    private String msgProtectionMemberRemovedTeleported;
    private String msgProtectionLimitMaxProtections;
    private String msgProtectionLimitRadius;
    private String msgProtectionLimitPrice;
    private String msgProtectionHomeSet;
    private String msgProtectionHomeOutside;
    private String msgProtectionHomeSaveError;
    private String msgProtectionFlagUpdated;
    private String msgProtectionFlagDenied;
    private List<String> protectionItemLore;
    private String protectionDefaultActionbarEnter;
    private String protectionDefaultActionbarExit;

    public MainConfigManager(ArgosProtect plugin) {
        this.plugin = plugin;
        configFile = new CustomConfig("config.yml", null, plugin, false);
        configFile.registerConfig();
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = configFile.getConfig();

        String selectedLanguage = config.getString("general.language", "en");
        String langPath = "messages_" + selectedLanguage + ".yml";
        langFile = new CustomConfig(langPath, "lang", plugin, false);
        langFile.registerConfig();
        FileConfiguration lang = langFile.getConfig();

        // CONFIG
        prefix = config.getString("general.prefix", "&#27F554&lArgosProtect &8» ");
        updateCheckEnabled = config.getBoolean("general.update-check", true);
        bStatsEnabled = config.getBoolean("general.bstats", true);
        creationMenuTitle = config.getString("menus.creation.title", "&8Create Warding Stone");
        creationMenuSize = normalizeInventorySize(config.getInt("menus.creation.size", 27));
        creationMenuFillerMaterial = parseMaterial(
                config.getString("menus.creation.filler.material", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        creationMenuFillerName = config.getString("menus.creation.filler.name", " ");
        creationMenuItems = loadCreationMenuItems(config);
        creationCurrentValueLine = config.getString("menus.creation.current-value-line", "&7Current: %value%");
        languageMenuTitle = config.getString("menus.language.title", "&8Select Language");
        languageMenuSize = normalizeInventorySize(config.getInt("menus.language.size", 27));
        languageMenuFillerMaterial = parseMaterial(
                config.getString("menus.language.filler.material", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        languageMenuFillerName = config.getString("menus.language.filler.name", " ");
        languageMenuItems = loadMenuItems(config, "menus.language.items");
        protectionMenuTitle = config.getString("menus.protection.title", "&6» &8Protection Menu &6«");
        protectionMenuSize = normalizeInventorySize(config.getInt("menus.protection.size", 27));
        protectionMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionMenuFillerName = config.getString("menus.protection.filler.name", " ");
        protectionMenuItems = loadMenuItems(config, "menus.protection.items");
        if (protectionMenuItems.isEmpty()) {
            protectionMenuItems = defaultProtectionMenuItems();
        }
        protectionInfoMenuTitle = config.getString("menus.info.title", "&6» &8Protection Info &6«");
        protectionInfoMenuSize = normalizeInventorySize(config.getInt("menus.info.size", 27));
        protectionInfoMenuFillerMaterial = parseMaterial(
                config.getString("menus.info.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionInfoMenuFillerName = config.getString("menus.info.filler.name", " ");
        protectionInfoMenuItems = loadMenuItems(config, "menus.info.items");
        if (protectionInfoMenuItems.isEmpty()) {
            protectionInfoMenuItems = defaultProtectionInfoMenuItems();
        }
        protectionSettingsMenuTitle = config.getString("menus.protection-settings.title",
                "&6» &8Protection Settings &6«");
        protectionSettingsMenuSize = normalizeInventorySize(config.getInt("menus.protection-settings.size", 45));
        protectionSettingsMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection-settings.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionSettingsMenuFillerName = config.getString("menus.protection-settings.filler.name", " ");
        protectionSettingsMenuPreviousItem = loadNavigationItem(config, "menus.protection-settings.previous-item", 36,
                Material.ARROW, "&e&l« Previous Page", List.of("&fClick to go to page %page%"));
        protectionSettingsMenuNextItem = loadNavigationItem(config, "menus.protection-settings.next-item", 44,
                Material.ARROW, "&e&lNext Page »", List.of("&fClick to go to page %page%"));
        protectionSettingsMenuItems = loadMenuItems(config, "menus.protection-settings.items");
        if (protectionSettingsMenuItems.isEmpty()) {
            protectionSettingsMenuItems = defaultProtectionSettingsMenuItems();
        }
        protectionSettingsAccessNobody = config.getString(
                "menus.protection-settings.status-labels.access.nobody", ProtectionFlagLevel.NOBODY.accessLabel());
        protectionSettingsAccessMembers = config.getString(
                "menus.protection-settings.status-labels.access.members", ProtectionFlagLevel.MEMBERS.accessLabel());
        protectionSettingsAccessEveryone = config.getString(
                "menus.protection-settings.status-labels.access.everyone", ProtectionFlagLevel.EVERYONE.accessLabel());
        protectionSettingsToggleDisabled = config.getString(
                "menus.protection-settings.status-labels.toggle.disabled", ProtectionFlagLevel.NOBODY.toggleLabel());
        protectionSettingsToggleEnabled = config.getString(
                "menus.protection-settings.status-labels.toggle.enabled", ProtectionFlagLevel.EVERYONE.toggleLabel());
        protectionHomeMenuTitle = config.getString("menus.protection-home.title", "&a&l>> &8Your Homes &a&l<<");
        protectionHomeMenuSize = normalizeInventorySize(config.getInt("menus.protection-home.size", 54));
        protectionHomeMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection-home.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionHomeMenuFillerName = config.getString("menus.protection-home.filler.name", " ");
        protectionHomeMenuContentSlots = normalizeMenuSlots(
                loadIntegerList(config, "menus.protection-home.content-slots",
                        defaultContentSlots(protectionHomeMenuSize)),
                protectionHomeMenuSize, defaultContentSlots(protectionHomeMenuSize));
        protectionHomeMenuItems = loadMenuItems(config, "menus.protection-home.items");
        protectionHomeMenuItem = loadMenuDisplay(config, "menus.protection-home.item", Material.STONE,
                "&a&lProtection &f%alias% &8(&7%protection_id%&8)",
                List.of("&8Teleport menu", "", "&6Access: %role%", "&aSize: &f%size%",
                        "&dLocation: &f%stone_x%, %stone_y%, %stone_z%", "",
                        "&e&l> &eClick to teleport"));
        protectionHomeMenuPreviousItem = loadNavigationItem(config, "menus.protection-home.navigation.previous",
                45, Material.ARROW, "&a&lPrevious page", List.of("&7Go back to page &f%page%"));
        protectionHomeMenuNextItem = loadNavigationItem(config, "menus.protection-home.navigation.next",
                53, Material.ARROW, "&a&lNext page", List.of("&7Go to page &f%page%"));
        protectionHomeRoleOwner = config.getString("menus.protection-home.role-labels.owner", "&cMain Owner");
        protectionHomeRoleAdmin = config.getString("menus.protection-home.role-labels.admin", "&dStaff Access");
        protectionHomeRoleMember = config.getString("menus.protection-home.role-labels.member", "&bMember");

        protectionMembersMenuTitle = config.getString("menus.protection-members.title",
                "&a&l>> &8Protection Members &a&l<<");
        protectionMembersMenuSize = normalizeInventorySize(config.getInt("menus.protection-members.size", 54));
        protectionMembersMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection-members.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionMembersMenuFillerName = config.getString("menus.protection-members.filler.name", " ");
        protectionMembersMenuContentSlots = normalizeMenuSlots(
                loadIntegerList(config, "menus.protection-members.content-slots",
                        defaultContentSlots(protectionMembersMenuSize)),
                protectionMembersMenuSize, defaultContentSlots(protectionMembersMenuSize));
        protectionMembersMenuItems = loadMenuItems(config, "menus.protection-members.items");
        protectionMembersMenuItem = loadMenuDisplay(config, "menus.protection-members.item", Material.PLAYER_HEAD,
                "&e&l%member%",
                List.of("&8Protection member", "", "&6Access: %role%", "&bSince: &f%created_date%", "",
                        "&e&l> &eClick to manage this member", "&8Use /p member add/remove to make changes."));
        protectionMembersMenuPreviousItem = loadNavigationItem(config,
                "menus.protection-members.navigation.previous", 45, Material.ARROW, "&a&lPrevious page",
                List.of("&7Go back to page &f%page%"));
        protectionMembersMenuNextItem = loadNavigationItem(config, "menus.protection-members.navigation.next",
                53, Material.ARROW, "&a&lNext page", List.of("&7Go to page &f%page%"));
        protectionMembersRoleOwner = config.getString("menus.protection-members.role-labels.owner", "&cMain Owner");
        protectionMembersRoleAdmin = config.getString("menus.protection-members.role-labels.admin", "&dAdmin");
        protectionMembersRoleMember = config.getString("menus.protection-members.role-labels.member", "&bMember");
        protectionMembersUnknownDate = config.getString("menus.protection-members.unknown-date", "Unknown");
        protectionLogsMenuTitle = config.getString("menus.protection-logs.title",
                "&a&l>> &8Protection Logs &a&l<<");
        protectionLogsMenuSize = normalizeInventorySize(config.getInt("menus.protection-logs.size", 45));
        protectionLogsMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection-logs.filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        protectionLogsMenuFillerName = config.getString("menus.protection-logs.filler.name", " ");
        protectionLogsMenuContentSlots = normalizeMenuSlots(
                loadIntegerList(config, "menus.protection-logs.content-slots",
                        defaultContentSlots(protectionLogsMenuSize)),
                protectionLogsMenuSize, defaultContentSlots(protectionLogsMenuSize));
        protectionLogsMenuItem = loadMenuDisplay(config, "menus.protection-logs.log-item", Material.PLAYER_HEAD,
                "&e&l%type%",
                List.of("&8%date%", "", "&6Actor: &f%actor%", "&bTarget: &f%target%", "&aDetail: &f%detail%"));
        protectionLogsMenuDefaultTexture = config.getString("menus.protection-logs.default-log-texture", "");
        protectionLogsMenuTextures = new HashMap<>();
        if (config.isConfigurationSection("menus.protection-logs.log-textures")) {
            for (String key : config.getConfigurationSection("menus.protection-logs.log-textures").getKeys(false)) {
                protectionLogsMenuTextures.put(key, config.getString("menus.protection-logs.log-textures." + key, ""));
            }
        }
        protectionLogsMenuItems = loadMenuItems(config, "menus.protection-logs.items");
        protectionLogsMenuPreviousItem = loadNavigationItem(config, "menus.protection-logs.navigation.previous",
                45, Material.ARROW, "&a&lPrevious", List.of());
        protectionLogsMenuNextItem = loadNavigationItem(config, "menus.protection-logs.navigation.next",
                53, Material.ARROW, "&a&lNext", List.of());

        adminPlacedMenuTitle = config.getString("menus.admin-placed.title", "&8Placed Protections");
        adminPlacedMenuSize = normalizeInventorySize(config.getInt("menus.admin-placed.size", 54));
        adminPlacedMenuFillerMaterial = parseMaterial(
                config.getString("menus.admin-placed.filler.material", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        adminPlacedMenuFillerName = config.getString("menus.admin-placed.filler.name", " ");
        adminPlacedMenuContentSlots = normalizeMenuSlots(
                loadIntegerList(config, "menus.admin-placed.content-slots",
                        defaultAdminPlacedContentSlots(adminPlacedMenuSize)),
                adminPlacedMenuSize, defaultAdminPlacedContentSlots(adminPlacedMenuSize));
        adminPlacedMenuItems = loadMenuItems(config, "menus.admin-placed.items");
        if (adminPlacedMenuItems.isEmpty()) {
            adminPlacedMenuItems = defaultAdminPlacedMenuItems();
        }
        adminPlacedMenuItem = loadMenuDisplay(config, "menus.admin-placed.item", Material.LIME_CONCRETE,
                "&e&l%alias%",
                List.of("&8%region%", "", "&6Owner: &f%owner%", "&bProtection: &f%protection_id%",
                        "&dWorld: &f%world%", "&aStone: &f%stone_x%, %stone_y%, %stone_z%", "",
                        "&7WorldGuard: %state_worldguard%", "&7YAML: %state_yaml%",
                        "&7Stone block: %state_stone%", "", "&eClick to print debug details"));
        adminPlacedMenuPreviousItem = loadNavigationItem(config,
                "menus.admin-placed.navigation.previous", 45, Material.ARROW, "&a&lPrevious page",
                List.of("&7Go back to page &f%page%"));
        adminPlacedMenuNextItem = loadNavigationItem(config, "menus.admin-placed.navigation.next",
                53, Material.ARROW, "&a&lNext page", List.of("&7Go to page &f%page%"));
        adminPlacedStatusOk = config.getString("menus.admin-placed.status-labels.ok", "&#55FF55✔");
        adminPlacedStatusMissing = config.getString("menus.admin-placed.status-labels.missing", "&cMissing");
        adminPlacedUseProtectionBlock = config.getBoolean("menus.admin-placed.item.use-protection-block", true);
        adminPlacedHealthyMaterial = parseMaterial(
                config.getString("menus.admin-placed.item.healthy-material", adminPlacedMenuItem.material().name()),
                adminPlacedMenuItem.material());
        adminPlacedBrokenMaterial = parseMaterial(
                config.getString("menus.admin-placed.item.broken-material", "RED_CONCRETE"),
                Material.RED_CONCRETE);
        protectionRemoveConfirmMenuTitle = config.getString("menus.protection-remove-confirm.title",
                "&c&lConfirm protection removal");
        protectionRemoveConfirmMenuSize = normalizeInventorySize(
                config.getInt("menus.protection-remove-confirm.size", 27));
        protectionRemoveConfirmMenuFillerMaterial = parseMaterial(
                config.getString("menus.protection-remove-confirm.filler.material", "BLACK_STAINED_GLASS_PANE"),
                Material.BLACK_STAINED_GLASS_PANE);
        protectionRemoveConfirmMenuFillerName = config.getString("menus.protection-remove-confirm.filler.name", " ");
        protectionRemoveConfirmMenuItems = loadMenuItems(config, "menus.protection-remove-confirm.items");
        if (protectionRemoveConfirmMenuItems.isEmpty()) {
            protectionRemoveConfirmMenuItems = defaultProtectionRemoveConfirmMenuItems();
        }
        protectionHomeCooldownSeconds = Math.max(0, config.getInt("protection.home-cooldown-seconds", 5));
        protectionFeedbackConfig = ProtectionFeedbackConfig.from(config);
        protectionRentSettings = new ProtectionRentSettings(
                config.getBoolean("protection.rent.enabled", false),
                config.getInt("protection.rent.period-hours", 24),
                config.getInt("protection.rent.check-interval-minutes", 10));
        defaultProtectionLimitProfile = loadLimitProfile(config, "limits.default", "default", "", 0);
        protectionLimitGroups = loadLimitGroups(config);
        registerLimitPermissions();

        // MESSAGES
        msgUpdateAvailable = lang.getString("messages.update-available", "&f&lNEW VERSION: &7{version}");
        msgUpdateCurrent = lang.getString("messages.update-current", "&7Your current version: &c{version}");
        msgUpdateDownload = lang.getString("messages.update-download", "&eDownload it to get improvements and fixes.");
        msgNoPermission = lang.getString("messages.commands.no-permission", "&cYou do not have permission.");
        msgPluginReload = lang.getString("messages.commands.plugin-reload", "&aConfiguration successfully reloaded.");
        msgUsageCommand = lang.getString("messages.commands.usage",
                "&cUse: /argosprotect <create|reload|lang|debug|listplaced|report|repair|migrate>");
        msgPlayerOnly = lang.getString("messages.commands.player-only",
                "&cThis command can only be executed by a player.");
        msgLanguageChanged = lang.getString("messages.commands.language-changed",
                "&aLanguage changed to &f%language%&a.");
        msgLanguageFileMissing = lang.getString("messages.commands.language-file-missing",
                "&cCould not find language config: &f%file%&c.");
        msgLanguageNameEnglish = lang.getString("messages.commands.language-names.en", "English");
        msgLanguageNameSpanish = lang.getString("messages.commands.language-names.es", "Spanish");
        msgAdminDebugUsage = lang.getString("messages.admin.debug.usage", "&cUsage: /ap debug <region>");
        msgAdminDebugNotFound = lang.getString("messages.admin.debug.not-found",
                "&cNo placed protection or WorldGuard region was found for &f%lookup%&c.");
        msgAdminListPlacedUsage = lang.getString("messages.admin.listplaced.usage", "&cUsage: /ap listplaced");
        msgAdminReportUsage = lang.getString("messages.admin.report.usage", "&cUsage: /ap report protections");
        msgAdminLogsUsage = lang.getString("messages.admin.logs.usage", "&cUsage: /ap logs <region|alias>");
        msgAdminLogsEmpty = lang.getString("messages.admin.logs.empty",
                "&eNo protection events were found for &f%alias%&e.");
        msgAdminLogsHeader = lang.getString("messages.admin.logs.header",
                "&6Recent events for &f%alias% &7(%region%)");
        msgAdminLogsEntry = lang.getString("messages.admin.logs.entry",
                "&8- &7%date% &f%type% &8| &e%actor% &7-> &b%target% &8| &7%detail%");
        adminReportProtectionLines = lang.getStringList("messages.admin.report.protections.lines");
        if (adminReportProtectionLines.isEmpty()) {
            adminReportProtectionLines = List.of(
                    "&6&lArgosProtect Protection Report",
                    "&eTotal tracked: &f%total%",
                    "&aActive: &f%active%",
                    "&cOrphaned: &f%orphaned%",
                    "&eLargest regions:",
                    "%largest_regions%",
                    "&eTop owners:",
                    "%top_owners%");
        }
        adminReportProtectionEmptyEntry = lang.getString("messages.admin.report.protections.empty-entry", "&7None");
        adminReportProtectionLargestEntry = lang.getString("messages.admin.report.protections.largest-entry",
                "&8%index%. &f%name% &7(%blocks% blocks, %owner%)");
        adminReportProtectionTopOwnerEntry = lang.getString("messages.admin.report.protections.top-owner-entry",
                "&8%index%. &f%owner% &7(%count%)");
        msgAdminRepairUsage = lang.getString("messages.admin.repair.usage", "&cUsage: /ap repair [cleanup]");
        msgAdminRepairSummary = lang.getString("messages.admin.repair.summary",
                "&6Repair summary: &a%repaired% repaired&7, &b%cleaned% cleaned&7, &e%skipped% skipped&7.");
        msgAdminRepairSaveError = lang.getString("messages.admin.repair.save-error",
                "&cRepair could not be saved.");
        msgAdminRepairSkippedWorldUnavailable = lang.getString(
                "messages.admin.repair.skipped-world-unavailable",
                "&eSkipped &f%region% &7(world unavailable).");
        msgAdminRepairCleanedStaleTracker = lang.getString(
                "messages.admin.repair.cleaned-stale-tracker",
                "&aCleaned stale tracker entry &f%region%&a.");
        msgAdminRepairOrphanTracker = lang.getString("messages.admin.repair.orphan-tracker",
                "&eOrphan tracker found: &f%region% &7(run /ap repair cleanup to remove it).");
        msgAdminRepairSkippedWorldGuardRegion = lang.getString(
                "messages.admin.repair.skipped-worldguard-region",
                "&eSkipped WG region &f%region% &7(%reason%).");
        msgAdminRepairRepairedWorldGuardRegion = lang.getString(
                "messages.admin.repair.repaired-worldguard-region",
                "&aRepaired tracking for WG region &f%region%&a.");
        msgAdminRepairSaveRollback = lang.getString("messages.admin.repair.save-rollback",
                "&cCould not save protections.db. No in-memory changes were kept.");
        msgAdminRepairReasonRegionIdMismatch = lang.getString(
                "messages.admin.repair.skip-reasons.region-id-mismatch",
                "region id does not match a known ArgosProtect protection id");
        msgAdminRepairReasonProtectionYamlInvalid = lang.getString(
                "messages.admin.repair.skip-reasons.protection-yaml-invalid",
                "protection YAML is missing or invalid");
        msgAdminRepairReasonNoOwner = lang.getString("messages.admin.repair.skip-reasons.no-owner",
                "WorldGuard region has no UUID owner");
        msgAdminRepairReasonStoneNotFound = lang.getString("messages.admin.repair.skip-reasons.stone-not-found",
                "could not safely identify a unique protection stone at the region center");
        msgAdminMigrateUsage = lang.getString("messages.admin.migrate.usage",
                "&cUsage: /ap migrate protectionstones <preview|apply>");
        msgAdminMigrateSummary = lang.getString("messages.admin.migrate.summary",
                "&6Migration summary: &a%imported% imported&7, &b%importable% importable&7, &e%warnings% warnings&7, &c%skipped% skipped&7, &d%already% already tracked&7, &4%failed% failed&7.");
        msgAdminMigrateEntry = lang.getString("messages.admin.migrate.entry",
                "&8- &f%region% &7(%world%) &8%status% &7- %reason%");
        msgAdminMigrateBackupWarning = lang.getString("messages.admin.migrate.backup-warning",
                "&eBefore apply, back up WorldGuard regions.yml files and ArgosProtect protections.db.");
        adminDebugHeader = lang.getString("messages.admin.debug.header", "&6&lArgosProtect Debug &8- &f%region%");
        adminDebugTrackingMissing = lang.getString("messages.admin.debug.tracking-missing",
                "&eTracking: &cMissing from protections.db");
        adminDebugTrackingPresent = lang.getString("messages.admin.debug.tracking-present",
                "&eTracking: &aPresent in protections.db");
        adminDebugProtectionYaml = lang.getString("messages.admin.debug.protection-yaml",
                "&eProtection YAML: %state% &7(%protection_id%.yml)");
        adminDebugOwner = lang.getString("messages.admin.debug.owner",
                "&eOwner: &f%owner% &8(%owner_uuid%)");
        adminDebugMembers = lang.getString("messages.admin.debug.members", "&eMembers: &f%members%");
        adminDebugMemberEntry = lang.getString("messages.admin.debug.member-entry",
                "&8- &f%member% &7%rank% &8(%member_uuid%)");
        adminDebugWorld = lang.getString("messages.admin.debug.world", "&eWorld: &f%world%");
        adminDebugStone = lang.getString("messages.admin.debug.stone",
                "&eStone: &f%stone_x%, %stone_y%, %stone_z% &7%state%");
        adminDebugBounds = lang.getString("messages.admin.debug.bounds",
                "&eBounds: &f%min_x%,%min_y%,%min_z% &8-> &f%max_x%,%max_y%,%max_z%");
        adminDebugCreated = lang.getString("messages.admin.debug.created", "&eCreated: &f%created_at%");
        adminDebugWorldGuardMissing = lang.getString("messages.admin.debug.worldguard-missing",
                "&eWorldGuard: &cMissing");
        adminDebugWorldGuardPresent = lang.getString("messages.admin.debug.worldguard-present",
                "&eWorldGuard: &aPresent &7(world: &f%world%&7)");
        adminDebugWorldGuardOwners = lang.getString("messages.admin.debug.worldguard-owners",
                "&eWG Owners: &f%owners%");
        adminDebugWorldGuardOwnerEntry = lang.getString("messages.admin.debug.worldguard-owner-entry",
                "&8- &f%owner% &8(%owner_uuid%)");
        adminDebugWorldGuardMembers = lang.getString("messages.admin.debug.worldguard-members",
                "&eWG Members: &f%members%");
        adminDebugWorldGuardBounds = lang.getString("messages.admin.debug.worldguard-bounds",
                "&eWG Bounds: &f%min% &8-> &f%max%");
        adminDebugStatusOk = lang.getString("messages.admin.debug.status.ok", "&#55FF55✔");
        adminDebugStatusMissing = lang.getString("messages.admin.debug.status.missing", "&cMissing");
        adminDebugUnknownDate = lang.getString("messages.admin.debug.unknown-date", "Unknown");
        creationPromptProtectionId = lang.getString("messages.creation.prompts.protection-id",
                "&eType the protection ID in chat. Use &fcancel &eto go back.");
        creationPromptDisplayName = lang.getString("messages.creation.prompts.display-name",
                "&eType the display name in chat. Color codes are allowed. Use &fcancel &eto go back.");
        creationPromptBlockType = lang.getString("messages.creation.prompts.block-type",
                "&eHold the block you want to use, then click Block Type.");
        creationPromptRadius = lang.getString("messages.creation.prompts.radius",
                "&eType the protection radius as a whole number. Use &fcancel &eto go back.");
        creationPromptPrice = lang.getString("messages.creation.prompts.price",
                "&eType the Vault price. Use &f0 &efor free. Use &fcancel &eto go back.");
        creationPromptDefault = lang.getString("messages.creation.prompts.default",
                "&eType the value in chat. Use &fcancel &eto go back.");
        msgCreationInputCancelled = lang.getString("messages.creation.input-cancelled", "&cCreation input cancelled.");
        msgCreationInputSaved = lang.getString("messages.creation.input-saved", "&aValue saved.");
        msgCreationIncomplete = lang.getString("messages.creation.incomplete",
                "&cComplete every field before confirming.");
        msgCreationAlreadyExists = lang.getString("messages.creation.already-exists",
                "&cA protection with this ID already exists.");
        msgCreationCreated = lang.getString("messages.creation.created", "&aProtection stone &f%id% &awas created.");
        msgCreationSaveError = lang.getString("messages.creation.save-error", "&cCould not save the protection file.");
        msgCreationInvalidId = lang.getString("messages.creation.invalid-id",
                "&cInvalid ID. Use only lowercase letters, numbers, _ or -.");
        msgCreationInvalidName = lang.getString("messages.creation.invalid-name",
                "&cThe display name cannot be empty.");
        msgCreationInvalidMaterial = lang.getString("messages.creation.invalid-material",
                "&cYou must hold a placeable block in your main hand.");
        msgCreationInvalidRadius = lang.getString("messages.creation.invalid-radius",
                "&cRadius must be a positive whole number.");
        msgCreationInvalidPrice = lang.getString("messages.creation.invalid-price",
                "&cPrice must be a positive number or 0.");
        creationUnsetValue = lang.getString("messages.creation.unset-value", "&cNot set");
        msgCreationUnknownField = lang.getString("messages.creation.unknown-field", "&cUnknown creation field.");
        msgProtectionUsage = lang.getString("messages.protection.usage", "&cUse: /p <give|buy|rent|menu|invite>");
        msgProtectionGiveUsage = lang.getString("messages.protection.give-usage",
                "&cUse: /p give <player> <protection> <amount>");
        msgProtectionBuyUsage = lang.getString("messages.protection.buy-usage", "&cUse: /p buy <protection>");
        msgProtectionPlayerOffline = lang.getString("messages.protection.player-offline",
                "&cPlayer &f%player% &cis not online.");
        msgProtectionNotFound = lang.getString("messages.protection.not-found",
                "&cThere is no protection with ID &f%id%&c.");
        msgProtectionInvalidAmount = lang.getString("messages.protection.invalid-amount",
                "&cAmount must be a positive whole number.");
        msgProtectionInventoryFull = lang.getString("messages.protection.inventory-full",
                "&eInventory full. Delivered &f%delivered% &eof &f%amount%&e.");
        msgProtectionGiveSender = lang.getString("messages.protection.give-sender",
                "&aYou gave &f%amount%x %protection% &ato &f%player%&a.");
        msgProtectionGiveTarget = lang.getString("messages.protection.give-target",
                "&aYou received &f%amount%x %protection%&a.");
        msgProtectionNoInventorySpace = lang.getString("messages.protection.no-inventory-space",
                "&cYou need inventory space to buy this protection.");
        msgProtectionEconomyUnavailable = lang.getString("messages.protection.economy-unavailable",
                "&cEconomy is not available right now.");
        msgProtectionNotEnoughMoney = lang.getString("messages.protection.not-enough-money",
                "&cYou do not have enough money. Price: &f%price%");
        msgProtectionBuyError = lang.getString("messages.protection.buy-error",
                "&cCould not complete the purchase: &f%error%");
        msgProtectionBoughtWithPrice = lang.getString("messages.protection.bought-with-price",
                "&aYou bought &f%protection% &afor &f%price%&a.");
        msgProtectionBoughtFree = lang.getString("messages.protection.bought-free",
                "&aYou bought &f%protection%&a.");
        msgProtectionRentUsage = lang.getString("messages.protection.rent-usage",
                "&cUse: /p rent [region|alias]");
        msgProtectionRentDisabled = lang.getString("messages.protection.rent-disabled",
                "&cProtection rent is disabled.");
        msgProtectionRentAlreadyPaid = lang.getString("messages.protection.rent-already-paid",
                "&eRent for &f%alias% &eis already paid until &f%paid_until%&e.");
        msgProtectionRentPaid = lang.getString("messages.protection.rent-paid",
                "&aRent paid for &f%alias%&a. Active until &f%paid_until%&a.");
        msgProtectionRentSuspended = lang.getString("messages.protection.rent-suspended",
                "&cProtection &f%alias% &cwas suspended because rent could not be paid.");
        msgProtectionRentReactivated = lang.getString("messages.protection.rent-reactivated",
                "&aProtection &f%alias% &awas reactivated until &f%paid_until%&a.");
        msgProtectionRentPaymentFailed = lang.getString("messages.protection.rent-payment-failed",
                "&cCould not pay rent for &f%alias%&c: &f%reason%");
        msgProtectionFlyUsage = lang.getString("messages.protection.fly-usage", "&cUse: /p fly");
        msgProtectionFlyEnabled = lang.getString("messages.protection.fly-enabled",
                "&aProtection fly enabled inside &f%alias%&a.");
        msgProtectionFlyDisabled = lang.getString("messages.protection.fly-disabled",
                "&eProtection fly disabled.");
        msgProtectionFlyLeftRegion = lang.getString("messages.protection.fly-left-region",
                "&cProtection fly was disabled because you left the protection.");
        msgProtectionFlyNotAccessible = lang.getString("messages.protection.fly-not-accessible",
                "&cYou can only fly inside protections where you are owner or member.");
        msgProtectionPlaced = lang.getString("messages.protection.placed",
                "&aProtection &f%protection% &awas placed. Region: &f%region%&a.");
        msgProtectionRegionUnavailable = lang.getString("messages.protection.region-unavailable",
                "&cWorldGuard regions are not available in this world.");
        msgProtectionRegionExists = lang.getString("messages.protection.region-exists",
                "&cA protection region already exists at this exact location.");
        msgProtectionRegionOverlap = lang.getString("messages.protection.region-overlap",
                "&cYou cannot place this protection here because another protection is nearby.");
        msgProtectionRegionSaveError = lang.getString("messages.protection.region-save-error",
                "&cThe protection region could not be saved.");
        msgProtectionRemoved = lang.getString("messages.protection.removed",
                "&aYou removed &f%protection%&a. Region deleted: &f%region%&a.");
        msgProtectionRemoveNotOwner = lang.getString("messages.protection.remove-not-owner",
                "&cOnly the owner can remove this protection.");
        msgProtectionRemoveSaveError = lang.getString("messages.protection.remove-save-error",
                "&cThe protection could not be removed safely.");
        msgProtectionRemoveConfirmOpen = lang.getString("messages.protection.remove-confirm-open",
                "&eConfirm the removal in the menu.");
        msgProtectionRemoveCancelled = lang.getString("messages.protection.remove-cancelled",
                "&aProtection removal cancelled.");
        msgProtectionMenuNotInOwnProtection = lang.getString("messages.protection.menu.not-in-own-protection",
                "&cThere is no protection owned by you at your position.");
        msgProtectionTeleportUsage = lang.getString("messages.protection.teleport-usage",
                "&cUse: /p teleport <id>");
        msgProtectionAliasUsage = lang.getString("messages.protection.alias-usage",
                "&cUse: /p alias <alias>");
        msgProtectionMemberUsage = lang.getString("messages.protection.member-usage",
                "&cUse: /p member <add|remove|list> [player]");
        msgProtectionInviteUsage = lang.getString("messages.protection.invite-usage",
                "&cUse: /p invite <accept|deny> <id>");
        msgProtectionTransferUsage = lang.getString("messages.protection.transfer-usage",
                "&cUse: /p transfer <player>");
        msgProtectionLogsUsage = lang.getString("messages.protection.logs-usage",
                "&cUse: /p logs [region|alias]");
        msgProtectionNotInProtection = lang.getString("messages.protection.not-in-protection",
                "&cYou are not inside a protection.");
        msgProtectionNotAccessible = lang.getString("messages.protection.not-accessible",
                "&cYou do not have access to that protection.");
        msgProtectionWorldUnavailable = lang.getString("messages.protection.world-unavailable",
                "&cThe protection world is not available.");
        msgProtectionTeleportSuccess = lang.getString("messages.protection.teleport-success",
                "&aTeleported to &f%alias%&a.");
        msgProtectionAliasInvalid = lang.getString("messages.protection.alias-invalid",
                "&cAlias must use 3-24 letters, numbers, _ or -.");
        msgProtectionAliasTaken = lang.getString("messages.protection.alias-taken",
                "&cThat alias is already being used by another protection.");
        msgProtectionAliasUpdated = lang.getString("messages.protection.alias-updated",
                "&aAlias for the current protection is now &f%alias%&a.");
        msgProtectionTransferSuccess = lang.getString("messages.protection.transfer-success",
                "&aTransferred &f%alias% &ato &f%player%&a.");
        msgProtectionTransferReceived = lang.getString("messages.protection.transfer-received",
                "&aYou are now the owner of &f%alias%&a.");
        msgProtectionTransferTargetOwner = lang.getString("messages.protection.transfer-target-owner",
                "&cThat player already owns this protection.");
        msgProtectionLogsEmpty = lang.getString("messages.protection.logs-empty",
                "&eNo protection events were found for &f%alias%&e.");
        msgProtectionLogsHeader = lang.getString("messages.protection.logs-header",
                "&6Recent events for &f%alias% &7(%region%)");
        msgProtectionLogsEntry = lang.getString("messages.protection.logs-entry",
                "&8- &7%date% &f%type% &8| &e%actor% &7-> &b%target% &8| &7%detail%");
        msgProtectionLeaveOwnDenied = lang.getString("messages.protection.leave-own-denied",
                "&cYou cannot leave your own protection.");
        msgProtectionLeaveSuccess = lang.getString("messages.protection.leave-success",
                "&aYou left the protection &f%alias%&a.");
        msgProtectionViewShown = lang.getString("messages.protection.view-shown",
                "&aShowing the border for &f%alias%&a.");
        msgProtectionViewCooldown = lang.getString("messages.protection.view-cooldown",
                "&cWait &f%seconds%s &cbefore viewing this border again.");
        msgProtectionListEmpty = lang.getString("messages.protection.list-empty",
                "&cYou do not have protections yet.");
        msgProtectionInvalidConfig = lang.getString("messages.protection.invalid-config",
                "&cProtection &f%id% &chas an incomplete or invalid YAML file.");
        msgProtectionInfoUnknownDate = lang.getString("messages.protection.info.unknown-date", "Unknown");
        protectionListHeader = lang.getString("messages.protection.list.header", "&6&l>> &e&lMY PROTECTIONS");
        protectionListEntry = lang.getString("messages.protection.list.entry",
                "&f%alias% &7(%protection_id%) &8| &d%stone_x%, %stone_y%, %stone_z% &8| &e%size%x%size% &8| ");
        protectionListInfoLabel = lang.getString("messages.protection.list.info-label", "&a&lInfo");
        protectionListInfoHover = lang.getString("messages.protection.list.info-hover",
                "&eClick to view information\n&7Runs: &f/p info %alias%");
        msgProtectionHomeEmpty = lang.getString("messages.protection.home-empty",
                "&cYou do not have homes yet.");
        msgProtectionHomeCooldown = lang.getString("messages.protection.home-cooldown",
                "&eWait &f%seconds%s &ebefore teleporting to another home.");
        msgProtectionMemberAdded = lang.getString("messages.protection.member-added",
                "&aAdded &f%player% &ato &f%alias%&a.");
        msgProtectionMemberRemoved = lang.getString("messages.protection.member-removed",
                "&aRemoved &f%player% &afrom &f%alias%&a.");
        msgProtectionMemberPromoted = lang.getString("messages.protection.member-promoted",
                "&aPromoted &f%player% &ato admin in &f%alias%&a.");
        msgProtectionMemberDemoted = lang.getString("messages.protection.member-demoted",
                "&aDemoted &f%player% &ato member in &f%alias%&a.");
        msgProtectionMemberListEmpty = lang.getString("messages.protection.member-list-empty",
                "&cThis protection has no members.");
        msgProtectionMemberAlready = lang.getString("messages.protection.member-already",
                "&cThat player is already a member.");
        msgProtectionMemberNotMember = lang.getString("messages.protection.member-not-member",
                "&cThat player is not a member.");
        msgProtectionMemberTargetOwner = lang.getString("messages.protection.member-target-owner",
                "&cOwners cannot be managed as members.");
        msgProtectionMemberSaveError = lang.getString("messages.protection.member-save-error",
                "&cCould not save the member changes.");
        msgProtectionInviteSent = lang.getString("messages.protection.invite-sent",
                "&aInvitation sent to &f%player% &afor &f%alias%&a.");
        msgProtectionInviteExists = lang.getString("messages.protection.invite-exists",
                "&eThat player already has a pending invitation for &f%alias%&e.");
        msgProtectionInviteInvalid = lang.getString("messages.protection.invite-invalid",
                "&cThat invitation is not available.");
        msgProtectionInviteExpired = lang.getString("messages.protection.invite-expired",
                "&cThat invitation has expired.");
        msgProtectionInviteNotForYou = lang.getString("messages.protection.invite-not-for-you",
                "&cThat invitation is not for you.");
        msgProtectionInviteAccepted = lang.getString("messages.protection.invite-accepted",
                "&aYou joined &f%alias%&a.");
        msgProtectionInviteAcceptedOwner = lang.getString("messages.protection.invite-accepted-owner",
                "&f%player% &aaccepted the invitation to &f%alias%&a.");
        msgProtectionInviteDenied = lang.getString("messages.protection.invite-denied",
                "&eYou denied the invitation to &f%alias%&e.");
        msgProtectionInviteDeniedOwner = lang.getString("messages.protection.invite-denied-owner",
                "&f%player% &edenied the invitation to &f%alias%&e.");
        protectionInviteLines = lang.getStringList("messages.protection.invite-message.lines");
        if (protectionInviteLines.isEmpty()) {
            protectionInviteLines = List.of(
                    "&8&m----------------------------------------",
                    "&#FFE259&lProtection Invitation",
                    "&f%inviter% &7invited you to join &f%alias%&7.",
                    "%actions%",
                    "&8&m----------------------------------------");
        }
        protectionInviteAcceptLabel = lang.getString("messages.protection.invite-message.actions.accept-label",
                "&#7CFF6B&l[ACCEPT]");
        protectionInviteAcceptHover = lang.getString("messages.protection.invite-message.actions.accept-hover",
                "&aJoin this protection");
        protectionInviteDenyLabel = lang.getString("messages.protection.invite-message.actions.deny-label",
                "&#FF6B6B&l[DENY]");
        protectionInviteDenyHover = lang.getString("messages.protection.invite-message.actions.deny-hover",
                "&cDeny this invitation");
        protectionInviteActionSeparator = lang.getString("messages.protection.invite-message.actions.separator",
                " &8- ");
        msgProtectionIntrusionAlert = lang.getString("messages.protection.intrusion-alert",
                "&cAlert: &f%player% &ctried to %action% in &f%alias%&c.");
        protectionIntrusionActionEntry = lang.getString("messages.protection.intrusion-actions.entry", "enter");
        protectionIntrusionActionStorage = lang.getString("messages.protection.intrusion-actions.storage",
                "open storage");
        protectionIntrusionActionBreakBlocks = lang.getString("messages.protection.intrusion-actions.break-blocks",
                "break blocks");
        protectionIntrusionActionPlaceBlocks = lang.getString("messages.protection.intrusion-actions.place-blocks",
                "place blocks");
        protectionIntrusionActionDoors = lang.getString("messages.protection.intrusion-actions.doors", "use doors");
        protectionIntrusionActionButtons = lang.getString("messages.protection.intrusion-actions.buttons",
                "use buttons");
        protectionIntrusionActionPressurePlates = lang.getString(
                "messages.protection.intrusion-actions.pressure-plates", "use pressure plates");
        protectionIntrusionActionWorkstations = lang.getString("messages.protection.intrusion-actions.workstations",
                "use workstations");
        protectionIntrusionActionInteract = lang.getString("messages.protection.intrusion-actions.interact",
                "interact");
        protectionEventMemberRemoved = lang.getString("messages.protection.events.member-removed",
                "Member removed from protection");
        protectionEventInviteCreated = lang.getString("messages.protection.events.invite-created",
                "Member invite created");
        protectionEventInviteAccepted = lang.getString("messages.protection.events.invite-accepted",
                "Member invite accepted");
        protectionEventInviteDenied = lang.getString("messages.protection.events.invite-denied",
                "Member invite denied");
        protectionEventOwnerTransferred = lang.getString("messages.protection.events.owner-transferred",
                "Ownership transferred from %old_owner% to %new_owner%");
        msgProtectionMemberRemovedTeleported = lang.getString("messages.protection.member-removed-teleported",
                "&eYou were removed from &f%alias% &eand moved outside.");
        msgProtectionLimitMaxProtections = lang.getString("messages.protection.limits.max-protections",
                "&cYou reached your protection limit: &f%current%/%max%&c.");
        msgProtectionLimitRadius = lang.getString("messages.protection.limits.radius",
                "&cYour limit only allows radius up to &f%max_radius%&c. This protection uses &f%radius%&c.");
        msgProtectionLimitPrice = lang.getString("messages.protection.limits.price",
                "&cYour rank can only use protection prices between &f%min_price% &cand &f%max_price%&c.");
        msgProtectionHomeSet = lang.getString("messages.protection.home-set",
                "&aHome for &f%alias% &awas updated to your current location.");
        msgProtectionHomeOutside = lang.getString("messages.protection.home-outside",
                "&cYou must be inside your protection to set its home.");
        msgProtectionHomeSaveError = lang.getString("messages.protection.home-save-error",
                "&cCould not save the protection home.");
        msgProtectionFlagUpdated = lang.getString("messages.protection.flags.updated",
                "&aProtection flag &f%flag% &awas updated to &f%state%&a.");
        msgProtectionFlagDenied = lang.getString("messages.protection.flags.denied",
                "&cYou cannot use &f%flag% &cin this protection.");
        protectionItemLore = lang.getStringList("messages.protection.item-lore");
        if (protectionItemLore.isEmpty()) {
            protectionItemLore = List.of(
                    "&7Protection stone",
                    "&7ID: &f%id%",
                    "&7Radius: &f%radius%");
        }
        protectionDefaultActionbarEnter = config.getString("protection.default-actionbar.enter",
                "&aEntering &f%owner%&a's protection.");
        protectionDefaultActionbarExit = config.getString("protection.default-actionbar.exit",
                "&cLeaving &f%owner%&c's protection.");
    }

    public void reloadConfig() {
        configFile.reloadConfig();
        if (langFile != null) {
            langFile.reloadConfig();
        }
        loadConfig();
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public boolean isBStatsEnabled() {
        return bStatsEnabled;
    }

    public void unregisterLimitPermissions() {
        for (String permission : registeredLimitPermissions) {
            if (plugin.getServer().getPluginManager().getPermission(permission) != null) {
                plugin.getServer().getPluginManager().removePermission(permission);
            }
        }
        registeredLimitPermissions.clear();
    }

    private List<MenuItemConfig> loadCreationMenuItems(FileConfiguration config) {
        return loadMenuItems(config, "menus.creation.items");
    }

    private List<MenuItemConfig> defaultProtectionInfoMenuItems() {
        List<MenuItemConfig> items = new ArrayList<>();
        items.add(new MenuItemConfig("name", List.of(10), 0, Material.NAME_TAG,
                "&b&lName",
                List.of("&f%alias% &7(%protection%)"),
                "", false, "", Material.NAME_TAG, Material.NAME_TAG));
        items.add(new MenuItemConfig("size", List.of(11), 0, Material.GRASS_BLOCK,
                "&a&lSize",
                List.of("&f%size%"),
                "", false, "", Material.GRASS_BLOCK, Material.GRASS_BLOCK));
        items.add(new MenuItemConfig("created", List.of(12), 0, Material.CLOCK,
                "&9&lCreated",
                List.of("&f%created_at%"),
                "", false, "", Material.CLOCK, Material.CLOCK));
        items.add(new MenuItemConfig("owners", List.of(13), 0, Material.PLAYER_HEAD,
                "&c&lOwners",
                List.of("&f%owners%"),
                "", false, "", Material.PLAYER_HEAD, Material.PLAYER_HEAD));
        items.add(new MenuItemConfig("members", List.of(14), 0, Material.PLAYER_HEAD,
                "&b&lMembers",
                List.of("&f%members%"),
                "", false, "", Material.PLAYER_HEAD, Material.PLAYER_HEAD));
        items.add(new MenuItemConfig("location", List.of(15), 0, Material.COMPASS,
                "&d&lLocation",
                List.of("&f%stone_x%, %stone_y%, %stone_z%"),
                "", false, "", Material.COMPASS, Material.COMPASS));
        items.add(new MenuItemConfig("home", List.of(16), 0, Material.RED_BED,
                "&a&lHome",
                List.of("&f%home_x%, %home_y%, %home_z%"),
                "", false, "", Material.RED_BED, Material.RED_BED));
        return items;
    }

    private List<MenuItemConfig> defaultProtectionMenuItems() {
        List<MenuItemConfig> items = new ArrayList<>();
        items.add(new MenuItemConfig("settings", List.of(10), 0, Material.REDSTONE,
                "&e&l☀ SETTINGS",
                List.of("&8Protection settings", "",
                        "&fManage your protection settings,",
                        "&flike building permissions, PvP,",
                        "&fchest access and other options.",
                        "",
                        "&e← Click to open settings"),
                "", false, "", Material.REDSTONE, Material.REDSTONE));
        items.add(new MenuItemConfig("information", List.of(14), 0, Material.BOOK,
                "&b&lⓘ INFORMATION",
                List.of("&8Protection details", "",
                        "&e☮ &fName: &e%protection%",
                        "&a⌁ &fSize: &a%size%",
                        "",
                        "&c♛ &fOwners: &c%owners%",
                        "&b♟ &fMembers: &b%members%",
                        "",
                        "&d• &fLocation: &d%x%, %y%, %z%",
                        "&3◺ &fBorders: &3(%min_x%, %min_z%) → (%max_x%, %max_z%)"),
                "", false, "", Material.BOOK, Material.BOOK));
        items.add(new MenuItemConfig("members", List.of(16), 0, Material.PLAYER_HEAD,
                "&a&l♣ MEMBERS",
                List.of("&8Member management", "",
                        "&fManage the members of your protection,",
                        "&fadd users and manage access",
                        "&fand manage individual permissions.",
                        "",
                        "&e← Click to manage members"),
                "", false, "", Material.PLAYER_HEAD, Material.PLAYER_HEAD));
        items.add(new MenuItemConfig("remove", List.of(22), 0, Material.BARRIER,
                "&c&lREMOVE",
                List.of("&8Danger zone", "",
                        "&fOpen a confirmation menu before",
                        "&fdeleting this protection.",
                        "",
                        "&cClick to confirm removal"),
                "", false, "", Material.BARRIER, Material.BARRIER));
        return items;
    }

    private List<MenuItemConfig> defaultProtectionSettingsMenuItems() {
        List<MenuItemConfig> items = new ArrayList<>();
        items.add(new MenuItemConfig("pvp", List.of(10), 0, Material.DIAMOND_SWORD, "&c&lPVP PLAYERS",
                List.of("&8Combat settings", "", "&fAllows PvP combat between players",
                        "&finside your protection. Controls who", "&fcan attack and be attacked.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.DIAMOND_SWORD, Material.DIAMOND_SWORD));
        items.add(new MenuItemConfig("pvp-mobs", List.of(11), 0, Material.SLIME_BLOCK, "&a&lPVP MOBS",
                List.of("&8Creature combat settings", "",
                        "&fAllows combat between players and mobs", "&finside your protection.",
                        "&fPlayers can attack mobs and vice versa.", "", "&fCurrent status: %state%", "",
                        "&e← Click to cycle to the next level"),
                "", false, "", Material.SLIME_BLOCK, Material.SLIME_BLOCK));
        items.add(new MenuItemConfig("entry", List.of(12), 0, Material.IRON_BARS, "&7&lALLOW ENTRY",
                List.of("&8Access settings", "", "&fControls who can enter your protection.",
                        "&fPlayers without permissions will be", "&fblocked at the border.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.IRON_BARS, Material.IRON_BARS));
        items.add(new MenuItemConfig("pressure-plates", List.of(15), 0, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                "&e&lPRESSURE PLATES",
                List.of("&8Activation settings", "", "&fControls who can activate pressure plates",
                        "&fby stepping on them and trigger", "&fautomated systems.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE));
        items.add(new MenuItemConfig("doors", List.of(16), 0, Material.OAK_DOOR, "&6&lUSE DOORS",
                List.of("&8Access settings", "", "&fControls who can open and close",
                        "&fdoors, trapdoors and gates to", "&faccess different areas.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.OAK_DOOR, Material.OAK_DOOR));
        items.add(new MenuItemConfig("buttons", List.of(17), 0, Material.STONE_BUTTON, "&c&lUSE BUTTONS",
                List.of("&8Redstone settings", "", "&fControls who can press buttons",
                        "&fand redstone levers to", "&factivate mechanisms.", "", "&fCurrent status: %state%",
                        "", "&e← Click to cycle to the next level"),
                "", false, "", Material.STONE_BUTTON, Material.STONE_BUTTON));
        items.add(new MenuItemConfig("place-blocks", List.of(19), 0, Material.GRASS_BLOCK, "&a&lPLACE BLOCKS",
                List.of("&8Building settings", "", "&fControls who can place blocks",
                        "&finside your protected area. Includes", "&fall block types.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.GRASS_BLOCK, Material.GRASS_BLOCK));
        items.add(new MenuItemConfig("break-blocks", List.of(20), 0, Material.DIAMOND_PICKAXE, "&b&lBREAK BLOCKS",
                List.of("&8Destruction settings", "", "&fControls who can break blocks",
                        "&finside your protected area. Includes", "&fmining and demolition.", "",
                        "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.DIAMOND_PICKAXE, Material.DIAMOND_PICKAXE));
        items.add(new MenuItemConfig("storage", List.of(21), 0, Material.CHEST, "&d&lOPEN STORAGE",
                List.of("&8Storage settings", "", "&fControls who can open chests",
                        "&fand other storage blocks", "&fsuch as barrels, shulkers, furnaces, hoppers, etc.",
                        "", "&fCurrent status: %state%", "", "&e← Click to cycle to the next level"),
                "", false, "", Material.CHEST, Material.CHEST));
        items.add(new MenuItemConfig("workstations", List.of(23), 0, Material.CRAFTING_TABLE, "&6&lWORKSTATIONS",
                List.of("&8Crafting utility settings", "",
                        "&fControls who can use crafting tables,", "&fenchanting tables and anvils.",
                        "&fThese are utility blocks that do not store items.", "", "&fCurrent status: %state%", "",
                        "&e← Click to cycle to the next level"),
                "", false, "", Material.CRAFTING_TABLE, Material.CRAFTING_TABLE));
        items.add(new MenuItemConfig("tnt", List.of(24), 0, Material.TNT, "&c&lTNT EXPLOSIONS",
                List.of("&8Explosive settings", "", "&fControls whether TNT can explode",
                        "&fand deal damage inside your protection.", "&fIncludes creepers and other explosions.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.TNT, Material.TNT));
        items.add(new MenuItemConfig("leaf-decay", List.of(25), 0, Material.OAK_LEAVES, "&a&lLEAF DECAY",
                List.of("&8Natural settings", "", "&fControls whether tree leaves",
                        "&fcan decay naturally", "&fwhen no logs are nearby.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.OAK_LEAVES, Material.OAK_LEAVES));
        items.add(new MenuItemConfig("keep-inventory", List.of(28), 1, Material.TOTEM_OF_UNDYING, "&6&lKEEP INVENTORY",
                List.of("&8Death settings", "", "&fWhen enabled, players keep their",
                        "&fitems upon death inside", "&fthe protection.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.TOTEM_OF_UNDYING, Material.TOTEM_OF_UNDYING));
        items.add(new MenuItemConfig("keep-exp", List.of(29), 1, Material.EXPERIENCE_BOTTLE, "&b&lKEEP EXP",
                List.of("&8Death settings", "", "&fWhen enabled, players keep their",
                        "&fexperience upon death inside", "&fthe protection.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.EXPERIENCE_BOTTLE, Material.EXPERIENCE_BOTTLE));
        items.add(new MenuItemConfig("fall-damage", List.of(30), 1, Material.FEATHER, "&f&lFALL DAMAGE",
                List.of("&8Damage settings", "", "&fControls whether players take",
                        "&ffall damage inside your", "&fprotection.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.FEATHER, Material.FEATHER));
        items.add(new MenuItemConfig("potion-splash", List.of(32), 1, Material.SPLASH_POTION, "&5&lPOTION SPLASH",
                List.of("&8Potion settings", "", "&fControls whether splash potions",
                        "&fcan affect players inside", "&fyour protection.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.SPLASH_POTION, Material.SPLASH_POTION));
        items.add(new MenuItemConfig("hunger-drain", List.of(33), 1, Material.GOLDEN_APPLE, "&e&lHUNGER DRAIN",
                List.of("&8Survival settings", "", "&fControls whether players lose",
                        "&fhunger inside your protection.", "&fWhen disabled, hunger is frozen.", "",
                        "&fCurrent status: %state%", "", "&e← Click to toggle the state"),
                "", false, "", Material.GOLDEN_APPLE, Material.GOLDEN_APPLE));
        items.add(new MenuItemConfig("back", List.of(40), -1, Material.BONE, "&c&lBACK TO MENU",
                List.of("&8Navigation", "", "&fReturns to the main protection menu",
                        "&fto access other options", "&fand settings.", "",
                        "&e← Click to go back"),
                "", false, "", Material.BONE, Material.BONE));
        return items;
    }

    private List<MenuItemConfig> defaultProtectionRemoveConfirmMenuItems() {
        List<MenuItemConfig> items = new ArrayList<>();
        items.add(new MenuItemConfig("confirm", List.of(11), 0, Material.LIME_CONCRETE, "&a&lCONFIRM REMOVE",
                List.of("&8Danger zone", "", "&fDeletes this WorldGuard region", "&fand returns the protection stone.",
                        "", "&aClick to confirm"),
                "", false, "", Material.LIME_CONCRETE, Material.LIME_CONCRETE));
        items.add(new MenuItemConfig("cancel", List.of(15), 0, Material.RED_CONCRETE, "&c&lCANCEL",
                List.of("&8Safety", "", "&fKeep the protection active.", "", "&cClick to cancel"),
                "", false, "", Material.RED_CONCRETE, Material.RED_CONCRETE));
        return items;
    }

    private ProtectionLimitProfile loadLimitProfile(FileConfiguration config, String path, String id,
            String permission, int priority) {
        return new ProtectionLimitProfile(
                id,
                config.getString(path + ".permission", permission),
                config.getInt(path + ".priority", priority),
                config.getInt(path + ".max-protections", 3),
                config.getInt(path + ".max-radius", ProtectionValidation.MAX_RADIUS),
                config.getDouble(path + ".min-price", ProtectionValidation.MIN_PRICE),
                config.getDouble(path + ".max-price", ProtectionValidation.MAX_PRICE));
    }

    private List<ProtectionLimitProfile> loadLimitGroups(FileConfiguration config) {
        List<ProtectionLimitProfile> groups = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("limits.groups");
        if (section == null) {
            return groups;
        }

        int fallbackPriority = 1;
        for (String key : section.getKeys(false)) {
            String path = "limits.groups." + key;
            groups.add(loadLimitProfile(config, path, key,
                    "argosprotect.limits." + key.toLowerCase(), fallbackPriority++));
        }
        return groups;
    }

    private void registerLimitPermissions() {
        unregisterLimitPermissions();
        for (ProtectionLimitProfile profile : protectionLimitGroups) {
            String permissionName = profile.permission().trim();
            if (permissionName.isBlank()
                    || plugin.getServer().getPluginManager().getPermission(permissionName) != null) {
                continue;
            }

            Permission permission = new Permission(permissionName,
                    "Applies the configurable " + profile.id() + " protection limits.",
                    PermissionDefault.FALSE);
            try {
                plugin.getServer().getPluginManager().addPermission(permission);
                registeredLimitPermissions.add(permissionName);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Could not register limit permission \"" + permissionName + "\": "
                        + exception.getMessage());
            }
        }
    }

    private List<MenuItemConfig> loadMenuItems(FileConfiguration config, String sectionPath) {
        List<MenuItemConfig> items = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        if (section == null) {
            return items;
        }

        for (String key : section.getKeys(false)) {
            String path = sectionPath + "." + key;
            List<Integer> slots = loadSlots(config, path);
            Material material = parseMaterial(config.getString(path + ".material", "BARRIER"), Material.BARRIER);

            if (slots.isEmpty()) {
                continue;
            }

            String name = config.getString(path + ".name", "&cUndefined");
            List<String> lore = config.getStringList(path + ".lore");
            int page = config.getInt(path + ".page", 0);
            String headTexture = config.getString(path + ".texture", config.getString(path + ".head-texture", ""));
            boolean decorative = config.getBoolean(path + ".decorative", false);
            String statusFor = config.getString(path + ".status-for", "");
            Material emptyMaterial = parseMaterial(config.getString(path + ".empty-material", material.name()),
                    material);
            Material filledMaterial = parseMaterial(config.getString(path + ".filled-material", material.name()),
                    material);
            items.add(new MenuItemConfig(key, slots, page, material, name, lore, headTexture, decorative, statusFor,
                    emptyMaterial, filledMaterial));
        }
        return items;
    }

    private List<Integer> loadSlots(FileConfiguration config, String path) {
        Set<Integer> slots = new LinkedHashSet<>();
        String slotsPath = path + ".slots";

        if (config.isList(slotsPath)) {
            for (Object rawSlot : config.getList(slotsPath, List.of())) {
                addSlot(slots, rawSlot);
            }
        } else if (config.contains(slotsPath)) {
            for (String rawSlot : config.getString(slotsPath, "").split(",")) {
                addSlot(slots, rawSlot.trim());
            }
        } else {
            addSlot(slots, config.getInt(path + ".slot", -1));
        }

        return new ArrayList<>(slots);
    }

    private List<Integer> loadIntegerList(FileConfiguration config, String path, List<Integer> defaultValues) {
        Set<Integer> values = new LinkedHashSet<>();
        if (config.isList(path)) {
            for (Object rawValue : config.getList(path, List.of())) {
                addSlot(values, rawValue);
            }
        } else if (config.contains(path)) {
            for (String rawValue : config.getString(path, "").split(",")) {
                addSlot(values, rawValue.trim());
            }
        }
        return values.isEmpty() ? defaultValues : new ArrayList<>(values);
    }

    private List<Integer> normalizeMenuSlots(List<Integer> slots, int inventorySize, List<Integer> defaultSlots) {
        List<Integer> normalized = slots.stream()
                .filter(slot -> slot >= 0 && slot < inventorySize)
                .toList();
        return normalized.isEmpty() ? defaultSlots : normalized;
    }

    private List<Integer> defaultContentSlots(int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        int contentLimit = Math.max(0, inventorySize - 9);
        for (int slot = 0; slot < contentLimit; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> defaultAdminPlacedContentSlots(int inventorySize) {
        List<Integer> preferred = List.of(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34);
        return preferred.stream()
                .filter(slot -> slot < inventorySize)
                .toList();
    }

    private List<MenuItemConfig> defaultAdminPlacedMenuItems() {
        List<MenuItemConfig> items = new ArrayList<>();
        items.add(new MenuItemConfig("information", List.of(4), 0, Material.COMPASS,
                "&6&lPlaced Protections",
                List.of("&7Tracked regions: &f%total%",
                        "&7Click a protection to print debug details."),
                "", true, "", Material.COMPASS, Material.COMPASS));
        return items;
    }

    private MenuDisplayConfig loadMenuDisplay(FileConfiguration config, String path, Material defaultMaterial,
            String defaultName, List<String> defaultLore) {
        Material material = parseMaterial(config.getString(path + ".material", defaultMaterial.name()),
                defaultMaterial);
        String name = config.getString(path + ".name", defaultName);
        List<String> lore = config.getStringList(path + ".lore");
        if (lore.isEmpty() && !config.contains(path + ".lore")) {
            lore = defaultLore;
        }
        String headTexture = config.getString(path + ".texture", config.getString(path + ".head-texture", ""));
        return new MenuDisplayConfig(material, name, lore, headTexture);
    }

    private MenuNavigationConfig loadNavigationItem(FileConfiguration config, String path, int defaultSlot,
            Material defaultMaterial, String defaultName, List<String> defaultLore) {
        int slot = config.getInt(path + ".slot", defaultSlot);
        MenuDisplayConfig display = loadMenuDisplay(config, path, defaultMaterial, defaultName, defaultLore);
        return new MenuNavigationConfig(slot, display.material(), display.name(), display.lore(),
                display.headTexture());
    }

    private void addSlot(Set<Integer> slots, Object rawSlot) {
        if (rawSlot == null) {
            return;
        }

        try {
            int slot = rawSlot instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(rawSlot.toString());
            if (slot >= 0) {
                slots.add(slot);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private Material parseMaterial(String rawMaterial, Material defaultMaterial) {
        Material parsed = Material.matchMaterial(rawMaterial);
        return parsed != null ? parsed : defaultMaterial;
    }

    private int normalizeInventorySize(int rawSize) {
        int clamped = Math.max(9, Math.min(54, rawSize));
        return clamped - (clamped % 9);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMsgUpdateAvailable() {
        return msgUpdateAvailable;
    }

    public String getMsgUpdateCurrent() {
        return msgUpdateCurrent;
    }

    public String getMsgUpdateDownload() {
        return msgUpdateDownload;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgPluginReload() {
        return msgPluginReload;
    }

    public String getMsgUsageCommand() {
        return msgUsageCommand;
    }

    public String getMsgPlayerOnly() {
        return msgPlayerOnly;
    }

    public String getCreationMenuTitle() {
        return creationMenuTitle;
    }

    public int getCreationMenuSize() {
        return creationMenuSize;
    }

    public Material getCreationMenuFillerMaterial() {
        return creationMenuFillerMaterial;
    }

    public String getCreationMenuFillerName() {
        return creationMenuFillerName;
    }

    public List<MenuItemConfig> getCreationMenuItems() {
        return Collections.unmodifiableList(creationMenuItems);
    }

    public String getCreationCurrentValueLine() {
        return creationCurrentValueLine;
    }

    public String getLanguageMenuTitle() {
        return languageMenuTitle;
    }

    public int getLanguageMenuSize() {
        return languageMenuSize;
    }

    public Material getLanguageMenuFillerMaterial() {
        return languageMenuFillerMaterial;
    }

    public String getLanguageMenuFillerName() {
        return languageMenuFillerName;
    }

    public List<MenuItemConfig> getLanguageMenuItems() {
        return Collections.unmodifiableList(languageMenuItems);
    }

    public String getProtectionMenuTitle() {
        return protectionMenuTitle;
    }

    public int getProtectionMenuSize() {
        return protectionMenuSize;
    }

    public Material getProtectionMenuFillerMaterial() {
        return protectionMenuFillerMaterial;
    }

    public String getProtectionMenuFillerName() {
        return protectionMenuFillerName;
    }

    public List<MenuItemConfig> getProtectionMenuItems() {
        return Collections.unmodifiableList(protectionMenuItems);
    }

    public String getProtectionInfoMenuTitle() {
        return protectionInfoMenuTitle;
    }

    public int getProtectionInfoMenuSize() {
        return protectionInfoMenuSize;
    }

    public Material getProtectionInfoMenuFillerMaterial() {
        return protectionInfoMenuFillerMaterial;
    }

    public String getProtectionInfoMenuFillerName() {
        return protectionInfoMenuFillerName;
    }

    public List<MenuItemConfig> getProtectionInfoMenuItems() {
        return Collections.unmodifiableList(protectionInfoMenuItems);
    }

    public String getProtectionSettingsMenuTitle() {
        return protectionSettingsMenuTitle;
    }

    public int getProtectionSettingsMenuSize() {
        return protectionSettingsMenuSize;
    }

    public Material getProtectionSettingsMenuFillerMaterial() {
        return protectionSettingsMenuFillerMaterial;
    }

    public String getProtectionSettingsMenuFillerName() {
        return protectionSettingsMenuFillerName;
    }

    public MenuNavigationConfig getProtectionSettingsMenuPreviousItem() {
        return protectionSettingsMenuPreviousItem;
    }

    public MenuNavigationConfig getProtectionSettingsMenuNextItem() {
        return protectionSettingsMenuNextItem;
    }

    public List<MenuItemConfig> getProtectionSettingsMenuItems() {
        return Collections.unmodifiableList(protectionSettingsMenuItems);
    }

    public String getProtectionFlagLabel(ProtectionFlagLevel level, boolean toggle) {
        if (toggle) {
            return level == ProtectionFlagLevel.NOBODY
                    ? protectionSettingsToggleDisabled
                    : protectionSettingsToggleEnabled;
        }
        return switch (level) {
            case NOBODY -> protectionSettingsAccessNobody;
            case MEMBERS -> protectionSettingsAccessMembers;
            case EVERYONE -> protectionSettingsAccessEveryone;
        };
    }

    public String getProtectionHomeMenuTitle() {
        return protectionHomeMenuTitle;
    }

    public int getProtectionHomeMenuSize() {
        return protectionHomeMenuSize;
    }

    public Material getProtectionHomeMenuFillerMaterial() {
        return protectionHomeMenuFillerMaterial;
    }

    public String getProtectionHomeMenuFillerName() {
        return protectionHomeMenuFillerName;
    }

    public List<Integer> getProtectionHomeMenuContentSlots() {
        return Collections.unmodifiableList(protectionHomeMenuContentSlots);
    }

    public List<MenuItemConfig> getProtectionHomeMenuItems() {
        return Collections.unmodifiableList(protectionHomeMenuItems);
    }

    public MenuDisplayConfig getProtectionHomeMenuItem() {
        return protectionHomeMenuItem;
    }

    public MenuNavigationConfig getProtectionHomeMenuPreviousItem() {
        return protectionHomeMenuPreviousItem;
    }

    public MenuNavigationConfig getProtectionHomeMenuNextItem() {
        return protectionHomeMenuNextItem;
    }

    public String getProtectionHomeRoleOwner() {
        return protectionHomeRoleOwner;
    }

    public String getProtectionHomeRoleAdmin() {
        return protectionHomeRoleAdmin;
    }

    public String getProtectionHomeRoleMember() {
        return protectionHomeRoleMember;
    }

    public String getProtectionMembersMenuTitle() {
        return protectionMembersMenuTitle;
    }

    public int getProtectionMembersMenuSize() {
        return protectionMembersMenuSize;
    }

    public Material getProtectionMembersMenuFillerMaterial() {
        return protectionMembersMenuFillerMaterial;
    }

    public String getProtectionMembersMenuFillerName() {
        return protectionMembersMenuFillerName;
    }

    public List<Integer> getProtectionMembersMenuContentSlots() {
        return Collections.unmodifiableList(protectionMembersMenuContentSlots);
    }

    public List<MenuItemConfig> getProtectionMembersMenuItems() {
        return Collections.unmodifiableList(protectionMembersMenuItems);
    }

    public MenuDisplayConfig getProtectionMembersMenuItem() {
        return protectionMembersMenuItem;
    }

    public MenuNavigationConfig getProtectionMembersMenuPreviousItem() {
        return protectionMembersMenuPreviousItem;
    }

    public MenuNavigationConfig getProtectionMembersMenuNextItem() {
        return protectionMembersMenuNextItem;
    }

    public String getProtectionMembersRoleOwner() {
        return protectionMembersRoleOwner;
    }

    public String getProtectionMembersRoleAdmin() {
        return protectionMembersRoleAdmin;
    }

    public String getProtectionMembersRoleMember() {
        return protectionMembersRoleMember;
    }

    public String getProtectionMembersUnknownDate() {
        return protectionMembersUnknownDate;
    }

    public String getProtectionLogsMenuTitle() {
        return protectionLogsMenuTitle;
    }

    public int getProtectionLogsMenuSize() {
        return protectionLogsMenuSize;
    }

    public Material getProtectionLogsMenuFillerMaterial() {
        return protectionLogsMenuFillerMaterial;
    }

    public String getProtectionLogsMenuFillerName() {
        return protectionLogsMenuFillerName;
    }

    public List<Integer> getProtectionLogsMenuContentSlots() {
        return Collections.unmodifiableList(protectionLogsMenuContentSlots);
    }

    public MenuDisplayConfig getProtectionLogsMenuItem() {
        return protectionLogsMenuItem;
    }

    public String getProtectionLogsMenuTexture(String type) {
        return protectionLogsMenuTextures.getOrDefault(type, protectionLogsMenuDefaultTexture);
    }

    public List<MenuItemConfig> getProtectionLogsMenuItems() {
        return Collections.unmodifiableList(protectionLogsMenuItems);
    }

    public MenuNavigationConfig getProtectionLogsMenuPreviousItem() {
        return protectionLogsMenuPreviousItem;
    }

    public MenuNavigationConfig getProtectionLogsMenuNextItem() {
        return protectionLogsMenuNextItem;
    }

    public String getAdminPlacedMenuTitle() {
        return adminPlacedMenuTitle;
    }

    public int getAdminPlacedMenuSize() {
        return adminPlacedMenuSize;
    }

    public Material getAdminPlacedMenuFillerMaterial() {
        return adminPlacedMenuFillerMaterial;
    }

    public String getAdminPlacedMenuFillerName() {
        return adminPlacedMenuFillerName;
    }

    public List<Integer> getAdminPlacedMenuContentSlots() {
        return Collections.unmodifiableList(adminPlacedMenuContentSlots);
    }

    public List<MenuItemConfig> getAdminPlacedMenuItems() {
        return Collections.unmodifiableList(adminPlacedMenuItems);
    }

    public MenuDisplayConfig getAdminPlacedMenuItem() {
        return adminPlacedMenuItem;
    }

    public MenuNavigationConfig getAdminPlacedMenuPreviousItem() {
        return adminPlacedMenuPreviousItem;
    }

    public MenuNavigationConfig getAdminPlacedMenuNextItem() {
        return adminPlacedMenuNextItem;
    }

    public String getAdminPlacedStatusOk() {
        return adminPlacedStatusOk;
    }

    public String getAdminPlacedStatusMissing() {
        return adminPlacedStatusMissing;
    }

    public boolean isAdminPlacedUseProtectionBlock() {
        return adminPlacedUseProtectionBlock;
    }

    public Material getAdminPlacedHealthyMaterial() {
        return adminPlacedHealthyMaterial;
    }

    public Material getAdminPlacedBrokenMaterial() {
        return adminPlacedBrokenMaterial;
    }

    public String getProtectionRemoveConfirmMenuTitle() {
        return protectionRemoveConfirmMenuTitle;
    }

    public int getProtectionRemoveConfirmMenuSize() {
        return protectionRemoveConfirmMenuSize;
    }

    public Material getProtectionRemoveConfirmMenuFillerMaterial() {
        return protectionRemoveConfirmMenuFillerMaterial;
    }

    public String getProtectionRemoveConfirmMenuFillerName() {
        return protectionRemoveConfirmMenuFillerName;
    }

    public List<MenuItemConfig> getProtectionRemoveConfirmMenuItems() {
        return Collections.unmodifiableList(protectionRemoveConfirmMenuItems);
    }

    public int getProtectionHomeCooldownSeconds() {
        return protectionHomeCooldownSeconds;
    }

    public ProtectionFeedbackConfig getProtectionFeedbackConfig() {
        return protectionFeedbackConfig;
    }

    public ProtectionRentSettings getProtectionRentSettings() {
        return protectionRentSettings;
    }

    public ProtectionLimitProfile getDefaultProtectionLimitProfile() {
        return defaultProtectionLimitProfile;
    }

    public List<ProtectionLimitProfile> getProtectionLimitGroups() {
        return Collections.unmodifiableList(protectionLimitGroups);
    }

    public String getMsgLanguageChanged() {
        return msgLanguageChanged;
    }

    public String getMsgLanguageFileMissing() {
        return msgLanguageFileMissing;
    }

    public String getLanguageDisplayName(String languageCode) {
        return switch (languageCode.toLowerCase()) {
            case "es" -> msgLanguageNameSpanish;
            case "en" -> msgLanguageNameEnglish;
            default -> languageCode;
        };
    }

    public String getMsgAdminDebugUsage() {
        return msgAdminDebugUsage;
    }

    public String getMsgAdminDebugNotFound() {
        return msgAdminDebugNotFound;
    }

    public String getMsgAdminListPlacedUsage() {
        return msgAdminListPlacedUsage;
    }

    public String getMsgAdminReportUsage() {
        return msgAdminReportUsage;
    }

    public String getMsgAdminLogsUsage() {
        return msgAdminLogsUsage;
    }

    public String getMsgAdminLogsEmpty() {
        return msgAdminLogsEmpty;
    }

    public String getMsgAdminLogsHeader() {
        return msgAdminLogsHeader;
    }

    public String getMsgAdminLogsEntry() {
        return msgAdminLogsEntry;
    }

    public List<String> getAdminReportProtectionLines() {
        return adminReportProtectionLines;
    }

    public String getAdminReportProtectionEmptyEntry() {
        return adminReportProtectionEmptyEntry;
    }

    public String getAdminReportProtectionLargestEntry() {
        return adminReportProtectionLargestEntry;
    }

    public String getAdminReportProtectionTopOwnerEntry() {
        return adminReportProtectionTopOwnerEntry;
    }

    public String getMsgAdminRepairUsage() {
        return msgAdminRepairUsage;
    }

    public String getMsgAdminRepairSummary() {
        return msgAdminRepairSummary;
    }

    public String getMsgAdminRepairSaveError() {
        return msgAdminRepairSaveError;
    }

    public String getMsgAdminRepairSkippedWorldUnavailable() {
        return msgAdminRepairSkippedWorldUnavailable;
    }

    public String getMsgAdminRepairCleanedStaleTracker() {
        return msgAdminRepairCleanedStaleTracker;
    }

    public String getMsgAdminRepairOrphanTracker() {
        return msgAdminRepairOrphanTracker;
    }

    public String getMsgAdminRepairSkippedWorldGuardRegion() {
        return msgAdminRepairSkippedWorldGuardRegion;
    }

    public String getMsgAdminRepairRepairedWorldGuardRegion() {
        return msgAdminRepairRepairedWorldGuardRegion;
    }

    public String getMsgAdminRepairSaveRollback() {
        return msgAdminRepairSaveRollback;
    }

    public String getMsgAdminRepairReasonRegionIdMismatch() {
        return msgAdminRepairReasonRegionIdMismatch;
    }

    public String getMsgAdminRepairReasonProtectionYamlInvalid() {
        return msgAdminRepairReasonProtectionYamlInvalid;
    }

    public String getMsgAdminRepairReasonNoOwner() {
        return msgAdminRepairReasonNoOwner;
    }

    public String getMsgAdminRepairReasonStoneNotFound() {
        return msgAdminRepairReasonStoneNotFound;
    }

    public String getMsgAdminMigrateUsage() {
        return msgAdminMigrateUsage;
    }

    public String getMsgAdminMigrateSummary() {
        return msgAdminMigrateSummary;
    }

    public String getMsgAdminMigrateEntry() {
        return msgAdminMigrateEntry;
    }

    public String getMsgAdminMigrateBackupWarning() {
        return msgAdminMigrateBackupWarning;
    }

    public String getAdminDebugHeader() {
        return adminDebugHeader;
    }

    public String getAdminDebugTrackingMissing() {
        return adminDebugTrackingMissing;
    }

    public String getAdminDebugTrackingPresent() {
        return adminDebugTrackingPresent;
    }

    public String getAdminDebugProtectionYaml() {
        return adminDebugProtectionYaml;
    }

    public String getAdminDebugOwner() {
        return adminDebugOwner;
    }

    public String getAdminDebugMembers() {
        return adminDebugMembers;
    }

    public String getAdminDebugMemberEntry() {
        return adminDebugMemberEntry;
    }

    public String getAdminDebugWorld() {
        return adminDebugWorld;
    }

    public String getAdminDebugStone() {
        return adminDebugStone;
    }

    public String getAdminDebugBounds() {
        return adminDebugBounds;
    }

    public String getAdminDebugCreated() {
        return adminDebugCreated;
    }

    public String getAdminDebugWorldGuardMissing() {
        return adminDebugWorldGuardMissing;
    }

    public String getAdminDebugWorldGuardPresent() {
        return adminDebugWorldGuardPresent;
    }

    public String getAdminDebugWorldGuardOwners() {
        return adminDebugWorldGuardOwners;
    }

    public String getAdminDebugWorldGuardOwnerEntry() {
        return adminDebugWorldGuardOwnerEntry;
    }

    public String getAdminDebugWorldGuardMembers() {
        return adminDebugWorldGuardMembers;
    }

    public String getAdminDebugWorldGuardBounds() {
        return adminDebugWorldGuardBounds;
    }

    public String getAdminDebugStatusOk() {
        return adminDebugStatusOk;
    }

    public String getAdminDebugStatusMissing() {
        return adminDebugStatusMissing;
    }

    public String getAdminDebugUnknownDate() {
        return adminDebugUnknownDate;
    }

    public String getCreationPrompt(String fieldId) {
        return switch (fieldId) {
            case "protection-id" -> creationPromptProtectionId;
            case "display-name" -> creationPromptDisplayName;
            case "block-type" -> creationPromptBlockType;
            case "radius" -> creationPromptRadius;
            case "price" -> creationPromptPrice;
            default -> creationPromptDefault;
        };
    }

    public String getCreationUnsetValue() {
        return creationUnsetValue;
    }

    public String getMsgCreationInputCancelled() {
        return msgCreationInputCancelled;
    }

    public String getMsgCreationInputSaved() {
        return msgCreationInputSaved;
    }

    public String getMsgCreationIncomplete() {
        return msgCreationIncomplete;
    }

    public String getMsgCreationAlreadyExists() {
        return msgCreationAlreadyExists;
    }

    public String getMsgCreationCreated() {
        return msgCreationCreated;
    }

    public String getMsgCreationSaveError() {
        return msgCreationSaveError;
    }

    public String getMsgCreationInvalidId() {
        return msgCreationInvalidId;
    }

    public String getMsgCreationInvalidName() {
        return msgCreationInvalidName;
    }

    public String getMsgCreationInvalidMaterial() {
        return msgCreationInvalidMaterial;
    }

    public String getMsgCreationInvalidRadius() {
        return msgCreationInvalidRadius;
    }

    public String getMsgCreationInvalidPrice() {
        return msgCreationInvalidPrice;
    }

    public String getMsgCreationUnknownField() {
        return msgCreationUnknownField;
    }

    public String getMsgProtectionUsage() {
        return msgProtectionUsage;
    }

    public String getMsgProtectionGiveUsage() {
        return msgProtectionGiveUsage;
    }

    public String getMsgProtectionBuyUsage() {
        return msgProtectionBuyUsage;
    }

    public String getMsgProtectionPlayerOffline() {
        return msgProtectionPlayerOffline;
    }

    public String getMsgProtectionNotFound() {
        return msgProtectionNotFound;
    }

    public String getMsgProtectionInvalidAmount() {
        return msgProtectionInvalidAmount;
    }

    public String getMsgProtectionInventoryFull() {
        return msgProtectionInventoryFull;
    }

    public String getMsgProtectionGiveSender() {
        return msgProtectionGiveSender;
    }

    public String getMsgProtectionGiveTarget() {
        return msgProtectionGiveTarget;
    }

    public String getMsgProtectionNoInventorySpace() {
        return msgProtectionNoInventorySpace;
    }

    public String getMsgProtectionEconomyUnavailable() {
        return msgProtectionEconomyUnavailable;
    }

    public String getMsgProtectionNotEnoughMoney() {
        return msgProtectionNotEnoughMoney;
    }

    public String getMsgProtectionBuyError() {
        return msgProtectionBuyError;
    }

    public String getMsgProtectionBoughtWithPrice() {
        return msgProtectionBoughtWithPrice;
    }

    public String getMsgProtectionBoughtFree() {
        return msgProtectionBoughtFree;
    }

    public String getMsgProtectionRentUsage() {
        return msgProtectionRentUsage;
    }

    public String getMsgProtectionRentDisabled() {
        return msgProtectionRentDisabled;
    }

    public String getMsgProtectionRentAlreadyPaid() {
        return msgProtectionRentAlreadyPaid;
    }

    public String getMsgProtectionRentPaid() {
        return msgProtectionRentPaid;
    }

    public String getMsgProtectionRentSuspended() {
        return msgProtectionRentSuspended;
    }

    public String getMsgProtectionRentReactivated() {
        return msgProtectionRentReactivated;
    }

    public String getMsgProtectionRentPaymentFailed() {
        return msgProtectionRentPaymentFailed;
    }

    public String getMsgProtectionFlyUsage() {
        return msgProtectionFlyUsage;
    }

    public String getMsgProtectionFlyEnabled() {
        return msgProtectionFlyEnabled;
    }

    public String getMsgProtectionFlyDisabled() {
        return msgProtectionFlyDisabled;
    }

    public String getMsgProtectionFlyLeftRegion() {
        return msgProtectionFlyLeftRegion;
    }

    public String getMsgProtectionFlyNotAccessible() {
        return msgProtectionFlyNotAccessible;
    }

    public String getMsgProtectionPlaced() {
        return msgProtectionPlaced;
    }

    public String getMsgProtectionRegionUnavailable() {
        return msgProtectionRegionUnavailable;
    }

    public String getMsgProtectionRegionExists() {
        return msgProtectionRegionExists;
    }

    public String getMsgProtectionRegionOverlap() {
        return msgProtectionRegionOverlap;
    }

    public String getMsgProtectionRegionSaveError() {
        return msgProtectionRegionSaveError;
    }

    public String getMsgProtectionRemoved() {
        return msgProtectionRemoved;
    }

    public String getMsgProtectionRemoveNotOwner() {
        return msgProtectionRemoveNotOwner;
    }

    public String getMsgProtectionRemoveSaveError() {
        return msgProtectionRemoveSaveError;
    }

    public String getMsgProtectionRemoveConfirmOpen() {
        return msgProtectionRemoveConfirmOpen;
    }

    public String getMsgProtectionRemoveCancelled() {
        return msgProtectionRemoveCancelled;
    }

    public String getMsgProtectionMenuNotInOwnProtection() {
        return msgProtectionMenuNotInOwnProtection;
    }

    public String getMsgProtectionTeleportUsage() {
        return msgProtectionTeleportUsage;
    }

    public String getMsgProtectionAliasUsage() {
        return msgProtectionAliasUsage;
    }

    public String getMsgProtectionMemberUsage() {
        return msgProtectionMemberUsage;
    }

    public String getMsgProtectionInviteUsage() {
        return msgProtectionInviteUsage;
    }

    public String getMsgProtectionTransferUsage() {
        return msgProtectionTransferUsage;
    }

    public String getMsgProtectionLogsUsage() {
        return msgProtectionLogsUsage;
    }

    public String getMsgProtectionNotInProtection() {
        return msgProtectionNotInProtection;
    }

    public String getMsgProtectionNotAccessible() {
        return msgProtectionNotAccessible;
    }

    public String getMsgProtectionWorldUnavailable() {
        return msgProtectionWorldUnavailable;
    }

    public String getMsgProtectionTeleportSuccess() {
        return msgProtectionTeleportSuccess;
    }

    public String getMsgProtectionAliasInvalid() {
        return msgProtectionAliasInvalid;
    }

    public String getMsgProtectionAliasTaken() {
        return msgProtectionAliasTaken;
    }

    public String getMsgProtectionAliasUpdated() {
        return msgProtectionAliasUpdated;
    }

    public String getMsgProtectionTransferSuccess() {
        return msgProtectionTransferSuccess;
    }

    public String getMsgProtectionTransferReceived() {
        return msgProtectionTransferReceived;
    }

    public String getMsgProtectionTransferTargetOwner() {
        return msgProtectionTransferTargetOwner;
    }

    public String getMsgProtectionLogsEmpty() {
        return msgProtectionLogsEmpty;
    }

    public String getMsgProtectionLogsHeader() {
        return msgProtectionLogsHeader;
    }

    public String getMsgProtectionLogsEntry() {
        return msgProtectionLogsEntry;
    }

    public String getMsgProtectionLeaveOwnDenied() {
        return msgProtectionLeaveOwnDenied;
    }

    public String getMsgProtectionLeaveSuccess() {
        return msgProtectionLeaveSuccess;
    }

    public String getMsgProtectionViewShown() {
        return msgProtectionViewShown;
    }

    public String getMsgProtectionViewCooldown() {
        return msgProtectionViewCooldown;
    }

    public String getMsgProtectionListEmpty() {
        return msgProtectionListEmpty;
    }

    public String getMsgProtectionInvalidConfig() {
        return msgProtectionInvalidConfig;
    }

    public String getMsgProtectionInfoUnknownDate() {
        return msgProtectionInfoUnknownDate;
    }



    public String getProtectionListHeader() {
        return protectionListHeader;
    }

    public String getProtectionListEntry() {
        return protectionListEntry;
    }

    public String getProtectionListInfoLabel() {
        return protectionListInfoLabel;
    }

    public String getProtectionListInfoHover() {
        return protectionListInfoHover;
    }

    public String getMsgProtectionHomeEmpty() {
        return msgProtectionHomeEmpty;
    }

    public String getMsgProtectionHomeCooldown() {
        return msgProtectionHomeCooldown;
    }

    public String getMsgProtectionMemberAdded() {
        return msgProtectionMemberAdded;
    }

    public String getMsgProtectionMemberRemoved() {
        return msgProtectionMemberRemoved;
    }

    public String getMsgProtectionMemberPromoted() {
        return msgProtectionMemberPromoted;
    }

    public String getMsgProtectionMemberDemoted() {
        return msgProtectionMemberDemoted;
    }

    public String getMsgProtectionMemberListEmpty() {
        return msgProtectionMemberListEmpty;
    }

    public String getMsgProtectionMemberAlready() {
        return msgProtectionMemberAlready;
    }

    public String getMsgProtectionMemberNotMember() {
        return msgProtectionMemberNotMember;
    }

    public String getMsgProtectionMemberTargetOwner() {
        return msgProtectionMemberTargetOwner;
    }

    public String getMsgProtectionMemberSaveError() {
        return msgProtectionMemberSaveError;
    }

    public String getMsgProtectionInviteSent() {
        return msgProtectionInviteSent;
    }

    public String getMsgProtectionInviteExists() {
        return msgProtectionInviteExists;
    }

    public String getMsgProtectionInviteInvalid() {
        return msgProtectionInviteInvalid;
    }

    public String getMsgProtectionInviteExpired() {
        return msgProtectionInviteExpired;
    }

    public String getMsgProtectionInviteNotForYou() {
        return msgProtectionInviteNotForYou;
    }

    public String getMsgProtectionInviteAccepted() {
        return msgProtectionInviteAccepted;
    }

    public String getMsgProtectionInviteAcceptedOwner() {
        return msgProtectionInviteAcceptedOwner;
    }

    public String getMsgProtectionInviteDenied() {
        return msgProtectionInviteDenied;
    }

    public String getMsgProtectionInviteDeniedOwner() {
        return msgProtectionInviteDeniedOwner;
    }

    public List<String> getProtectionInviteLines() {
        return protectionInviteLines;
    }

    public String getProtectionInviteAcceptLabel() {
        return protectionInviteAcceptLabel;
    }

    public String getProtectionInviteAcceptHover() {
        return protectionInviteAcceptHover;
    }

    public String getProtectionInviteDenyLabel() {
        return protectionInviteDenyLabel;
    }

    public String getProtectionInviteDenyHover() {
        return protectionInviteDenyHover;
    }

    public String getProtectionInviteActionSeparator() {
        return protectionInviteActionSeparator;
    }

    public String getMsgProtectionIntrusionAlert() {
        return msgProtectionIntrusionAlert;
    }

    public String getProtectionIntrusionAction(String actionId) {
        return switch (actionId.toLowerCase(Locale.ROOT)) {
            case "entry" -> protectionIntrusionActionEntry;
            case "storage" -> protectionIntrusionActionStorage;
            case "break-blocks" -> protectionIntrusionActionBreakBlocks;
            case "place-blocks" -> protectionIntrusionActionPlaceBlocks;
            case "doors" -> protectionIntrusionActionDoors;
            case "buttons" -> protectionIntrusionActionButtons;
            case "pressure-plates" -> protectionIntrusionActionPressurePlates;
            case "workstations" -> protectionIntrusionActionWorkstations;
            default -> protectionIntrusionActionInteract;
        };
    }

    public String getProtectionEventMemberRemoved() {
        return protectionEventMemberRemoved;
    }

    public String getProtectionEventInviteCreated() {
        return protectionEventInviteCreated;
    }

    public String getProtectionEventInviteAccepted() {
        return protectionEventInviteAccepted;
    }

    public String getProtectionEventInviteDenied() {
        return protectionEventInviteDenied;
    }

    public String getProtectionEventOwnerTransferred() {
        return protectionEventOwnerTransferred;
    }

    public String getMsgProtectionMemberRemovedTeleported() {
        return msgProtectionMemberRemovedTeleported;
    }

    public String getMsgProtectionLimitMaxProtections() {
        return msgProtectionLimitMaxProtections;
    }

    public String getMsgProtectionLimitRadius() {
        return msgProtectionLimitRadius;
    }

    public String getMsgProtectionLimitPrice() {
        return msgProtectionLimitPrice;
    }

    public String getMsgProtectionHomeSet() {
        return msgProtectionHomeSet;
    }

    public String getMsgProtectionHomeOutside() {
        return msgProtectionHomeOutside;
    }

    public String getMsgProtectionHomeSaveError() {
        return msgProtectionHomeSaveError;
    }

    public String getMsgProtectionFlagUpdated() {
        return msgProtectionFlagUpdated;
    }

    public String getMsgProtectionFlagDenied() {
        return msgProtectionFlagDenied;
    }

    public List<String> getProtectionItemLore() {
        return Collections.unmodifiableList(protectionItemLore);
    }

    public String getProtectionDefaultActionbarEnter() {
        return protectionDefaultActionbarEnter;
    }

    public String getProtectionDefaultActionbarExit() {
        return protectionDefaultActionbarExit;
    }

    public boolean applyLanguageConfig(String languageCode) {
        Path dataFolder = plugin.getDataFolder().toPath();
        Path extraLangFolder = dataFolder.resolve("extra_lang");
        Path targetConfig = dataFolder.resolve("config.yml");
        Path sourceConfig = extraLangFolder.resolve("config_" + languageCode.toLowerCase() + ".yml");

        try {
            Files.createDirectories(extraLangFolder);
            if (Files.notExists(sourceConfig)) {
                String bundledPath = "extra_lang/config_" + languageCode.toLowerCase() + ".yml";
                try (InputStream stream = plugin.getResource(bundledPath)) {
                    if (stream != null) {
                        Files.copy(stream, sourceConfig);
                    }
                }
            }

            if (Files.notExists(sourceConfig)) {
                return false;
            }

            Files.copy(sourceConfig, targetConfig, StandardCopyOption.REPLACE_EXISTING);
            reloadConfig();
            return true;
        } catch (IOException exception) {
            plugin.getLogger()
                    .warning("Could not apply language config for '" + languageCode + "': " + exception.getMessage());
            return false;
        }
    }

    public record MenuItemConfig(String id, List<Integer> slots, int page, Material material, String name, List<String> lore,
            String headTexture, boolean decorative, String statusFor, Material emptyMaterial, Material filledMaterial) {

        public int slot() {
            return slots.isEmpty() ? -1 : slots.get(0);
        }

        public boolean hasSlot(int slot) {
            return slots.contains(slot);
        }

        public boolean statusItem() {
            return statusFor != null && !statusFor.isBlank();
        }
    }

    public record MenuDisplayConfig(Material material, String name, List<String> lore, String headTexture) {
    }

    public record MenuNavigationConfig(int slot, Material material, String name, List<String> lore,
            String headTexture) {
    }
}
