package org.mod.nova_ui.tweaks.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Жесты с зажатой кнопкой и двойной клик: раскладка по одному (ПКМ), деление поровну (ЛКМ),
// перенос Shift-проведением, сбор в стек. Возврат true означает «событие поглощено»
// Held-button gestures and the double click: one-per-slot (RMB), even split (LMB),
// Shift-sweep move, stack collection. Returning true means "event consumed"
public final class GestureEngine {
    private enum Mode { NONE, SPREAD_ONE, SPREAD_SPLIT, SHIFT_DRAG, SWALLOW }

    private final TweakSettings settings;
    private final ClickSink sink;
    private final ClickBudget budget;

    private Mode mode = Mode.NONE;
    private int button = -1;
    private int startSlotId = -1;
    private Object startKey = null;
    // Слоты, уже обработанные за жест / Slots already handled during the gesture
    private final Set<Integer> visited = new HashSet<>();
    // Слоты раскладки поровну / Slots of the even split
    private final List<Integer> collected = new ArrayList<>();
    private final List<Integer> collectedView = Collections.unmodifiableList(collected);

    private int lastLeftSlot = -1;
    private long lastLeftTime = 0L;

    public GestureEngine(TweakSettings settings, ClickSink sink, ClickBudget budget) {
        this.settings = settings;
        this.sink = sink;
        this.budget = budget;
    }

    // Слоты, выбранные для деления поровну; нужны для подсветки / Slots picked for the even split; used for highlighting
    public List<Integer> collectedSlotIds() {
        return collectedView;
    }

    public boolean isSplitting() {
        return mode == Mode.SPREAD_SPLIT;
    }

    // Сбрасывает жест, но помнит последний клик для двойного / Resets the gesture but remembers the last click for the double click
    public void reset() {
        mode = Mode.NONE;
        button = -1;
        startSlotId = -1;
        startKey = null;
        visited.clear();
        collected.clear();
    }

    public void resetAll() {
        reset();
        lastLeftSlot = -1;
        lastLeftTime = 0L;
    }

    public boolean press(InventoryView view, SlotView slot, int pressed, boolean shift, long nowMs) {
        reset();
        if (slot == null) {
            lastLeftSlot = -1;
            return false;
        }
        long dt = nowMs - lastLeftTime;
        boolean doubleClick = pressed == ClickCodes.LEFT && !shift && slot.id() == lastLeftSlot
                && dt >= 0L && dt <= settings.doubleClickWindowMillis();
        if (pressed == ClickCodes.LEFT) {
            lastLeftSlot = slot.id();
            lastLeftTime = nowMs;
        }

        Object carried = view.carriedKey();
        if (carried != null) {
            if (shift) return false;
            if (doubleClick && settings.doubleClickEnabled()) {
                lastLeftSlot = -1;
                // Нажатие поглощено, значит и отпускание тоже: иначе ваниль выполнит свой клик
                // The press is consumed, so the release is consumed too: otherwise vanilla performs its own click
                begin(Mode.SWALLOW, slot, null, ClickCodes.LEFT);
                collectStack(view, slot, carried);
                return true;
            }
            if (pressed == ClickCodes.RIGHT && settings.spreadOneEnabled()) return beginSpreadOne(slot, carried);
            if (pressed == ClickCodes.LEFT && settings.spreadSplitEnabled()) return beginSpreadSplit(slot, carried);
            return false;
        }
        if (shift && pressed == ClickCodes.LEFT && settings.shiftDragEnabled()) {
            // Первый слот обработает ванильный Shift+клик, мы лишь запоминаем его / Vanilla Shift+click handles the first slot, we only remember it
            begin(Mode.SHIFT_DRAG, slot, null, ClickCodes.LEFT);
        }
        return false;
    }

    public boolean drag(InventoryView view, SlotView slot, int dragged, boolean shift) {
        if (mode == Mode.NONE || dragged != button) return false;
        return switch (mode) {
            case SPREAD_ONE -> dragSpreadOne(view, slot);
            case SPREAD_SPLIT -> dragSpreadSplit(view, slot);
            case SHIFT_DRAG -> dragShift(view, slot, shift);
            case SWALLOW -> true;
            default -> false;
        };
    }

    public boolean release(InventoryView view, int released) {
        if (mode == Mode.NONE || released != button) return false;
        Mode finished = mode;
        if (finished == Mode.SPREAD_SPLIT) finishSplit(view);
        reset();
        // Нажатие Shift-проведения обработала ваниль, значит и отпускание её / Vanilla handled the Shift-sweep press, so it handles the release too
        return finished != Mode.SHIFT_DRAG;
    }

    // ---------- ПКМ: по одному предмету в каждый слот / RMB: one item per slot ----------

    private boolean beginSpreadOne(SlotView slot, Object carried) {
        begin(Mode.SPREAD_ONE, slot, carried, ClickCodes.RIGHT);
        // Первый слот: обычный правый клик, как в ванили / The first slot: an ordinary right click, as in vanilla
        send(slot.id(), ClickCodes.RIGHT, ClickKind.PICKUP);
        return true;
    }

    private boolean dragSpreadOne(InventoryView view, SlotView slot) {
        if (!sameCarried(view)) {
            swallow();
            return true;
        }
        if (slot == null || visited.contains(slot.id())) return true;
        if (slot.roomFor(startKey) > 0) {
            if (!send(slot.id(), ClickCodes.RIGHT, ClickKind.PICKUP)) return true; // лимит: повторим на следующем событии / limit: retry on the next event
        }
        visited.add(slot.id());
        return true;
    }

    // ---------- ЛКМ: деление поровну / LMB: even split ----------

    private boolean beginSpreadSplit(SlotView slot, Object carried) {
        begin(Mode.SPREAD_SPLIT, slot, carried, ClickCodes.LEFT);
        if (slot.acceptsSpread(carried)) collected.add(slot.id());
        return true;
    }

    private boolean dragSpreadSplit(InventoryView view, SlotView slot) {
        if (!sameCarried(view)) {
            swallow();
            return true;
        }
        if (slot == null) return true;
        visited.add(slot.id());
        // Каждому слоту нужен хотя бы один предмет, поэтому слотов не больше, чем предметов в руке
        // Every slot needs at least one item, so there are no more slots than items in hand
        if (collected.size() < view.carriedCount() && !collected.contains(slot.id()) && slot.acceptsSpread(startKey)) {
            collected.add(slot.id());
        }
        return true;
    }

    private void finishSplit(InventoryView view) {
        if (!sameCarried(view)) return;
        if (visited.size() <= 1) {
            // Курсор не покидал слот: обычный левый клик / The cursor never left the slot: an ordinary left click
            send(startSlotId, ClickCodes.LEFT, ClickKind.PICKUP);
        } else if (collected.size() >= 2) {
            int total = collected.size() + 2;
            if (!budget.canSend(total)) return;
            sink.click(ClickCodes.OUTSIDE, ClickCodes.quickCraftMask(ClickCodes.STAGE_START, ClickCodes.DRAG_SPLIT), ClickKind.QUICK_CRAFT);
            for (int id : collected) {
                sink.click(id, ClickCodes.quickCraftMask(ClickCodes.STAGE_ADD, ClickCodes.DRAG_SPLIT), ClickKind.QUICK_CRAFT);
            }
            sink.click(ClickCodes.OUTSIDE, ClickCodes.quickCraftMask(ClickCodes.STAGE_END, ClickCodes.DRAG_SPLIT), ClickKind.QUICK_CRAFT);
            budget.spend(total);
        } else if (collected.size() == 1) {
            send(collected.get(0), ClickCodes.LEFT, ClickKind.PICKUP);
        }
    }

    // ---------- Shift + ЛКМ: перенос проведением / Shift + LMB: sweep move ----------

    private boolean dragShift(InventoryView view, SlotView slot, boolean shift) {
        if (view.carriedKey() != null || !shift) {
            reset();
            return false;
        }
        if (slot == null || visited.contains(slot.id())) return true;
        boolean movable = !slot.isEmpty() && slot.canPickup()
                && (!slot.isTakeOnly() || settings.shiftDragIncludesTakeOnly());
        if (movable && !send(slot.id(), ClickCodes.LEFT, ClickKind.QUICK_MOVE)) return true;
        visited.add(slot.id());
        return true;
    }

    // ---------- Двойной клик: сбор в стек / Double click: collect into a stack ----------

    private void collectStack(InventoryView view, SlotView slot, Object carried) {
        // Если подходящий предмет лежит в слоте результата, сбор мог бы забрать его оттуда: не отправляем ничего
        // If a matching item sits in a result slot, the collection could take it from there: send nothing
        for (SlotView s : view.slots()) {
            if (s.isTakeOnly() && !s.isEmpty() && carried.equals(s.itemKey())) return;
        }
        send(slot.id(), ClickCodes.LEFT, ClickKind.PICKUP_ALL);
    }

    // ---------- Служебное / Helpers ----------

    private void begin(Mode newMode, SlotView slot, Object key, int btn) {
        mode = newMode;
        button = btn;
        startSlotId = slot.id();
        startKey = key;
        visited.add(slot.id());
    }

    // Жест оборван (предмет на курсоре изменился), но нажатие уже поглощено: гасим остаток жеста вместе с отпусканием
    // The gesture is broken off (the cursor item changed) but the press is already consumed: swallow the rest including the release
    private void swallow() {
        mode = Mode.SWALLOW;
        startKey = null;
        visited.clear();
        collected.clear();
    }

    // Курсор всё ещё держит тот же предмет, с которым начался жест / The cursor still holds the item the gesture started with
    private boolean sameCarried(InventoryView view) {
        Object now = view.carriedKey();
        return now != null && now.equals(startKey) && view.carriedCount() > 0;
    }

    private boolean send(int slotId, int btn, ClickKind kind) {
        if (!budget.canSend(1)) return false;
        sink.click(slotId, btn, kind);
        budget.spend(1);
        return true;
    }
}