package org.mod.nova_ui.tweaks.logic;

// Отправка клика по контейнеру; платформа шлёт его через игровой режим клиента
// Sends a container click; the platform sends it through the client game mode
public interface ClickSink {
    void click(int slotId, int button, ClickKind kind);
}