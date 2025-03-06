package com.baris.solocircuit.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TrashCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config/solocircuit/trash_blacklist.json");
    private static final Set<Identifier> TRASH_BLACKLIST = loadTrashBlacklist();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("trash")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("item", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        Registries.ITEM.stream()
                                                .map(item -> Registries.ITEM.getId(item).toString())
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String itemName = StringArgumentType.getString(context, "item");
                                        Identifier itemId = Identifier.tryParse(itemName);
                                        if (itemId == null || Registries.ITEM.get(itemId) == Items.AIR) {
                                            context.getSource().sendError(Text.literal("无效的物品名称: " + itemName));
                                            return 0;
                                        }
                                        TRASH_BLACKLIST.add(itemId);
                                        saveTrashBlacklist();
                                        context.getSource().sendFeedback(() -> Text.literal("已将 " + itemName + " 添加到垃圾黑名单"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("item", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        TRASH_BLACKLIST.forEach(id -> builder.suggest(id.toString()));
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String itemName = StringArgumentType.getString(context, "item");
                                        Identifier itemId = Identifier.tryParse(itemName);
                                        if (itemId == null || !TRASH_BLACKLIST.remove(itemId)) {
                                            context.getSource().sendError(Text.literal("未找到 " + itemName + " 在垃圾黑名单中"));
                                            return 0;
                                        }
                                        saveTrashBlacklist();
                                        context.getSource().sendFeedback(() -> Text.literal("已从垃圾黑名单中移除 " + itemName), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("list")
                            .executes(context -> {
                                if (TRASH_BLACKLIST.isEmpty()) {
                                    context.getSource().sendFeedback(() -> Text.literal("垃圾黑名单为空"), false);
                                } else {
                                    String list = TRASH_BLACKLIST.stream()
                                            .map(Identifier::toString)
                                            .collect(Collectors.joining(", "));
                                    context.getSource().sendFeedback(() -> Text.literal("垃圾黑名单: " + list), false);
                                }
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("clear")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                int cleared = clearInventory(player, false);
                                context.getSource().sendFeedback(() -> Text.literal("已清除 " + cleared + " 个黑名单物品"), false);
                                return 1;
                            })
                            .then(CommandManager.literal("all")
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                        int cleared = clearInventory(player, true);
                                        context.getSource().sendFeedback(() -> Text.literal("已清除 " + cleared + " 个物品"), false);
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

    private static int clearInventory(ServerPlayerEntity player, boolean clearAll) {
        int cleared = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (clearAll || TRASH_BLACKLIST.contains(itemId)) {
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private static Set<Identifier> loadTrashBlacklist() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Set<String> ids = GSON.fromJson(json, new TypeToken<Set<String>>() {}.getType());
                return ids.stream()
                        .map(Identifier::tryParse) // 使用 tryParse 以避免非法 ID 导致崩溃
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
            }
        } catch (IOException e) {
            LOGGER.error("无法加载垃圾黑名单文件: {}", CONFIG_PATH, e);
        }
        return new HashSet<>();
    }

    private static void saveTrashBlacklist() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Set<String> ids = TRASH_BLACKLIST.stream()
                    .map(Identifier::toString)
                    .collect(Collectors.toSet());
            Files.writeString(CONFIG_PATH, GSON.toJson(ids));
        } catch (IOException e) {
            LOGGER.error("无法保存垃圾黑名单文件: {}", CONFIG_PATH, e);
        }
    }
}
