package com.pilzbros.archiver.modules;

import com.pilzbros.archiver.ArchiverAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.util.Set;
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
 */
public class ArchiveModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private static final String LOG_PREFIX = "PilzBros Archiver >> ";
    private static final Logger LOG = Logger.getLogger("Minecraft");

    private final Set<Direction> validDirections = Set.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    public final Setting<Boolean> archiveEnabled = sgGeneral.add(new BoolSetting.Builder()
        .name("Enable Archive + Flying")
        .description("Fly to the appropriate height, then enable for the addon to take control and begin flying in the pattern for archiving.")
        .defaultValue(true)
        .onChanged(this::onEnableArchiving)
        .build()
    );

    public final Setting<Boolean> playerYawLockSetting = sgGeneral.add(new BoolSetting.Builder()
        .name("Lock Player Yaw")
        .description("When enabled, each tick will ensure the player's yaw is looking in the correct direction.")
        .defaultValue(true)
        .onChanged(this::onEnableArchiving)
        .build()
    );

    private final Setting<Integer> slowdownTicksSetting = sgGeneral.add(new IntSetting.Builder()
        .name("slowdownTicks")
        .description("The number of tickets to wait ")
        .defaultValue(10)
        .min(0)
        .sliderMax(120)
        .build()
    );

    private final Setting<Integer> startingDistanceSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Starting Distance")
        .description("The initial distance to use for the first leg of the archive sweep.")
        .defaultValue(50)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Integer> ongoingDistanceModifierSetting = sgGeneral.add(new IntSetting.Builder()
        .name("Ongoing Distance Modifier")
        .description("The distance that you must travel on top of the length of the last leg before turning..")
        .defaultValue(200)
        .min(1)
        .sliderMax(1000)
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

    private ChunkPos chunkPosAtLastTurn;
    private long traveledChunkDistance = startingDistanceSetting.get();
    private int speedSlowDownTicks = slowdownTicksSetting.get();
    private Direction lastPlayerDictatedDirection;

    public ClientPlayerEntity lastPlayer;

    public ArchiveModule() {
        super(ArchiverAddon.CATEGORY, "archiver-module", "Automatic flying to archive the entire world.");
    }

    private void onEnableArchiving(boolean setting) {
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        //LOG.info("Received message " + event.getMessage());
    }

    /**
     * Resets archive progress and checkpoint locations. This can be used when you need to manually resume by setting
     * lengths yourself.
     */
    public void resetThings() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        chunkPosAtLastTurn = player.getChunkPos();
        lastPlayerDictatedDirection = player.getMovementDirection();
    }

    /**
     * When called, this manually sets the chunk distance to the distance between the player's current chunk and the
     * lastChunkPos location. This is used when manually resuming an archive job.
     */
    public void manuallySetChunkDistance() {
        setTraveledChunkDistance(chunkPosAtLastTurn.getSquaredDistance(MinecraftClient.getInstance().player.getChunkPos()) / 2);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!archiveEnabled.get()) {
            return;
        }

        if (lastPlayer == null) {
            lastPlayer = player;
        }

        if (chunkPosAtLastTurn == null) {
            chunkPosAtLastTurn = player.getChunkPos();
        }

        if (lastPlayerDictatedDirection == null) {
            lastPlayerDictatedDirection = player.getMovementDirection();
        }

        if (!lastPlayer.equals(MinecraftClient.getInstance().player)) {
            // We need our helper module to toggle us on and off.
            return;
        }

        if (player.isOnGround() || player.isTouchingWater() || player.isInLava() || player.isClimbing())
            // The player needs to be flying in order for us to be able to proceed with the movement handling.
            return;


        // We need to keep track of how many chunks we're traveling since the last time we turned. We use this to determine
        // when it's time for us to turn again.
        int distanceTraveledSinceLastTurn = player.getChunkPos().getSquaredDistance(chunkPosAtLastTurn) / 2;

        // Determine how far we still need to go before we've reached the point when we can turn.
        long distanceYetToGo = (traveledChunkDistance + ongoingDistanceModifierSetting.get()) - distanceTraveledSinceLastTurn;

        if (distanceYetToGo >= 0) {
            // Move the player in the same direction.
            Direction directionForMovement = lastPlayerDictatedDirection;

             if (playerYawLockSetting.get()) {
                 // Lock the player's yaw to the direction we _should_ be heading. This can correct for mistakes when you
                 // move your mouse across the screen. When disabled, you can get yourself off course real fast.
                 setPlayerYawTowardsDirection(directionForMovement);
             }

            // Now we can move the player forward! This is what actually drives the hold W mechanic.
            movePlayerDirection(directionForMovement);

             if (distanceYetToGo % 100 == 0) {
                 LOG.info(LOG_PREFIX + "Player is moving in direction " + player.getMovementDirection().name() + " has traveled " + distanceTraveledSinceLastTurn + " chunks since last turn, needs to travel an additional " + distanceYetToGo + " more before turning.");
             }

            return;
        }

        // We've moved as far as we need to in this direction, it's time to turn.
        if (speedSlowDownTicks-- > 0) {
            // They're moving too fast for us to turn without losing momentum into the next turn. Let's wait until they stop.
            return;
        }

        // Reset the slowdown speed ticks remaining to the setting for the next time we're in velocity cooldown.
        speedSlowDownTicks = slowdownTicksSetting.get();

        // It's time to turn!
        LOG.info(LOG_PREFIX + "Time to turn! Player was traveling " + player.getMovementDirection().name() + ", turning to now go " + getDirectionToTurnFromCurrentDirection(player.getMovementDirection()).name());
        setPlayerYawTowardsDirection(getDirectionToTurnFromCurrentDirection(player.getMovementDirection()));

        // Record how far we've traveled in total now that we've turned, this calculation gets used for ongoing movement.
        setTraveledChunkDistance(player.getChunkPos().getSquaredDistance(chunkPosAtLastTurn) / 2);
        chunkPosAtLastTurn = player.getChunkPos();
    }

    protected void resetLastPlayer() {
        lastPlayer = MinecraftClient.getInstance().player;
    }

    /**
     * Sets the chunk distance attribute that dictates how far the next leg of the pass should go before turning.
     */
    private void setTraveledChunkDistance(long distance) {
        traveledChunkDistance = distance;
        LOG.info("New chunk distance target = " + traveledChunkDistance);
    }

    /**
     * Moves the player in the provided direction.
     */
    public void movePlayerDirection(Direction direction) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        // Ensure the player is facing the right direction.
        setPlayerYawTowardsDirection(direction);

        switch (direction) {
            case NORTH ->
                setPlayerVelocity(player.getMovement().x, player.getMovement().y, player.getMovement().z - speed.get());
            case SOUTH ->
                setPlayerVelocity(player.getMovement().x, player.getMovement().y, player.getMovement().z + speed.get());
            case EAST ->
                setPlayerVelocity(player.getMovement().x + speed.get(), player.getMovement().y, player.getMovement().z);
            case WEST ->
                setPlayerVelocity(player.getMovement().x - speed.get(), player.getMovement().y, player.getMovement().z);
            default -> LOG.warning("Player needs to move, but direction " + direction + " is unknown.");
        }
    }

    /**
     * Sets the velocity of the player.
     */
    private void setPlayerVelocity(double x, double y, double z) {
        MinecraftClient.getInstance().player.setVelocity(x, y, z);
    }

    /**
     * Sets the player's jaw to look in the provided direction.
     * @param direction Direction for the player to look.
     */
    public void setPlayerYawTowardsDirection(Direction direction) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        float yaw = 0;

        switch (direction) {
            case NORTH -> yaw = -180;
            case EAST -> yaw = -90;
            case SOUTH -> yaw = 0;
            case WEST -> yaw = 90;
        }

        player.setYaw(yaw);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
        lastPlayerDictatedDirection = direction;
    }

    /**
     * Determines the direction the player should be rotated after completing the segment of the supplied direction.
     */
    public Direction getDirectionToTurnFromCurrentDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new RuntimeException("Unsupported direction " + direction);
        };
    }
}
