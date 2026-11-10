package org.mod.nova_ui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import org.mod.nova_core.Nova_core;
import org.mod.nova_ui.menu.NovaCoreMenus;
import org.mod.nova_ui.menu.minecraft.NovaWorkbenchScreen;
import org.mod.nova_core.ui.animation.Animator;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.debug.DebugLog;
import org.mod.nova_core.ui.minecraft.McPlatform;
import org.mod.nova_core.ui.minecraft.demo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NovaCoreClient {
    private static final String CATEGORY = "key.categories.nova_core";

    // Клавиши создаются сразу, по одной на демо-экран / Keys are created eagerly, one per demo screen
    public static final KeyMapping OPEN_DEMO = key("open_demo", GLFW.GLFW_KEY_K);
    public static final KeyMapping OPEN_SCROLL_DEMO = key("open_scroll_demo", GLFW.GLFW_KEY_J);
    public static final KeyMapping OPEN_STATE_DEMO = key("open_state_demo", GLFW.GLFW_KEY_H);
    public static final KeyMapping OPEN_ANIMATION_DEMO = key("open_animation_demo", GLFW.GLFW_KEY_G);
    public static final KeyMapping OPEN_OVERLAY_DEMO = key("open_overlay_demo", GLFW.GLFW_KEY_O);
    public static final KeyMapping OPEN_CONTROLS_DEMO = key("open_controls_demo", GLFW.GLFW_KEY_I);
    public static final KeyMapping OPEN_COLLECTIONS_DEMO = key("open_collections_demo", GLFW.GLFW_KEY_U);
    public static final KeyMapping OPEN_DND_DEMO = key("open_dnd_demo", GLFW.GLFW_KEY_N);
    public static final KeyMapping OPEN_FORM_DEMO = key("open_form_demo", GLFW.GLFW_KEY_M);
    public static final KeyMapping OPEN_LIMITS_DEMO = key("open_limits_demo", GLFW.GLFW_KEY_L);

    private NovaCoreClient() {}

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping("key.nova_core." + name, glfwKey, CATEGORY);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        UiContext.install(new McPlatform());

        // Дамп дерева инспектора (Alt+P) идёт в лог игры / The inspector's tree dump (Alt+P) goes to the game log
        Logger logger = LoggerFactory.getLogger("Nova Core");
        DebugLog.setSink(message -> logger.info("\n{}", message));

        // Экран верстака привязывается к типу меню / The workbench screen is bound to its menu type
        event.enqueueWork(() -> MenuScreens.register(NovaCoreMenus.WORKBENCH.get(), NovaWorkbenchScreen::new));
    }

    // Шина мода: регистрация клавиш / Mod bus: key registration
    @Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_DEMO);
            event.register(OPEN_SCROLL_DEMO);
            event.register(OPEN_STATE_DEMO);
            event.register(OPEN_ANIMATION_DEMO);
            event.register(OPEN_OVERLAY_DEMO);
            event.register(OPEN_CONTROLS_DEMO);
            event.register(OPEN_COLLECTIONS_DEMO);
            event.register(OPEN_DND_DEMO);
            event.register(OPEN_FORM_DEMO);
            event.register(OPEN_LIMITS_DEMO);
        }
    }

    // Шина Forge: проверка нажатий каждый тик / Forge bus: check the presses every tick
    @Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) Animator.shared().tick(System.nanoTime());
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            openOnPress(mc, OPEN_DEMO, DemoScreen::new);
            openOnPress(mc, OPEN_SCROLL_DEMO, ScrollDemoScreen::new);
            openOnPress(mc, OPEN_STATE_DEMO, StateDemoScreen::new);
            openOnPress(mc, OPEN_ANIMATION_DEMO, AnimationDemoScreen::new);
            openOnPress(mc, OPEN_OVERLAY_DEMO, OverlayDemoScreen::new);
            openOnPress(mc, OPEN_CONTROLS_DEMO, ControlsDemoScreen::new);
            openOnPress(mc, OPEN_COLLECTIONS_DEMO, CollectionsDemoScreen::new);
            openOnPress(mc, OPEN_DND_DEMO, DndDemoScreen::new);
            openOnPress(mc, OPEN_FORM_DEMO, FormDemoScreen::new);
            openOnPress(mc, OPEN_LIMITS_DEMO, LimitsDemoScreen::new);
        }

        // consumeClick() в цикле, чтобы не копились нажатия; открываем только из мира и без другого экрана
        // loop consumeClick() so presses don't pile up; open only in-world with no other screen
        private static void openOnPress(Minecraft mc, KeyMapping key, Supplier<Screen> factory) {
            while (key.consumeClick()) {
                if (mc.level != null && mc.screen == null) mc.setScreen(factory.get());
            }
        }
    }
}