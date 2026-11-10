package org.mod.nova_ui.tweaks.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import org.mod.nova_ui.tweaks.logic.ClickKind;
import org.mod.nova_ui.tweaks.logic.ClickSink;

// Единственное место, где модуль что-то отправляет: клики идут через игровой режим клиента, слоты напрямую не меняются
// The only place where the module sends anything: clicks go through the client game mode, slots are never changed directly
final class McClickSink implements ClickSink {

    @Override
    public void click(int slotId, int button, ClickKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || gameMode == null) return;
        gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slotId, button, toType(kind), player);
    }

    private static ClickType toType(ClickKind kind) {
        return switch (kind) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
        };
    }
}