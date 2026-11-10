package org.mod.nova_ui.tweaks.platform;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_core.ui.minecraft.container.interaction.InventoryInteraction;
import org.mod.nova_core.ui.minecraft.container.interaction.InventoryMouseEvent;

import javax.annotation.Nullable;

// Подключение к точке расширения Nova Core: окна на базе NovaContainerScreen
// The hookup to the Nova Core extension point: windows based on NovaContainerScreen
public final class NovaTweaksInteraction implements InventoryInteraction {

    @Override
    public boolean appliesTo(AbstractContainerScreen<?> screen) {
        return TweaksRuntime.eligible(screen);
    }

    @Override
    public boolean onSlotPress(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        return TweaksRuntime.press(screen, slot, event.button());
    }

    @Override
    public boolean onSlotDrag(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        return TweaksRuntime.drag(screen, slot, event.button());
    }

    @Override
    public boolean onSlotRelease(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        return TweaksRuntime.release(screen, event.button());
    }

    @Override
    public boolean onSlotScroll(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        return TweaksRuntime.scroll(screen, slot, event.scrollDelta());
    }
}