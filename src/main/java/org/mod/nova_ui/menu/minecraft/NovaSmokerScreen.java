package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmokerMenu;

// Пока как печь; здесь можно менять только коптильню / For now like the furnace; change only the smoker here
public class NovaSmokerScreen extends NovaFurnaceScreen {
    public NovaSmokerScreen(SmokerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}