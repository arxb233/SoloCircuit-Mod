package com.baris.solocircuit;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoloCircuitMod implements ModInitializer {
    public static final String MOD_ID = "solocircuit-mod";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        fly();
    }
    public void fly() {
        // 注册 /fly 指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("fly")
                            .then(
                                    CommandManager.literal("speed")
                                            .then(
                                                    CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10.0f))
                                                            .executes(context -> {
                                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                                if (player == null) {
                                                                    context.getSource().sendError(Text.of("此命令只能由玩家执行！"));
                                                                    return 0;
                                                                }

                                                                // 获取速度参数
                                                                float speed = FloatArgumentType.getFloat(context, "value");
                                                                player.getAbilities().setFlySpeed(speed / 20.0f); // Minecraft 默认速度单位转换
                                                                player.sendAbilitiesUpdate(); // 同步到客户端

                                                                // 反馈消息
                                                                context.getSource().sendFeedback(
                                                                        () -> Text.of("飞行速度已设置为 " + speed), false
                                                                );
                                                                return 1;
                                                            })
                                            )
                            )
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player == null) {
                                    context.getSource().sendError(Text.of("此命令只能由玩家执行！"));
                                    return 0;
                                }

                                // 切换飞行状态
                                boolean isFlying = player.getAbilities().allowFlying;
                                player.getAbilities().allowFlying = !isFlying;
                                player.getAbilities().flying = !isFlying; // 如果启用飞行，立即进入飞行状态
                                player.sendAbilitiesUpdate(); // 同步到客户端

                                // 反馈消息
                                String message = isFlying ? "飞行已关闭" : "飞行已启用";
                                context.getSource().sendFeedback(() -> Text.of(message), false);
                                return 1;
                            })
            );
        });
    }
}