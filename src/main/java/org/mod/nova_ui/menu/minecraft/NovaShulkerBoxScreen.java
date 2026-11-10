package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.mod.nova_core.ui.minecraft.vanilla.NovaGridScreen;

public class NovaShulkerBoxScreen extends NovaGridScreen<ShulkerBoxMenu> {
    public NovaShulkerBoxScreen(ShulkerBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 9, 3);
    }
}