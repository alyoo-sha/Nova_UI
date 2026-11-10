package org.mod.nova_ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.minecraft.McSprite;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

// Полоса активных эффектов игрока по центру: значок, оставшееся время, подсказка с названием
// A centered strip of the player's active effects: an icon, the time left, a tooltip with the name
public final class McEffectsBar extends Widget {
    private static final int CELL_W = 28;
    private static final int CELL_H = 32;
    private static final int GAP = 4;

    private final int barWidth;
    private List<String> shownKeys = List.of();

    public McEffectsBar(int x, int y, int width) {
        super(x, y, width, CELL_H);
        this.barWidth = width;
        setPointerTransparent(true);
    }

    // Набор ячеек пересобирается, только когда меняется набор эффектов / The cells are rebuilt only when the set of effects changes
    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        List<MobEffectInstance> active = new ArrayList<>(player.getActiveEffects());
        List<String> keys = new ArrayList<>();
        for (MobEffectInstance e : active) keys.add(e.getDescriptionId() + "#" + e.getAmplifier());
        if (keys.equals(shownKeys)) return;
        shownKeys = keys;

        clearChildren();
        int n = active.size();
        int x = (barWidth - (n * CELL_W + Math.max(0, n - 1) * GAP)) / 2;
        for (MobEffectInstance e : active) {
            EffectCell cell = new EffectCell(x, e.getEffect(), e.getAmplifier());
            add(cell);
            Tooltips.attach(cell, cell::tooltip);
            x += CELL_W + GAP;
        }
    }

    private static final class EffectCell extends Widget {
        private final MobEffect effect;
        private final int amplifier;

        EffectCell(int x, MobEffect effect, int amplifier) {
            super(x, 0, CELL_W, CELL_H);
            this.effect = effect;
            this.amplifier = amplifier;
        }

        private MobEffectInstance instance() {
            LocalPlayer p = Minecraft.getInstance().player;
            return p == null ? null : p.getEffect(effect);
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            MobEffectInstance inst = instance();
            if (inst == null) return;
            SlotStyle s = ThemeManager.current().get(Styles.SLOT);
            int ax = absX();
            int ay = absY();
            int radius = Math.max(s.radius(), 3);
            g.fillRounded(ax, ay, ax + getWidth(), ay + getHeight(), radius, s.background());
            int edge = effect.isBeneficial() ? s.border() : 0xFFB04040;
            if ((edge >>> 24) != 0) g.strokeRounded(ax, ay, ax + getWidth(), ay + getHeight(), radius, 1f, edge);

            g.drawImage(McSprite.effect(effect), ax + 5, ay + 3, 18, 18);
            UiText time = McText.of(MobEffectUtil.formatDuration(inst, 1.0F));
//            UiText time = UiText.literal(String.valueOf(MobEffectUtil.formatDuration(inst, 1.0F)));
            g.drawText(time, ax + (getWidth() - UiContext.platform().textWidth(time)) / 2, ay + 22, 0xFFFFFFFF, false);
        }

        TooltipBubble tooltip() {
            if (instance() == null) return null;
            MutableComponent text = Component.translatable(effect.getDescriptionId());
            if (amplifier >= 1 && amplifier <= 9) {
                text.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (amplifier + 1)));
            }
            return new TooltipBubble(McText.of(text));
        }
    }
}