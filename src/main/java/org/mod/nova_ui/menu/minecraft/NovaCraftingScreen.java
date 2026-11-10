package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaCraftingScreen extends NovaVanillaScreen<CraftingMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(130, 80);

    public NovaCraftingScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    @Override
    protected void buildExtras(Widget root) {
        installRecipeBook(root, menu);
    }

    // Слоты: 0 результат, 1..9 сетка 3x3 / Slots: 0 result, 1..9 the 3x3 grid
    @Override
    protected int[][] machineSlotPositions() {
        int[][] positions = new int[10][];
        positions[0] = new int[] {101, 37};
        for (int i = 0; i < 9; i++) positions[i + 1] = new int[] {9 + (i % 3) * 18, 19 + (i / 3) * 18};
        return positions;
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {0};
    }

    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new ArrowWidget(68, 38));
    }
}