package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.client.network.packet.EntitiesDestroyS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                portal.getEntitiesToTeleport().forEach(entity -> {
                    if (!(entity instanceof ServerPlayerEntity)) {
                        ModMain.serverTaskList.addTask(() -> {
                            teleportRegularEntity(entity, portal);
                            return true;
                        });
                    }
                })
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        int portalId
    ) {
        Entity portalEntity = Helper.getServer()
            .getWorld(dimensionBefore).getEntityById(portalId);
    
        if (canPlayerTeleport(player, dimensionBefore, posBefore, portalEntity)) {
            if (isTeleporting(player)) {
                Helper.log(player.toString() + "tried to teleport for multiple times. rejected.");
                return;
            }
        
            DimensionType dimensionTo = ((Portal) portalEntity).dimensionTo;
            Vec3d newPos = ((Portal) portalEntity).applyTransformationToPoint(posBefore);
        
            teleportPlayer(player, dimensionTo, newPos);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().asString(),
                player.dimension,
                player.getPos(),
                portalId
            ));
            sendPositionConfirmMessage(player);
        }
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        Entity portalEntity
    ) {
        return canPlayerReachPos(player, dimensionBefore, posBefore) &&
            portalEntity instanceof Portal &&
            isClose(posBefore, portalEntity.getPos());
    }
    
    private boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        return player.dimension == dimension ?
            isClose(pos, player.getPos())
            :
            Helper.getEntitiesNearby(player, Portal.class, 10)
                .anyMatch(
                    portal -> portal.dimensionTo == dimension &&
                        isClose(pos, portal.destination)
                );
    }
    
    private static boolean isClose(Vec3d a, Vec3d b) {
        return a.squaredDistanceTo(b) < 10 * 10;
    }
    
    private void teleportPlayer(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = Helper.getServer().getWorld(dimensionTo);
    
        if (player.dimension == dimensionTo) {
            player.setPosition(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos);
        }
    
        sendConfirmMessageTwice(player);
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d destination
    ) {
        teleportingEntities.add(player);
    
        //TODO fix travel when riding entity
        player.detach();
    
        fromWorld.removePlayer(player);
        player.removed = false;
    
        player.x = destination.x;
        player.y = destination.y;
        player.z = destination.z;
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.respawnPlayer(player);
        
        toWorld.checkChunk(player);
        
        Helper.getServer().getPlayerManager().sendWorldInfo(
            player, toWorld
        );
        
        player.interactionManager.setWorld(toWorld);
    
        Helper.log(String.format(
            "%s changed dimension on server from %s to %s",
            player,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        ));
    }
    
    private void sendConfirmMessageTwice(ServerPlayerEntity player) {
        //send a confirm message now
        //and send one again after 1 second
        
        sendPositionConfirmMessage(player);
        
        long startTickTime = Helper.getServerGameTime();
        ModMain.serverTaskList.addTask(() -> {
            if (Helper.getServerGameTime() - startTickTime > 20) {
                sendPositionConfirmMessage(player);
                return true;
            }
            else {
                return false;
            }
        });
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        CustomPayloadS2CPacket packet = MyNetwork.createStcDimensionConfirm(
            player.dimension,
            player.getPos()
        );
        
        player.networkHandler.sendPacket(packet);
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        if (Helper.getServerGameTime() % 100 == 42) {
            new ArrayList<>(Helper.getServer().getPlayerManager().getPlayerList())
                .forEach(this::sendPositionConfirmMessage);
        }
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        assert entity.dimension == portal.dimension;
        assert !(entity instanceof ServerPlayerEntity);
        
        Vec3d newPos = portal.applyTransformationToPoint(entity.getPos());
        
        if (portal.dimensionTo != entity.dimension) {
            changeEntityDimension(entity, portal.dimensionTo, newPos);
        }
        
        entity.setPosition(
            newPos.x, newPos.y, newPos.z
        );
    }
    
    /**
     * {@link Entity#changeDimension(DimensionType)}
     */
    private void changeEntityDimension(
        Entity entity,
        DimensionType toDimension,
        Vec3d destination
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = Helper.getServer().getWorld(toDimension);
        entity.detach();
        
        Stream<ServerPlayerEntity> watchingPlayers = Helper.getEntitiesNearby(
            entity,
            ServerPlayerEntity.class,
            128
        );
        
        fromWorld.removeEntity(entity);
        entity.removed = false;
        
        entity.x = destination.x;
        entity.y = destination.y;
        entity.z = destination.z;
        
        entity.world = toWorld;
        entity.dimension = toDimension;
        toWorld.method_18769(entity);
        
        //this entity was untracked and retracked.
        //so it will not be in the player's unloading entity list
        //manually send destroy packet to avoid duplicate
        
        CustomPayloadS2CPacket removeEntityPacket = RedirectedMessageManager.createRedirectedMessage(
            fromWorld.dimension.getType(),
            new EntitiesDestroyS2CPacket(entity.getEntityId())
        );
        
        watchingPlayers.forEach(
            player -> player.networkHandler.sendPacket(removeEntityPacket)
        );
    }
}
