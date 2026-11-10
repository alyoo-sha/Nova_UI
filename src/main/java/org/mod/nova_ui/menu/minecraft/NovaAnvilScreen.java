package org.mod.nova_ui.menu.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;
import org.mod.nova_core.ui.state.Signal;

public class NovaAnvilScreen extends NovaVanillaScreen<AnvilMenu> implements ContainerListener {
    public static final MachineLayout LAYOUT = new MachineLayout(150, 84);

    // Состояние живёт в экране, а не в buildMachine: сборка повторяется при ресайзе
    // State lives in the screen, not in buildMachine: the build repeats on resize
    private final Signal<String> name = Signal.of("");
    private TextField nameField = null;
    private TextWidget costText = null;
    private boolean listening = false;
    private int lastCostKey = Integer.MIN_VALUE;

    public NovaAnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 и 1 входы, 2 результат / Slots: 0 and 1 inputs, 2 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{15, 41}, {51, 41}, {113, 41}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {2};
    }

    @Override
    protected void buildMachine(Panel panel) {
        nameField = new TextField(8, 16, 134, 18, name);
        nameField.setMaxLength(50);
        nameField.setActive(menu.getSlot(0).hasItem());
        panel.add(nameField);

        panel.add(new TextWidget(39, 45, UiText.literal("+")));
        panel.add(new ArrowWidget(78, 42));

        costText = new TextWidget(8, 68, 134, 12, UiText.literal("")).setAlignment(Alignment.CENTER);
        panel.add(costText);
        lastCostKey = Integer.MIN_VALUE;

        // Переименование уходит на сервер при каждом изменении текста / The rename goes to the server on every text change
        panel.whileMounted(() -> panel.scope().subscribe(name, this::sendRename));

        if (!listening) {
            menu.addSlotListener(this);
            listening = true;
        }
    }

    @Override
    protected Widget initialFocus() {
        return nameField;
    }

    // Как в ванильной наковальне: пустое имя не отправляется, а имя по умолчанию превращается в пустое
    // As in the vanilla anvil: an empty name is not sent, and the default name becomes empty
    private void sendRename(String text) {
        if (text.isEmpty()) return;
        String send = text;
        Slot slot = menu.getSlot(0);
        if (slot != null && slot.hasItem() && !slot.getItem().hasCustomHoverName()
                && text.equals(slot.getItem().getHoverName().getString())) {
            send = "";
        }
        if (menu.setItemName(send) && minecraft != null && minecraft.player != null) {
            minecraft.player.connection.send(new ServerboundRenameItemPacket(send));
        }
    }

    // В левый слот положили или убрали предмет: подставляем его имя / An item was put into or taken from the left slot: show its name
    @Override
    public void slotChanged(AbstractContainerMenu changedMenu, int slotId, ItemStack stack) {
        if (slotId != 0 || nameField == null) return;
        nameField.setText(stack.isEmpty() ? "" : stack.getHoverName().getString());
        nameField.setActive(!stack.isEmpty());
    }

    @Override
    public void dataChanged(AbstractContainerMenu changedMenu, int dataSlotIndex, int value) {}

    // Цена обновляется по данным меню / The cost follows the menu data
    private void updateCost() {
        if (costText == null || minecraft == null || minecraft.player == null) return;
        int cost = menu.getCost();
        boolean hasResult = menu.getSlot(2).hasItem();
        boolean creative = minecraft.player.getAbilities().instabuild;
        boolean canTake = hasResult && menu.getSlot(2).mayPickup(minecraft.player);
        int key = cost * 8 + (hasResult ? 1 : 0) + (creative ? 2 : 0) + (canTake ? 4 : 0);
        if (key == lastCostKey) return;
        lastCostKey = key;

        Component text = null;
        int color = 0xFF80FF20;
        if (cost > 0) {
            if (cost >= 40 && !creative) {
                text = Component.translatable("container.repair.expensive");
                color = 0xFFFF6060;
            } else if (hasResult) {
                text = Component.translatable("container.repair.cost", cost);
                if (!canTake) color = 0xFFFF6060;
            }
        }
        costText.setText(text == null ? UiText.literal("") : McText.of(text));
        costText.setColor(color);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateCost();
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        if (listening) {
            menu.removeSlotListener(this);
            listening = false;
        }
        super.removed();
    }
}