package org.zkaleejoo.commands.main;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.zkaleejoo.commands.core.PluginSubCommand;
import org.zkaleejoo.config.MainConfigManager.MenuItemConfig;
import org.zkaleejoo.utils.ItemBuilder;
import org.zkaleejoo.utils.ArgosProtectHolder;
import org.zkaleejoo.utils.MessageUtils;

public class LanguageSubCommand implements PluginSubCommand {

    private final MainCommandContext context;

    public LanguageSubCommand(MainCommandContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("lang");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return context.hasAdminPermission(sender, MainCommandContext.ADMIN_PERMISSION);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!canUse(sender)) {
            context.sendNoPermission(sender);
            return;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, context.plugin().getConfigManager().getMsgPlayerOnly());
            return;
        }

        Inventory inv = Bukkit.createInventory(new ArgosProtectHolder("LANG_MENU"),
                context.plugin().getConfigManager().getLanguageMenuSize(),
                MessageUtils.getColoredMessage(context.plugin().getConfigManager().getLanguageMenuTitle()));

        ItemStack filler = new ItemBuilder(context.plugin().getConfigManager().getLanguageMenuFillerMaterial())
                .setName(context.plugin().getConfigManager().getLanguageMenuFillerName())
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        for (MenuItemConfig item : context.plugin().getConfigManager().getLanguageMenuItems()) {
            for (int slot : item.slots()) {
                if (slot >= inv.getSize()) {
                    continue;
                }
                inv.setItem(slot, new ItemBuilder(item.material())
                        .setName(item.name())
                        .setLore(item.lore().toArray(new String[0]))
                        .setHeadTexture(item.headTexture())
                        .build());
            }
        }

        player.openInventory(inv);
        context.play(player, Sound.UI_BUTTON_CLICK);
    }
}
