package org.zkaleejoo.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    public static Component getColoredMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(message)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static void broadcastToPlayersOnly(String message) {
        if (message == null || message.isEmpty())
            return;

        Component coloredMessage = getColoredMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null) {
                player.sendMessage(coloredMessage);
            }
        }
    }
}