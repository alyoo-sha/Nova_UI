package org.mod.nova_ui.tweaks.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.mod.nova_ui.tweaks.logic.InventoryView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Меню контейнера для логики; обёртки слотов создаются сразу, в конструкторе
// A container menu for the logic; the slot wrappers are created at once, in the constructor
final class McInventoryView implements InventoryView {
    private final AbstractContainerMenu menu;
    private final List<McSlotView> slots = new ArrayList<>();

    McInventoryView(AbstractContainerMenu menu, Player player) {
        this.menu = menu;
        for (int i = 0; i < menu.slots.size(); i++) {
            slots.add(new McSlotView(menu.slots.get(i), i, menu, player));
        }
    }

    // Подходит ли обёртка к этому меню (то же меню и то же число слотов) / Whether the wrapper fits this menu (same menu, same slot count)
    boolean matches(AbstractContainerMenu other) {
        return other == menu && other.slots.size() == slots.size();
    }

    @Nullable
    McSlotView wrap(@Nullable Slot slot) {
        if (slot == null) return null;
        int index = slot.index;
        if (index >= 0 && index < slots.size() && slots.get(index).slot() == slot) return slots.get(index);
        for (McSlotView view : slots) {
            if (view.slot() == slot) return view;
        }
        return null;
    }

    @Override
    public List<? extends McSlotView> slots() {
        return slots;
    }

    @Override
    public Object carriedKey() {
        ItemStack carried = menu.getCarried();
        return carried.isEmpty() ? null : McItemKey.of(carried);
    }

    @Override
    public int carriedCount() {
        return menu.getCarried().getCount();
    }

    @Override
    public int carriedMaxStack() {
        return menu.getCarried().getMaxStackSize();
    }
}