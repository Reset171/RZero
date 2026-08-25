package ru.reset.rzero.command;

import ru.reset.rzero.runtime.RZeroRuntime;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.reset.rzero.anchor.AnchorMode;
import ru.reset.rzero.anchor.AnchorSelector;
import ru.reset.rzero.anchor.RZeroAnchorSettings;
import ru.reset.rzero.anchor.RollbackCooldown;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.CheckpointManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CommandRZeroBase {
    private CommandRZeroBase() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> rzeroNode = Commands.literal("rzero")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("set")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CheckpointManager.setCheckpoint(player);
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("command.rzero.set.success"), true);
                            return 1;
                        }))
                .then(Commands.literal("anchor")
                        .then(Commands.literal("status")
                                .executes(ctx -> reportAnchors(ctx.getSource())))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                                        .executes(ctx -> setCooldown(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "seconds")))))
                        .then(Commands.literal("mode")
                                .then(Commands.literal("fixed")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> setFixed(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"))))
                                        .executes(ctx -> setFixed(ctx.getSource(),
                                                ctx.getSource().getPlayerOrException())))
                                .then(Commands.literal("multi")
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> setMulti(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players")))))
                                .then(Commands.literal("everyone")
                                        .executes(ctx -> setEveryone(ctx.getSource())))
                                .then(Commands.literal("rotating")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(
                                                        RZeroAnchorSettings.MIN_ROTATION_SECONDS, 86400))
                                                .executes(ctx -> setRotating(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "seconds")))))));

        DevHooks.fireRegisterCommands(rzeroNode, dispatcher, registryAccess, environment);

        dispatcher.register(rzeroNode);
    }


    private static int applyAnchorSettings(CommandSourceStack source,
                                          RZeroAnchorSettings updated,
                                          Component feedback) {
        RZeroRuntime.setAnchorSettings(updated);
        ru.reset.rzero.RZeroConfig.save();
        source.sendSuccess(() -> feedback, true);
        source.sendSuccess(() -> Component.translatable("command.rzero.anchor.hint.newCheckpoint"), false);
        return 1;
    }

    private static int setFixed(CommandSourceStack source, ServerPlayer player) {
        RZeroAnchorSettings updated = RZeroRuntime.anchorSettings()
                .withMode(AnchorMode.FIXED)
                .withPinned(List.of(player.getUUID()));
        return applyAnchorSettings(source, updated, Component.translatable(
                "command.rzero.anchor.mode",
                Component.translatable("command.rzero.anchor.mode.fixed", player.getName().getString())));
    }

    private static int setMulti(CommandSourceStack source, Collection<ServerPlayer> players) {
        List<UUID> ids = new ArrayList<>(players.size());
        for (ServerPlayer p : players) {
            ids.add(p.getUUID());
        }
        RZeroAnchorSettings updated = RZeroRuntime.anchorSettings()
                .withMode(AnchorMode.MULTI)
                .withPinned(ids);
        return applyAnchorSettings(source, updated, Component.translatable(
                "command.rzero.anchor.mode",
                Component.translatable("command.rzero.anchor.mode.multi", String.valueOf(ids.size()))));
    }

    private static int setEveryone(CommandSourceStack source) {
        RZeroAnchorSettings updated = RZeroRuntime.anchorSettings()
                .withMode(AnchorMode.EVERYONE);
        return applyAnchorSettings(source, updated, Component.translatable(
                "command.rzero.anchor.mode",
                Component.translatable("command.rzero.anchor.mode.everyone")));
    }

    private static int setRotating(CommandSourceStack source, int seconds) {
        RZeroAnchorSettings updated = RZeroRuntime.anchorSettings()
                .withMode(AnchorMode.ROTATING)
                .withRotationSeconds(seconds);
        return applyAnchorSettings(source, updated, Component.translatable(
                "command.rzero.anchor.mode",
                Component.translatable("command.rzero.anchor.mode.rotating", String.valueOf(seconds))));
    }

    private static int setCooldown(CommandSourceStack source, int seconds) {
        RZeroAnchorSettings updated = RZeroRuntime.anchorSettings()
                .withRollbackCooldownSeconds(seconds);
        RZeroRuntime.setAnchorSettings(updated);
        ru.reset.rzero.RZeroConfig.save();
        RollbackCooldown.reset();
        source.sendSuccess(() -> seconds <= 0
                ? Component.translatable("command.rzero.anchor.cooldown.off")
                : Component.translatable("command.rzero.anchor.cooldown", String.valueOf(seconds)), true);
        return 1;
    }

    private static int reportAnchors(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        RZeroAnchorSettings settings = RZeroRuntime.anchorSettings();

        final Component modeText = describeMode(settings);
        final Component cooldownText = describeCooldown(settings, server);
        final Component anchorsText = describeActiveAnchors(server);

        source.sendSuccess(() -> Component.translatable(
                "command.rzero.anchor.status", modeText, cooldownText, anchorsText), false);
        return 1;
    }

    private static Component describeMode(RZeroAnchorSettings settings) {
        return switch (settings.mode()) {
            case ROTATING -> Component.translatable("command.rzero.anchor.mode.rotating", String.valueOf(settings.rotationSeconds()));
            case MULTI -> Component.translatable("command.rzero.anchor.mode.multi", String.valueOf(settings.pinned().size()));
            case FIXED -> {
                if (settings.pinned().isEmpty()) {
                    yield Component.translatable("config.rzero.anchorMode.fixed");
                }
                yield Component.translatable("command.rzero.anchor.mode.multi", String.valueOf(settings.pinned().size()));
            }
            case EVERYONE -> Component.translatable("command.rzero.anchor.mode.everyone");
        };
    }

    private static Component describeCooldown(RZeroAnchorSettings settings, MinecraftServer server) {
        if (settings.rollbackCooldownSeconds() <= 0) {
            return Component.translatable("command.rzero.anchor.cooldown.disabled");
        }
        long remainingSeconds = server == null
                ? 0L
                : RollbackCooldown.remainingTicks(
                        settings.rollbackCooldownSeconds(), server.getTickCount()) / 20L;
        return Component.translatable("command.rzero.anchor.cooldown.format",
                String.valueOf(settings.rollbackCooldownSeconds()),
                String.valueOf(remainingSeconds));
    }

    private static Component describeActiveAnchors(MinecraftServer server) {
        if (server == null) {
            return Component.translatable("command.rzero.anchor.none");
        }
        Set<UUID> anchors = AnchorSelector.resolveAnchors(
                server, server.overworld().getGameTime(), null);
        if (anchors.isEmpty()) {
            return Component.translatable("command.rzero.anchor.none");
        }
        StringBuilder sb = new StringBuilder();
        for (UUID anchor : anchors) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            ServerPlayer player = server.getPlayerList().getPlayer(anchor);
            sb.append(player != null ? player.getName().getString() : anchor.toString());
        }
        return Component.literal(sb.toString());
    }
}
