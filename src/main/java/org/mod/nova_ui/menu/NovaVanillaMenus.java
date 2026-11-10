package org.mod.nova_ui.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Открывает ванильные меню без блока. У меню с доступом к миру переопределён stillValid, иначе оно
// закрылось бы сразу: рядом нет настоящего блока
// Opens vanilla menus without a block. Menus with world access override stillValid, otherwise they
// would close right away: there is no real block nearby
public final class NovaVanillaMenus {
    @FunctionalInterface
    private interface Builder {
        AbstractContainerMenu build(int id, Inventory inventory, ContainerLevelAccess access);
    }

    private record Entry(Component title, Builder builder) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        put("chest", "container.chest", (id, inv, a) -> ChestMenu.threeRows(id, inv, new SimpleContainer(27)));
        put("barrel", "container.barrel", (id, inv, a) -> ChestMenu.threeRows(id, inv, new SimpleContainer(27)));
        put("shulker", "container.shulkerBox", (id, inv, a) -> new ShulkerBoxMenu(id, inv));
        put("dispenser", "container.dispenser", (id, inv, a) -> new DispenserMenu(id, inv));
        put("dropper", "container.dropper", (id, inv, a) -> new DispenserMenu(id, inv));
        put("hopper", "container.hopper", (id, inv, a) -> new HopperMenu(id, inv));
        put("furnace", "container.furnace", (id, inv, a) -> new FurnaceMenu(id, inv));
        put("blast_furnace", "container.blast_furnace", (id, inv, a) -> new BlastFurnaceMenu(id, inv));
        put("smoker", "container.smoker", (id, inv, a) -> new SmokerMenu(id, inv));
        put("brewing", "container.brewing", (id, inv, a) -> new BrewingStandMenu(id, inv));
        put("crafting", "container.crafting", (id, inv, a) -> new CraftingMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("grindstone", "container.grindstone_title", (id, inv, a) -> new GrindstoneMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("smithing", "container.upgrade", (id, inv, a) -> new SmithingMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("cartography", "container.cartography_table", (id, inv, a) -> new CartographyTableMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("anvil", "container.repair", (id, inv, a) -> new AnvilMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        // Эти четыре пока открываются как ванильные / These four still open as vanilla
        put("enchanting", "container.enchant", (id, inv, a) -> new EnchantmentMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("stonecutter", "container.stonecutter", (id, inv, a) -> new StonecutterMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("loom", "container.loom", (id, inv, a) -> new LoomMenu(id, inv, a) {
            @Override
            public boolean stillValid(Player player) { return true; }
        });
        put("beacon", "block.minecraft.beacon", (id, inv, a) -> new BeaconMenu(id, inv));
    }

    private NovaVanillaMenus() {}

    private static void put(String name, String titleKey, Builder builder) {
        ENTRIES.put(name, new Entry(Component.translatable(titleKey), builder));
    }

    public static final List<String> NAMES = List.copyOf(ENTRIES.keySet());

    // false = такого меню нет / false = there is no such menu
    public static boolean open(ServerPlayer player, String name) {
        Entry entry = ENTRIES.get(name);
        if (entry == null) return false;
        ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), player.blockPosition());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> entry.builder().build(id, inventory, access), entry.title()));
        return true;
    }
}