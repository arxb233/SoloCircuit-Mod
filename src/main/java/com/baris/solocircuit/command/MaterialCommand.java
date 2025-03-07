package com.baris.solocircuit.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MaterialCommand {
    private static final int SHULKER_BOX_SLOTS = 27;
    private static final String ITEMS_TAG = "Items";
    private static final File VIRTUAL_INVENTORY_FILE = new File("config/virtual_inventory.json");
    private static final File PROJECTION_FILE = new File("config/projection.json");
    private static Map<String, Integer> virtualInventory = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static void register() {
        CommandRegistrationCallback.EVENT.register(MaterialCommand::registerCommands);
        loadVirtualInventory();
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("material")
                .then(CommandManager.literal("add").executes(ctx -> addMaterials(ctx.getSource())))
                .then(CommandManager.literal("list").executes(ctx -> listMaterials(ctx.getSource())))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("item", StringArgumentType.string())
                                .executes(ctx -> removeMaterial(ctx.getSource(), StringArgumentType.getString(ctx, "item")))))
                .then(CommandManager.literal("user").executes(ctx -> provideUserMaterials(ctx.getSource()))));
    }

    private static void loadVirtualInventory() {
        if (VIRTUAL_INVENTORY_FILE.exists()) {
            try (FileReader reader = new FileReader(VIRTUAL_INVENTORY_FILE)) {
                virtualInventory = GSON.fromJson(reader, new TypeToken<Map<String, Integer>>(){}.getType());
            } catch (Exception e) {
                System.err.println("加载虚拟库存失败: " + e.getMessage());
            }
        }
        if (virtualInventory == null) virtualInventory = new HashMap<>();
    }

    private static void saveVirtualInventory() {
        try {
            VIRTUAL_INVENTORY_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(VIRTUAL_INVENTORY_FILE)) {
                GSON.toJson(virtualInventory, writer);
            }
        } catch (Exception e) {
            System.err.println("保存虚拟库存失败: " + e.getMessage());
        }
    }

    private static int addMaterials(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.SHULKER_BOX) {
                    NbtCompound nbt = getNbtFromStack(stack);
                    if (nbt != null && nbt.contains(ITEMS_TAG)) {
                        NbtList items = nbt.getList(ITEMS_TAG, NbtElement.COMPOUND_TYPE);
                        for (int j = 0; j < items.size(); j++) {
                            ItemStack item = parseItemStack(items.getCompound(j));
                            addToVirtualInventory(item);
                        }
                    }
                } else {
                    addToVirtualInventory(stack);
                }
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }

        saveVirtualInventory();
        player.sendMessage(Text.literal("物品已添加到虚拟库存！"), false);
        return 1;
    }

    private static int listMaterials(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        if (virtualInventory.isEmpty()) {
            player.sendMessage(Text.literal("虚拟库存为空！"), false);
        } else {
            player.sendMessage(Text.literal("虚拟库存物品："), false);
            virtualInventory.forEach((item, count) ->
                    player.sendMessage(Text.literal(item + ": " + count), false));
        }
        return 1;
    }

    private static int removeMaterial(ServerCommandSource source, String itemId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        int count = virtualInventory.getOrDefault(itemId, 0);
        if (count <= 0) {
            player.sendMessage(Text.literal("虚拟库存中没有 " + itemId), false);
            return 0;
        }

        ItemStack shulkerBox = packIntoShulkerBox(itemId, count);
        virtualInventory.remove(itemId);
        saveVirtualInventory();

        if (!player.getInventory().insertStack(shulkerBox)) {
            player.dropItem(shulkerBox, false);
        }
        player.sendMessage(Text.literal("已移除 " + itemId + " 并生成潜影盒！"), false);
        return 1;
    }

    private static int provideUserMaterials(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("此命令必须由玩家执行"));
            return 0;
        }

        Map<String, Integer> projection = loadProjection();
        if (projection == null || projection.isEmpty()) {
            player.sendMessage(Text.literal("投影文件为空或加载失败！"), false);
            return 0;
        }

        Map<String, Integer> missing = new HashMap<>();
        List<ItemStack> shulkerBoxes = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : projection.entrySet()) {
            String itemId = entry.getKey();
            int required = entry.getValue();
            int available = virtualInventory.getOrDefault(itemId, 0);

            if (available < required) {
                missing.put(itemId, required - available);
            } else {
                shulkerBoxes.addAll(packIntoShulkerBoxes(itemId, required));
                virtualInventory.put(itemId, available - required);
                if (virtualInventory.get(itemId) == 0) virtualInventory.remove(itemId);
            }
        }

        if (!missing.isEmpty()) {
            player.sendMessage(Text.literal("库存不足，缺少以下物品："), false);
            missing.forEach((item, count) ->
                    player.sendMessage(Text.literal(item + ": " + count), false));
            return 0;
        }

        for (ItemStack box : shulkerBoxes) {
            if (!player.getInventory().insertStack(box)) {
                player.dropItem(box, false);
            }
        }
        saveVirtualInventory();
        player.sendMessage(Text.literal("已根据投影文件生成潜影盒！"), false);
        return 1;
    }

    private static NbtCompound getNbtFromStack(ItemStack stack) {
        DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
        return (NbtCompound) result.result().orElse(null);
    }

    private static ItemStack parseItemStack(NbtCompound nbt) {
        return ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(ItemStack.EMPTY);
    }

    private static void addToVirtualInventory(ItemStack stack) {
        String itemId = stack.getItem().toString();
        int count = stack.getCount();
        virtualInventory.merge(itemId, count, Integer::sum);
    }

    // 修改：使用 ItemStack.getMaxCount()
    private static ItemStack packIntoShulkerBox(String itemId, int count) {
        ItemStack shulkerBox = new ItemStack(Items.SHULKER_BOX);
        NbtCompound nbt = new NbtCompound();
        NbtList items = new NbtList();

        // 根据物品 ID 创建临时 ItemStack 以获取最大堆叠数量
        ItemStack tempStack = new ItemStack(Identifier.of(itemId).getPath().equals("diamond") ? Items.DIAMOND : Items.IRON_INGOT);
        int maxStackSize = tempStack.getMaxCount();
        int stacks = (int) Math.ceil((double) count / maxStackSize);

        for (int i = 0; i < Math.min(stacks, SHULKER_BOX_SLOTS); i++) {
            int stackCount = Math.min(count, maxStackSize);
            ItemStack stack = new ItemStack(Identifier.of(itemId).getPath().equals("diamond") ? Items.DIAMOND : Items.IRON_INGOT, stackCount);
            NbtCompound itemNbt = getNbtFromStack(stack);
            itemNbt.putByte("Slot", (byte) i);
            items.add(itemNbt);
            count -= stackCount;
        }

        nbt.put(ITEMS_TAG, items);
        shulkerBox = ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(shulkerBox);
        return shulkerBox;
    }

    // 修改：使用 ItemStack.getMaxCount()
    private static List<ItemStack> packIntoShulkerBoxes(String itemId, int count) {
        List<ItemStack> boxes = new ArrayList<>();
        ItemStack tempStack = new ItemStack(Identifier.of(itemId).getPath().equals("diamond") ? Items.DIAMOND : Items.IRON_INGOT);
        int maxStackSize = tempStack.getMaxCount();

        while (count > 0) {
            int packCount = Math.min(count, SHULKER_BOX_SLOTS * maxStackSize);
            ItemStack box = packIntoShulkerBox(itemId, packCount);
            boxes.add(box);
            count -= packCount;
        }
        return boxes;
    }

    private static Map<String, Integer> loadProjection() {
        if (PROJECTION_FILE.exists()) {
            try (FileReader reader = new FileReader(PROJECTION_FILE)) {
                return GSON.fromJson(reader, new TypeToken<Map<String, Integer>>(){}.getType());
            } catch (Exception e) {
                System.err.println("加载投影文件失败: " + e.getMessage());
            }
        }
        return null;
    }
}