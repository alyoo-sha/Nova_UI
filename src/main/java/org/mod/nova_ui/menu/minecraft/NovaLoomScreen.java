package org.mod.nova_ui.menu.minecraft;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextCell;
import org.mod.nova_core.ui.api.VirtualList;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;
import org.mod.nova_core.ui.state.Signal;

import java.util.List;

public class NovaLoomScreen extends NovaVanillaScreen<LoomMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(190, 92);

    private final ListModel<Holder<BannerPattern>> patterns = new ListModel<>();
    private final Signal<Integer> selected = Signal.of(-1);
    private List<Holder<BannerPattern>> shown = null;
    private int pending = -1;
    private int pendingTicks = 0;

    public NovaLoomScreen(LoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Слоты: 0 знамя, 1 краситель, 2 предмет узора, 3 результат / Slots: 0 banner, 1 dye, 2 pattern item, 3 result
    @Override
    protected int[][] machineSlotPositions() {
        return new int[][] {{9, 19}, {31, 19}, {20, 43}, {163, 39}};
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {3};
    }

    @Override
    protected void buildMachine(Panel panel) {
        ItemAdapter<Holder<BannerPattern>> adapter = TextCell.adapter(holder -> McText.of(Component.translatable(
                "block.minecraft.banner." + holder.unwrapKey().map(k -> k.location().toShortLanguageKey()).orElse("base") + ".white")));
        VirtualList<Holder<BannerPattern>> list = new VirtualList<>(56, 18, 96, 64, 14, patterns, adapter, selected);
        panel.add(list);

        shown = null;
        panel.whileMounted(() -> panel.scope().subscribe(selected, this::onUserSelect));
    }

    private void onUserSelect(Integer index) {
        if (index == null || index < 0 || index == menu.getSelectedBannerPatternIndex()
                || index >= patterns.size() || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        pending = index;
        pendingTicks = 0;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        List<Holder<BannerPattern>> now = menu.getSelectablePatterns();
        if (now != shown) {
            shown = now;
            patterns.setAll(now);
        }
        if (pending >= 0) {
            if (menu.getSelectedBannerPatternIndex() == pending || ++pendingTicks > 10) pending = -1;
            else return;
        }
        int actual = menu.getSelectedBannerPatternIndex();
        if (selected.peek() != actual) selected.set(actual);
    }
}