package com.baris.solocircuit.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HurtCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(HurtCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("hurt")
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("ratio", FloatArgumentType.floatArg(0.0f, 1.0f))
                                .executes(ctx -> setHurtRatio(ctx.getSource(), FloatArgumentType.getFloat(ctx, "ratio"))))));
    }

    private static int setHurtRatio(ServerCommandSource source, float ratio) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow(); // 确保是玩家执行

        if (ScCommand.isCheatMode()) {
            source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
            return 0;
        }

        // 计算 Resistance 等级
        int resistanceLevel = Math.round((1.0f - ratio) / 0.2f);
        if (resistanceLevel > 5) resistanceLevel = 5; // 最高 5 级
        if (resistanceLevel < 0) resistanceLevel = 0; // 最低 0 级

        // 清除现有的 Resistance 效果
        player.removeStatusEffect(StatusEffects.RESISTANCE);

        // 如果比例不是 1.0（全额伤害），添加 Resistance 效果
        if (resistanceLevel > 0) {
            StatusEffectInstance resistance = new StatusEffectInstance(
                    StatusEffects.RESISTANCE, // 抗性效果
                    1000000, // 持续时间（秒）
                    resistanceLevel - 1, // 等级从 0 开始（0=一级）
                    false, // 不显示环境效果
                    false // 不显示粒子
            );
            player.addStatusEffect(resistance);
        }

        // 反馈消息
        player.sendMessage(Text.literal("伤害比例已设置为 " + String.format("%.1f", ratio) +
                "（抗性等级 " + resistanceLevel + "）"), false);
        return 1;
    }
}