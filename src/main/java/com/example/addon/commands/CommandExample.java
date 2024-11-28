package com.example.addon.commands;

import com.example.addon.modules.ArchiveModule;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * The Meteor Client command API uses the <a href="https://github.com/Mojang/brigadier">same command system as Minecraft does</a>.
 */
public class CommandExample extends Command {

    private final ArchiveModule archiveModule;

    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public CommandExample(ArchiveModule archiveModule) {
        super("archive", "Archiver commands for world downloading.");

        this.archiveModule = archiveModule;
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("reset").executes(context -> {
            archiveModule.resetThings();
            info("Reset archiver attributes.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("resume").executes(context -> {
            archiveModule.manuallySetChunkDistance();
            info("Manually set chunk distance. Ready to resume archiving.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("snap").executes(context -> {
            archiveModule.setPlayerYawTowardsDirection(MinecraftClient.getInstance().player.getMovementDirection());
            info("Snapped player yaw to the true direction they're heading.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("token").executes(context -> {
            info("Your Access Token: " + MinecraftClient.getInstance().getSession().getAccessToken());
            info("Session: " + MinecraftClient.getInstance().getSession().getSessionId());

            return SINGLE_SUCCESS;
        }));



//        builder.then(literal("name").then(argument("nameArgument", StringArgumentType.word()).executes(context -> {
//            String argument = StringArgumentType.getString(context, "nameArgument");
//            info("hi, " + argument);
//            return SINGLE_SUCCESS;
//        })));
    }
}
