package qouteall.imm_ptl.core.ducks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public interface IEThreadedAnvilChunkStorage {
    int ip_getWatchDistance();
    
    ServerLevel ip_getWorld();
    
    ThreadedLevelLightEngine ip_getLightingProvider();
    
    ChunkHolder ip_getChunkHolder(long long_1);
    
    void ip_onPlayerUnload(ServerPlayer oldPlayer);
    
    @Deprecated
    void ip_onPlayerDisconnected(ServerPlayer player);
    
    void ip_onDimensionRemove();
    
    void ip_updateEntityTrackersAfterSendingChunkPacket(
        LevelChunk chunk,
        ServerPlayer playerEntity
    );
    
    void ip_resendSpawnPacketToTrackers(Entity entity);
    
    boolean portal_isChunkGenerated(ChunkPos chunkPos);
    
    Int2ObjectMap<ChunkMap.TrackedEntity> ip_getEntityTrackerMap();
}
