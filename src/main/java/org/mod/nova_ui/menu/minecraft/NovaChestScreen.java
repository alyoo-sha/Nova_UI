package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.mod.nova_core.ui.minecraft.vanilla.NovaGridScreen;

public class NovaChestScreen extends NovaGridScreen<ChestMenu> {
    public NovaChestScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 9, menu.getRowCount());
    }
}