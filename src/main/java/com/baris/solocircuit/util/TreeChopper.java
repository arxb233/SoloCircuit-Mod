package com.baris.solocircuit.util;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class TreeChopper {

    private static final Set<PlayerEntity> enabledPlayers = new HashSet<>();

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient && enabledPlayers.contains(player)) { // 服务器端检查
                ItemStack heldItem = player.getStackInHand(hand);
                if (heldItem.getItem() == Items.DIAMOND_AXE) {
                    BlockState state = world.getBlockState(pos);
                    if (isLog(state.getBlock())) {
                        chopTree(world, pos, player, heldItem);
                        return ActionResult.SUCCESS; // 阻止默认破坏行为
                    }
                }
            }
            return ActionResult.PASS; // 继续默认行为
        });
    }

    public static void setPlayerEnabled(PlayerEntity player, boolean enabled) {
        if (enabled) {
            enabledPlayers.add(player);
        } else {
            enabledPlayers.remove(player);
        }
    }

    private static boolean isLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG ||
                block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
                block == Blocks.CHERRY_LOG || block == Blocks.MANGROVE_LOG;
    }

    private static void chopTree(World world, BlockPos startPos, PlayerEntity player, ItemStack axe) {
        Set<BlockPos> logsToBreak = new HashSet<>();
        findTreeBlocks(world, startPos, logsToBreak);

        for (BlockPos pos : logsToBreak) {
            world.breakBlock(pos, true, player); // 破坏方块并掉落物品
            if (!axe.isEmpty()) {
                // 使用 ItemStack.damage 处理耐久减少，自动通知客户端
                axe.damage(1, player, null); // null 表示不额外处理损坏回调
                if (axe.isEmpty()) break; // 工具损坏后停止
            }
        }
    }

    private static void findTreeBlocks(World world, BlockPos pos, Set<BlockPos> logsToBreak) {
        if (logsToBreak.contains(pos) || logsToBreak.size() > 100) return;

        BlockState state = world.getBlockState(pos);
        if (isLog(state.getBlock())) {
            logsToBreak.add(pos);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos newPos = pos.add(x, y, z);
                        findTreeBlocks(world, newPos, logsToBreak);
                    }
                }
            }
        }
    }
}
