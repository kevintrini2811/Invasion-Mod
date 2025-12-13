package com.invasion;

import java.util.Map;
import java.util.function.Consumer;

import com.invasion.entity.ElectricityBoltEntity;
import com.invasion.nexus.ControllableNexusAccess;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.nexus.test.Tester;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NumberRangeArgumentType;
import net.minecraft.predicate.NumberRange.IntRange;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class InvasionCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> create(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registries) {
        return addTestCommands(CommandManager.literal("invasion")
                .then(CommandManager.literal("help").executes(context -> help(dispatcher, context.getSource())))
                .then(CommandManager.literal("pause").executes(context -> pause(context.getSource())))
                .then(CommandManager.literal("status").executes(context -> status(context.getSource())))
                .then(CommandManager.literal("start").then(CommandManager.argument("wave", IntegerArgumentType.integer(1)).executes(context -> start(context.getSource(), IntegerArgumentType.getInteger(context, "wave")))))
                .then(CommandManager.literal("stop").executes(context -> stop(context.getSource())))
                .then(CommandManager.literal("radius")
                        .then(CommandManager.literal("get").executes(context -> getRadius(context.getSource())))
                        .then(CommandManager.literal("set").then(CommandManager.argument("radius", IntegerArgumentType.integer(32, 128)).executes(context -> setRadius(context.getSource(), IntegerArgumentType.getInteger(context, "radius"))))))
                .then(CommandManager.literal("bolt").executes(context -> bolt(context.getSource(), Vec3i.ZERO))
                    .then(CommandManager.argument("offset", BlockPosArgumentType.blockPos()).executes(context -> bolt(context.getSource(), BlockPosArgumentType.getBlockPos(context, "offset"))))
                ));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> addTestCommands(LiteralArgumentBuilder<ServerCommandSource> builder) {
        if (!InvasionMod.getConfig().debugMode) {
            return builder;
        }
        return builder.then(CommandManager.literal("test").requires(source -> InvasionMod.getConfig().debugMode)
                .then(CommandManager.literal("status").executes(context -> printDebugStatus(context.getSource())))
                .then(CommandManager.literal("spawner").executes(context -> testSpawner(context.getSource(), IntRange.between(1, 11)))
                        .then(CommandManager.argument("waves", NumberRangeArgumentType.intRange()).executes(context -> testSpawner(context.getSource(), NumberRangeArgumentType.IntRangeArgumentType.getRangeArgument(context, "waves")))))
                .then(CommandManager.literal("spawnPoints").executes(context -> testSpawnpoints(context.getSource())))
                .then(CommandManager.literal("waveBuilder").executes(context -> testWaveBuilder(context.getSource(), 1, 1, 160))
                        .then(CommandManager.argument("difficuly", FloatArgumentType.floatArg(0)).executes(context -> testWaveBuilder(context.getSource(),
                                        FloatArgumentType.getFloat(context, "difficuly"), 1, 160))
                                .then(CommandManager.argument("tier", FloatArgumentType.floatArg(1)).executes(context -> testWaveBuilder(context.getSource(),
                                            FloatArgumentType.getFloat(context, "difficuly"),
                                            FloatArgumentType.getFloat(context, "tier"), 160))
                                        .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1000)).executes(context -> testWaveBuilder(context.getSource(),
                                                FloatArgumentType.getFloat(context, "difficuly"),
                                                FloatArgumentType.getFloat(context, "tier"),
                                                IntegerArgumentType.getInteger(context, "duration")))))))
        );
    }

    private static void handleWithNexus(ServerCommandSource source, Consumer<ControllableNexusAccess> nexusConsumer) {
        WorldNexusStorage.of(source.getWorld()).getNexus().ifPresentOrElse(nexusConsumer, () -> {
            source.sendFeedback(() -> Text.literal("Right-click the Nexus first to set target for commands.").formatted(Formatting.GOLD), false);
        });
    }

    private static int start(ServerCommandSource source, int startingWave) {
        handleWithNexus(source, nexus -> {
            nexus.start(startingWave);
            source.getServer().sendMessage(Text.literal(source.getName() + " has started the invasion!").formatted(Formatting.YELLOW));
        });
        return 0;
    }

    private static int stop(ServerCommandSource source) {
        handleWithNexus(source, nexus -> {
            InvasionMod.LOGGER.info("Nexus manually stopped by command");
            nexus.stop(true);
            source.getServer().sendMessage(Text.literal(source.getName() + " has ended the invasion!").formatted(Formatting.RED));
        });
        return 0;
    }

    private static int getRadius(ServerCommandSource source) {
        handleWithNexus(source, nexus -> {
            source.sendFeedback(() -> Text.literal("The nexus spawn radius is " + nexus.getSpawnRadius()).formatted(Formatting.GREEN), false);
        });
        return 0;
    }

    private static int setRadius(ServerCommandSource source, int radius) {
        handleWithNexus(source, nexus -> {
            if (nexus.setSpawnRadius(radius)) {
                source.sendFeedback(() -> Text.literal("Set nexus range to " + radius).formatted(Formatting.GREEN), false);
            } else {
                source.sendFeedback(() -> Text.literal("Can't change range while Nexus is active.").formatted(Formatting.RED), false);
            }
        });
        return 0;
    }

    private static int testSpawner(ServerCommandSource source, IntRange waves) {
        new Tester(message -> {
            source.sendFeedback(() -> Text.literal(message), false);
        }).doWaveSpawnerTest(waves.min().orElseThrow(), waves.max().orElseThrow());
        return 0;
    }

    private static int testSpawnpoints(ServerCommandSource source) {
        new Tester(message -> {
            source.sendFeedback(() -> Text.literal(message), false);
        }).doSpawnPointSelectionTest();
        return 0;
    }

    private static int testWaveBuilder(ServerCommandSource source, float difficulty, float tier, int duration) {
        new Tester(message -> {
            source.sendFeedback(() -> Text.literal(message), false);
        }).doWaveBuilderTest(difficulty, tier, duration);
        return 0;
    }

    private static int printDebugStatus(ServerCommandSource source) {
        handleWithNexus(source, nexus -> {
            nexus.getStatus().forEach(line -> source.sendFeedback(() -> line, false));
        });
        return 0;
    }

	private static int bolt(ServerCommandSource source, Vec3i offset) {
	    handleWithNexus(source, nexus -> {
	        BlockPos nexusPos = nexus.getOrigin();
	        source.getWorld().spawnEntity(new ElectricityBoltEntity(source.getWorld(), nexusPos.toCenterPos(), nexusPos.add(offset).toCenterPos(), 40, true));
	    });
        return 0;
    }

	private static int status(ServerCommandSource source) {
	    handleWithNexus(source, nexus -> {
	        source.sendFeedback(() -> Text.literal("Nexus status: ").formatted(Formatting.GREEN).append(Text.literal("" + nexus.isActive()).formatted(Formatting.DARK_GREEN)), false);
	    });

	    return 0;
	}
    private static int pause(ServerCommandSource source) {
        handleWithNexus(source, nexus -> {
            // Wenn weder aktiv noch pausiert → es gibt nichts zu tun
            if (!nexus.isActive() && !nexus.isPaused()) {
                source.sendFeedback(
                        () -> Text.literal("Es läuft gerade keine Invasion, die pausiert oder fortgesetzt werden könnte.")
                                .formatted(Formatting.RED),
                        false
                );
                return;
            }

            boolean nowPaused = nexus.togglePause();

            if (nowPaused) {
                // Jetzt PAUSIERT
                source.getServer().sendMessage(
                        Text.literal(source.getName() + " hat die Invasion pausiert. Alle Invasions-Mobs wurden entfernt.")
                                .formatted(Formatting.GOLD)
                );
            } else {
                // Jetzt wieder aktiv
                source.getServer().sendMessage(
                        Text.literal(source.getName() + " hat die Invasion fortgesetzt.")
                                .formatted(Formatting.GREEN)
                );
            }
        });
        return 0;
    }


    private static int help(CommandDispatcher<ServerCommandSource> dispatcher, ServerCommandSource source) {
	    Map<CommandNode<ServerCommandSource>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot().getChild("invasion"), source);

        for (String name : map.values()) {
            source.sendFeedback(() -> Text.literal("/" + name), false);
        }

        return map.size();
	}
}