package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.ProgressBar;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaFurnaceScreen extends NovaVanillaScreen<AbstractFurnaceMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(130, 80);

    public NovaFurnaceScreen(AbstractFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    @Override
    protected void buildExtras(Widget root) {
        installRecipeBook(root, menu);
    }

    // Слоты: 0 исходник, 1 топливо, 2 результат / Slots: 0 input, 1 fuel, 2 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{19, 19}, {19, 55}, {89, 37}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {2};
    }

    @Override
    protected void buildMachine(Panel panel) {
        ProgressBar flame = new ProgressBar(20, 39, 14, 14, ProgressBar.Direction.UP);
        flame.setFillColor(0xFFFF9A2E);
        flame.setProgressSource(() -> menu.isLit() ? menu.getLitProgress() / 13f : 0f);
        panel.add(flame);

        ArrowWidget arrow = new ArrowWidget(48, 38);
        arrow.setProgressSource(() -> menu.getBurnProgress() / 24f);
        panel.add(arrow);
    }
}