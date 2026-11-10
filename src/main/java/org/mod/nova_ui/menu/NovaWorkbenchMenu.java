package org.mod.nova_ui.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.menu.NovaMachineMenu;

import java.util.Optional;

public class NovaWorkbenchMenu extends NovaMachineMenu {
    // Панель механизма 130x80; размер можно менять, экран подстроится сам / The machine panel is 130x80; the size can be changed, the screen adapts itself
    public static final MachineLayout LAYOUT = new MachineLayout(130, 80);
    public static final int BUTTON_CLEAR = 0;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player player;

    public NovaWorkbenchMenu(int containerId, Inventory inventory) {
        super(NovaCoreMenus.WORKBENCH.get(), containerId, LAYOUT);
        this.player = inventory.player;

        // Слот 0 = результат (большая рамка), 1..9 = сетка 3x3 / Slot 0 = the result (large frame), 1..9 = the 3x3 grid
        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, LAYOUT.machineX(101), LAYOUT.machineY(37)));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3,
                        LAYOUT.machineX(9 + col * 18), LAYOUT.machineY(19 + row * 18)));
            }
        }
        addPlayerInventory(inventory);
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        if (container == craftSlots) updateResult();
    }

    // Та же логика, что у ванильного верстака / The same logic as the vanilla crafting table
    private void updateResult() {
        Level level = player.level();
        if (level.isClientSide) return;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots, level);
        if (recipe.isPresent()) {
            CraftingRecipe found = recipe.get();
            if (resultSlots.setRecipeUsed(level, serverPlayer, found)) {
                ItemStack assembled = found.assemble(craftSlots, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) result = assembled;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    // Кнопка «Clear» из экрана: вернуть предметы сетки игроку (выполняется на сервере)
    // The "Clear" button from the screen: return the grid items to the player (runs on the server)
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_CLEAR) return false;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
        }
        slotsChanged(craftSlots);
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, craftSlots); // предметы из сетки возвращаются игроку / grid items go back to the player
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}