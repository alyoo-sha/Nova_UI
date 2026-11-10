package org.mod.nova_ui.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.minecraft.McSprite;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NovaBeaconScreen extends NovaVanillaScreen<BeaconMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(200, 118);

    // Выбранные эффекты живут в экране, как в ванильном маяке / The chosen effects live in the screen, as in the vanilla beacon
    private MobEffect primary;
    private MobEffect secondary;
    private final List<EffectButton> primaryButtons = new ArrayList<>();
    private EffectButton regeneration = null;
    private EffectButton upgrade = null;
    private ButtonWidget confirm = null;

    public NovaBeaconScreen(BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
        this.primary = menu.getPrimaryEffect();
        this.secondary = menu.getSecondaryEffect();
    }

    // Слот: 0 оплата / Slot: 0 payment
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{111, 91}};
    }

    @Override
    protected void buildMachine(Panel panel) {
        primaryButtons.clear();

        panel.add(new TextWidget(8, 17, McText.of(Component.translatable("block.minecraft.beacon.primary"))));
        panel.add(new TextWidget(110, 17, McText.of(Component.translatable("block.minecraft.beacon.secondary"))));

        // Основные эффекты: три уровня маяка / Primary effects: three beacon tiers
        for (int tier = 0; tier < 3; tier++) {
            MobEffect[] effects = BeaconBlockEntity.BEACON_EFFECTS[tier];
            for (int j = 0; j < effects.length; j++) {
                EffectButton button = new EffectButton(14 + j * 26, 30 + tier * 26, effects[j], tier, true, false);
                primaryButtons.add(button);
                panel.add(button);
                Tooltips.attach(button, button::tooltip);
            }
        }

        // Вторичные: регенерация и усиленный основной эффект / Secondary: regeneration and the upgraded primary effect
        regeneration = new EffectButton(110, 30, BeaconBlockEntity.BEACON_EFFECTS[3][0], 3, false, false);
        upgrade = new EffectButton(136, 30, primary, 3, false, true);
        panel.add(regeneration);
        panel.add(upgrade);
        Tooltips.attach(regeneration, regeneration::tooltip);
        Tooltips.attach(upgrade, upgrade::tooltip);

        confirm = new ButtonWidget(138, 90, 26, 18, UiText.literal("OK"), this::confirmEffects);
        panel.add(confirm);
        panel.add(new ButtonWidget(168, 90, 26, 18, UiText.literal("X"), () -> {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }));
        refresh();
    }

    // Как в ванильном маяке: пакет с эффектами, затем закрытие / As in the vanilla beacon: the effects packet, then closing
    private void confirmEffects() {
        if (minecraft == null || minecraft.getConnection() == null || minecraft.player == null) return;
        minecraft.getConnection().send(new ServerboundSetBeaconPacket(Optional.ofNullable(primary), Optional.ofNullable(secondary)));
        minecraft.player.closeContainer();
    }

    // Доступность кнопок зависит от уровня маяка и оплаты / The button availability depends on the beacon level and the payment
    private void refresh() {
        int levels = menu.getLevels();
        for (EffectButton b : primaryButtons) b.setActive(levels > b.tier);
        if (regeneration != null) regeneration.setActive(levels >= 4);
        if (upgrade != null) {
            upgrade.effect = primary;
            upgrade.setVisible(primary != null);
            upgrade.setActive(levels >= 4);
        }
        if (confirm != null) confirm.setActive(menu.hasPayment() && primary != null);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    // Кнопка эффекта со значком / An effect button with an icon
    private final class EffectButton extends Widget {
        private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
        private final int tier;
        private final boolean isPrimary;
        private final boolean isUpgrade;
        private MobEffect effect;
        private boolean pressed = false;

        EffectButton(int x, int y, MobEffect effect, int tier, boolean isPrimary, boolean isUpgrade) {
            super(x, y, 22, 22);
            this.effect = effect;
            this.tier = tier;
            this.isPrimary = isPrimary;
            this.isUpgrade = isUpgrade;
        }

        private boolean selected() {
            return effect != null && effect == (isPrimary ? primary : secondary);
        }

        TooltipBubble tooltip() {
            if (effect == null) return null;
            MutableComponent name = Component.translatable(effect.getDescriptionId());
            if (isUpgrade) name.append(" II");
            return new TooltipBubble(McText.of(name));
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            if (effect == null) return;
            ButtonStyle s = style.get();
            boolean hovered = isActive() && isHovered();
            int bg;
            if (!isActive()) bg = s.disabled();
            else if (selected() || (pressed && hovered)) bg = s.pressed();
            else if (hovered) bg = s.hover();
            else bg = s.normal();
            Draw.box(g, absX(), absY(), getWidth(), getHeight(), s.radius(), bg,
                    selected() ? s.focusedBorder() : s.border(), 1);

            if (!isActive()) g.pushOpacity(0.35f);
            g.drawImage(McSprite.effect(effect), absX() + 2, absY() + 2, 18, 18);
            if (!isActive()) g.popOpacity();
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT || !isActive() || effect == null) return;
            pressed = true;
            e.stopPropagation();
        }

        @Override
        protected void onPointerUp(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT || !pressed) return;
            pressed = false;
            // Выбранный эффект снять нельзя, как в ванильном маяке / A chosen effect cannot be unselected, as in the vanilla beacon
            if (contains(e.getX(), e.getY()) && !selected() && effect != null) {
                if (isPrimary) primary = effect;
                else secondary = effect;
                refresh();
            }
            e.stopPropagation();
        }

        @Override
        protected void onPointerCancel(PointerEvent e) {
            pressed = false;
        }
    }
}