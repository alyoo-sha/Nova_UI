package org.mod.nova_ui.tweaks.logic;

// Настройки глазами логики; платформа подставляет конфиг / Settings as the logic sees them; the platform plugs in the config
public interface TweakSettings {
    boolean spreadOneEnabled();

    boolean spreadSplitEnabled();

    boolean shiftDragEnabled();

    // Переносить ли слоты результата при Shift-проведении / Whether result slots are moved by a Shift sweep
    boolean shiftDragIncludesTakeOnly();

    boolean wheelEnabled();

    WheelDirection wheelDirection();

    // Предметов за один щелчок колеса / Items per wheel notch
    int wheelStep();

    boolean doubleClickEnabled();

    int doubleClickWindowMillis();
}