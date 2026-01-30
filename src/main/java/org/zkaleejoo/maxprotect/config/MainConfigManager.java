package org.zkaleejoo.maxprotect.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.zkaleejoo.maxprotect.MaxProtect;

public class MainConfigManager {
    
    private final MaxProtect plugin;
    private CustomConfig configFile;
    private CustomConfig langFile;
    
    private String prefix;
    private String noPermission;
    private String protectionCreated;
    private String protectionOverlaps;

    public MainConfigManager(MaxProtect plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new CustomConfig("config.yml", null, plugin);
        configFile.registerConfig();
        
        String lang = configFile.getConfig().getString("general.language", "es");
        langFile = new CustomConfig("messages_" + lang + ".yml", "lang", plugin);
        langFile.registerConfig();
        
        loadVariables();
    }

    private void loadVariables() {
        FileConfiguration config = configFile.getConfig();
        FileConfiguration msg = langFile.getConfig();

        this.prefix = config.getString("general.prefix", "&b&lMaxProtect &8» ");
        
        this.noPermission = msg.getString("messages.no-permission", "&cNo tienes permisos.");
        this.protectionCreated = msg.getString("messages.protection-created", "&a¡Protección creada con éxito!");
        this.protectionOverlaps = msg.getString("messages.protection-overlaps", "&cNo puedes crear una protección aquí, se superpone con otra.");
    }

    public void reload() {
        configFile.reloadConfig();
        langFile.reloadConfig();
        loadVariables();
    }

    public String getPrefix() { return prefix; }
    public String getNoPermission() { return noPermission; }
    public String getProtectionCreated() { return protectionCreated; }
    public String getProtectionOverlaps() { return protectionOverlaps; }
}