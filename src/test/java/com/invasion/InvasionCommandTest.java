package com.invasion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InvasionCommandTest {
    private static final List<String> RESTRICTED_COMMANDS = List.of(
            "help", "pause", "continue", "destroy", "debug", "status", "set", "start", "stop", "radius");

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void disableDebugMode() {
        InvasionMod.getConfig().debugMode = false;
    }

    @Test
    void administrativeCommandsRequireGameMasterPermission() {
        CommandNode<CommandSourceStack> root = commandRoot(false);
        CommandSourceStack player = Commands.createCompilationContext(LevelBasedPermissionSet.ALL);
        CommandSourceStack gameMaster = Commands.createCompilationContext(LevelBasedPermissionSet.GAMEMASTER);

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
        assertFalse(test.canUse(Commands.createCompilationContext(LevelBasedPermissionSet.ALL)));
        assertTrue(test.canUse(Commands.createCompilationContext(LevelBasedPermissionSet.GAMEMASTER)));
    }

    private CommandNode<CommandSourceStack> commandRoot(boolean debugMode) {
        InvasionMod.getConfig().debugMode = debugMode;
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        return InvasionCommand.create(dispatcher, null).build();
    }
}
