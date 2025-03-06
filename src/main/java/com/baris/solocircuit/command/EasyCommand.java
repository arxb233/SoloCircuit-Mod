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
import java.util.HashMap;
import java.util.Map;

public class EasyCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config/solocircuit/easy_commands.json");
    private static final Map<String, String> EASY_COMMANDS = loadEasyCommands();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("easy")
                    .requires(source -> source.hasPermissionLevel(2))

                    // 添加自定义指令
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("original", StringArgumentType.string())
                                    .then(CommandManager.argument("custom", StringArgumentType.string())
                                            .executes(context -> {
                                                String original = StringArgumentType.getString(context, "original");
                                                String custom = StringArgumentType.getString(context, "custom");
                                                EASY_COMMANDS.put(custom, original);
                                                saveEasyCommands();
                                                context.getSource().sendFeedback(() ->
                                                        Text.literal("已添加自定义指令: /" + custom + " -> " + original), false);
                                                return 1;
                                            })
                                    )
                            )
                    )

                    // 列出所有自定义指令
                    .then(CommandManager.literal("list")
                            .executes(context -> {
                                if (EASY_COMMANDS.isEmpty()) {
                                    context.getSource().sendFeedback(() -> Text.literal("未定义任何自定义指令"), false);
                                } else {
                                    StringBuilder list = new StringBuilder("自定义指令:\n");
                                    EASY_COMMANDS.forEach((custom, original) ->
                                            list.append("/").append(custom).append(" -> ").append(original).append("\n"));
                                    context.getSource().sendFeedback(() -> Text.literal(list.toString().trim()), false);
                                }
                                return 1;
                            })
                    )

                    // 删除自定义指令
                    .then(CommandManager.literal("remove") // 由 deleted 改为 remove
                            .then(CommandManager.argument("custom", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        EASY_COMMANDS.keySet().forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String custom = StringArgumentType.getString(context, "custom");
                                        if (!EASY_COMMANDS.containsKey(custom)) {
                                            context.getSource().sendError(Text.literal("未找到自定义指令: " + custom));
                                            return 0;
                                        }
                                        EASY_COMMANDS.remove(custom);
                                        saveEasyCommands();
                                        context.getSource().sendFeedback(() ->
                                                Text.literal("已删除自定义指令: " + custom), false);
                                        return 1;
                                    })
                            )
                    )

                    // 执行自定义指令
                    .then(CommandManager.argument("custom", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                EASY_COMMANDS.keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String custom = StringArgumentType.getString(context, "custom");
                                String original = EASY_COMMANDS.get(custom);
                                if (original == null) {
                                    context.getSource().sendError(Text.literal("未找到自定义指令: " + custom));
                                    return 0;
                                }
                                context.getSource().getServer().getCommandManager().getDispatcher()
                                        .execute(original, context.getSource());
                                return 1;
                            })
                    )
            );
        });
    }

    private static Map<String, String> loadEasyCommands() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
            }
        } catch (IOException e) {
            LOGGER.error("无法加载自定义指令文件: {}", CONFIG_PATH, e);
        }
        return new HashMap<>();
    }

    private static void saveEasyCommands() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(EASY_COMMANDS));
        } catch (IOException e) {
            LOGGER.error("无法保存自定义指令文件: {}", CONFIG_PATH, e);
        }
    }
}
