package org.zkaleejoo.utils;

import org.zkaleejoo.MaxProtect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final String GITHUB_VERSION_URL = "https://gist.githubusercontent.com/ZkAleeJoo/16e20017d7c91f93789ee5c831635758/raw/MaxProtect";

    private final MaxProtect plugin;

    public UpdateChecker(MaxProtect plugin) {
        this.plugin = plugin;
    }

    public void getVersion(final Consumer<String> consumer) {
        plugin.getSchedulerUtils().runTaskAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(GITHUB_VERSION_URL).toURL();
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "MaxProtect-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int statusCode = connection.getResponseCode();
                if (statusCode < 200 || statusCode >= 300) {
                    plugin.getLogger().warning(
                            "The update server could not be found; I recommend reporting this to Discord support. "
                                    + statusCode);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String latestVersion = reader.readLine();
                    if (latestVersion != null && !latestVersion.isBlank()) {
                        String trimmedVersion = latestVersion.trim();
                        if (plugin.isEnabled()) {
                            plugin.getSchedulerUtils().runTask(() -> consumer.accept(trimmedVersion));
                        }
                    } else {
                        plugin.getLogger().info("The plugin version is empty");
                    }
                }
            } catch (Exception exception) {
                plugin.getLogger().info("There is no connection to the Updates server: " + exception.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}