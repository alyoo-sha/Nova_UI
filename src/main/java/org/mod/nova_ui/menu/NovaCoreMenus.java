package org.mod.nova_ui.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mod.nova_core.Nova_core;

public final class NovaCoreMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Nova_core.MOD_ID);

    public static final RegistryObject<MenuType<NovaWorkbenchMenu>> WORKBENCH =
            MENUS.register("workbench", () -> IForgeMenuType.create((id, inventory, data) -> new NovaWorkbenchMenu(id, inventory)));

    private NovaCoreMenus() {}

    public static void init() {
        MENUS.register(Nova_core.modEventBus);
    }
}