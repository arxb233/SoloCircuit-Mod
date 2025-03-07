package com.baris.solocircuit.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class DigCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("dig")
                    .then(CommandManager.literal("speed")
                            .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 10))
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayerOrThrow();
                                        if (ScCommand.isCheatMode()) {
                                            source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
                                            return 0;
                                        }
                                        int level = IntegerArgumentType.getInteger(context, "level");

                                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 6000, level - 1));

                                        source.sendFeedback(() -> Text.literal("挖掘速度已设置为: " + level), false);
                                        return 1;
                                    }))));
        });
    }
}
