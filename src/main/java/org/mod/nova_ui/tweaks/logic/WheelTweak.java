package org.mod.nova_ui.tweaks.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Колесо над слотом: перенос предметов в другой инвентарь и обратно. План кликов строится по снимку
// состояния и отправляется целиком, чтобы на курсоре никогда не оставались лишние предметы
// The wheel over a slot: moving items to the other inventory and back. The click plan is built from a
// state snapshot and sent as a whole, so that no stray items ever stay on the cursor
public final class WheelTweak {
    private record PlannedClick(int slotId, int button, ClickKind kind) {}

    private final TweakSettings settings;
    private final ClickSink sink;
    private final ClickBudget budget;

    public WheelTweak(TweakSettings settings, ClickSink sink, ClickBudget budget) {
        this.settings = settings;
        this.sink = sink;
        this.budget = budget;
    }

    // delta > 0: колесо вверх / delta > 0: wheel up
    public boolean scroll(InventoryView view, SlotView slot, double delta) {
        if (!settings.wheelEnabled() || slot == null || delta == 0.0 || view.carriedKey() != null) return false;
        boolean push = settings.wheelDirection() == WheelDirection.DOWN_PUSH ? delta < 0.0 : delta > 0.0;
        int notches = Math.min(8, Math.max(1, (int) Math.round(Math.abs(delta))));
        int amount = Math.max(1, settings.wheelStep()) * notches;

        List<PlannedClick> plan = push ? planPush(view, slot, amount) : planPull(view, slot, amount);
        if (plan.isEmpty()) return false;
        if (!budget.canSend(plan.size())) return true; // не успеваем в этом тике: событие поглощаем без действий / over the limit this tick: swallow the event
        for (PlannedClick c : plan) sink.click(c.slotId(), c.button(), c.kind());
        budget.spend(plan.size());
        return true;
    }

    // Из слота в другой инвентарь / From the slot to the other inventory
    private List<PlannedClick> planPush(InventoryView view, SlotView from, int amount) {
        List<PlannedClick> out = new ArrayList<>();
        if (from.isEmpty() || !from.canPickup()) return out;
        if (from.isTakeOnly()) {
            // Результат переносится целиком одним Shift-кликом / A result is moved whole by one Shift click
            out.add(new PlannedClick(from.id(), ClickCodes.LEFT, ClickKind.QUICK_MOVE));
            return out;
        }
        Object key = from.itemKey();
        int total = Math.min(amount, from.count());

        // Снимок вместимости: сначала слоты с тем же предметом, затем пустые
        // A capacity snapshot: same-item slots first, then empty ones
        List<int[]> targets = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (SlotView s : view.slots()) {
                if (s.id() == from.id() || s.isPlayerSide() == from.isPlayerSide() || s.isTakeOnly()) continue;
                if (s.isEmpty() != (pass == 1)) continue;
                int room = s.roomFor(key);
                if (room > 0) targets.add(new int[] {s.id(), room});
            }
        }

        int remaining = total;
        List<int[]> allocations = new ArrayList<>();
        for (int[] t : targets) {
            if (remaining == 0) break;
            int part = Math.min(t[1], remaining);
            allocations.add(new int[] {t[0], part});
            remaining -= part;
        }
        if (remaining == total) return out;

        out.add(new PlannedClick(from.id(), ClickCodes.LEFT, ClickKind.PICKUP));
        int inHand = from.count();
        for (int[] a : allocations) {
            if (a[1] == inHand) {
                out.add(new PlannedClick(a[0], ClickCodes.LEFT, ClickKind.PICKUP)); // кладём всё, что в руке / place everything in hand
            } else {
                for (int i = 0; i < a[1]; i++) out.add(new PlannedClick(a[0], ClickCodes.RIGHT, ClickKind.PICKUP));
            }
            inHand -= a[1];
        }
        if (inHand > 0) out.add(new PlannedClick(from.id(), ClickCodes.LEFT, ClickKind.PICKUP)); // возвращаем остаток / return the rest
        return out;
    }

    // Из другого инвентаря в слот / From the other inventory into the slot
    private List<PlannedClick> planPull(InventoryView view, SlotView target, int amount) {
        List<PlannedClick> out = new ArrayList<>();
        if (target.isTakeOnly()) return out;
        Object key = target.isEmpty() ? null : target.itemKey();

        List<SlotView> sources = new ArrayList<>();
        for (SlotView s : view.slots()) {
            if (s.id() == target.id() || s.isPlayerSide() == target.isPlayerSide()) continue;
            if (s.isEmpty() || s.isTakeOnly() || !s.canPickup()) continue;
            if (key == null) {
                if (target.roomFor(s.itemKey()) <= 0) continue;
                key = s.itemKey();
            } else if (!key.equals(s.itemKey())) {
                continue;
            }
            sources.add(s);
        }
        if (key == null) return out;
        int want = Math.min(amount, target.roomFor(key));
        if (want <= 0) return out;

        sources.sort(Comparator.comparingInt(SlotView::count)); // сначала мелкие стопки / small stacks first
        for (SlotView s : sources) {
            if (want == 0) break;
            int part = Math.min(s.count(), want);
            out.add(new PlannedClick(s.id(), ClickCodes.LEFT, ClickKind.PICKUP));
            for (int i = 0; i < part; i++) out.add(new PlannedClick(target.id(), ClickCodes.RIGHT, ClickKind.PICKUP));
            if (part < s.count()) out.add(new PlannedClick(s.id(), ClickCodes.LEFT, ClickKind.PICKUP));
            want -= part;
        }
        return out;
    }
}