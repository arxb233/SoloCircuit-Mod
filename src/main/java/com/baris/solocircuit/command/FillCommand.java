package com.baris.solocircuit.command;

import com.baris.solocircuit.state.FillLimitState;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FillCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sc")
                    .then(CommandManager.literal("update")
                            .then(CommandManager.literal("fill")
                                    .then(CommandManager.argument("limit", IntegerArgumentType.integer(1, 500000))
                                            .executes(context -> {
                                                int newLimit = IntegerArgumentType.getInteger(context, "limit");
                                                FillLimitState.setFillLimit(newLimit);

                                                ServerCommandSource source = context.getSource();
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                if (ScCommand.isCheatMode()) {
                                                    source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
                                                    return 0;
                                                }
                                                if (player != null) {
                                                    player.sendMessage(Text.of("填充限制更新为 " + newLimit + "块（将在重新启动时重置）"), false);
                                                }

                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        });
    }
}
