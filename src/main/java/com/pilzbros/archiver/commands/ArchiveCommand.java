package com.pilzbros.archiver.commands;

import com.pilzbros.archiver.modules.ArchiveModule;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

/**
 * This the command handler for the archive addon. It will allow you to manually configure elements of the ongoing archive
 * session without needing to pause or restart the current leg.
 */
public class ArchiveCommand extends Command {

    private final ArchiveModule archiveModule;

    /**
     * The {@code name} parameter should be in kebab-case.
     */
    public ArchiveCommand(ArchiveModule archiveModule) {
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
    }
}
