package com.pilzbros.archiver.modules;

import com.pilzbros.archiver.ArchiverAddon;
import com.google.common.collect.Sets;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * This module handles
 *
 * - When a message is received from a player that we should avoid, we can disconnect the session. This can be in the form
 * of a chat message, direct message, or the server "X joined" messages that are broadcast to all players.
 * - When a player to avoid is detected to be on the server, we can disconnect the session.
 */
public class ConflictAvoidanceModule extends Module {

    private static final String INTERROGATION_MESSAGE = "Stop trying to get out and respond to the interrogation!";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disconnectOnAvoidPlayerMessageSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect on Avoid Player Message")
        .description("When enabled, the session to the server will be disconnected if we receive a message from a list of players to avoid being online with.")
        .defaultValue(false)
        .onChanged(this::doNothing)
        .build()
    );

    private final Setting<Boolean> disconnectOnAvoidPlayerPresenceSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect on Avoid Player Presence")
        .description("When enabled, the session to the server will be disconnected if we detect a player to avoid is currently online.")
        .defaultValue(false)
        .onChanged(this::doNothing)
        .build()
    );

    private final Setting<Integer> avoidPlayerPresenceTickDelaySetting = sgGeneral.add(new IntSetting.Builder()
        .name("Presence Detection Tick Delay")
        .description("The number of ticks to wait before we check the online player list for players to avoid.")
        .defaultValue(200)
        .min(5)
        .sliderMax(1000)
        .build()
    );

    private int ticksSinceLastAvoidPresenceDetection;

    private final Set<String> mods = Set.of("FuzbolMC");
    private final Set<String> admins = Set.of("14mRh4X0r", "AlphaAlex115", "Anna_28", "AppleSilly", "BastetFurry", "BryBer", "ChivalrousWeasel", "Darkdiplomat", "Dorn284", "Doublehelix457", "Eclypto18", "Jimmyd93", "Krenath", "Lacrosse1991", "LewisD95", "Lothendal", "Flippeh", "MattyQ", "Nosefish", "Rtkwe", "Shobble", "SlowRiot", "SnappyMuppetman", "Stromhurst", "Techkid6", "Ted1246", "TheDemetri", "The_Jackal_249", "Tyhdefu", "Void42", "WaffleNomster", "Werdnaz", "Zfleming1", "Zomon333");

    public ConflictAvoidanceModule() {
        super(ArchiverAddon.CATEGORY, "conflict-avoidance-module", "Automatically disconnect when specific players join.");
    }

    @EventHandler
    public void handleReceiveMessage(ReceiveMessageEvent event) {
        if (!disconnectOnAvoidPlayerMessageSetting.get()) {
            return;
        }

        if (event.getMessage().contains(Text.literal(INTERROGATION_MESSAGE))) {
            warning("Archiver Disconnect - You were hauled away by a f'in mod for interrogation. You're as good as banned. Disconnected session so you can manually switch accounts.");
            MinecraftClient.getInstance().disconnect();
            return;
        }

        Optional<String> maybeUserToAvoid = Sets.union(mods, admins).stream().filter(username -> event.getMessage().contains(Text.literal(username))).findFirst();

        if (maybeUserToAvoid.isPresent()) {
            // We're going to disconnect the session as there was a message from or about a player we wish to avoid being online with.
            warning("Archiver Disconnect - Player to avoid " + maybeUserToAvoid.get() + " was detected in chat. Disconnected session to avoid detection/ban.");
            MinecraftClient.getInstance().disconnect();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        if (disconnectOnAvoidPlayerPresenceSetting.get() && (ticksSinceLastAvoidPresenceDetection++ >= avoidPlayerPresenceTickDelaySetting.get())) {
           ticksSinceLastAvoidPresenceDetection = 0;

           if (MinecraftClient.getInstance().getServer() == null || MinecraftClient.getInstance().getServer().getPlayerNames() == null) {
               // We don't yet have access to all the player's names on the server...
               return;
           }

            Optional<String> maybeUserToAvoid = Arrays.stream(MinecraftClient.getInstance().getServer().getPlayerNames()).filter(name -> admins.contains(name)).findFirst();

            if (maybeUserToAvoid.isPresent()) {
                // We're going to disconnect the session as a player we want to avoid appears to be online.
                warning("Archiver Disconnect - Player to avoid " + maybeUserToAvoid.get() + " was detected to be online in the player list. Disconnected session to avoid detection/ban.");
                MinecraftClient.getInstance().disconnect();
            }
        }
    }

    private void doNothing(boolean setting) {
    }
}
