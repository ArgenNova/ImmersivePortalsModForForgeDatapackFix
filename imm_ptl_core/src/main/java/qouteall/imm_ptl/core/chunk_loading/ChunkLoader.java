package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.Objects;

//the players and portals are chunk loaders
public class ChunkLoader {
    public DimensionalChunkPos center;
    public int radius;
    public boolean isDirectLoader = false;
    
    public ChunkLoader(DimensionalChunkPos center, int radius) {
        this(center, radius, false);
    }
    
    public ChunkLoader(DimensionalChunkPos center, int radius, boolean isDirectLoader) {
        this.center = center;
        this.radius = radius;
        this.isDirectLoader = isDirectLoader;
    }
    
    public int getLoadedChunkNum() {
        int[] numBox = {0};
        foreachChunkPos((dim, x, z, dist) -> {
            LevelChunk chunk = McHelper.getServerChunkIfPresent(dim, x, z);
            if (chunk != null) {
                numBox[0] += 1;
            }
        });
        return numBox[0];
    }
    
    public int getChunkNum() {
        return (this.radius * 2 + 1) * (this.radius * 2 + 1);
    }
    
    public void foreachChunkPos(ChunkPosConsumer func) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                func.consume(
                    center.dimension,
                    center.x + dx,
                    center.z + dz,
                    Math.max(Math.abs(dx), Math.abs(dz))
                );
            }
        }
    }
    
    public LenientChunkRegion createChunkRegion() {
        ServerLevel world = MiscHelper.getServer().getLevel(center.dimension);
        
        return LenientChunkRegion.createLenientChunkRegion(center, radius, world);
    }
    
    public void loadChunksAndDo(Runnable runnable) {
        NewChunkTrackingGraph.addGlobalAdditionalChunkLoader(this);
        
        IPGlobal.serverTaskList.addTask(MyTaskList.withDelayCondition(
            () -> getLoadedChunkNum() < getChunkNum(),
            MyTaskList.oneShotTask(() -> {
                NewChunkTrackingGraph.removeGlobalAdditionalChunkLoader(this);
                runnable.run();
            })
        ));
    }
    
    @Override
    public String toString() {
        return "{" +
            "center=" + center +
            ", radius=" + radius +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkLoader that = (ChunkLoader) o;
        return radius == that.radius &&
            center.equals(that.center);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }
    
    public interface ChunkPosConsumer {
        void consume(ResourceKey<Level> dimension, int x, int z, int distanceToSource);
    }
}
