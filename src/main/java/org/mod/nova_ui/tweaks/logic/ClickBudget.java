package org.mod.nova_ui.tweaks.logic;

// Лимит кликов за игровой тик: защита от лавины пакетов и от рассинхронизации. Сброс на каждом тике клиента
// A per-tick click limit: protection from a packet flood and from desync. Reset on every client tick
public final class ClickBudget {
    private final int perTick;
    private int used = 0;

    public ClickBudget(int perTick) {
        this.perTick = perTick;
    }

    public void onTick() {
        used = 0;
    }

    public boolean canSend(int clicks) {
        return used + clicks <= perTick;
    }

    public void spend(int clicks) {
        used += clicks;
    }
}