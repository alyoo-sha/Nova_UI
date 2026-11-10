package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaSmithingScreen extends NovaVanillaScreen<SmithingMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(154, 46);

    public NovaSmithingScreen(SmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 шаблон, 1 основа, 2 добавка, 3 результат / Slots: 0 template, 1 base, 2 addition, 3 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{9, 17}, {35, 17}, {61, 17}, {121, 17}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {3};
    }

    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new ArrowWidget(88, 18));
    }
}