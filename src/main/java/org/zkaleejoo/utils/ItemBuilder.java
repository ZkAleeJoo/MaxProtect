package org.zkaleejoo.utils;

import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemBuilder {

    private static final Pattern TEXTURE_URL_PATTERN = Pattern
            .compile("https?://textures\\.minecraft\\.net/texture/[A-Za-z0-9_./-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXTURE_HASH_PATTERN = Pattern.compile("^[A-Fa-f0-9]{32,}$");

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (meta != null) {
            meta.displayName(MessageUtils.getColoredMessage(name));
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        if (meta != null) {
            List<Component> coloredLore = Arrays.stream(lore)
                    .map(MessageUtils::getColoredMessage)
                    .toList();

            meta.lore(coloredLore);
        }
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder hideAttributes() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        return this;
    }

    public ItemBuilder setHeadTexture(String texture) {
        if (!(meta instanceof SkullMeta skullMeta) || texture == null || texture.isBlank()) {
            return this;
        }

        String trimmedTexture = texture.trim();
        URL textureUrl = parseTextureUrl(trimmedTexture);
        String textureBase64 = toTextureBase64(trimmedTexture, textureUrl);

        if (applyPaperProfileProperty(skullMeta, textureBase64)) {
            return this;
        }

        if (textureUrl != null && applyBukkitProfile(skullMeta, textureUrl)) {
            return this;
        }

        applyGameProfile(skullMeta, textureBase64);
        return this;
    }

    public ItemBuilder setHeadOwner(UUID ownerUuid) {
        if (!(meta instanceof SkullMeta skullMeta) || ownerUuid == null) {
            return this;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        skullMeta.setOwningPlayer(owner);
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    private URL parseTextureUrl(String texture) {
        String normalizedTexture = texture;
        if (texture.startsWith("textures.minecraft.net/texture/")) {
            normalizedTexture = "http://" + texture;
        } else if (TEXTURE_HASH_PATTERN.matcher(texture).matches()) {
            normalizedTexture = "http://textures.minecraft.net/texture/" + texture;
        }

        try {
            if (normalizedTexture.startsWith("http://") || normalizedTexture.startsWith("https://")) {
                return URI.create(normalizedTexture).toURL();
            }
        } catch (IllegalArgumentException | MalformedURLException ignored) {
            return null;
        }

        return parseTextureUrlFromBase64(texture);
    }

    private URL parseTextureUrlFromBase64(String texture) {
        try {
            String decodedTexture = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
            Matcher matcher = TEXTURE_URL_PATTERN.matcher(decodedTexture);
            if (matcher.find()) {
                return URI.create(matcher.group()).toURL();
            }
        } catch (IllegalArgumentException | MalformedURLException ignored) {
            return null;
        }
        return null;
    }

    private String toTextureBase64(String texture, URL textureUrl) {
        if (textureUrl == null) {
            return texture;
        }

        String textureJson = String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}", textureUrl);
        return Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));
    }

    private boolean applyBukkitProfile(SkullMeta skullMeta, URL textureUrl) {
        Object profile = createProfile("createPlayerProfile", true);
        if (profile == null) {
            profile = createProfile("createProfile", true);
        }
        if (profile == null) {
            profile = createProfile("createPlayerProfile", false);
        }
        if (profile == null) {
            profile = createProfile("createProfile", false);
        }
        if (profile == null) {
            return false;
        }

        try {
            Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            textures.getClass().getMethod("setSkin", URL.class).invoke(textures, textureUrl);
            invokeSingleArgumentMethod(profile, "setTextures", textures);
            return invokeSingleArgumentMethod(skullMeta, "setOwnerProfile", profile)
                    || invokeSingleArgumentMethod(skullMeta, "setPlayerProfile", profile);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private Object createProfile(String methodName, boolean withName) {
        try {
            if (withName) {
                return Bukkit.class.getMethod(methodName, UUID.class, String.class)
                        .invoke(null, UUID.randomUUID(), "MaxProtect");
            }
            return Bukkit.class.getMethod(methodName, UUID.class).invoke(null, UUID.randomUUID());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private boolean applyPaperProfileProperty(SkullMeta skullMeta, String textureBase64) {
        if (textureBase64 == null) {
            return false;
        }
        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "MaxProtect");
            profile.setProperty(new ProfileProperty("textures", textureBase64));
            skullMeta.setPlayerProfile(profile);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean invokeSingleArgumentMethod(Object target, String methodName, Object argument)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argument.getClass())) {
                method.invoke(target, argument);
                return true;
            }
        }
        return false;
    }

    private void applyGameProfile(SkullMeta skullMeta, String textureBase64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);
            Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = createGameProfileProperty(propertyClass, textureBase64);
            if (property == null) {
                return;
            }
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

            try {
                Method setProfile = skullMeta.getClass().getDeclaredMethod("setProfile", gameProfileClass);
                setProfile.setAccessible(true);
                setProfile.invoke(skullMeta, gameProfile);
                return;
            } catch (NoSuchMethodException ignored) {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, gameProfile);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Keep the head as a normal PLAYER_HEAD if the running server does not expose a
            // supported profile API.
        }
    }

    private Object createGameProfileProperty(Class<?> propertyClass, String textureBase64)
            throws ReflectiveOperationException {
        try {
            return propertyClass.getConstructor(String.class, String.class).newInstance("textures", textureBase64);
        } catch (NoSuchMethodException ignored) {
            return propertyClass.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", textureBase64, null);
        }
    }
}
