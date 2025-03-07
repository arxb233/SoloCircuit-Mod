package com.baris.solocircuit.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import java.util.ArrayList;
import java.util.List;

public class BoxCommand {
    private static final int SHULKER_BOX_SLOTS = 27; // 潜影盒的最大槽位数
    private static final String ITEMS_TAG = "Items";

    public static void register() {
        CommandRegistrationCallback.EVENT.register(BoxCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("box")
                .executes(ctx -> packInventory(ctx.getSource()))
                .then(CommandManager.literal("split")
                        .executes(ctx -> unpackShulkerBox(ctx.getSource()))));
    }

    private static int packInventory(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        int itemCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() != Items.SHULKER_BOX) {
                itemCount++;
            }
        }

        if (itemCount == 0) {
            player.sendMessage(Text.literal("没有可打包的物品！"), false);
            return 0;
        }

        if (itemCount > SHULKER_BOX_SLOTS) {
            player.sendMessage(Text.literal("物品数量过多，无法放入一个潜影盒！"), false);
            return 0;
        }

        ItemStack shulkerBox = new ItemStack(Items.SHULKER_BOX);
        NbtCompound nbt = new NbtCompound();
        NbtList invList = new NbtList();
        List<Integer> clearedSlots = new ArrayList<>();

        try {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() != Items.SHULKER_BOX) {
                    // 使用 CODEC 编码 ItemStack 到 NbtCompound
                    DataResult<NbtElement> encodeResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
                    NbtCompound itemNbt = (NbtCompound) encodeResult.result().orElseThrow(() ->
                            new RuntimeException("无法序列化物品: " + encodeResult.error().map(Object::toString).orElse("未知错误")));
                    itemNbt.putByte("Slot", (byte) i); // 记录槽位
                    invList.add(itemNbt);
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                    clearedSlots.add(i);
                }
            }

            if (invList.isEmpty()) {
                player.sendMessage(Text.literal("没有打包任何物品！"), false);
                return 0;
            }

            nbt.put(ITEMS_TAG, invList);
            // 将 NBT 数据应用到潜影盒
            DataResult<NbtElement> shulkerResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, shulkerBox);
            NbtCompound shulkerNbt = (NbtCompound) shulkerResult.result().orElseThrow(() ->
                    new RuntimeException("无法序列化潜影盒"));
            shulkerNbt.put(ITEMS_TAG, invList);
            shulkerBox = ItemStack.CODEC.parse(NbtOps.INSTANCE, shulkerNbt).result().orElse(shulkerBox);

            if (!player.getInventory().insertStack(shulkerBox)) {
                // 回滚
                for (int slot : clearedSlots) {
                    NbtCompound itemNbt = invList.getCompound(clearedSlots.indexOf(slot));
                    ItemStack item = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemNbt).result().orElse(ItemStack.EMPTY);
                    player.getInventory().setStack(slot, item);
                }
                player.sendMessage(Text.literal("背包没有空间存放潜影盒！"), false);
                return 0;
            }

            player.sendMessage(Text.literal("背包已打包到一个潜影盒中！"), false);
            return 1;

        } catch (Exception e) {
            player.sendMessage(Text.literal("打包背包时出错：" + e.getMessage()), false);
            // 回滚
            for (int slot : clearedSlots) {
                NbtCompound itemNbt = invList.getCompound(clearedSlots.indexOf(slot));
                ItemStack item = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemNbt).result().orElse(ItemStack.EMPTY);
                player.getInventory().setStack(slot, item);
            }
            return 0;
        }
    }

    private static int unpackShulkerBox(ServerCommandSource source) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        int shulkerSlot = -1;
        NbtCompound shulkerNbt = null;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.SHULKER_BOX) {
                shulkerSlot = i;
                DataResult<NbtElement> encodeResult = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
                shulkerNbt = (NbtCompound) encodeResult.result().orElse(null);
                break;
            }
        }

        if (shulkerSlot == -1 || shulkerNbt == null || !shulkerNbt.contains(ITEMS_TAG)) {
            player.sendMessage(Text.literal("未找到有效的已打包潜影盒！"), false);
            return 0;
        }

        NbtList items = shulkerNbt.getList(ITEMS_TAG, NbtElement.COMPOUND_TYPE);
        if (items.isEmpty()) {
            player.sendMessage(Text.literal("潜影盒为空！"), false);
            return 0;
        }

        // 使用自定义方法计算空槽位总数
        int emptySlots = getEmptySlotCount(player.getInventory());
        if (emptySlots < items.size()) {
            player.sendMessage(Text.literal("背包空间不足！需要 " + items.size() + " 个空槽位"), false);
            return 0;
        }

        try {
            for (int i = 0; i < items.size(); i++) {
                NbtCompound itemNbt = items.getCompound(i);
                DataResult<ItemStack> decodeResult = ItemStack.CODEC.parse(NbtOps.INSTANCE, itemNbt);
                ItemStack item = decodeResult.result().orElseThrow(() ->
                        new RuntimeException("无法反序列化物品"));
                if (!player.getInventory().insertStack(item)) {
                    throw new RuntimeException("无法插入物品到背包");
                }
            }

            player.getInventory().setStack(shulkerSlot, ItemStack.EMPTY);
            player.sendMessage(Text.literal("潜影盒已解包！"), false);
            return 1;

        } catch (Exception e) {
            player.sendMessage(Text.literal("解包潜影盒时出错：" + e.getMessage()), false);
            return 0;
        }
    }

    // 自定义方法：计算空槽位总数
    private static int getEmptySlotCount(PlayerInventory inventory) {
        int emptySlots = 0;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots;
    }
}