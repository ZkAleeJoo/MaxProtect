package org.zkaleejoo.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zkaleejoo.MaxProtect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class CustomConfig {
    private final MaxProtect plugin;
    private final String fileName;
    private FileConfiguration fileConfiguration = null;
    private File file = null;
    private final String folderName;
    private final boolean newFile;

    public CustomConfig(String fileName, String folderName, MaxProtect plugin, boolean newFile) {
        this.fileName = fileName;
        this.folderName = folderName;
        this.plugin = plugin;
        this.newFile = newFile;
    }

    public String getPath() {
        return this.fileName;
    }

    public void registerConfig() {
        if (folderName != null) {
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            file = new File(folder, fileName);
        } else {
            file = new File(plugin.getDataFolder(), fileName);
        }

        if (!file.exists()) {
            if (newFile) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    logConfigError("Could not create config file", e);
                }
            } else {
                if (folderName != null) {
                    plugin.saveResource(folderName + File.separator + fileName, false);
                } else if (fileName != null) {
                    plugin.saveResource(fileName, false);
                }
            }
        }

        fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
            if (!newFile) {
                updateConfig();
            }
        } catch (IOException | InvalidConfigurationException e) {
            logConfigError("Could not load config file", e);
        }
    }

    public void updateConfig() {
        try {
            String resourcePath = (folderName != null) ? folderName + "/" + fileName : fileName;
            if (resourcePath == null) {
                return;
            }
            InputStream resourceStream = plugin.getResource(resourcePath);

            if (resourceStream == null)
                return;

            YamlConfiguration jarConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));

            boolean changed = false;
            for (String key : jarConfig.getKeys(true)) {
                if (!fileConfiguration.contains(key)) {
                    fileConfiguration.set(key, jarConfig.get(key));
                    changed = true;
                }
            }

            if (changed) {
                saveConfig();
            }
        } catch (Exception e) {
            logConfigError("Could not update config file", e);
        }
    }

    public void saveConfig() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            logConfigError("Could not save config file", e);
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }

    public boolean reloadConfig() {
        if (folderName != null) {
            file = new File(plugin.getDataFolder() + File.separator + folderName, fileName);
        } else {
            file = new File(plugin.getDataFolder(), fileName);
        }

        fileConfiguration = YamlConfiguration.loadConfiguration(file);

        String resourcePath = (folderName != null) ? folderName + "/" + fileName : fileName;
        if (resourcePath == null) {
            return false;
        }
        InputStream resourceStream = plugin.getResource(resourcePath);

        if (resourceStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            fileConfiguration.setDefaults(defConfig);
        }

        return true;
    }

    private void logConfigError(String message, Exception exception) {
        String path = file == null ? fileName : file.getPath();
        plugin.getLogger().log(Level.SEVERE, message + " '" + path + "'", exception);
    }
}
