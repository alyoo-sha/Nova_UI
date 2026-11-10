package org.mod.nova_ui.tweaks.logic;

// Константы протокола кликов по контейнеру / Constants of the container click protocol
public final class ClickCodes {
    public static final int OUTSIDE = -999;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    // Этапы и типы раскладки перетаскиванием / Stages and types of drag distribution
    public static final int STAGE_START = 0;
    public static final int STAGE_ADD = 1;
    public static final int STAGE_END = 2;
    public static final int DRAG_SPLIT = 0;
    public static final int DRAG_ONE = 1;

    private ClickCodes() {}

    // Кодирует этап и тип в одно число кнопки / Packs a stage and a type into a single button value
    public static int quickCraftMask(int stage, int type) {
        return (stage & 3) | ((type & 3) << 2);
    }
}