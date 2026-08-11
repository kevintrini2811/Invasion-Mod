package com.invasion;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.MinMaxBounds.Ints;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.invasion.block.InvBlockEntities;
import com.invasion.block.NexusBlockEntity;
import com.invasion.nexus.ControllableNexusAccess;
import com.invasion.nexus.Mode;
import com.invasion.nexus.WorldNexusStorage;
import com.invasion.nexus.test.Tester;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

public class InvasionCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> create(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registries) {
        return addTestCommands(Commands.literal("invasion")
                .then(Commands.literal("help").executes(context -> help(dispatcher, context.getSource())))
                .then(Commands.literal("pause").executes(context -> pause(context.getSource())))
                .then(Commands.literal("continue").executes(context -> continueInvasion(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("wave", IntegerArgumentType.integer(1))
                                .executes(context -> setWave(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "wave")))))
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource(), 1))
                        .then(Commands.argument("wave", IntegerArgumentType.integer(1))
                                .executes(context -> start(context.getSource(), IntegerArgumentType.getInteger(context, "wave")))))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                .then(Commands.literal("radius")
                        .then(Commands.literal("get").executes(context -> getRadius(context.getSource())))
                        .then(Commands.literal("set").then(Commands.argument("radius", IntegerArgumentType.integer(32, 128)).executes(context -> setRadius(context.getSource(), IntegerArgumentType.getInteger(context, "radius"))))))
                .then(Commands.literal("bolt").executes(context -> bolt(context.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addTestCommands(LiteralArgumentBuilder<CommandSourceStack> builder) {
        if (!InvasionMod.getConfig().debugMode) {
            return builder;
        }
        return builder.then(Commands.literal("test").requires(source -> InvasionMod.getConfig().debugMode)
                .then(Commands.literal("status").executes(context -> printDebugStatus(context.getSource())))
                .then(Commands.literal("spawner").executes(context -> testSpawner(context.getSource(), Ints.between(1, 11)))
                        .then(Commands.argument("waves", RangeArgument.intRange()).executes(context -> testSpawner(context.getSource(), RangeArgument.Ints.getRange(context, "waves")))))
                .then(Commands.literal("spawnPoints").executes(context -> testSpawnpoints(context.getSource())))
                .then(Commands.literal("waveBuilder").executes(context -> testWaveBuilder(context.getSource(), 1, 1, 160))
                        .then(Commands.argument("difficuly", FloatArgumentType.floatArg(0)).executes(context -> testWaveBuilder(context.getSource(),
                                        FloatArgumentType.getFloat(context, "difficuly"), 1, 160))
                                .then(Commands.argument("tier", FloatArgumentType.floatArg(1)).executes(context -> testWaveBuilder(context.getSource(),
                                            FloatArgumentType.getFloat(context, "difficuly"),
                                            FloatArgumentType.getFloat(context, "tier"), 160))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000)).executes(context -> testWaveBuilder(context.getSource(),
                                                FloatArgumentType.getFloat(context, "difficuly"),
                                                FloatArgumentType.getFloat(context, "tier"),
                                                IntegerArgumentType.getInteger(context, "duration")))))))
        );
    }

    private static void handleWithNexus(CommandSourceStack source, Consumer<ControllableNexusAccess> nexusConsumer) {
        WorldNexusStorage.of(source.getLevel()).getNexus().ifPresentOrElse(nexusConsumer, () -> {
            source.sendSuccess(() -> Component.literal("Right-click the Nexus first to set target for commands.").withStyle(ChatFormatting.GOLD), false);
        });
    }

    private static int start(CommandSourceStack source, int startingWave) {
        WorldNexusStorage storage = WorldNexusStorage.of(source.getLevel());
        ControllableNexusAccess activeNexus = storage.getNexus().orElse(null);
        if (activeNexus == null) {
            activeNexus = storage.getNearestNexus(
                    BlockPos.containing(source.getPosition())).orElse(null);
        }
        if (activeNexus == null) {
            activeNexus = storage.recoverNearestLoadedNexus(
                    BlockPos.containing(source.getPosition())).orElse(null);
        }
        if (activeNexus == null) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.place_nexus").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (activeNexus.isActive()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.invasion_already_active").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!activeNexus.start(startingWave)) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.invasion_start_failed").withStyle(ChatFormatting.RED));
            return 0;
        }

        source.getServer().sendSystemMessage(Component.translatable(
                "invmod.message.command.invasion_started",
                source.getDisplayName(), startingWave).withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        ControllableNexusAccess nexus =
                WorldNexusStorage.of(source.getLevel()).getNexus().orElse(null);
        if (nexus == null || !nexus.isActive()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_invasion_to_stop")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        InvasionMod.LOGGER.debug("Nexus manually stopped by command");
        nexus.stop(true);
        source.sendSuccess(() -> Component.translatable(
                "invmod.message.command.invasion_stopped")
                .withStyle(ChatFormatting.RED), true);
        return 1;
    }

    private static int getRadius(CommandSourceStack source) {
        handleWithNexus(source, nexus -> {
            source.sendSuccess(() -> Component.literal("The nexus spawn radius is " + nexus.getSpawnRadius()).withStyle(ChatFormatting.GREEN), false);
        });
        return 0;
    }

    private static int setRadius(CommandSourceStack source, int radius) {
        handleWithNexus(source, nexus -> {
            if (nexus.setSpawnRadius(radius)) {
                source.sendSuccess(() -> Component.literal("Set nexus range to " + radius).withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() -> Component.literal("Can't change range while Nexus is active.").withStyle(ChatFormatting.RED), false);
            }
        });
        return 0;
    }

    private static int testSpawner(CommandSourceStack source, Ints waves) {
        new Tester(message -> {
            source.sendSuccess(() -> Component.literal(message), false);
        }).doWaveSpawnerTest(waves.min().orElseThrow(), waves.max().orElseThrow());
        return 0;
    }

    private static int testSpawnpoints(CommandSourceStack source) {
        new Tester(message -> {
            source.sendSuccess(() -> Component.literal(message), false);
        }).doSpawnPointSelectionTest();
        return 0;
    }

    private static int testWaveBuilder(CommandSourceStack source, float difficulty, float tier, int duration) {
        new Tester(message -> {
            source.sendSuccess(() -> Component.literal(message), false);
        }).doWaveBuilderTest(difficulty, tier, duration);
        return 0;
    }

    private static int printDebugStatus(CommandSourceStack source) {
        handleWithNexus(source, nexus -> {
            nexus.getStatus().forEach(line -> source.sendSuccess(() -> line, false));
        });
        return 0;
    }

	private static int bolt(CommandSourceStack source) {
        var nexus = WorldNexusStorage.of(source.getLevel()).getNexus();
        if (nexus.isEmpty()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_nexus_for_beam")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        NexusBlockEntity blockEntity = source.getLevel()
                .getBlockEntity(nexus.get().getOrigin(), InvBlockEntities.NEXUS)
                .map(entity -> (NexusBlockEntity) entity)
                .orElse(null);
        if (blockEntity == null) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_nexus_for_beam")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean enabled = blockEntity.toggleBeam();
        source.sendSuccess(() -> Component.translatable(enabled
                ? "invmod.message.command.nexus_beam_enabled"
                : "invmod.message.command.nexus_beam_disabled")
                .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
        return 1;
	}

	private static int status(CommandSourceStack source) {
	    handleWithNexus(source, nexus -> {
            int totalMobs = nexus.getMobsToKillInWave();
            int defeatedMobs = Math.min(totalMobs,
                    Math.max(0, totalMobs - nexus.getMobsLeftInWave()));
            source.sendSuccess(() -> Component.translatable(
                    nexus.getMode() == Mode.CONTINUOUS
                            ? "invmod.message.command.continuous_status"
                            : "invmod.message.command.invasion_status",
                    nexus.isActive(),
                    nexus.getProgressionLevel(),
                    defeatedMobs,
                    totalMobs,
                    nexus.getHealthPercent()).withStyle(ChatFormatting.GREEN), false);
	    });

	    return 0;
	}
    private static int pause(CommandSourceStack source) {
        ControllableNexusAccess nexus =
                WorldNexusStorage.of(source.getLevel()).getNexus().orElse(null);
        if (nexus == null || !nexus.isActive()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_invasion_to_pause")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (nexus.isPaused()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.invasion_already_paused")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        nexus.togglePause();
        source.sendSuccess(() -> Component.translatable(
                "invmod.message.command.invasion_paused")
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int continueInvasion(CommandSourceStack source) {
        ControllableNexusAccess nexus =
                WorldNexusStorage.of(source.getLevel()).getNexus().orElse(null);
        if (nexus == null || !nexus.isActive()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_invasion_to_continue")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!nexus.isPaused()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.invasion_already_running")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        nexus.togglePause();
        source.sendSuccess(() -> Component.translatable(
                "invmod.message.command.invasion_continued")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setWave(CommandSourceStack source, int wave) {
        ControllableNexusAccess nexus =
                WorldNexusStorage.of(source.getLevel()).getNexus().orElse(null);
        if (nexus == null || !nexus.isActive()) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.no_invasion_to_set")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (nexus.getCurrentWave() == wave) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.wave_already_set", wave)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!nexus.setWave(wave)) {
            source.sendFailure(Component.translatable(
                    "invmod.message.command.wave_set_failed", wave)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable(
                "invmod.message.command.wave_set", wave)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }


    private static int help(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source) {
	    Map<CommandNode<CommandSourceStack>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot().getChild("invasion"), source);

        for (String name : map.values()) {
            source.sendSuccess(() -> Component.literal("/" + name), false);
        }

        return map.size();
	}
}
