package org.zkaleejoo.listeners;

import org.zkaleejoo.MaxProtections;
import org.zkaleejoo.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MaxProtections plugin;

    public PlayerJoinListener(MaxProtections plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("maxprotections.admin")) {
            String latest = plugin.getLatestVersion();
            if (latest != null && !plugin.getPluginMeta().getVersion().equalsIgnoreCase(latest)) {
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getPrefix()
                        + plugin.getConfigManager().getMsgUpdateAvailable().replace("{version}", latest)));
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgUpdateCurrent()
                        .replace("{version}", plugin.getPluginMeta().getVersion())));
                player.sendMessage(MessageUtils.getColoredMessage(plugin.getConfigManager().getMsgUpdateDownload()));
                player.sendMessage(MessageUtils.getColoredMessage("&f" + MaxProtections.UPDATE_DOWNLOAD_URL));
            }
        }
    }
}
