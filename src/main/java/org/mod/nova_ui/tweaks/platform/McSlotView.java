package org.mod.nova_ui.tweaks.platform;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.mod.nova_ui.tweaks.logic.SlotView;

// Слот меню для логики; читает живое состояние слота / A menu slot for the logic; reads the live slot state
final class McSlotView implements SlotView {
    private final Slot slot;
    private final int id;
    private final AbstractContainerMenu menu;
    private final Player player;

    McSlotView(Slot slot, int id, AbstractContainerMenu menu, Player player) {
        this.slot = slot;
        this.id = id;
        this.menu = menu;
        this.player = player;
    }

    Slot slot() {
        return slot;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public boolean isEmpty() {
        return !slot.hasItem();
    }

    @Override
    public int count() {
        return slot.getItem().getCount();
    }

    @Override
    public Object itemKey() {
        ItemStack stack = slot.getItem();
        return stack.isEmpty() ? null : McItemKey.of(stack);
    }

    @Override
    public boolean isPlayerSide() {
        return slot.container instanceof Inventory;
    }

    // Пустой слот считаем «только забрать» лишь для слота результата; непустой: если в него нельзя вернуть свой же предмет
    // An empty slot counts as take-only only for a result slot; a non-empty one: if its own item cannot be put back
    @Override
    public boolean isTakeOnly() {
        ItemStack stack = slot.getItem();
        return stack.isEmpty() ? slot instanceof ResultSlot : !slot.mayPlace(stack);
    }

    @Override
    public boolean canPickup() {
        return slot.isActive() && slot.hasItem() && slot.mayPickup(player);
    }

    @Override
    public int roomFor(Object itemKey) {
        ItemStack probe = ((McItemKey) itemKey).probe();
        if (!slot.isActive() || !slot.mayPlace(probe)) return 0;
        ItemStack current = slot.getItem();
        int max = slot.getMaxStackSize(probe);
        if (current.isEmpty()) return max;
        if (!ItemStack.isSameItemSameTags(current, probe)) return 0;
        return Math.max(0, max - current.getCount());
    }

    @Override
    public boolean acceptsSpread(Object itemKey) {
        ItemStack probe = ((McItemKey) itemKey).probe();
        if (!slot.isActive() || !slot.mayPlace(probe) || !menu.canDragTo(slot)) return false;
        ItemStack current = slot.getItem();
        return current.isEmpty() || ItemStack.isSameItemSameTags(current, probe);
    }

    // Решение принимает само меню: ровно та проверка, по которой ванильный двойной клик выбирает слоты
    // The menu itself decides: exactly the check by which the vanilla double click picks slots
    @Override
    public boolean pickupAllWouldTake(Object itemKey) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !isTakeOnly()) return false;
        ItemStack probe = ((McItemKey) itemKey).probe();
        return ItemStack.isSameItemSameTags(stack, probe) && slot.mayPickup(player) && menu.canTakeItemForPickAll(probe, slot);
    }
}