package org.mod.nova_ui.tweaks.platform;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mod.nova_core.ui.minecraft.container.NovaContainerScreen;
import org.mod.nova_ui.Nova_ui;

// События Forge для окон, не подменённых Nova (в том числе чужих модов); окна Nova идут через точку расширения
// Forge events for windows not replaced by Nova (including other mods); Nova windows go through the extension point
@Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeScreenHooks {
    // Подсветка слотов деления поровну (ARGB) / Highlight of the even-split slots (ARGB)
    private static final int SPLIT_HIGHLIGHT = 0x60FFFFFF;

    private ForgeScreenHooks() {}

    // Экран, которым занимаются события Forge, или null / The screen handled by Forge events, or null
    private static AbstractContainerScreen<?> target(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> container
                && !(screen instanceof NovaContainerScreen<?>)
                && TweaksRuntime.eligible(container)) {
            return container;
        }
        return null;
    }

    @SubscribeEvent
    public static void onPress(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = target(event.getScreen());
        if (screen == null) return;
        Slot slot = TweaksRuntime.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (TweaksRuntime.press(screen, slot, event.getButton())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrag(ScreenEvent.MouseDragged.Pre event) {
        AbstractContainerScreen<?> screen = target(event.getScreen());
        if (screen == null) return;
        Slot slot = TweaksRuntime.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (TweaksRuntime.drag(screen, slot, event.getMouseButton())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        AbstractContainerScreen<?> screen = target(event.getScreen());
        if (screen == null) return;
        if (TweaksRuntime.release(screen, event.getButton())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        AbstractContainerScreen<?> screen = target(event.getScreen());
        if (screen == null) return;
        Slot slot = TweaksRuntime.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (TweaksRuntime.scroll(screen, slot, event.getScrollDelta())) event.setCanceled(true);
    }

    // Подсветка слотов, выбранных для деления поровну: ванильный предпросмотр при этом жесте отключён
    // Highlights the slots picked for the even split: the vanilla preview is off during this gesture
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!TweaksRuntime.isSplitting() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F); // над предметами / above the items
        for (int id : TweaksRuntime.splitSlotIds()) {
            if (id < 0 || id >= screen.getMenu().slots.size()) continue;
            Slot slot = screen.getMenu().slots.get(id);
            int x = screen.getGuiLeft() + slot.x;
            int y = screen.getGuiTop() + slot.y;
            g.fill(x, y, x + 16, y + 16, SPLIT_HIGHLIGHT);
        }
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        TweaksRuntime.reset();
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        TweaksRuntime.reset();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TweaksRuntime.tick();
    }
}