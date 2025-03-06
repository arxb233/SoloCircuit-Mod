package com.baris.solocircuit.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FlyCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("fly")
                            .then(CommandManager.literal("speed")
                                    .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10.0f))
                                            .executes(context -> setFlySpeed(context.getSource(),
                                                    FloatArgumentType.getFloat(context, "value")))))
                            .executes(context -> toggleFlight(context.getSource()))
            );
        });
    }

    private static int toggleFlight(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.of("此命令只能由玩家执行！"));
            return 0;
        }

        boolean isFlying = player.getAbilities().allowFlying;
        player.getAbilities().allowFlying = !isFlying;
        player.getAbilities().flying = !isFlying;
        player.sendAbilitiesUpdate();

        source.sendFeedback(() -> Text.of(isFlying ? "飞行已关闭" : "飞行已启用"), false);
        return 1;
    }

    private static int setFlySpeed(ServerCommandSource source, float speed) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.of("此命令只能由玩家执行！"));
            return 0;
        }

        player.getAbilities().setFlySpeed(speed / 20.0f);
        player.sendAbilitiesUpdate();

        source.sendFeedback(() -> Text.of("飞行速度已设置为 " + speed), false);
        return 1;
    }
}
