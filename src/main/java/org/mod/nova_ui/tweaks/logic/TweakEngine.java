package org.mod.nova_ui.tweaks.logic;

import java.util.List;

// Фасад логики твиков: один объект на все платформенные хуки. Не зависит от Minecraft
// The tweak logic facade: one object for all platform hooks. Independent of Minecraft
public final class TweakEngine {
    private static final int CLICKS_PER_TICK = 128;

    private final ClickBudget budget = new ClickBudget(CLICKS_PER_TICK);
    private final GestureEngine gestures;
    private final WheelTweak wheel;

    public TweakEngine(TweakSettings settings, ClickSink sink) {
        this.gestures = new GestureEngine(settings, sink, budget);
        this.wheel = new WheelTweak(settings, sink, budget);
    }

    public boolean press(InventoryView view, SlotView slot, int button, boolean shift, long nowMs) {
        return gestures.press(view, slot, button, shift, nowMs);
    }

    public boolean drag(InventoryView view, SlotView slot, int button, boolean shift) {
        return gestures.drag(view, slot, button, shift);
    }

    public boolean release(InventoryView view, int button) {
        return gestures.release(view, button);
    }

    public boolean scroll(InventoryView view, SlotView slot, double delta) {
        return wheel.scroll(view, slot, delta);
    }

    // Вызывать каждый тик клиента / Call on every client tick
    public void tick() {
        budget.onTick();
    }

    // Вызывать при смене или закрытии экрана / Call when the screen changes or closes
    public void reset() {
        gestures.resetAll();
    }

    // Слоты, которые нужно подсветить (деление поровну в процессе); пусто, если подсвечивать нечего
    // Slots to highlight (an even split in progress); empty if there is nothing to highlight
    public List<Integer> highlightedSlotIds() {
        return gestures.highlightedSlotIds();
    }
}