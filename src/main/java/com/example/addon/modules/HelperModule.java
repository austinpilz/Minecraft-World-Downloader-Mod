package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

public class HelperModule extends Module {

    private ArchiveModule archiveModule;

    public HelperModule(ArchiveModule archiveModule) {
        super(AddonTemplate.CATEGORY, "helper-module", "Automatic flying to archive the entire world.");

        this.archiveModule = archiveModule;
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (archiveModule.isActive() && archiveModule.lastPlayer != null && !archiveModule.lastPlayer.equals(MinecraftClient.getInstance().player)) {
            archiveModule.toggle();
        }

        if (!archiveModule.isActive() && archiveModule.lastPlayer != null && !archiveModule.lastPlayer.equals(MinecraftClient.getInstance().player)) {

            archiveModule.toggle();
            archiveModule.resetLastPlayer();
        }
    }

}
