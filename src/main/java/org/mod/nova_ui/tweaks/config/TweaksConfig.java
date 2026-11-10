package org.mod.nova_ui.tweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.mod.nova_ui.tweaks.logic.TweakSettings;
import org.mod.nova_ui.tweaks.logic.WheelDirection;

import java.util.List;

// Клиентский конфиг твиков. Ключи перевода: nova_ui.tweaks.config.<имя>
// The tweaks client config. Translation keys: nova_ui.tweaks.config.<name>
public final class TweaksConfig {
    private static final String KEY = "nova_ui.tweaks.config.";
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Главный выключатель модуля / Master switch of the module")
            .translation(KEY + "enabled")
            .define("enabled", true);

    public static final ForgeConfigSpec.BooleanValue RMB_SPREAD = BUILDER
            .comment("ПКМ с предметами на курсоре: по одному предмету в каждый слот / RMB with a cursor stack: one item per slot")
            .translation(KEY + "rmb_spread")
            .define("rmbSpread", true);

    public static final ForgeConfigSpec.BooleanValue SPREAD_SPLIT = BUILDER
            .comment("ЛКМ с предметами на курсоре: деление поровну между слотами / LMB with a cursor stack: even split between slots")
            .translation(KEY + "spread_split")
            .define("spreadSplit", true);

    public static final ForgeConfigSpec.BooleanValue LMB_COLLECT = BUILDER
            .comment("ЛКМ-проведение со стеком, взятым этим же нажатием: собирает такие же предметы (с Shift переносит их) / LMB sweep with a stack picked up by the same press: collects the same items (moves them with Shift)")
            .translation(KEY + "lmb_collect")
            .define("lmbCollect", true);

    public static final ForgeConfigSpec.BooleanValue SHIFT_DRAG = BUILDER
            .comment("Shift + ЛКМ без предмета на курсоре: перенос каждого слота под курсором / Shift + LMB with an empty cursor: move every slot under the cursor")
            .translation(KEY + "shift_drag")
            .define("shiftDrag", true);

    public static final ForgeConfigSpec.BooleanValue SHIFT_DRAG_RESULTS = BUILDER
            .comment("Переносить слоты результата при Shift-проведении / Also move result slots during a Shift sweep")
            .translation(KEY + "shift_drag_results")
            .define("shiftDragResults", false);

    public static final ForgeConfigSpec.BooleanValue WHEEL = BUILDER
            .comment("Колесо над слотом переносит предметы / The wheel over a slot moves items")
            .translation(KEY + "wheel")
            .define("wheel", true);

    public static final ForgeConfigSpec.EnumValue<WheelDirection> WHEEL_DIRECTION = BUILDER
            .comment("Какая прокрутка переносит из слота в другой инвентарь: DOWN_PUSH или UP_PUSH / Which direction moves items out of the slot: DOWN_PUSH or UP_PUSH")
            .translation(KEY + "wheel_direction")
            .defineEnum("wheelDirection", WheelDirection.DOWN_PUSH);

    public static final ForgeConfigSpec.IntValue WHEEL_STEP = BUILDER
            .comment("Предметов за один щелчок колеса / Items per wheel notch")
            .translation(KEY + "wheel_step")
            .defineInRange("wheelStep", 1, 1, 64);

    public static final ForgeConfigSpec.BooleanValue DOUBLE_CLICK_GUARD = BUILDER
            .comment("Двойной клик не собирает предметы из слота результата / A double click does not collect from a result slot")
            .translation(KEY + "double_click_guard")
            .define("doubleClickGuard", true);

    public static final ForgeConfigSpec.BooleanValue CONTAINERS_ONLY = BUILDER
            .comment("Только контейнеры: не работать в собственном инвентаре игрока / Containers only: do not work in the player's own inventory")
            .translation(KEY + "containers_only")
            .define("containersOnly", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_SCREENS = BUILDER
            .comment("Исключённые экраны: полное имя класса или префикс со звёздочкой, например com.example.gui.* / Excluded screens: a full class name or a prefix with a star, e.g. com.example.gui.*")
            .translation(KEY + "excluded_screens")
            .defineListAllowEmpty("excludedScreens", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // Настройки для логики; читают значения конфига при каждом обращении / Settings for the logic; read the config values on every access
    public static final TweakSettings SETTINGS = new Settings();

    private TweaksConfig() {}

    // Исключён ли экран с таким именем класса / Whether a screen with this class name is excluded
    public static boolean isScreenExcluded(String className) {
        for (String entry : EXCLUDED_SCREENS.get()) {
            if (entry.endsWith("*") ? className.startsWith(entry.substring(0, entry.length() - 1)) : className.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private static final class Settings implements TweakSettings {
        @Override public boolean rmbSpreadEnabled() { return ENABLED.get() && RMB_SPREAD.get(); }
        @Override public boolean spreadSplitEnabled() { return ENABLED.get() && SPREAD_SPLIT.get(); }
        @Override public boolean lmbCollectEnabled() { return ENABLED.get() && LMB_COLLECT.get(); }
        @Override public boolean shiftDragEnabled() { return ENABLED.get() && SHIFT_DRAG.get(); }
        @Override public boolean shiftDragIncludesTakeOnly() { return SHIFT_DRAG_RESULTS.get(); }
        @Override public boolean wheelEnabled() { return ENABLED.get() && WHEEL.get(); }
        @Override public WheelDirection wheelDirection() { return WHEEL_DIRECTION.get(); }
        @Override public int wheelStep() { return WHEEL_STEP.get(); }
        @Override public boolean doubleClickGuardEnabled() { return ENABLED.get() && DOUBLE_CLICK_GUARD.get(); }
    }
}