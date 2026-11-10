package org.mod.nova_ui.tweaks.logic;

// Настройки глазами логики; платформа подставляет конфиг / Settings as the logic sees them; the platform plugs in the config
public interface TweakSettings {
    // ПКМ с предметами на курсоре: по одному предмету в каждый слот / RMB with a cursor stack: one item per slot
    boolean rmbSpreadEnabled();

    // ЛКМ с предметами на курсоре: стек делится поровну между слотами, по которым провели
    // LMB with a cursor stack: the stack is split evenly between the slots swept over
    boolean spreadSplitEnabled();

    // ЛКМ-проведение со стеком, взятым этим же нажатием: собирает такие же предметы из слотов
    // LMB sweep with a stack picked up by the same press: collects the same items from slots
    boolean lmbCollectEnabled();

    // Shift + ЛКМ-проведение: Shift-клик по каждому слоту / Shift + LMB sweep: a Shift click on every slot
    boolean shiftDragEnabled();

    // Переносить ли слоты результата при Shift-проведении / Whether result slots are moved by a Shift sweep
    boolean shiftDragIncludesTakeOnly();

    boolean wheelEnabled();

    WheelDirection wheelDirection();

    // Предметов за один щелчок колеса / Items per wheel notch
    int wheelStep();

    // Двойной клик не должен собирать предметы из слота результата / A double click must not collect from a result slot
    boolean doubleClickGuardEnabled();
}