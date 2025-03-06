package com.baris.solocircuit.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class EnderBoxCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("enderbox")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        if (ScCommand.isCheatMode()) {
                            source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
                            return 0;
                        }
                        // 获取玩家的末影箱物品栏
                        EnderChestInventory enderChest = player.getEnderChestInventory();

                        // 打开真正的末影箱界面
                        player.openHandledScreen(new NamedScreenHandlerFactory() {
                            @Override
                            public Text getDisplayName() {
                                return Text.literal("末影箱");
                            }

                            @Override
                            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                                return GenericContainerScreenHandler.createGeneric9x3(syncId, inv, enderChest);
                            }
                        });

                        return 1;
                    }));
        });
    }
}
