package com.baris.solocircuit.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class NetherHereCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("netherhere")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        Vec3d pos = source.getPosition();

                        // 计算地狱坐标（Y 坐标通常保持不变）
                        double netherX = pos.x / 8.0;
                        double netherY = pos.y; // Y 坐标通常不变
                        double netherZ = pos.z / 8.0;

                        // 发送消息给玩家
                        source.sendFeedback(() -> Text.literal(String.format(
                                "当前坐标: X=%.2f, Y=%.2f, Z=%.2f\n地狱对应坐标: X=%.2f, Y=%.2f, Z=%.2f",
                                pos.x, pos.y, pos.z, netherX, netherY, netherZ
                        )), false);
                        return 1;
                    }));
        });
    }
}
