package com.baris.solocircuit.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SkyChunkCommand {
    // 创建一个静态线程池用于异步任务
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("skychunk")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
                            .executes(context -> clearChunks(context.getSource(),
                                    IntegerArgumentType.getInteger(context, "radius"), false))
                            .then(CommandManager.argument("option", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        builder.suggest("air");
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        int radius = IntegerArgumentType.getInteger(context, "radius");
                                        String option = StringArgumentType.getString(context, "option");
                                        boolean clearBedrock = "air".equalsIgnoreCase(option);
                                        return clearChunks(context.getSource(), radius, clearBedrock);
                                    }))));
        });
    }

    private static int clearChunks(ServerCommandSource source, int radius, boolean clearBedrock) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("此命令只能由玩家执行"));
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        World world = player.getWorld();
        var server = world.getServer();
        if (server == null) {
            source.sendError(Text.literal("无法获取服务器实例"));
            return 0;
        }

        // 计算中心区块坐标
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        // 计算边界区块
        int startChunkX = chunkX - radius;
        int endChunkX = chunkX + radius;
        int startChunkZ = chunkZ - radius;
        int endChunkZ = chunkZ + radius;

        // 计算方块坐标范围
        int startX = startChunkX << 4;
        int endX = (endChunkX << 4) + 15;
        int startZ = startChunkZ << 4;
        int endZ = (endChunkZ << 4) + 15;

        // 获取世界高度范围（这里仅用于边界检查）
        int bottomY = world.getBottomY();

        // 异步处理
        CompletableFuture.runAsync(() -> {
            processChunks(world, startX, endX, startZ, endZ, bottomY, clearBedrock);
            // 在主线程发送完成反馈
            server.execute(() -> {
                source.sendFeedback(() -> Text.literal(
                        String.format("已清空 %d×%d 个区块范围内的所有方块%s",
                                (endChunkX - startChunkX + 1),
                                (endChunkZ - startChunkZ + 1),
                                clearBedrock ? "（包括基岩）" : "")
                ), false);
            });
        }, EXECUTOR); // 使用自定义线程池

        source.sendFeedback(() -> Text.literal("正在异步清空区块，请稍候..."), false);
        return 1;
    }

    private static void processChunks(World world, int startX, int endX, int startZ, int endZ,
                                      int bottomY, boolean clearBedrock) {
        int chunkSize = 16;
        for (int x = startX; x <= endX; x += chunkSize) {
            for (int z = startZ; z <= endZ; z += chunkSize) {
                int maxX = Math.min(x + chunkSize - 1, endX);
                int maxZ = Math.min(z + chunkSize - 1, endZ);

                for (int cx = x; cx <= maxX; cx++) {
                    for (int cz = z; cz <= maxZ; cz++) {
                        // 使用带参数的 getTopY 获取顶部高度
                        int chunkTopY = world.getTopY(Heightmap.Type.WORLD_SURFACE, cx, cz);
                        for (int y = bottomY; y < chunkTopY; y++) {
                            BlockPos pos = new BlockPos(cx, y, cz);
                            if (clearBedrock || world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
                                // 在主线程中更新方块状态
                                Objects.requireNonNull(world.getServer()).execute(() ->
                                        world.setBlockState(pos, Blocks.AIR.getDefaultState())
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}