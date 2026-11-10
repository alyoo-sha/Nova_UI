package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.ProgressBar;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaBrewingScreen extends NovaVanillaScreen<BrewingStandMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(130, 82);

    public NovaBrewingScreen(BrewingStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0..2 бутылки, 3 ингредиент, 4 топливо / Slots: 0..2 bottles, 3 ingredient, 4 fuel
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{21, 57}, {57, 57}, {93, 57}, {57, 19}, {13, 19}};
    }

    @Override
    protected void buildMachine(Panel panel) {
        // Топливо: полоса под слотом; варка: полоса вниз от ингредиента к бутылкам
        // Fuel: a bar under the slot; brewing: a bar going down from the ingredient to the bottles
        ProgressBar fuel = new ProgressBar(12, 38, 18, 4, ProgressBar.Direction.RIGHT);
        fuel.setFillColor(0xFFFFB030);
        fuel.setProgressSource(() -> menu.getFuel() / 20f);
        panel.add(fuel);

        ProgressBar brew = new ProgressBar(62, 38, 6, 14, ProgressBar.Direction.DOWN);
        brew.setProgressSource(() -> {
            int ticks = menu.getBrewingTicks();
            return ticks > 0 ? 1f - ticks / 400f : 0f;
        });
        panel.add(brew);
    }
}