package org.mod.nova_ui.tweaks.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mod.nova_core.ui.minecraft.container.interaction.InventoryInteractions;
import org.mod.nova_ui.Nova_ui;

// Регистрирует модуль в реестре Nova Core при старте клиента / Registers the module in the Nova Core registry at client start
@Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TweaksClient {
    private TweaksClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> InventoryInteractions.register(new NovaTweaksInteraction()));
    }
}