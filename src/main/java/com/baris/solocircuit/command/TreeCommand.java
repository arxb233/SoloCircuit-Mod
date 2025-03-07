package com.baris.solocircuit.command;

import com.baris.solocircuit.util.TreeChopper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class TreeCommand {

    // 存储启用树砍功能的玩家
    private static final Set<PlayerEntity> enabledPlayers = new HashSet<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tree")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        PlayerEntity player = source.getPlayer();
                        if (ScCommand.isCheatMode()) {
                            source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
                            return 0;
                        }
                        if (player != null) {
                            if (enabledPlayers.contains(player)) {
                                enabledPlayers.remove(player);
                                source.sendFeedback(() -> Text.literal("Tree Chopper 已禁用"), false);
                            } else {
                                enabledPlayers.add(player);
                                source.sendFeedback(() -> Text.literal("Tree Chopper 已启用"), false);
                            }
                            // 将启用状态传递给 TreeChopper
                            TreeChopper.setPlayerEnabled(player, enabledPlayers.contains(player));
                            return 1;
                        }
                        return 0;
                    }));
        });
    }

    // 获取启用状态（供其他类使用）
    public static boolean isPlayerEnabled(PlayerEntity player) {
        return enabledPlayers.contains(player);
    }
}
