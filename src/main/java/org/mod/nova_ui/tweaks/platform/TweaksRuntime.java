package org.mod.nova_ui.tweaks.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_ui.menu.minecraft.NovaCreativeScreen;
import org.mod.nova_ui.tweaks.config.TweaksConfig;
import org.mod.nova_ui.tweaks.logic.TweakEngine;

import javax.annotation.Nullable;
import java.util.List;

// Связка логики с игрой: один движок на клиент, проверка допустимости экрана, обёртка текущего меню
// Glue between the logic and the game: one engine per client, screen eligibility checks, the current menu wrapper
public final class TweaksRuntime {
    private static final TweakEngine ENGINE = new TweakEngine(TweaksConfig.SETTINGS, new McClickSink());
    // Кэш обёртки текущего меню; пересоздаётся при смене меню / A cache of the current menu wrapper; rebuilt when the menu changes
    private static McInventoryView view = null;

    private TweaksRuntime() {}

    // Работает ли модуль в этом экране: включён, не креатив, не исключён, экран соответствует меню игрока
    // Whether the module works in this screen: enabled, not creative, not excluded, the screen matches the player's menu
    public static boolean eligible(AbstractContainerScreen<?> screen) {
        if (!TweaksConfig.ENABLED.get()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || player.isSpectator()) return false;
        AbstractContainerMenu menu = screen.getMenu();
        if (menu != player.containerMenu) return false;
        // Креатив-инвентарь шлёт клики через handleCreativeModeItemAdd: исключён целиком / The creative inventory sends clicks through handleCreativeModeItemAdd: excluded entirely
        if (screen instanceof CreativeModeInventoryScreen || screen instanceof NovaCreativeScreen) return false;
        if (TweaksConfig.CONTAINERS_ONLY.get() && menu instanceof InventoryMenu) return false;
        return !TweaksConfig.isScreenExcluded(screen.getClass().getName());
    }

    // Слот под точкой экрана; та же геометрия и порядок, что у ванильного findSlot
    // The slot at a screen point; the same geometry and order as vanilla findSlot
    @Nullable
    public static Slot slotAt(AbstractContainerScreen<?> screen, double mx, double my) {
        double px = mx - screen.getGuiLeft();
        double py = my - screen.getGuiTop();
        for (Slot s : screen.getMenu().slots) {
            if (s.isActive() && px >= s.x - 1 && px < s.x + 17 && py >= s.y - 1 && py < s.y + 17) return s;
        }
        return null;
    }

    public static boolean press(AbstractContainerScreen<?> screen, @Nullable Slot slot, int button) {
        McInventoryView v = viewFor(screen);
        return ENGINE.press(v, v.wrap(slot), button, Screen.hasShiftDown(), System.currentTimeMillis());
    }

    public static boolean drag(AbstractContainerScreen<?> screen, @Nullable Slot slot, int button) {
        McInventoryView v = viewFor(screen);
        return ENGINE.drag(v, v.wrap(slot), button, Screen.hasShiftDown());
    }

    public static boolean release(AbstractContainerScreen<?> screen, int button) {
        return ENGINE.release(viewFor(screen), button);
    }

    public static boolean scroll(AbstractContainerScreen<?> screen, @Nullable Slot slot, double delta) {
        McInventoryView v = viewFor(screen);
        return ENGINE.scroll(v, v.wrap(slot), delta);
    }

    public static void tick() {
        ENGINE.tick();
    }

    // При смене и закрытии экрана / On screen change and close
    public static void reset() {
        ENGINE.reset();
        view = null;
    }

    public static boolean isSplitting() {
        return ENGINE.isSplitting();
    }

    public static List<Integer> splitSlotIds() {
        return ENGINE.splitSlotIds();
    }

    private static McInventoryView viewFor(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        if (view == null || !view.matches(menu)) {
            view = new McInventoryView(menu, Minecraft.getInstance().player);
        }
        return view;
    }
}