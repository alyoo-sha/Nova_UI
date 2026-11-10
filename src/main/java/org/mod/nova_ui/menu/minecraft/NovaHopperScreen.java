package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HopperMenu;
import org.mod.nova_core.ui.minecraft.vanilla.NovaGridScreen;

public class NovaHopperScreen extends NovaGridScreen<HopperMenu> {
    public NovaHopperScreen(HopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 5, 1);
    }
}