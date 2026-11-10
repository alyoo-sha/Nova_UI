package org.mod.nova_ui.tweaks.platform;

import net.minecraft.world.item.ItemStack;

// Ключ предмета для логики: предмет и теги, количество не учитывается
// An item key for the logic: the item and its tags, the count is ignored
final class McItemKey {
    private final ItemStack probe;

    private McItemKey(ItemStack probe) {
        this.probe = probe;
    }

    static McItemKey of(ItemStack source) {
        ItemStack copy = source.copy();
        copy.setCount(1);
        return new McItemKey(copy);
    }

    // Образец в одном экземпляре для проверок слота / A single-item sample for slot checks
    ItemStack probe() {
        return probe;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof McItemKey key && ItemStack.isSameItemSameTags(probe, key.probe);
    }

    @Override
    public int hashCode() {
        return probe.getItem().hashCode();
    }
}