package org.mod.nova_ui;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.mod.nova_ui.menu.NovaCoreMenus;
import org.mod.nova_ui.tweaks.config.TweaksConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Nova_ui.MOD_ID)
public class Nova_ui {

    public static final String MOD_ID = "nova_ui";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nova_ui() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        NovaCoreMenus.init();

        // Клиентский конфиг твиков инвентаря / The inventory tweaks client config
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TweaksConfig.SPEC, "nova_ui-tweaks.toml");

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}