package com.baris.solocircuit.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.world.Heightmap;


public class FlatCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("flat")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> flattenArea(context.getSource(), null))
                    .then(CommandManager.argument("block", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                // 限制建议数量以提高性能
                                Registries.BLOCK.stream()
                                        .limit(50) // 可调整限制
                                        .forEach(block -> {
                                            Identifier id = Registries.BLOCK.getId(block);
                                            builder.suggest(id.toString());
                                        });
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String blockName = StringArgumentType.getString(context, "block");
                                return flattenArea(context.getSource(), blockName);
                            })));
        });
    }

    private static int flattenArea(ServerCommandSource source, String blockName) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("此命令只能由玩家执行"));
            return 0;
        }

        if (ScCommand.isCheatMode()) {
            source.sendError(Text.of("当前功能需要开启SoloCircuit-Mod作弊模式，请联系op！ /sc cheat true"));
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        World world = player.getWorld();

        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int startX = (chunkX - 3) << 4;
        int endX = ((chunkX + 3) << 4) + 15;
        int startZ = (chunkZ - 3) << 4;
        int endZ = ((chunkZ + 3) << 4) + 15;
        int playerY = playerPos.getY();

        boolean replaceGround = blockName != null;
        Block replaceBlock = Blocks.AIR;
        if (replaceGround) {
            try {
                Identifier blockId = Identifier.tryParse(blockName.toLowerCase());
                if (blockId == null) {
                    source.sendError(Text.literal("无效的方块名称格式: " + blockName));
                    return 0;
                }
                replaceBlock = Registries.BLOCK.get(blockId);
                if (replaceBlock == Blocks.AIR && !blockId.equals(Identifier.tryParse("minecraft:air"))) {
                    source.sendError(Text.literal("无效的方块名称: " + blockName));
                    return 0;
                }
            } catch (Exception e) {
                source.sendError(Text.literal("无效的方块名称格式: " + blockName));
                return 0;
            }
        }

        Block finalReplaceBlock = replaceBlock;
        var server = world.getServer();
        if (server == null) {
            source.sendError(Text.literal("无法获取服务器实例"));
            return 0;
        }
        server.execute(() -> {
            processAreaInChunks(world, startX, endX, startZ, endZ, playerY, finalReplaceBlock, replaceGround);
        });

        // 使用更友好的方块名称显示
        String blockDisplayName = replaceGround ?
                Text.translatable(replaceBlock.getTranslationKey()).getString() : "";
        source.sendFeedback(() -> Text.literal(
                String.format("已清理 %d×%d 个区块范围内高于 Y=%d 的方块%s",
                        7, 7, playerY,
                        replaceGround ? "，并替换地面为 " + blockDisplayName : "")
        ), false);
        return 1;
    }

    private static void processAreaInChunks(World world, int startX, int endX, int startZ, int endZ,
                                            int playerY, Block replaceBlock, boolean replaceGround) {
        int chunkSize = 16;
        for (int x = startX; x <= endX; x += chunkSize) {
            for (int z = startZ; z <= endZ; z += chunkSize) {
                int maxX = Math.min(x + chunkSize - 1, endX);
                int maxZ = Math.min(z + chunkSize - 1, endZ);

                for (int cx = x; cx <= maxX; cx++) {
                    for (int cz = z; cz <= maxZ; cz++) {
                        int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx, cz);
                        for (int y = topY - 1; y > playerY; y--) {
                            BlockPos pos = new BlockPos(cx, y, cz);
                            if (!world.isAir(pos)) {
                                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                            }
                        }
                        if (replaceGround) {
                            world.setBlockState(new BlockPos(cx, playerY, cz),
                                    replaceBlock.getDefaultState());
                        }
                    }
                }
            }
        }
    }
}