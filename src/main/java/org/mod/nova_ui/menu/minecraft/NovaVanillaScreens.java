package org.mod.nova_ui.menu.minecraft;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mod.nova_core.Nova_core;
import org.mod.nova_core.ui.minecraft.container.NovaContainerScreen;
import org.mod.nova_core.ui.minecraft.container.SlotAccess;
import org.mod.nova_ui.Nova_ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Подмена ванильных экранов на Nova Core. Срабатывает при любом открытии: правый клик по блоку, команда, другой мод.
// Типы, которых нет в таблице, остаются ванильными. Таблицу можно менять: register и unregister
// Replaces vanilla screens with Nova Core ones. Triggers on any opening: a right click on a block, a command, another mod.
// Types missing from the table stay vanilla. The table can be changed: register and unregister
@Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NovaVanillaScreens {
    @FunctionalInterface
    public interface Factory {
        Screen create(AbstractContainerMenu menu, Inventory inventory, Component title);
    }

    private static final Map<MenuType<?>, Factory> FACTORIES = new HashMap<>();
    private static boolean enabled = true;

    static {
        for (MenuType<?> type : List.of(MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
                MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6)) {
            FACTORIES.put(type, (m, inv, t) -> new NovaChestScreen((ChestMenu) m, inv, t));
        }
        FACTORIES.put(MenuType.SHULKER_BOX, (m, inv, t) -> new NovaShulkerBoxScreen((ShulkerBoxMenu) m, inv, t));
        FACTORIES.put(MenuType.GENERIC_3x3, (m, inv, t) -> new NovaDispenserScreen((DispenserMenu) m, inv, t));
        FACTORIES.put(MenuType.HOPPER, (m, inv, t) -> new NovaHopperScreen((HopperMenu) m, inv, t));
        FACTORIES.put(MenuType.FURNACE, (m, inv, t) -> new NovaFurnaceScreen((AbstractFurnaceMenu) m, inv, t));
        FACTORIES.put(MenuType.BLAST_FURNACE, (m, inv, t) -> new NovaBlastFurnaceScreen((BlastFurnaceMenu) m, inv, t));
        FACTORIES.put(MenuType.SMOKER, (m, inv, t) -> new NovaSmokerScreen((SmokerMenu) m, inv, t));
        FACTORIES.put(MenuType.CRAFTING, (m, inv, t) -> new NovaCraftingScreen((CraftingMenu) m, inv, t));
        FACTORIES.put(MenuType.BREWING_STAND, (m, inv, t) -> new NovaBrewingScreen((BrewingStandMenu) m, inv, t));
        FACTORIES.put(MenuType.GRINDSTONE, (m, inv, t) -> new NovaGrindstoneScreen((GrindstoneMenu) m, inv, t));
        FACTORIES.put(MenuType.CARTOGRAPHY_TABLE, (m, inv, t) -> new NovaCartographyScreen((CartographyTableMenu) m, inv, t));
        FACTORIES.put(MenuType.SMITHING, (m, inv, t) -> new NovaSmithingScreen((SmithingMenu) m, inv, t));
        FACTORIES.put(MenuType.ANVIL, (m, inv, t) -> new NovaAnvilScreen((AnvilMenu) m, inv, t));
        FACTORIES.put(MenuType.STONECUTTER, (m, inv, t) -> new NovaStonecutterScreen((StonecutterMenu) m, inv, t));
        FACTORIES.put(MenuType.ENCHANTMENT, (m, inv, t) -> new NovaEnchantmentScreen((EnchantmentMenu) m, inv, t));
        FACTORIES.put(MenuType.LOOM, (m, inv, t) -> new NovaLoomScreen((LoomMenu) m, inv, t));
        FACTORIES.put(MenuType.BEACON, (m, inv, t) -> new NovaBeaconScreen((BeaconMenu) m, inv, t));
    }

    private NovaVanillaScreens() {}

    // Заменить или добавить экран для типа меню; подойдёт и тип другого мода
    // Replace or add a screen for a menu type; a type from another mod works too
    public static void register(MenuType<?> type, Factory factory) {
        FACTORIES.put(type, factory);
    }

    // Вернуть ванильный экран для типа меню / Bring the vanilla screen back for a menu type
    public static void unregister(MenuType<?> type) {
        FACTORIES.remove(type);
    }

    // Включить или выключить подмену, например для сравнения с ванильными экранами / Turn the replacement on or off, e.g. to compare with vanilla screens
    public static void setEnabled(boolean value) { enabled = value; }

    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!enabled || !SlotAccess.isAvailable()) return;
        Screen next = event.getNewScreen();
        Minecraft mc = Minecraft.getInstance();
        if (next == null || mc.player == null) return;

        // Инвентарь выживания (E). В креативе ванильный InventoryScreen сам открывает креатив-экран, поэтому не трогаем
        // The survival inventory (E). In creative the vanilla InventoryScreen opens the creative screen itself, so we leave it alone
        if (next instanceof InventoryScreen) {
            if (mc.gameMode != null && mc.gameMode.hasInfiniteItems()) return;
            LocalPlayer player = mc.player;
            event.setNewScreen(new NovaInventoryScreen(player.inventoryMenu, player.getInventory(),
                    Component.translatable("container.crafting")));
            return;
        }

        if (next instanceof CreativeModeInventoryScreen && !(next instanceof NovaCreativeScreen)) {
            try {
                event.setNewScreen(new NovaCreativeScreen(mc.player, mc.player.connection.enabledFeatures(),
                        mc.options.operatorItemsTab().get()));
            } catch (Throwable t) {
                LogUtils.getLogger().error("Nova Core creative screen failed, keeping the vanilla one", t);
            }
            return;
        }

        if (!(next instanceof AbstractContainerScreen<?> screen) || screen instanceof NovaContainerScreen<?>) return;

        AbstractContainerMenu menu = screen.getMenu();
        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (UnsupportedOperationException ex) {
            return; // меню без типа / a menu without a type
        }

        Factory factory = FACTORIES.get(type);
        if (factory == null) return;
        event.setNewScreen(factory.create(menu, mc.player.getInventory(), screen.getTitle()));
    }
}