package ru.reset.rzero.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import ru.reset.rzero.command.CommandRZeroBase;

public final class CommandEvents {

    private CommandEvents() {
    }

    public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                          CommandBuildContext buildContext,
                                          Commands.CommandSelection selection) {
        CommandRZeroBase.register(dispatcher, buildContext, selection);
    }
}
