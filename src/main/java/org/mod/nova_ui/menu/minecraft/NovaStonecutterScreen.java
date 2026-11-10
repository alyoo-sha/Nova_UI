package org.mod.nova_ui.menu.minecraft;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ItemCell;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.VirtualGrid;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.minecraft.McItem;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;
import org.mod.nova_core.ui.state.Signal;

import java.util.List;

public class NovaStonecutterScreen extends NovaVanillaScreen<StonecutterMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(190, 80);

    // Состояние живёт в экране, а не в buildMachine: сборка повторяется при ресайзе
    // State lives in the screen, not in buildMachine: the build repeats on resize
    private final ListModel<StonecutterRecipe> recipes = new ListModel<>();
    private final Signal<Integer> selected = Signal.of(-1);
    private List<StonecutterRecipe> shown = null;
    // Выбор, отправленный на сервер и ещё не подтверждённый меню / A choice sent to the server and not yet confirmed by the menu
    private int pending = -1;
    private int pendingTicks = 0;

    public NovaStonecutterScreen(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 вход, 1 результат / Slots: 0 input, 1 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{9, 35}, {163, 37}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {1};
    }

    @Override
    protected void buildMachine(Panel panel) {
        ItemAdapter<StonecutterRecipe> adapter = ItemAdapter.<StonecutterRecipe, ItemCell>of(ItemCell::new,
                (cell, recipe, index, sel) -> cell.set(
                        McItem.of(recipe.getResultItem(minecraft.level.registryAccess())), sel));
        VirtualGrid<StonecutterRecipe> grid = new VirtualGrid<>(34, 18, 120, 54, recipes, adapter, selected);
        grid.setCellSize(18, 18);
        panel.add(grid);

        shown = null;
        panel.whileMounted(() -> panel.scope().subscribe(selected, this::onUserSelect));
    }

    // Выбор пользователя уходит на сервер, как в ванильном камнерезе / The user's choice goes to the server, as in the vanilla stonecutter
    private void onUserSelect(Integer index) {
        if (index == null || index < 0 || index >= menu.getNumRecipes() || index == menu.getSelectedRecipeIndex()
                || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        pending = index;
        pendingTicks = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        List<StonecutterRecipe> now = menu.getRecipes();
        if (now != shown) {
            shown = now;
            recipes.setAll(now);
        }
        if (pending >= 0) {
            if (menu.getSelectedRecipeIndex() == pending || ++pendingTicks > 10) pending = -1;
            else return;
        }
        int actual = menu.getSelectedRecipeIndex();
        if (selected.peek() != actual) selected.set(actual);
    }
}