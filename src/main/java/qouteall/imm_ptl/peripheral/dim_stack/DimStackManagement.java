package qouteall.imm_ptl.peripheral.dim_stack;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.portal.global_portals.VerticalConnectingPortal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.dimension.DimId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DimStackManagement {
    // This is for client dimension stack initialization
    public static DimStackInfo dimStackToApply = null;
    public static Map<ResourceKey<Level>, BlockState> bedrockReplacementMap = new HashMap<>();
    
    public static void init() {
    
    }
    
    // at that time, only overworld has been created, only overworld data can be read
    // it's going to generate overworld spawn chunks
    // make sure the bedrock replacement map for overworld is initialized in time
    public static void onServerEarlyInit(MinecraftServer server) {
        Map<ResourceKey<Level>, BlockState> newMap = new HashMap<>();

        if (dimStackToApply == null && Paths.get(FMLPaths.CONFIGDIR.get().toString(), "imm_ptl_dim_stack.json").toFile().exists()) {
            try {
                String dimStackConfig = Files.readString(Paths.get(FMLPaths.CONFIGDIR.get().toString(), "imm_ptl_dim_stack.json"));
                JsonElement JSON = new GsonBuilder().create().fromJson(dimStackConfig, JsonElement.class);
                DataResult<CompoundTag> result = CompoundTag.CODEC.parse(JsonOps.INSTANCE, JSON);
                DimStackInfo info = DimStackInfo.fromNbt(result.result().get());

                dimStackToApply = info;

                Helper.log("Generating dimension stack world");
            } catch (JsonSyntaxException | IOException e) {
                LogManager.getLogger().error("Failed to read ImmersivePortals Dimension Stack Config: ", e);
            }
        }
        
        if (dimStackToApply != null) {
            for (DimStackEntry entry : dimStackToApply.entries) {
                newMap.put(entry.dimension, DimStackInfo.parseBlockString(entry.bedrockReplacementStr));
            }
        }
        
        bedrockReplacementMap = newMap;
    }
    
    public static void onServerCreatedWorlds(MinecraftServer server) {
        if (dimStackToApply != null) {
            dimStackToApply.apply();
            dimStackToApply = null;
        }
        else {
            
            updateBedrockReplacementFromStorage(server);
            
            GameRules gameRules = server.getGameRules();
            GameRules.BooleanValue o = gameRules.getRule(DimStackGameRule.dimensionStackKey);
            if (o.get()) {
                // legacy dimension stack
                
                o.set(false, server);
                
                upgradeLegacyDimensionStack(server);
            }
        }
    }
    
    private static void updateBedrockReplacementFromStorage(MinecraftServer server) {
        // do not mutate the old map to avoid data race
        Map<ResourceKey<Level>, BlockState> newMap = new HashMap<>();
        for (ServerLevel world : server.getAllLevels()) {
            BlockState replacement = GlobalPortalStorage.get(world).bedrockReplacement;
            newMap.put(world.dimension(), replacement);
            Helper.log(String.format(
                "Bedrock Replacement %s %s",
                world.dimension().location(),
                replacement != null ? Registry.BLOCK.getKey(replacement.getBlock()) : "null"
            ));
        }
        bedrockReplacementMap = newMap;
    }
    
    public static void upgradeLegacyDimensionStack(MinecraftServer server) {
        
        for (ServerLevel world : server.getAllLevels()) {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            gps.bedrockReplacement = Blocks.OBSIDIAN.defaultBlockState();
            gps.onDataChanged();
        }
        
        updateBedrockReplacementFromStorage(server);
        
        Helper.log("Legacy Dimension Stack Upgraded");
    }
    
    public static void replaceBedrock(ServerLevel world, ChunkAccess chunk) {
        if (bedrockReplacementMap == null) {
            Helper.err("Dimension Stack Bedrock Replacement Abnormal");
            return;
        }
        
        BlockState replacement = bedrockReplacementMap.get(world.dimension());
        
        if (replacement != null) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                        mutable.set(x, y, z);
                        BlockState blockState = chunk.getBlockState(mutable);
                        if (blockState.getBlock() == Blocks.BEDROCK) {
                            chunk.setBlockState(
                                mutable,
                                replacement,
                                false
                            );
                        }
                    }
                }
            }
        }
    }
    
    public static class RemoteCallables {
        @OnlyIn(Dist.CLIENT)
        public static void clientOpenScreen(List<String> dimensions) {
            List<ResourceKey<Level>> dimensionList =
                dimensions.stream().map(DimId::idToKey).collect(Collectors.toList());
            
            Minecraft.getInstance().setScreen(new DimStackScreen(
                null,
                (screen) -> dimensionList,
                dimStackInfo -> {
                    if (dimStackInfo != null) {
                        McRemoteProcedureCall.tellServerToInvoke(
                            "qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement.RemoteCallables.serverSetupDimStack",
                            dimStackInfo.toNbt()
                        );
                    }
                    else {
                        McRemoteProcedureCall.tellServerToInvoke(
                            "qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement.RemoteCallables.serverRemoveDimStack"
                        );
                    }
                })
            );
        }
        
        public static void serverSetupDimStack(
            ServerPlayer player, CompoundTag infoTag
        ) {
            if (!player.hasPermissions(2)) {
                Helper.err("one player without permission tries to change dimension stack");
                return;
            }
            
            DimStackInfo dimStackInfo = DimStackInfo.fromNbt(infoTag);
            
            clearDimStackPortals();
            
            dimStackInfo.apply();
            
            player.displayClientMessage(
                new TranslatableComponent("imm_ptl.dim_stack_established"),
                false
            );
        }
        
        public static void serverRemoveDimStack(
            ServerPlayer player
        ) {
            if (!player.hasPermissions(2)) {
                Helper.err("one player without permission tries to change dimension stack");
                return;
            }
            
            clearDimStackPortals();
            
            player.displayClientMessage(
                new TranslatableComponent("imm_ptl.dim_stack_removed"),
                false
            );
        }
    }
    
    private static void clearDimStackPortals() {
        MinecraftServer server = MiscHelper.getServer();
        for (ServerLevel world : server.getAllLevels()) {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            gps.data.removeIf(p -> p instanceof VerticalConnectingPortal);
            gps.bedrockReplacement = null;
            gps.onDataChanged();
        }
        
        updateBedrockReplacementFromStorage(server);
    }
}
