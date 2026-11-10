package org.mod.nova_ui;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.mod.nova_core.Nova_core;
import org.mod.nova_ui.menu.NovaVanillaMenus;
import org.mod.nova_ui.menu.NovaWorkbenchMenu;

@Mod.EventBusSubscriber(modid = Nova_ui.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NovaCoreCommands {
    private NovaCoreCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("novaui")
                // /novacore workbench: наш собственный верстак / our own workbench
                .then(Commands.literal("workbench").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    NetworkHooks.openScreen(player, new SimpleMenuProvider(
                            (id, inventory, p) -> new NovaWorkbenchMenu(id, inventory),
                            Component.translatable("menu.nova_core.workbench")));
                    return 1;
                }))
                // /novacore open <имя>: ванильное меню без блока / a vanilla menu without a block
                .then(Commands.literal("open").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(NovaVanillaMenus.NAMES, builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            if (!NovaVanillaMenus.open(player, name)) {
                                ctx.getSource().sendFailure(Component.literal("Unknown menu: " + name));
                                return 0;
                            }
                            return 1;
                        }))));
    }
}