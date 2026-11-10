package org.mod.nova_ui.menu.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

public class NovaEnchantmentScreen extends NovaVanillaScreen<EnchantmentMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(190, 98);

    public NovaEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 предмет, 1 лазурит / Slots: 0 item, 1 lapis lazuli
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{15, 33}, {15, 63}};
    }

    @Override
    protected void buildMachine(Panel panel) {
        for (int i = 0; i < 3; i++) {
            EnchantOption option = new EnchantOption(i);
            panel.add(option);
            Tooltips.attach(option, option::tooltip);
        }
    }

    // Случайные «звёздные» названия берутся из зерна меню; семя ставится раз в кадр, как в ванильном экране
    // The random "galactic" names come from the menu seed; the seed is set once per frame, as in the vanilla screen
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        EnchantmentNames.getInstance().initSeed(menu.getEnchantmentSeed());
        super.render(g, mouseX, mouseY, partialTick);
    }

    // Один из трёх вариантов зачарования / One of the three enchantment options
    private final class EnchantOption extends Widget {
        private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
        private final int index;
        private boolean pressed = false;

        EnchantOption(int index) {
            super(48, 18 + index * 26, 134, 22);
            this.index = index;
        }

        private boolean hasOffer() {
            return menu.costs[index] > 0;
        }

        private boolean affordable() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;
            if (player.getAbilities().instabuild) return true;
            return menu.getGoldCount() >= index + 1 && player.experienceLevel >= menu.costs[index];
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            ButtonStyle s = style.get();
            boolean offer = hasOffer();
            boolean hovered = offer && isHovered();
            int bg;
            if (!offer) bg = s.disabled();
            else if (pressed && hovered) bg = s.pressed();
            else if (hovered) bg = s.hover();
            else bg = s.normal();
            Draw.box(g, absX(), absY(), getWidth(), getHeight(), s.radius(), bg, s.border(), 1);
            if (!offer) return;

            boolean ok = affordable();
            UiPlatform p = UiContext.platform();
            int textY = absY() + (getHeight() - p.lineHeight()) / 2;

            UiText name = McText.of(McText.fromFormatted(
                    EnchantmentNames.getInstance().getRandomName(Minecraft.getInstance().font, getWidth() - 40)));
            g.drawText(name, absX() + 8, textY, ok ? 0xFFFFFFFF : 0xFF909090, false);

            UiText cost = UiText.literal(String.valueOf(menu.costs[index]));
            g.drawText(cost, absX() + getWidth() - 8 - p.textWidth(cost), textY, ok ? 0xFF80FF20 : 0xFFFF6060, false);
        }

        // Подсказка как в ванильном экране: намёк, требования в лазурите и уровнях / The tooltip as in the vanilla screen: the clue, the lapis and level requirements
        TooltipBubble tooltip() {
            if (!hasOffer()) return null;
            LocalPlayer player = Minecraft.getInstance().player;
            MutableComponent text = Component.empty();

            Enchantment clue = menu.enchantClue[index] >= 0 ? BuiltInRegistries.ENCHANTMENT.byId(menu.enchantClue[index]) : null;
            boolean hasClue = clue != null && menu.levelClue[index] >= 0;
            if (hasClue) {
                text.append(Component.translatable("container.enchant.clue", clue.getFullname(menu.levelClue[index]))
                        .withStyle(ChatFormatting.WHITE));
            }

            if (player != null && !player.getAbilities().instabuild) {
                if (hasClue) text.append("\n");
                int need = index + 1;
                if (player.experienceLevel < menu.costs[index]) {
                    text.append(Component.translatable("container.enchant.level.requirement", menu.costs[index])
                            .withStyle(ChatFormatting.RED));
                } else {
                    MutableComponent lapis = need == 1
                            ? Component.translatable("container.enchant.lapis.one")
                            : Component.translatable("container.enchant.lapis.many", need);
                    text.append(lapis.withStyle(menu.getGoldCount() >= need ? ChatFormatting.GRAY : ChatFormatting.RED));
                    text.append("\n");
                    MutableComponent levels = need == 1
                            ? Component.translatable("container.enchant.level.one")
                            : Component.translatable("container.enchant.level.many", need);
                    text.append(levels.withStyle(ChatFormatting.GRAY));
                }
            }
            return text.getString().isEmpty() ? null : new TooltipBubble(McText.of(text));
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT || !hasOffer()) return;
            pressed = true;
            e.stopPropagation();
        }

        @Override
        protected void onPointerUp(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT || !pressed) return;
            pressed = false;
            LocalPlayer player = Minecraft.getInstance().player;
            // Проверка на клиенте та же, что в ванильном экране / The client check is the same as in the vanilla screen
            if (contains(e.getX(), e.getY()) && hasOffer() && player != null && minecraft != null
                    && minecraft.gameMode != null && menu.clickMenuButton(player, index)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
            }
            e.stopPropagation();
        }

        @Override
        protected void onPointerCancel(PointerEvent e) {
            pressed = false;
        }
    }
}