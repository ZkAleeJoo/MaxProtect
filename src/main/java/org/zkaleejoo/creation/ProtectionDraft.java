package org.zkaleejoo.creation;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ProtectionDraft {

    private String protectionId;
    private String displayName;
    private ItemStack blockItem;
    private Integer radius;
    private Double price;

    public String getProtectionId() {
        return protectionId;
    }

    public void setProtectionId(String protectionId) {
        this.protectionId = protectionId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Material getBlockType() {
        return blockItem == null ? null : blockItem.getType();
    }

    public ItemStack getBlockItem() {
        return blockItem == null ? null : blockItem.clone();
    }

    public void setBlockType(Material blockType) {
        this.blockItem = blockType == null ? null : new ItemStack(blockType);
    }

    public void setBlockItem(ItemStack blockItem) {
        this.blockItem = blockItem == null ? null : blockItem.clone();
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public boolean isComplete() {
        return protectionId != null
                && displayName != null
                && blockItem != null
                && radius != null
                && price != null;
    }

    public String getDisplayValue(String fieldId, String unsetValue) {
        return switch (fieldId) {
            case "protection-id" -> protectionId == null ? unsetValue : "&f" + protectionId;
            case "display-name" -> displayName == null ? unsetValue : displayName;
            case "block-type" -> blockItem == null ? unsetValue : "&f" + blockItem.getType().name();
            case "radius" -> radius == null ? unsetValue : "&f" + radius + "x" + radius;
            case "price" -> price == null ? unsetValue : "&f" + price;
            default -> "";
        };
    }
}
