package org.zkaleejoo.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MaxProtectHolder implements InventoryHolder {

    private final String menuId;
    private final String contextId;

    public MaxProtectHolder(String menuId) {
        this.menuId = menuId;
        this.contextId = "";
    }

    public MaxProtectHolder(String menuId, String contextId) {
        this.menuId = menuId;
        this.contextId = contextId == null ? "" : contextId;
    }

    public String getMenuId() {
        return menuId;
    }

    public String getContextId() {
        return contextId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
