package ru.reset.rzero.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

@FunctionalInterface
public interface RegisterCommandsHook {
    void apply(LiteralArgumentBuilder<CommandSourceStack> rzeroNode,
               CommandDispatcher<CommandSourceStack> dispatcher,
               CommandBuildContext registryAccess,
               Commands.CommandSelection environment);
}
