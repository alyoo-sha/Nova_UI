package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.MapPreview;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.minecraft.McItem;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

public class NovaCartographyScreen extends NovaVanillaScreen<CartographyTableMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(210, 82);

    public NovaCartographyScreen(CartographyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 карта, 1 дополнение, 2 результат / Slots: 0 map, 1 additional item, 2 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{25, 17}, {25, 45}, {173, 35}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {2};
    }

    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new TextWidget(30, 35, UiText.literal("+")));
        panel.add(new ArrowWidget(54, 32));
        panel.add(new MapPreview(84, 8, 66, () -> McItem.of(menu.getSlot(0).getItem())));
    }
}