package org.mod.nova_ui.menu.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.mod.nova_ui.menu.NovaWorkbenchMenu;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.container.NovaMachineScreen;

public class NovaWorkbenchScreen extends NovaMachineScreen<NovaWorkbenchMenu> {
    public NovaWorkbenchScreen(NovaWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // Координаты в панели механизма: сетка (рамки) x 8..62, y 18..72; результат (рамка) x 96..122, y 32..58
    // Coordinates in the machine panel: the grid (frames) x 8..62, y 18..72; the result (frame) x 96..122, y 32..58
    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new ArrowWidget(68, 38));

        ButtonWidget clear = new ButtonWidget(86, 2, 36, 14,
                McText.of(Component.translatable("menu.nova_core.workbench.clear")), () -> {
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) gameMode.handleInventoryButtonClick(menu.containerId, NovaWorkbenchMenu.BUTTON_CLEAR);
        });
        panel.add(clear);
    }
}