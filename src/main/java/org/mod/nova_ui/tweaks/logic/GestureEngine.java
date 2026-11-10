package org.mod.nova_ui.tweaks.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Жесты с зажатой кнопкой и защита двойного клика: раскладка по одному (ПКМ), деление поровну (ЛКМ),
// проведение с пустым курсором (сбор и Shift-перенос). Возврат true означает «событие поглощено»
// Held-button gestures and the double-click guard: one-per-slot (RMB), even split (LMB),
// the sweep with an empty cursor (collect and Shift move). Returning true means "event consumed"
public final class GestureEngine {
    private enum Mode { NONE, RMB_SPREAD, LMB_SPLIT, LMB_DRAG, SWALLOW }

    private static final List<Integer> NO_SLOTS = List.of();
    // Окно двойного клика как в ванили; должно совпадать, иначе мы и ваниль по-разному поймём «двойной»
    // The double click window as in vanilla; it must match, otherwise we and vanilla would disagree on what "double" is
    private static final long DOUBLE_CLICK_MS = 250L;

    private final TweakSettings settings;
    private final ClickSink sink;
    private final ClickBudget budget;

    private Mode mode = Mode.NONE;
    private int button = -1;
    private int startSlotId = -1;
    private Object startKey = null;
    // Слоты, уже обработанные за жест / Slots already handled during the gesture
    private final Set<Integer> visited = new HashSet<>();
    // Слоты деления поровну / Slots of the even split
    private final List<Integer> collected = new ArrayList<>();
    private final List<Integer> collectedView = Collections.unmodifiableList(collected);

    private int lastLeftSlot = -1;
    private long lastLeftTime = 0L;

    public GestureEngine(TweakSettings settings, ClickSink sink, ClickBudget budget) {
        this.settings = settings;
        this.sink = sink;
        this.budget = budget;
    }

    // Слоты для подсветки: пока идёт деление и выбрано хотя бы два слота (как в ванили) / Slots to highlight: while a split is in progress and at least two slots are picked (as in vanilla)
    public List<Integer> highlightedSlotIds() {
        return mode == Mode.LMB_SPLIT && collected.size() >= 2 ? collectedView : NO_SLOTS;
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

    // true: нажатие поглощено / true: the press is consumed
    public boolean press(InventoryView view, SlotView slot, int pressed, boolean shift, long nowMs) {
        reset();
        if (slot == null) {
            lastLeftSlot = -1;
            return false;
        }
        long dt = nowMs - lastLeftTime;
        boolean doubleClick = pressed == ClickCodes.LEFT && !shift && slot.id() == lastLeftSlot
                && dt >= 0L && dt < DOUBLE_CLICK_MS;
        if (pressed == ClickCodes.LEFT) {
            lastLeftSlot = slot.id();
            lastLeftTime = nowMs;
        }

        Object carried = view.carriedKey();
        if (pressed == ClickCodes.LEFT) {
            if (carried != null) {
                if (shift) return false;
                // Ванильный двойной клик собирает стек сам; мы лишь не даём собрать из результата
                // The vanilla double click collects the stack itself; we only stop collecting from a result
                if (doubleClick) {
                    if (settings.doubleClickGuardEnabled() && wouldCollectFromResult(view, carried)) {
                        lastLeftSlot = -1;
                        begin(Mode.SWALLOW, slot, null, ClickCodes.LEFT);
                        return true;
                    }
                    return false;
                }
                if (settings.spreadSplitEnabled()) {
                    begin(Mode.LMB_SPLIT, slot, carried, ClickCodes.LEFT);
                    if (slot.acceptsSpread(carried)) collected.add(slot.id());
                    return true;
                }
                return false;
            }
            // Курсор пуст: ваниль возьмёт стек этим нажатием, мы запоминаем начало жеста
            // The cursor is empty: vanilla picks the stack up with this press, we remember the start of the gesture
            if (settings.lmbCollectEnabled() || settings.shiftDragEnabled()) begin(Mode.LMB_DRAG, slot, null, ClickCodes.LEFT);
            return false;
        }

        if (pressed == ClickCodes.RIGHT && carried != null && settings.rmbSpreadEnabled()) {
            begin(Mode.RMB_SPREAD, slot, carried, ClickCodes.RIGHT);
            // Первый слот: обычный правый клик, как в ванили / The first slot: an ordinary right click, as in vanilla
            send(slot.id(), ClickCodes.RIGHT, ClickKind.PICKUP);
            return true;
        }
        return false;
    }

    // Перетаскивание никогда не поглощаем: ванильное перетаскивание в наших режимах ничего не делает
    // A drag is never consumed: the vanilla drag does nothing in our modes
    public boolean drag(InventoryView view, SlotView slot, int dragged, boolean shift) {
        if (mode == Mode.NONE || dragged != button) return false;
        if (mode == Mode.RMB_SPREAD) dragRmb(view, slot);
        else if (mode == Mode.LMB_SPLIT) dragSplit(view, slot);
        else if (mode == Mode.LMB_DRAG) dragLeft(view, slot, shift);
        return false;
    }

    // true: отпускание поглощено / true: the release is consumed
    public boolean release(InventoryView view, int released) {
        if (mode == Mode.NONE || released != button) return false;
        Mode finished = mode;
        if (finished == Mode.LMB_SPLIT) finishSplit(view);
        reset();
        // Нажатие ЛКМ-проведения с пустым курсором обработала ваниль, значит и отпускание её
        // Vanilla handled the press of the empty-cursor sweep, so it handles the release too
        return finished != Mode.LMB_DRAG;
    }

    // ---------- ПКМ: по одному предмету в каждый слот / RMB: one item per slot ----------

    private void dragRmb(InventoryView view, SlotView slot) {
        if (!sameCarried(view)) {
            swallow();
            return;
        }
        if (slot == null || visited.contains(slot.id())) return;
        if (slot.roomFor(startKey) > 0 && !send(slot.id(), ClickCodes.RIGHT, ClickKind.PICKUP)) return; // лимит: повторим позже / limit: retry later
        visited.add(slot.id());
    }

    // ---------- ЛКМ: деление поровну / LMB: even split ----------

    private void dragSplit(InventoryView view, SlotView slot) {
        if (!sameCarried(view)) {
            swallow();
            return;
        }
        if (slot == null) return;
        visited.add(slot.id());
        // Каждому слоту нужен хотя бы один предмет, поэтому слотов не больше, чем предметов в руке
        // Every slot needs at least one item, so there are no more slots than items in hand
        if (collected.size() < view.carriedCount() && !collected.contains(slot.id()) && slot.acceptsSpread(startKey)) {
            collected.add(slot.id());
        }
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

    // ---------- ЛКМ-проведение с пустым курсором в начале / LMB sweep that started with an empty cursor ----------

    private void dragLeft(InventoryView view, SlotView slot, boolean shift) {
        if (slot == null || visited.contains(slot.id())) return;
        if (slot.isEmpty()) {
            visited.add(slot.id());
            return;
        }
        Object carried = view.carriedKey();
        if (carried == null) {
            // Shift + ЛКМ без предмета: Shift-клик по каждому слоту / Shift + LMB without an item: a Shift click on every slot
            if (!settings.shiftDragEnabled() || !shift) return;
            shiftMove(slot);
            return;
        }
        if (!settings.lmbCollectEnabled() || !carried.equals(slot.itemKey())) {
            visited.add(slot.id());
            return;
        }
        if (shift) {
            shiftMove(slot);
        } else {
            collectInto(view, slot, carried);
        }
    }

    private void shiftMove(SlotView slot) {
        boolean movable = slot.canPickup() && (!slot.isTakeOnly() || settings.shiftDragIncludesTakeOnly());
        if (movable && !send(slot.id(), ClickCodes.LEFT, ClickKind.QUICK_MOVE)) return;
        visited.add(slot.id());
    }

    // Сливаем стек с курсора в слот и берём обратно: на курсоре оказывается сумма. Только если сумма помещается в стек
    // Merge the cursor stack into the slot and pick it up again: the cursor ends up with the sum. Only if the sum fits a stack
    private void collectInto(InventoryView view, SlotView slot, Object carried) {
        boolean fits = !slot.isTakeOnly()
                && view.carriedCount() + slot.count() <= view.carriedMaxStack()
                && slot.roomFor(carried) >= view.carriedCount();
        if (fits) {
            if (!budget.canSend(2)) return;
            sink.click(slot.id(), ClickCodes.LEFT, ClickKind.PICKUP);
            sink.click(slot.id(), ClickCodes.LEFT, ClickKind.PICKUP);
            budget.spend(2);
        }
        visited.add(slot.id());
    }

    // ---------- Двойной клик / Double click ----------

    // Забрал бы ванильный сбор в стек предмет из результата: тогда поглощаем клик целиком
    // Whether the vanilla collection would take an item from a result: then the whole click is consumed
    private boolean wouldCollectFromResult(InventoryView view, Object carried) {
        for (SlotView s : view.slots()) {
            if (s.pickupAllWouldTake(carried)) return true;
        }
        return false;
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