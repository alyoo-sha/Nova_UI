package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DispenserMenu;
import org.mod.nova_core.ui.minecraft.vanilla.NovaGridScreen;

public class NovaDispenserScreen extends NovaGridScreen<DispenserMenu> {
    public NovaDispenserScreen(DispenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 3, 3);
    }
}