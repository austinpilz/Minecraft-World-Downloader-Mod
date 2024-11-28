package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.entity.BoatMoveEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.logging.Logger;

/**
 * The PilzBros Fly-Assisted Archiver is a Meteor Client addon that handles the flight navigation to (hopefully) have the
 * player visit every chunk on a server for the purposes of archival. With the assistance of other tools that will download
 * visited chunks, the goal is to create a snapshot and preserve a world for archival and community enjoyment.
 *
 * When enabled in the Meteor client by the player, this client will automatically fly the player in a spiral pattern
 * moving outward from their current location (hopefully the center of the world). It will travel the player on a path
 * that will, in combination with the render distance, cover all chunks on a world.
 *
 * This was originally designed for the archival of MinecraftOnline.com. With the server being on 1.12, client hacks
 * like boat fly still function. This addon made it possible for me to fly in a spiral pattern, visiting all the chunks
 * on the world to create the first full offline copy of the 10+ year old map. If you're looking to archive a more modern
 * server with better anti-cheat protections, you may find Baritone useful for it's Elytra auto flight.
 *
 * Recommendations:
 * - Meteor
 *
 * To Begin Archiving:
 * 1. Set up your world downloading tool, point it to your destination server, and connect your client (with Meteor client)
 * to it.
 *
 * 2. Navigate to the middle most point of the world you wish to download.
 *
 *
 * TODO Record the last coordinate + direction so we can resume from there if we get disconnected
 */
public class BoatArchiveModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private static final String LOG_PREFIX = "PilzBros Archiver >>";
    private static final Logger LOG = Logger.getLogger("Minecraft");

    public final Setting<Boolean> archiveEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("Enable Archive + Flying")
        .description("Fly to the appropriate height, then enable for the addon to take control and begin flying in the pattern for archiving.")
        .defaultValue(true)
        .onChanged(this::onEnableArchiving)
        .build()
    );

    private final Setting<Integer> slowdownTicksSetting = sgGeneral.add(new IntSetting.Builder()
        .name("slowdownTicks")
        .description("The number of tickets to wait ")
        .defaultValue(40)
        .min(0)
        .sliderMax(120)
        .build()
    );

    private final Setting<Integer> chunkDistanceSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Chunk Distance")
        .description("The chunk multiplier that each rotation leg should travel. This should be set to the chunk distance setting or the extended chunk setting in the world downloader, whichever is higher.")
        .defaultValue(50)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Integer> chunkDistanceMultiplierSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Chunk Distance Multiplier")
        .description("The chunk multiplier that each rotation leg should travel. This should be set to the chunk distance setting or the extended chunk setting in the world downloader, whichever is higher.")
        .defaultValue(50)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your movement speed while flying. The higher the movement speed, the more likely you are to be kicked.")
        .defaultValue(0.2)
        .min(0.0)
        .sliderMax(10)
        .build()
    );

    // Boat Fly Settings
    private final Setting<Double> boatVerticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Double> boatFallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you fall in blocks per second.")
        .defaultValue(0.8)
        .min(0)
        .build()
    );

    private ChunkPos lastChunkPos;
    private ChunkPos chunkPosAtLastTurn;
    private BoatEntity playerBoat;

    private long chunkDistance = chunkDistanceSetting.get();
    private int speedSlowDownTicks = slowdownTicksSetting.get();

    public BoatArchiveModule() {
        super(AddonTemplate.CATEGORY, "boat-archiver-module", "Automatic flying (via BOAT) to archive the entire world.");
    }

    private void doNothing() {
        //
    }

    private void onEnableArchiving(boolean setting) {
        if (setting) {

        }
    }

    @EventHandler()
    private void onBoatMove(BoatMoveEvent event) {
        if (event.boat.getControllingPassenger() != mc.player) return;

        playerBoat = event.boat;

        if (!archiveEnabled.get()) {
            // Our addon is enabled, but we're not supposed to be taking over movement yet. Allow boat fly to get into place.

            // Horizontal movement
            Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
            double velX = vel.getX();
            double velY = 0;
            double velZ = vel.getZ();

            // Vertical movement
            if (mc.options.jumpKey.isPressed()) velY += boatVerticalSpeed.get() / 20;
            if (mc.options.sprintKey.isPressed()) velY -= boatVerticalSpeed.get() / 20;
            else velY -= boatFallSpeed.get() / 20;

            // Apply velocity
            ((IVec3d) event.boat.getVelocity()).meteor$set(velX, velY, velZ);

            return;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!archiveEnabled.get() || playerBoat == null) {
            return;
        }

        ClientPlayerEntity player = mc.player;

        if (lastChunkPos == null) {
            lastChunkPos = player.getChunkPos();
        }

        if (chunkPosAtLastTurn == null) {
            chunkPosAtLastTurn = player.getChunkPos();
        }

        if (player.isOnGround() || player.isTouchingWater() || player.isInLava() || player.isClimbing())
            // The player needs to be flying in order for us to be able to proceed with the movement handling.
            return;

        // We need to keep track of how many chunks we're traveling since the last time we turned. We use this to determine
        // when it's time for us to turn again.
        int numChunksTraveledSinceLastTurn = playerBoat.getChunkPos().getSquaredDistance(chunkPosAtLastTurn) / 2;

        // If we haven't reached the end of the current leg we're traveling, we want to continue moving until we get there.
        boolean shouldContinueInDirection = numChunksTraveledSinceLastTurn <= chunkDistance;

        if (shouldContinueInDirection) {

            Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
            double velX = vel.getX();
            double velY = vel.getY() + (boatFallSpeed.get() / 20);
            double velZ = vel.getZ();

            // We need to move the player forward in whatever direction.
            switch (playerBoat.getMovementDirection()) {
                case NORTH ->
                    //playerBoat.setVelocity(playerBoat.getMovement().x, playerBoat.getMovement().y, playerBoat.getMovement().z - speed.get());
                    velZ -= speed.get();
                case SOUTH ->
                    //playerBoat.setVelocity(playerBoat.getMovement().x, playerBoat.getMovement().y, playerBoat.getMovement().z + speed.get());
                    velZ += speed.get();
                case EAST ->
                    //playerBoat.setVelocity(playerBoat.getMovement().x + speed.get(), playerBoat.getMovement().y, playerBoat.getMovement().z);
                    velX += speed.get();
                case WEST ->
                    //playerBoat.setVelocity(playerBoat.getMovement().x - speed.get(), playerBoat.getMovement().y, playerBoat.getMovement().z);
                    velX -= speed.get();
            }

            ((IVec3d) playerBoat.getVelocity()).meteor$set(velX, velY, velZ);

            LOG.info(LOG_PREFIX + "Player has traveled " + numChunksTraveledSinceLastTurn + " chunks since last turn, needs to travel an additional " + (chunkDistance - numChunksTraveledSinceLastTurn) + " more before turning.");
            return;
        }

        // We've moved as far as we need to in this direction, it's time to turn.
        if (speedSlowDownTicks-- > 0) {
            // They're moving too fast for us to turn without losing momentum into the next turn. Let's wait until they stop.
            LOG.info(LOG_PREFIX + "Destination chunk reached, waiting " + speedSlowDownTicks + " ticks to slow down before turning.");

            // If we don't continue to hold the boat where we are during the cooldown, it'll fall.
            Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
            ((IVec3d) playerBoat.getVelocity()).meteor$set(vel.getX(), vel.getY() + (boatFallSpeed.get() / 20), vel.getZ());
            return;
        }

        // Reset the slowdown speed ticks remaining to the setting for the next time we're in velocity cooldown.
        speedSlowDownTicks = slowdownTicksSetting.get();

        float yaw = 0;

        switch (player.getMovementDirection()) {
            case NORTH -> yaw = -90;
            case SOUTH -> yaw = 90;
            case EAST -> yaw = 0;
            case WEST -> yaw = 180;
        }

        player.setYaw(yaw);


        playerBoat.setYaw(yaw);
        playerBoat.setBodyYaw(yaw);
        playerBoat.setHeadYaw(yaw);


        // Calculate how far we need to travel for this next leg of the journey. Every time we turn and begin a new direction,
        // the distance of the next leg gets longer to account for the spiral shape we're traveling.
        chunkDistance = Math.round(chunkDistance + (chunkDistanceSetting.get() * 1.5));
        chunkPosAtLastTurn = playerBoat.getChunkPos();

        LOG.info("New chunk distance target = " + chunkDistance);








        // TODO If we receive the chat message about the invisible force, we need to do something. ReceiveMessageEvent

        // TODO Need to store enough info to be able to resume if you get disconnected for flying. Store the direction
        // they were going and the last location they were known to be at. When reconnecting, navigate back to that
        // exact location, ensure they're in the right direction, and then resume.


//        if (minHeight.get() > 0) {
//            Box box = player.getBoundingBox();
//            box = box.union(box.offset(0, -minHeight.get(), 0));
//            if (!mc.world.isSpaceEmpty(box)) return;
//
        //mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX()+2, mc.player.getY(), mc.player.getZ(), false));

        //;
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        LOG.info("Received message " + event.getMessage());
    }
}
