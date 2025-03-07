package com.baris.solocircuit.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ScCommand {
    private static boolean cheatMode = false;
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sc")
                    .then(CommandManager.literal("cheat")
                            .then(CommandManager.literal("true")
                                    .executes(context -> {
                                        cheatMode = !cheatMode;
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            player.sendMessage(Text.of("SoloCircuit-Mod作弊模式: " + (cheatMode ? "开启" : "关闭")), false);
                                        }
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }
    public static boolean isCheatMode() {
        return !cheatMode;
    }
}
