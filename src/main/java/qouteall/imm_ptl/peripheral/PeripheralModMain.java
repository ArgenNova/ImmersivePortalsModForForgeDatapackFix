package qouteall.imm_ptl.peripheral;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.alternate_dimension.ChaosBiomeSource;
import qouteall.imm_ptl.peripheral.alternate_dimension.ErrorTerrainGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackGameRule;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.guide.IPGuide;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;


public class PeripheralModMain {
    
    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        IPGuide.initClient();
    }

    private static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR = DeferredRegister.create(Registry.CHUNK_GENERATOR_REGISTRY, "immersive_portals");
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCE = DeferredRegister.create(Registry.BIOME_SOURCE_REGISTRY, "immersive_portals");

    public static final RegistryObject<Codec<? extends ChunkGenerator>> ERROR_TERRAIN_GENERATOR = CHUNK_GENERATOR.register("error_terrain_generator", () -> ErrorTerrainGenerator.codec);
    public static final RegistryObject<Codec<? extends ChunkGenerator>> NORMAL_SKYLAND_GENERATOR = CHUNK_GENERATOR.register("normal_skyland_generator", () -> ErrorTerrainGenerator.codec);

    public static final RegistryObject<Codec<? extends BiomeSource>> CHAOS_BIOME_SOURCE = BIOME_SOURCE.register("chaos_biome_source", () -> ChaosBiomeSource.CODEC);

    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        DimStackGameRule.init();
        DimStackManagement.init();
        
        AlternateDimensions.init();

        CHUNK_GENERATOR.register(FMLJavaModLoadingContext.get().getModEventBus());
        BIOME_SOURCE.register(FMLJavaModLoadingContext.get().getModEventBus());
        
//        Registry.register( //Fixme removal
//            Registry.CHUNK_GENERATOR,
//            new ResourceLocation("immersive_portals:error_terrain_generator"),
//            ErrorTerrainGenerator.codec
//        );
//        Registry.register(
//            Registry.CHUNK_GENERATOR,
//            new ResourceLocation("immersive_portals:normal_skyland_generator"),
//            NormalSkylandGenerator.codec
//        );
    
//        Registry.register(
//            Registry.BIOME_SOURCE,
//            new ResourceLocation("immersive_portals:chaos_biome_source"),
//            ChaosBiomeSource.CODEC
//        );
    }
    
    public static void registerCommandStickTypes() {
        //registerPortalSubCommandStick("delete_portal");
        //registerPortalSubCommandStick("remove_connected_portals");
        //registerPortalSubCommandStick("eradicate_portal_cluster");
        //registerPortalSubCommandStick("complete_bi_way_bi_faced_portal");
        //registerPortalSubCommandStick("complete_bi_way_portal");
        //registerPortalSubCommandStick("bind_cluster", "set_portal_nbt {bindCluster:true}");
        //registerPortalSubCommandStick("move_portal_front", "move_portal 0.5");
        //registerPortalSubCommandStick("move_portal_back", "move_portal -0.5");
        //registerPortalSubCommandStick("move_portal_destination_front", "move_portal_destination 0.5");
        //registerPortalSubCommandStick("move_portal_destination_back", "move_portal_destination -0.5");
        //registerPortalSubCommandStick("rotate_x", "rotate_portal_rotation_along x 15");
        //registerPortalSubCommandStick("rotate_y", "rotate_portal_rotation_along y 15");
        //registerPortalSubCommandStick("rotate_z", "rotate_portal_rotation_along z 15");
        //registerPortalSubCommandStick("make_unbreakable", "set_portal_nbt {unbreakable:true}");
        //registerPortalSubCommandStick("make_fuse_view", "set_portal_nbt {fuseView:true}");
        //registerPortalSubCommandStick("enable_pos_adjust", "set_portal_nbt {adjustPositionAfterTeleport:true}");
        //registerPortalSubCommandStick("disable_rendering_yourself", "set_portal_nbt {doRenderPlayer:false}");
        //registerPortalSubCommandStick("enable_isometric", "debug isometric_enable 50");
        //registerPortalSubCommandStick("disable_isometric", "debug isometric_disable");
        //registerPortalSubCommandStick("create_5_connected_rooms", "create_connected_rooms roomSize 6 4 6 roomNumber 5");
        //registerPortalSubCommandStick("accelerate50", "debug accelerate 50");
        //registerPortalSubCommandStick("accelerate200", "debug accelerate 200");
        //registerPortalSubCommandStick("reverse_accelerate50", "debug accelerate -50");
        //registerPortalSubCommandStick("enable_gravity_change", "set_portal_nbt {teleportChangesGravity:true}");
        //CommandStickItem.registerType("imm_ptl:reset_scale", new CommandStickItem.Data("/scale set pehkui:base 1", "imm_ptl.command.reset_scale", Lists.newArrayList("imm_ptl.command_desc.reset_scale")));
        //CommandStickItem.registerType("imm_ptl:long_reach", new CommandStickItem.Data("/scale set pehkui:reach 5", "imm_ptl.command.long_reach", Lists.newArrayList("imm_ptl.command_desc.long_reach")));
        
        //registerPortalSubCommandStick("goback");
        //registerPortalSubCommandStick("show_wiki", "wiki");
    }
    
    private static void registerPortalSubCommandStick(String name) {
        registerPortalSubCommandStick(name, name);
    }
    
    private static void registerPortalSubCommandStick(String name, String subCommand) {
        CommandStickItem.registerType("imm_ptl:" + name, new CommandStickItem.Data(
            "/portal " + subCommand,
            "imm_ptl.command." + name,
            Lists.newArrayList("imm_ptl.command_desc." + name), true
        ));
    }
}
