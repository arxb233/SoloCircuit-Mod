package com.baris.solocircuit.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EasyCommand {
    private static final Logger LOGGER = LogManager.getLogger("SoloCircuit");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "solocircuit", "easy_commands.json");
    private static final Map<String, String> EASY_COMMANDS = Collections.synchronizedMap(loadEasyCommands());

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("easy")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("original", StringArgumentType.string())
                                        .then(CommandManager.argument("custom", StringArgumentType.string())
                                                .executes(context -> {
                                                    String original = StringArgumentType.getString(context, "original");
                                                    String custom = StringArgumentType.getString(context, "custom");

                                                    if (custom.contains(" ")) {
                                                        sendError(context.getSource(), "自定义命令名不能包含空格");
                                                        return 0;
                                                    }

                                                    EASY_COMMANDS.put(custom, original);
                                                    saveEasyCommandsAsync();
                                                    sendFeedback(context.getSource(),
                                                            "已添加自定义指令: /" + custom + " -> " + original);
                                                    return 1;
                                                }))))
                        .then(CommandManager.literal("list")
                                .executes(context -> {
                                    if (EASY_COMMANDS.isEmpty()) {
                                        sendFeedback(context.getSource(), "未定义任何自定义指令");
                                    } else {
                                        StringBuilder list = new StringBuilder("自定义指令列表:\n");
                                        EASY_COMMANDS.forEach((custom, original) ->
                                                list.append("/").append(custom).append(" -> ").append(original).append("\n"));
                                        sendFeedback(context.getSource(), list.toString().trim());
                                    }
                                    return 1;
                                }))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("custom", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            EASY_COMMANDS.keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String custom = StringArgumentType.getString(context, "custom");
                                            if (!EASY_COMMANDS.containsKey(custom)) {
                                                sendError(context.getSource(), "未找到自定义指令: " + custom);
                                                return 0;
                                            }
                                            EASY_COMMANDS.remove(custom);
                                            saveEasyCommandsAsync();
                                            sendFeedback(context.getSource(), "已删除自定义指令: " + custom);
                                            return 1;
                                        })))
                        .then(CommandManager.argument("custom", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    EASY_COMMANDS.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String custom = StringArgumentType.getString(context, "custom");
                                    String original = EASY_COMMANDS.get(custom);
                                    if (original == null) {
                                        sendError(context.getSource(), "未找到自定义指令: " + custom);
                                        return 0;
                                    }
                                    context.getSource().getServer().getCommandManager()
                                            .executeWithPrefix(context.getSource(), original);
                                    return 1;
                                }))));
    }

    private static Map<String, String> loadEasyCommands() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return GSON.fromJson(Files.readString(CONFIG_PATH),
                        new TypeToken<Map<String, String>>(){}.getType());
            }
        } catch (IOException e) {
            LOGGER.error("加载自定义指令文件失败: {}", CONFIG_PATH, e);
        }
        return new HashMap<>();
    }

    private static void saveEasyCommandsAsync() {
        new Thread(() -> {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GSON.toJson(EASY_COMMANDS));
            } catch (IOException e) {
                LOGGER.error("保存自定义指令文件失败: {}", CONFIG_PATH, e);
            }
        }, "EasyCommand-Save").start();
    }

    private static void sendFeedback(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }

    private static void sendError(ServerCommandSource source, String message) {
        source.sendError(Text.literal(message));
    }
}