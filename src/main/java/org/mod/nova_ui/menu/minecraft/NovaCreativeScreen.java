package org.mod.nova_ui.menu.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import org.mod.nova_core.ui.api.*;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.*;
import org.mod.nova_core.ui.input.*;
import org.mod.nova_core.ui.minecraft.*;
import org.mod.nova_core.ui.minecraft.container.SlotAccess;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.*;
import org.mod.nova_core.ui.theme.StyleRef;
import org.mod.nova_ui.minecraft.McEffectsBar;
import org.mod.nova_ui.minecraft.McWorldButtons;

import java.util.*;

public class NovaCreativeScreen extends AbstractContainerScreen<InventoryMenu> {
    // Размеры окна (GUI-пиксели) / Window size (GUI pixels)
    private static final int WIDTH = 360;
    private static final int HEIGHT = 200; //176
    private static final int TABS_WIDTH = 112;
    private static final int X0 = 8 + TABS_WIDTH + 8; // левый край правой части / left edge of the right part
    private static final int GRID_Y = 26;
    private static final int COLUMNS = 12;
    private static final int HOTBAR_Y = 150;
    private static final int OFFSCREEN = -1000;
    private static final int RECENT_SIZE = 3;
    private static final int SEARCH_WIDTH = 92;
    private static final int TITLE_WIDTH = WIDTH - X0 - 8 - SEARCH_WIDTH - 6;

    // Живут между открытиями окна / They live between window openings
    private static CreativeModeTab lastTab = null;
    private static final List<ItemStack> RECENT = new ArrayList<>();

    // Вкладка или заголовок группы в списке слева / A tab or a group header in the list on the left
    private record TabEntry(CreativeModeTab tab, UiText header) {
        boolean isHeader() { return tab == null; }
    }

    private final UiHost host = new UiHost(this::buildTree, () -> null);
    private final FeatureFlagSet features;
    private final boolean operatorTab;

    // Состояние живёт в экране, а не в buildTree: дерево пересобирается при ресайзе / State lives in the screen, not in buildTree: the tree rebuilds on resize
    private final ListModel<TabEntry> entries = new ListModel<>();
    private final Signal<Integer> tabIndex = Signal.of(0);
    private final Signal<String> query = Signal.of("");
    private final ListModel<ItemStack> items = new ListModel<>();
    private final Signal<Integer> gridSelection = Signal.of(-1);
    private final ListModel<Integer> savedRows = new ListModel<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
    private final Signal<Integer> savedSelection = Signal.of(-1);
    private final Map<Slot, int[]> originals = new HashMap<>();

    private CreativeModeTab current = null;
    private TextWidget titleText = null;
    private VirtualGrid<ItemStack> grid = null;
    private Widget inventoryView = null;
    private Widget savedView = null;
    private final List<ItemCell> recentCells = new ArrayList<>();

    public NovaCreativeScreen(LocalPlayer player, FeatureFlagSet features, boolean operatorTab) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("itemGroup.search"));
        this.features = features;
        this.operatorTab = operatorTab;
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    // ---------- Создание и пересборка / Creation and rebuild ----------

    @Override
    protected void init() {
        if (minecraft == null || minecraft.gameMode == null || !minecraft.gameMode.hasInfiniteItems()) {
            if (minecraft != null) minecraft.setScreen(new InventoryScreen(minecraft.player));
            return;
        }
        super.init();
        boolean operatorItems = operatorTab && minecraft.player != null && minecraft.player.canUseGameMasterBlocks();
        CreativeModeTabs.tryRebuildTabContents(features, operatorItems, minecraft.level.registryAccess());

        entries.setAll(buildEntries());
        CreativeModeTab wanted = lastTab != null ? lastTab : CreativeModeTabs.getDefaultTab();
        int start = indexOfTab(wanted);
        if (start < 0) start = firstTabIndex();
        tabIndex.set(Math.max(0, start));
        current = start >= 0 && start < entries.size() ? entries.get(start).tab() : null;

        host.init(width, height);
    }

    // Группы как в SEER: служебные вкладки, Minecraft, моды с несколькими вкладками, «Другие моды» (по одной вкладке)
    // Groups as in SEER: utility tabs, Minecraft, mods with several tabs, "Other mods" (one tab each)
    private List<TabEntry> buildEntries() {
        List<TabEntry> out = new ArrayList<>();
        List<CreativeModeTab> all = CreativeModeTabs.tabs();
        for (CreativeModeTab t : all) {
            if (t.getType() == CreativeModeTab.Type.INVENTORY || t.getType() == CreativeModeTab.Type.HOTBAR) {
                out.add(new TabEntry(t, null));
            }
        }

        Map<String, List<CreativeModeTab>> byMod = new LinkedHashMap<>();
        for (CreativeModeTab t : all) {
            if (t.getType() != CreativeModeTab.Type.CATEGORY) continue;
            ResourceLocation key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(t);
            byMod.computeIfAbsent(key == null ? "minecraft" : key.getNamespace(), k -> new ArrayList<>()).add(t);
        }

        List<CreativeModeTab> singles = new ArrayList<>();
        for (Map.Entry<String, List<CreativeModeTab>> e : byMod.entrySet()) {
            boolean vanilla = e.getKey().equals("minecraft");
            if (vanilla || e.getValue().size() > 1) {
                UiText fallback = vanilla ? NovaTexts.MINECRAFT : UiText.literal(modName(e.getKey()));
                out.add(new TabEntry(null, NovaTexts.groupName(e.getKey(), fallback)));
                for (CreativeModeTab t : e.getValue()) out.add(new TabEntry(t, null));
            } else {
                singles.addAll(e.getValue());
            }
        }
        if (!singles.isEmpty()) {
            out.add(new TabEntry(null, NovaTexts.OTHER_MODS));
            for (CreativeModeTab t : singles) out.add(new TabEntry(t, null));
        }
        return out;
    }

    private static String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    private int indexOfTab(CreativeModeTab tab) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).tab() == tab) return i;
        }
        return -1;
    }

    private int firstTabIndex() {
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).isHeader()) return i;
        }
        return -1;
    }

    private void buildTree(Widget root) {
        if (originals.isEmpty()) {
            for (Slot s : menu.slots) originals.put(s, new int[] {s.x, s.y});
        }

        Panel panel = new Panel(leftPos, topPos, WIDTH, HEIGHT, Styles.POPUP);
        panel.setDebugName("creative");
        root.add(panel);

        // Вкладки по группам / Tabs by groups
        ItemAdapter<TabEntry> tabAdapter = ItemAdapter.<TabEntry, TabCell>of(
                TabCell::new, (cell, entry, i, sel) -> cell.set(entry, sel));
        panel.add(new VirtualList<>(8, 8, TABS_WIDTH, HEIGHT - 16, 20, entries, tabAdapter, tabIndex));

        // Заголовок обрезается по ширине, чтобы не наезжать на поиск / The title is clipped by width so it never overlaps the search
        titleText = new TextWidget(X0, 10, TITLE_WIDTH, 9, UiText.literal("")).setEllipsize(true);
        panel.add(titleText);

        TextField search = new TextField(WIDTH - 8 - SEARCH_WIDTH, 5, SEARCH_WIDTH, 16, query);
        search.setPlaceholder(NovaTexts.SEARCH);
        panel.add(search);

        // Палитра предметов / The item palette
        ItemAdapter<ItemStack> cellAdapter = ItemAdapter.<ItemStack, ItemCell>of(
                () -> new ItemCell().setSlotBackground(true), (cell, stack, i, sel) -> cell.set(McItem.of(stack), false));
        grid = new VirtualGrid<>(X0, GRID_Y, 224, 108, items, cellAdapter, gridSelection);
        grid.setColumns(COLUMNS).setCellSize(18, 18);
        grid.addListener(EventType.POINTER_DOWN, e -> {
            int i = grid.indexAt(e.getX(), e.getY());
            if (i >= 0 && i < items.size()) paletteClick(items.get(i), e.getButton(), Screen.hasShiftDown());
            grid.setSelectedIndex(-1);
        });
        panel.add(grid);

        inventoryView = buildInventoryView();
        panel.add(inventoryView);

        // Сохранённые хотбары / Saved hotbars
        ItemAdapter<Integer> savedAdapter = ItemAdapter.<Integer, SavedHotbarRow>of(
                SavedHotbarRow::new, (row, index, i, sel) -> row.set(index));
        savedView = new VirtualList<>(X0, GRID_Y, 224, 108, 20, savedRows, savedAdapter, savedSelection);
        panel.add(savedView);

        // Хотбар и недавние / Hotbar and recently used
        panel.add(new TextWidget(X0, 139, NovaTexts.HOTBAR));
        for (int i = 0; i < 9; i++) frame(panel, X0 + i * 18, HOTBAR_Y - 1);
        int recentX = X0 + 9 * 18 + 8;
        panel.add(new TextWidget(recentX, 139, NovaTexts.RECENT));
        recentCells.clear();
        for (int i = 0; i < RECENT_SIZE; i++) {
            frame(panel, recentX + i * 18, HOTBAR_Y - 1);
            ItemCell cell = new ItemCell();
            cell.setBounds(recentX + i * 18, HOTBAR_Y - 1, 18, 18);
            int index = i;
            cell.addListener(EventType.POINTER_DOWN, e -> {
                if (index < RECENT.size()) paletteClick(RECENT.get(index), e.getButton(), Screen.hasShiftDown());
            });
            recentCells.add(cell);
            panel.add(cell);
        }
        refreshRecent();

        McWorldButtons.add(root, leftPos, topPos - 24);
        root.add(new McEffectsBar(leftPos, topPos + HEIGHT + 4, WIDTH));

        panel.whileMounted(() -> {
            panel.scope().subscribe(tabIndex, this::onTabIndex);
            panel.scope().subscribe(query, q -> applyView());
        });
        applyView();
    }

    // Вид вкладки «Инвентарь»: броня, предпросмотр, вторая рука, корзина и основной инвентарь
    // The "Inventory" tab view: armor, the preview, the offhand, the trash and the main inventory
    private Widget buildInventoryView() {
        Widget view = new Widget(X0, GRID_Y, 224, 108);
        view.setPointerTransparent(true);
        for (int i = 0; i < 4; i++) frame(view, 7 + i * 18, 7);
        view.add(new PlayerPreview(86, 0, 54, 52).setScale(20));
        frame(view, 147, 7);
        view.add(new TrashSlot(175, 7));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) frame(view, 31 + c * 18, 55 + r * 18);
        }
        return view;
    }

    private static void frame(Widget parent, int x, int y) {
        SlotFrame f = new SlotFrame(false);
        f.setPosition(x, y);
        parent.add(f);
    }

    // ---------- Вкладки, поиск, вид / Tabs, search, view ----------

    private void onTabIndex(Integer index) {
        if (index == null || index < 0 || index >= entries.size()) return;
        TabEntry entry = entries.get(index);
        if (entry.isHeader()) { // заголовки не выбираются / headers are not selectable
            int back = current == null ? -1 : indexOfTab(current);
            if (back >= 0) tabIndex.set(back);
            return;
        }
        current = entry.tab();
        lastTab = current;
        applyView();
    }

    private boolean searching() {
        return !query.peek().isBlank();
    }

    private boolean showInventory() {
        return current != null && current.getType() == CreativeModeTab.Type.INVENTORY && !searching();
    }

    private boolean showSaved() {
        return current != null && current.getType() == CreativeModeTab.Type.HOTBAR && !searching();
    }

    private void applyView() {
        boolean inventory = showInventory();
        boolean saved = showSaved();
        if (grid != null) grid.setVisible(!inventory && !saved);
        if (inventoryView != null) inventoryView.setVisible(inventory);
        if (savedView != null) savedView.setVisible(saved);
        positionSlots(inventory);
        rebuildItems();
        updateTitle(!inventory && !saved);
    }

    private void rebuildItems() {
        String q = query.peek().trim().toLowerCase(Locale.ROOT);
        List<ItemStack> out = new ArrayList<>();
        if (!q.isEmpty()) {
            for (ItemStack s : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
                if (matches(s, q)) out.add(s);
            }
        } else if (current != null && current.getType() == CreativeModeTab.Type.CATEGORY) {
            out.addAll(current.getDisplayItems());
        }
        items.setAll(out);
    }

    // Поиск по названию и по идентификатору; «#» ищет по тегам / Search by name and id; "#" searches by tags
    private static boolean matches(ItemStack stack, String q) {
        if (q.startsWith("#")) {
            String tag = q.substring(1);
            return stack.getTags().anyMatch(t -> t.location().getPath().contains(tag) || t.location().toString().contains(tag));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(q);
    }

    private void updateTitle(boolean palette) {
        if (titleText == null) return;
        UiText name = searching() ? NovaTexts.RESULTS
                : current != null ? NovaTexts.tabName(tabKey(current), McText.of(current.getDisplayName()))
                : UiText.literal("");
        MutableComponent title = McText.toComponent(name).copy().withStyle(ChatFormatting.BOLD);
        UiText shown = McText.of(title);
        if (palette) {
            UiText counted = McText.of(title.copy().append(Component.literal("  " + items.size()).withStyle(ChatFormatting.GRAY)));
            if (UiContext.platform().textWidth(counted) <= TITLE_WIDTH) shown = counted;
        }
        titleText.setText(shown);
    }

    private static String tabKey(CreativeModeTab tab) {
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return id == null ? "" : id.toString();
    }

    // Слоты инвентаря переставляются под вид; ненужные уходят за экран / The inventory slots are moved to the view; unneeded ones go off screen
    private void positionSlots(boolean inventoryView) {
        for (Slot slot : menu.slots) {
            int i = slot.index;
            int x = OFFSCREEN;
            int y = OFFSCREEN;
            if (i >= 36 && i <= 44) {
                x = X0 + 1 + (i - 36) * 18;
                y = HOTBAR_Y;
            } else if (inventoryView) {
                if (i >= 5 && i <= 8) {
                    x = X0 + 8 + (i - 5) * 18;
                    y = GRID_Y + 8;
                } else if (i == 45) {
                    x = X0 + 148;
                    y = GRID_Y + 8;
                } else if (i >= 9 && i <= 35) {
                    x = X0 + 32 + ((i - 9) % 9) * 18;
                    y = GRID_Y + 56 + ((i - 9) / 9) * 18;
                }
            }
            SlotAccess.setPosition(slot, x, y);
        }
    }

    // ---------- Предметы и курсор / Items and the cursor ----------

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    // Клик по палитре; правила как в ванильном креативе / A palette click; the rules are the vanilla creative ones
    private void paletteClick(ItemStack template, int button, boolean shift) {
        ItemStack carried = menu.getCarried();
        if (shift && button == MouseButton.LEFT) {
            giveToInventory(withCount(template, template.getMaxStackSize()));
            remember(template);
            return;
        }
        if (button == MouseButton.MIDDLE) {
            if (carried.isEmpty()) {
                menu.setCarried(withCount(template, template.getMaxStackSize()));
                remember(template);
            }
            return;
        }
        if (carried.isEmpty()) {
            menu.setCarried(withCount(template, 1));
            remember(template);
        } else if (ItemStack.isSameItemSameTags(carried, template)) {
            if (button == MouseButton.LEFT) {
                if (carried.getCount() < carried.getMaxStackSize()) carried.grow(1);
            } else {
                carried.shrink(1);
            }
        } else if (button == MouseButton.LEFT) {
            menu.setCarried(ItemStack.EMPTY);
        } else {
            carried.shrink(1);
        }
    }

    // Положить в инвентарь и отправить на сервер изменившиеся слоты / Put into the inventory and send the changed slots to the server
    private void giveToInventory(ItemStack stack) {
        Inventory inv = minecraft.player.getInventory();
        ItemStack[] before = new ItemStack[36];
        for (int i = 0; i < 36; i++) before[i] = inv.getItem(i).copy();
        inv.add(stack);
        for (int i = 0; i < 36; i++) {
            ItemStack now = inv.getItem(i);
            if (!ItemStack.matches(before[i], now)) {
                minecraft.gameMode.handleCreativeModeItemAdd(now.copy(), i < 9 ? 36 + i : i);
            }
        }
    }

    private void remember(ItemStack template) {
        ItemStack one = withCount(template, 1);
        RECENT.removeIf(s -> ItemStack.isSameItemSameTags(s, one));
        RECENT.add(0, one);
        while (RECENT.size() > RECENT_SIZE) RECENT.remove(RECENT.size() - 1);
        refreshRecent();
    }

    private void refreshRecent() {
        for (int i = 0; i < recentCells.size(); i++) {
            recentCells.get(i).set(i < RECENT.size() ? McItem.of(RECENT.get(i)) : null, false);
        }
    }

    // Записать стек в слот локально и на сервере (пакет креатива) / Write a stack into a slot locally and on the server (the creative packet)
    private void putSlot(Slot slot, ItemStack stack) {
        ItemStack s = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        slot.set(s);
        minecraft.gameMode.handleCreativeModeItemAdd(s.copy(), slot.index);
    }

    private void slotClick(Slot slot, int button) {
        ItemStack carried = menu.getCarried();
        ItemStack inSlot = slot.getItem();
        if (!carried.isEmpty()) {
            if (!slot.mayPlace(carried)) return;
            if (button == MouseButton.RIGHT) { // положить один / place one
                if (inSlot.isEmpty()) {
                    putSlot(slot, withCount(carried, 1));
                    carried.shrink(1);
                } else if (ItemStack.isSameItemSameTags(inSlot, carried) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                    putSlot(slot, withCount(inSlot, inSlot.getCount() + 1));
                    carried.shrink(1);
                }
            } else if (ItemStack.isSameItemSameTags(inSlot, carried)) { // досыпать / top up
                int move = Math.min(carried.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                if (move > 0) {
                    putSlot(slot, withCount(inSlot, inSlot.getCount() + move));
                    carried.shrink(move);
                }
            } else { // обмен / swap
                ItemStack old = inSlot.copy();
                putSlot(slot, carried);
                menu.setCarried(old);
            }
        } else if (!inSlot.isEmpty()) {
            if (button == MouseButton.RIGHT) { // половина / half
                int half = (inSlot.getCount() + 1) / 2;
                menu.setCarried(withCount(inSlot, half));
                putSlot(slot, withCount(inSlot, inSlot.getCount() - half));
            } else {
                menu.setCarried(inSlot.copy());
                putSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private void dropStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        minecraft.player.drop(stack, true);
        minecraft.gameMode.handleCreativeModeItemDrop(stack);
    }

    private void dropCarried() {
        dropStack(menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
    }

    // ---------- Клавиши над слотом: 1-9 и Q / Keys over a slot: 1-9 and Q ----------

    private double mouseX() {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
    }

    private double mouseY() {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
    }

    private boolean handleSlotKeys(int key, int scanCode) {
        for (int i = 0; i < 9; i++) {
            if (minecraft.options.keyHotbarSlots[i].matches(key, scanCode)) return hotbarKey(i);
        }
        if (minecraft.options.keyDrop.matches(key, scanCode)) return dropKey(hasControlDown());
        return false;
    }

    // Над палитрой кладёт стек в слот хотбара, над слотом меняет местами с ним / Over the palette puts a stack into the hotbar slot, over a slot swaps with it
    private boolean hotbarKey(int hotbarIndex) {
        Slot target = menu.getSlot(36 + hotbarIndex);
        if (grid != null && grid.isVisible() && grid.contains(mouseX(), mouseY())) {
            int i = grid.indexAt(mouseX(), mouseY());
            if (i < 0 || i >= items.size()) return false;
            ItemStack template = items.get(i);
            putSlot(target, withCount(template, template.getMaxStackSize()));
            remember(template);
            return true;
        }
        if (hoveredSlot != null && hoveredSlot != target) {
            ItemStack a = hoveredSlot.getItem().copy();
            ItemStack b = target.getItem().copy();
            putSlot(hoveredSlot, b);
            putSlot(target, a);
            return true;
        }
        return false;
    }

    private boolean dropKey(boolean whole) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;
        ItemStack s = hoveredSlot.getItem();
        int n = whole ? s.getCount() : 1;
        ItemStack dropped = withCount(s, n);
        putSlot(hoveredSlot, withCount(s, s.getCount() - n));
        dropStack(dropped);
        return true;
    }

    // ---------- Кадр и ввод / Frame and input ----------

    @Override
    protected void containerTick() {
        super.containerTick();
        // Режим сменили на выживание: открываем обычный инвентарь / The mode changed to survival: open the regular inventory
        if (minecraft != null && minecraft.player != null && minecraft.gameMode != null && !minecraft.gameMode.hasInfiniteItems()) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        host.renderBackdrop(g);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);
        drawHover(g);
    }
    public int getSlotColor(int index) {
        return 0;
    }

    private void drawHover(GuiGraphics g) {
        Slot s = hoveredSlot;
        if (s == null || !s.isActive()) return;
        host.drawSlotHighlight(g, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
    }

    // Shift+клик по корзине очищает весь инвентарь, как в ванильном / Shift+click on the trash clears the whole inventory, as in vanilla
    private void clearInventory() {
        for (Slot slot : menu.slots) {
            slot.set(ItemStack.EMPTY);
            minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, slot.index);
        }
        menu.setCarried(ItemStack.EMPTY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    // Подсказка слота нашей темы / The slot tooltip in our theme
    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        if (host.hasModal() || !menu.getCarried().isEmpty() || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        host.drawTooltip(g, UiContext.platform().itemTooltip(McItem.of(hoveredSlot.getItem())), x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        host.renderBackdrop(g);
        super.render(g, mouseX, mouseY, partialTick);
        host.renderTop(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // Клики по слотам обрабатываются здесь, а не ванильной логикой с пакетами контейнера
    // Slot clicks are handled here instead of the vanilla logic with container packets
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (host.mouseClicked(mx, my, button)) return true;
        if (hoveredSlot != null && hoveredSlot.isActive()) {
            slotClick(hoveredSlot, button);
            return true;
        }
        boolean inside = mx >= leftPos && mx < leftPos + WIDTH && my >= topPos && my < topPos + HEIGHT;
        if (!inside && !menu.getCarried().isEmpty()) {
            dropCarried();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return host.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return host.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return host.mouseScrolled(mx, my, delta);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        host.mouseMoved(mx, my);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (host.keyPressed(key, scanCode, modifiers)) return true;
        if (handleSlotKeys(key, scanCode)) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return host.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return host.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        menu.setCarried(ItemStack.EMPTY);
        for (Map.Entry<Slot, int[]> e : originals.entrySet()) {
            SlotAccess.setPosition(e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
        originals.clear();
        host.removed();
        super.removed();
    }

    // ---------- Виджеты окна / The window's widgets ----------

    // Ячейка списка слева: вкладка или заголовок группы / A cell of the list on the left: a tab or a group header
    private static final class TabCell extends Widget {
        private final StyleRef<ButtonStyle> buttonStyle = new StyleRef<>(Styles.BUTTON);
        private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);
        private McItem icon = null;
        private UiText name = UiText.literal("");
        private boolean header = false;
        private boolean selected = false;
        private boolean truncated = false;

        TabCell() {
            super(0, 0, 0, 0);
            Tooltips.attach(this, () -> truncated ? new TooltipBubble(name) : null, 250L);
        }

        void set(TabEntry entry, boolean selected) {
            this.header = entry.isHeader();
            this.selected = selected && !header;
            this.icon = header ? null : McItem.of(entry.tab().getIconItem());
            if (header) {
                this.name = entry.header();
            } else {
                ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(entry.tab());
                this.name = NovaTexts.tabName(id == null ? "" : id.toString(), McText.of(entry.tab().getDisplayName()));
            }
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            ButtonStyle b = buttonStyle.get();
            int ax = absX();
            int ay = absY();
            int w = getWidth();
            int h = getHeight();
            int textY = ay + (h - UiContext.platform().lineHeight()) / 2;
            if (header) {
                g.drawText(name, ax + 4, textY + 3, (b.text() & 0xFFFFFF) | 0x80000000, false);
                return;
            }
            if (selected) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), fieldStyle.get().selection());
            else if (isHovered()) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), b.hover());
            if (icon != null) g.drawItem(icon, ax + 3, ay + 2, false);
            UiText shown = UiContext.platform().ellipsize(name, w - 26);
            truncated = shown != name;
            g.drawText(shown, ax + 22, textY, b.text(), b.textShadow());
        }
    }

    // Строка сохранённого хотбара: номер, девять предметов и кнопки «Сохранить» и «Загрузить»
    // A saved hotbar row: the number, nine items and the "Save" and "Load" buttons
    private static final class SavedHotbarRow extends Widget {
        private int index = 0;

        SavedHotbarRow() {
            super(0, 0, 0, 0);
            add(new ButtonWidget(0, 2, 34, 16, NovaTexts.SAVE, () -> hotbarAction(index, false, true)));
            add(new ButtonWidget(0, 2, 34, 16, NovaTexts.LOAD, () -> hotbarAction(index, true, false)));
        }

        void set(int index) {
            this.index = index;
        }

        @Override
        protected void layoutChildren() {
            getChildren().get(0).setPosition(getWidth() - 74, 2);
            getChildren().get(1).setPosition(getWidth() - 38, 2);
            super.layoutChildren();
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            int ax = absX();
            int ay = absY();
            g.drawText(UiText.literal(String.valueOf(index + 1)), ax + 3, ay + 6, 0xFFFFFFFF, false);
            var hotbar = Minecraft.getInstance().getHotbarManager().get(index);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = hotbar.get(i);
                if (!stack.isEmpty()) g.drawItem(McItem.of(stack), ax + 14 + i * 16, ay + 2, true);
            }
        }
    }

    private static void hotbarAction(int index, boolean load, boolean save) {
        CreativeModeInventoryScreen.handleHotbarLoadOrSave(Minecraft.getInstance(), index, load, save);
    }

    // Корзина: клик с предметом на курсоре удаляет его / The trash: a click with an item on the cursor deletes it
    private final class TrashSlot extends Widget {
        private final StyleRef<SlotStyle> style = new StyleRef<>(Styles.SLOT);

        TrashSlot(int x, int y) {
            super(x, y, 18, 18);
            Tooltips.attach(this, () -> new TooltipBubble(McText.of(Component.translatable("inventory.binSlot"))));
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            SlotStyle s = style.get();
            float in = s.inset();
            g.fillRounded(absX() + in, absY() + in, absX() + 18 - in, absY() + 18 - in, s.radius(), s.background());
            g.strokeRounded(absX() + in, absY() + in, absX() + 18 - in, absY() + 18 - in, s.radius(), 1f, 0xFFB04040);
            Icons.TRASH.drawShadowed(g, absX() + 1, absY() + 1, isHovered() ? 0xFFFFFFFF : 0xFF909090);
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT) return;
            if (Screen.hasShiftDown()) clearInventory();
            else if (!menu.getCarried().isEmpty()) menu.setCarried(ItemStack.EMPTY);
            e.stopPropagation();
        }
    }
}