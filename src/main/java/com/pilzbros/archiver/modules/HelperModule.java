package com.pilzbros.archiver.modules;

import com.pilzbros.archiver.ArchiverAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

import java.time.LocalDateTime;

public class HelperModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> timeWindowEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("Time Restrictions")
        .description("When enabled, will automatically disconnect you from the server when the configured time of day has been reached.")
        .defaultValue(false)
        .onChanged(this::doNothing)
        .build()
    );

    private final Setting<Integer> timeWindowHourSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Time Restriction Disconnect Hour")
        .description("The numerical hour in the day which when reached will disconnect the player.")
        .defaultValue(5)
        .min(0)
        .max(23)
        .sliderMax(23)
        .build()
    );

    private ArchiveModule archiveModule;

    public HelperModule(ArchiveModule archiveModule) {
        super(ArchiverAddon.CATEGORY, "helper-module", "Automatic flying to archive the entire world.");
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

        if (timeWindowEnabled.get() && !isWithinAllowedTimePeriod()) {
            // We've reached the hour that we want to disconnect during.
            MinecraftClient.getInstance().disconnect();
        }
    }

    private void doNothing(boolean setting) {
        //
    }

    /**
     * As long as it's not the hour configured, we're ok to keep going.
     */
    private boolean isWithinAllowedTimePeriod() {
        return LocalDateTime.now().getHour() != timeWindowHourSetting.get();
    }
}
