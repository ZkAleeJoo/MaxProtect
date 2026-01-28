package org.zkaleejoo.maxprotect;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.zkaleejoo.maxprotect.config.MainConfigManager;
import org.zkaleejoo.maxprotect.utils.MessageUtils;

public class MaxProtect extends JavaPlugin {

    private static MaxProtect instance;
    private MainConfigManager mainConfigManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 1. Cargar Configuración
        mainConfigManager = new MainConfigManager(this);

        // 2. Registrar Comandos (Pendiente)
        // registerCommands();

        // 3. Registrar Eventos (Pendiente)
        // registerEvents();

        // Mensaje de inicio profesional
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
            mainConfigManager.getPrefix() + "&aPlugin habilitado correctamente (v" + getDescription().getVersion() + ")"
        ));
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(MessageUtils.getColoredMessage(
            mainConfigManager.getPrefix() + "&cPlugin deshabilitado."
        ));
    }

    public static MaxProtect getInstance() {
        return instance;
    }

    public MainConfigManager getMainConfigManager() {
        return mainConfigManager;
    }
}