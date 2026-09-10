package com.invasion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InvasionCommandTest {
    private static final List<String> RESTRICTED_COMMANDS = List.of(
            "help", "pause", "continue", "destroy", "debug", "status", "set", "start", "stop", "radius");

    @AfterEach
    void disableDebugMode() {
        InvasionMod.getConfig().debugMode = false;
    }

    @Test
    void administrativeCommandsRequireGameMasterPermission() {
        CommandNode<CommandSourceStack> root = commandRoot(false);
        CommandSourceStack player = sourceWithPermission(0);
        CommandSourceStack gameMaster = sourceWithPermission(2);

        for (String name : RESTRICTED_COMMANDS) {
            CommandNode<CommandSourceStack> command = root.getChild(name);
            assertNotNull(command, name);
            assertFalse(command.canUse(player), name);
            assertTrue(command.canUse(gameMaster), name);
        }

        assertTrue(root.getChild("bolt").canUse(player));
    }

    @Test
    void debugCommandsExistOnlyInDebugModeAndRequireGameMasterPermission() {
        assertNull(commandRoot(false).getChild("test"));

        CommandNode<CommandSourceStack> test = commandRoot(true).getChild("test");
        assertNotNull(test);
        assertFalse(test.canUse(sourceWithPermission(0)));
        assertTrue(test.canUse(sourceWithPermission(2)));
    }

    private CommandNode<CommandSourceStack> commandRoot(boolean debugMode) {
        InvasionMod.getConfig().debugMode = debugMode;
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        return InvasionCommand.create(dispatcher, null).build();
    }

    private CommandSourceStack sourceWithPermission(int permissionLevel) {
        return new CommandSourceStack(null, Vec3.ZERO, Vec2.ZERO, null, permissionLevel,
                "test", Component.literal("test"), null, null);
    }
}
