package org.mod.nova_ui.tweaks.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Колесо над слотом: перенос предметов в другой инвентарь и обратно. План кликов строится по снимку
// состояния и отправляется целиком, чтобы на курсоре никогда не оставались лишние предметы.
// Слот результата даёт за прокрутку одну партию, и только если она целиком раскладывается
// The wheel over a slot: moving items to the other inventory and back. The click plan is built from a
// state snapshot and sent as a whole, so that no stray items ever stay on the cursor.
// A result slot gives one batch per scroll, and only if the whole batch can be distributed
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

    // delta > 0: колесо вверх. Над слотом прокрутку всегда поглощаем, даже если предметы не двигались
    // delta > 0: wheel up. Over a slot the scroll is always consumed, even if no items moved
    public boolean scroll(InventoryView view, SlotView slot, double delta) {
        if (!settings.wheelEnabled() || slot == null || delta == 0.0) return false;
        if (view.carriedKey() != null) return true;
        boolean push = settings.wheelDirection() == WheelDirection.DOWN_PUSH ? delta < 0.0 : delta > 0.0;
        int notches = Math.min(8, Math.max(1, (int) Math.round(Math.abs(delta))));
        int amount = Math.max(1, settings.wheelStep()) * notches;

        List<PlannedClick> plan = push ? planPush(view, slot, amount) : planPull(view, slot, amount);
        if (plan.isEmpty() || !budget.canSend(plan.size())) return true;
        for (PlannedClick c : plan) sink.click(c.slotId(), c.button(), c.kind());
        budget.spend(plan.size());
        return true;
    }

    // ---------- Из слота в другой инвентарь / From the slot to the other inventory ----------

    private List<PlannedClick> planPush(InventoryView view, SlotView from, int amount) {
        List<PlannedClick> out = new ArrayList<>();
        if (from.isEmpty() || !from.canPickup()) return out;
        Object key = from.itemKey();
        boolean result = from.isTakeOnly();
        int total = result ? from.count() : Math.min(amount, from.count());

        List<int[]> allocations = new ArrayList<>();
        int remaining = allocate(view, from, key, total, allocations);
        if (remaining == total) return out;
        if (result && remaining > 0) return out; // партия целиком не помещается / the whole batch does not fit

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

    // Раскладывает count предметов по слотам другого инвентаря: сначала слоты с тем же предметом, затем пустые.
    // Возвращает, сколько не поместилось / Distributes count items over the other inventory slots: same-item slots first, then empty ones.
    // Returns how many did not fit
    private int allocate(InventoryView view, SlotView from, Object key, int count, List<int[]> out) {
        int remaining = count;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (SlotView s : view.slots()) {
                if (remaining == 0) break;
                if (s.id() == from.id() || s.isPlayerSide() == from.isPlayerSide() || s.isTakeOnly()) continue;
                if (s.isEmpty() != (pass == 1)) continue;
                int room = s.roomFor(key);
                if (room <= 0) continue;
                int part = Math.min(room, remaining);
                out.add(new int[] {s.id(), part});
                remaining -= part;
            }
        }
        return remaining;
    }

    // ---------- Из другого инвентаря в слот / From the other inventory into the slot ----------

    private List<PlannedClick> planPull(InventoryView view, SlotView target, int amount) {
        List<PlannedClick> out = new ArrayList<>();
        if (target.isTakeOnly()) return out;
        Object key = target.isEmpty() ? null : target.itemKey();

        List<SlotView> sources = new ArrayList<>();
        List<SlotView> results = new ArrayList<>();
        for (SlotView s : view.slots()) {
            if (s.id() == target.id() || s.isPlayerSide() == target.isPlayerSide()) continue;
            if (s.isEmpty() || !s.canPickup()) continue;
            if (key == null) {
                if (target.roomFor(s.itemKey()) <= 0) continue;
                key = s.itemKey();
            } else if (!key.equals(s.itemKey())) {
                continue;
            }
            (s.isTakeOnly() ? results : sources).add(s);
        }
        if (key == null) return out;
        int room = target.roomFor(key);
        int want = Math.min(amount, room);
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
        // Одна партия результата за прокрутку, если она целиком помещается в слот (шаг колеса не важен)
        // One result batch per scroll, if it fits the slot whole (the wheel step does not matter)
        int roomLeft = room - (Math.min(amount, room) - want);
        for (SlotView r : results) {
            if (want > 0 && roomLeft >= r.count()) {
                out.add(new PlannedClick(r.id(), ClickCodes.LEFT, ClickKind.PICKUP));
                out.add(new PlannedClick(target.id(), ClickCodes.LEFT, ClickKind.PICKUP));
                break;
            }
        }
        return out;
    }
}