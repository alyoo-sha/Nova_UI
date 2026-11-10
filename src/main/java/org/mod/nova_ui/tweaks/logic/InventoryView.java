package org.mod.nova_ui.tweaks.logic;

import java.util.List;

// Открытое окно контейнера глазами логики / An open container window as the logic sees it
public interface InventoryView {

    // Слоты в порядке меню: позиция в списке равна SlotView.id()
    // Slots in menu order: the list position equals SlotView.id()
    List<? extends SlotView> slots();

    // Ключ предмета на курсоре; null, если курсор пуст / The key of the cursor item; null if the cursor is empty
    Object carriedKey();

    int carriedCount();

    // Максимальный размер стека предмета на курсоре / The maximum stack size of the cursor item
    int carriedMaxStack();
}