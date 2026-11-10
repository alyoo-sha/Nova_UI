package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaGrindstoneScreen extends NovaVanillaScreen<GrindstoneMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(130, 72);

    public NovaGrindstoneScreen(GrindstoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 и 1 входы, 2 результат / Slots: 0 and 1 inputs, 2 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{25, 17}, {25, 45}, {93, 31}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {2};
    }

    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new ArrowWidget(54, 32));
    }
}