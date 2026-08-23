package org.zkaleejoo;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.utils.SchedulerUtils;
import org.zkaleejoo.commands.MainCommand;
import org.zkaleejoo.commands.ProtectionCommand;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.creation.ProtectionCreationManager;
import org.zkaleejoo.protection.ProtectionMenuManager;
import org.zkaleejoo.protection.ProtectionRegionManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateChecker;

import net.milkbowl.vault.economy.Economy;

public final class ArgosProtect extends JavaPlugin {

    public static final String UPDATE_DOWNLOAD_URL = "https://modrinth.com/plugin/argosprotect";
    private static final long UPDATE_CHECK_INTERVAL_TICKS = 20L * 60L * 60L * 5L;
    private static final int BSTATS_PLUGIN_ID = 33606;

    private MainConfigManager mainConfigManager;
    private ProtectionCreationManager protectionCreationManager;
    private ProtectionMenuManager protectionMenuManager;
    private ProtectionRegionManager protectionRegionManager;
    private Economy economy;
    private SchedulerUtils.TaskWrapper updateCheckTask;
    private String latestVersion;
    private Metrics metrics;
    private boolean isFolia;
    private SchedulerUtils schedulerUtils;

    // PLUGIN ENCIENDE
    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
        schedulerUtils = new SchedulerUtils(this, isFolia);

        mainConfigManager = new MainConfigManager(this);
        syncMetricsState();
        setupEconomy();
        protectionCreationManager = new ProtectionCreationManager(this);
        protectionRegionManager = new ProtectionRegionManager(this);
        protectionMenuManager = new ProtectionMenuManager(this);
        registerCommands();
        registerListeners();

        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage("&a&lArgosProtect &8» &fThe plugin was successfully activated"));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a   _____                            __________                __                 __   "));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a  /  _  \\_______  ____   ____  _____\\______   \\_______  _____/  |_  ____   _____/  |_ "));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a /  /_\\  \\_  __ \\/ ___\\ /  _ \\/  ___/|     ___/\\_  __ \\/  _ \\   __\\/ __ \\_/ ___\\   __\\"));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a/    |    \\  | \\/ /_/  >  <_> )___ \\ |    |     |  | \\(  <_> )  | \\  ___/\\  \\___|  |  "));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a\\____|__  /__|  \\___  / \\____/____  >|____|     |__|   \\____/|__|  \\___  >\\___  >__|  "));
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &a        \\/     /_____/            \\/                                   \\/     \\/      "));

        startUpdateChecks();
    }

    // PLUGIN APAGA
    @Override
    public void onDisable() {
        if (protectionRegionManager != null) {
            protectionRegionManager.shutdown();
        }
        if (mainConfigManager != null) {
            mainConfigManager.unregisterLimitPermissions();
        }
        Bukkit.getConsoleSender().sendMessage(
                MessageUtils
                        .getColoredMessage("&a&lArgosProtect &8» &cThe plugin was successfully desactivated"));
    }

    private void startUpdateChecks() {
        if (updateCheckTask != null) {
            updateCheckTask.cancel();
            updateCheckTask = null;
        }

        if (!getMainConfigManager().isUpdateCheckEnabled()) {
            return;
        }

        checkUpdates();
        updateCheckTask = schedulerUtils.runTaskTimer(this::checkUpdates,
                UPDATE_CHECK_INTERVAL_TICKS, UPDATE_CHECK_INTERVAL_TICKS);
    }

    private void checkUpdates() {
        if (!getMainConfigManager().isUpdateCheckEnabled())
            return;

        new UpdateChecker(this).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                this.latestVersion = null;
                Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
                        "&a&lArgosProtect &8» &fA check for updates was performed and nothing was found."));
            } else {
                this.latestVersion = version;

                Bukkit.getConsoleSender()
                        .sendMessage(MessageUtils
                                .getColoredMessage("&a&lArgosProtect &8» &f&lNEW VERSION " + version));
                Bukkit.getConsoleSender().sendMessage(
                        MessageUtils.getColoredMessage(
                                "&a&lArgosProtect &8» &fDownload it now at the following link: &7https://modrinth.com/plugin/argosprotect"));
            }
        });
    }

    // REGISTRO DE COMANDOS
    private void registerCommands() {
        MainCommand mainCommand = new MainCommand(this);
        ProtectionCommand protectionCommand = new ProtectionCommand(this);
        registerCommand("argosprotect", mainCommand, mainCommand);
        registerCommand("protection", protectionCommand, protectionCommand);
    }

    // METODO PARA REGISTRAR COMANDOS
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
            org.bukkit.command.TabCompleter tabCompleter) {
        if (name == null)
            return;
        org.bukkit.command.PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command \"" + name + "\" is missing in plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null)
            command.setTabCompleter(tabCompleter);
    }

    // GETTERS
    public MainConfigManager getConfigManager() {
        return mainConfigManager;
    }

    public ProtectionCreationManager getProtectionCreationManager() {
        return protectionCreationManager;
    }

    public ProtectionRegionManager getProtectionRegionManager() {
        return protectionRegionManager;
    }

    public ProtectionMenuManager getProtectionMenuManager() {
        return protectionMenuManager;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new org.zkaleejoo.listeners.GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.zkaleejoo.listeners.ProtectionFlagListener(this), this);
        Bukkit.getPluginManager().registerEvents(new org.zkaleejoo.listeners.PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(protectionRegionManager, this);
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger()
                    .warning("Vault was not found. Protection prices will be saved, but economy hooks are disabled.");
            return;
        }

        var registration = getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            getLogger().warning("Vault was found, but no economy provider is registered.");
            return;
        }

        economy = registration.getProvider();
        getLogger().info("Hooked into Vault economy: " + economy.getName());
    }

    private void syncMetricsState() {
        if (getMainConfigManager().isBStatsEnabled()) {
            if (metrics == null) {
                metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            }
            return;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }

    public boolean isFolia() {
        return isFolia;
    }

    public SchedulerUtils getSchedulerUtils() {
        return schedulerUtils;
    }

}
