package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BlastFurnaceMenu;

// Пока как печь; здесь можно менять только плавильную печь / For now like the furnace; change only the blast furnace here
public class NovaBlastFurnaceScreen extends NovaFurnaceScreen {
    public NovaBlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}