package org.zkaleejoo.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.zkaleejoo.MaxProtect;
import org.zkaleejoo.config.MainConfigManager;
import org.zkaleejoo.utils.MessageUtils;
import org.zkaleejoo.utils.UpdateNotificationFormatter;

public class PlayerJoinListener implements Listener {

    private static final String UPDATE_NOTIFY_PERMISSION = "maxprotect.admin";

    private final MaxProtect plugin;

    public PlayerJoinListener(MaxProtect plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(UPDATE_NOTIFY_PERMISSION)) {
            return;
        }

        MainConfigManager config = plugin.getConfigManager();
        if (!config.isUpdateCheckEnabled()) {
            return;
        }

        for (String line : UpdateNotificationFormatter.format(
                config.getPrefix(),
                config.getMsgUpdateAvailable(),
                plugin.getPluginMeta().getVersion(),
                plugin.getLatestVersion(),
                MaxProtect.UPDATE_DOWNLOAD_URL)) {
            player.sendMessage(MessageUtils.getColoredMessage(line));
        }
    }
}
