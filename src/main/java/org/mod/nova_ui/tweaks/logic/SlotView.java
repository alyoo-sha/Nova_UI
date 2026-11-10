package org.mod.nova_ui.tweaks.logic;

// Слот глазами логики; читает живое состояние, платформа реализует поверх своего Slot
// A slot as the logic sees it; reads the live state, the platform implements it over its own Slot
public interface SlotView {

    // Номер слота для кликов (позиция в меню) / The slot number used in clicks (position in the menu)
    int id();

    boolean isEmpty();

    int count();

    // Ключ предмета для сравнения через equals (предмет и теги); null, если слот пуст
    // An item key compared with equals (item and tags); null when the slot is empty
    Object itemKey();

    // true для слотов инвентаря игрока, false для остальных / true for player inventory slots, false for the rest
    boolean isPlayerSide();

    // В слот нельзя класть предметы (результат крафта и подобные) / Items cannot be placed here (crafting results and alike)
    boolean isTakeOnly();

    boolean canPickup();

    // Сколько ещё предметов такого вида слот примет (0, если не принимает вовсе)
    // How many more items of this kind the slot accepts (0 if it accepts none at all)
    int roomFor(Object itemKey);

    // Можно ли включить слот в раскладку перетаскиванием: пуст или тот же предмет, и слот разрешает
    // Whether the slot can join a drag distribution: empty or the same item, and the slot allows it
    boolean acceptsSpread(Object itemKey);

    // Забрало бы ванильное «собрать в стек» предмет из этого слота (результат с таким же предметом)
    // Whether the vanilla "collect into a stack" would take an item from this slot (a result holding the same item)
    boolean pickupAllWouldTake(Object itemKey);
}