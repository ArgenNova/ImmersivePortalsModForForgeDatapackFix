package qouteall.imm_ptl.core;

import com.mojang.blaze3d.platform.GlUtil;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import qouteall.imm_ptl.core.commands.ClientDebugCommand;
import qouteall.imm_ptl.core.compat.IPFlywheelCompat;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisCompatibilityPortalRenderer;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisPortalRenderer;
import qouteall.imm_ptl.core.miscellaneous.DubiousThings;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.portal.PortalAnimationManagement;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.context_management.CloudContext;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.optimization.GLResourceCache;
import qouteall.imm_ptl.core.render.optimization.SharedBlockMeshBuffers;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.UUID;

public class IPModMainClient {
    
    private static boolean fabulousWarned = false;
    
    public static void switchToCorrectRenderer() {
        if (PortalRendering.isRendering()) {
            //do not switch when rendering
            return;
        }
        
        if (Minecraft.getInstance().options.graphicsMode == GraphicsStatus.FABULOUS) {
            if (!fabulousWarned) {
                fabulousWarned = true;
                CHelper.printChat(new TranslatableComponent("imm_ptl.fabulous_warning"));
            }
        }
        
        if (IrisInterface.invoker.isIrisPresent()) {
            if (IrisInterface.invoker.isShaders()) {
                if (IPCGlobal.experimentalIrisPortalRenderer) {
//DISABLED_COMPILE                    switchRenderer(ExperimentalIrisPortalRenderer.instance);
                    return;
                }
                
                switch (IPGlobal.renderMode) {
                    case normal -> switchRenderer(IrisPortalRenderer.instance);
                    case compatibility -> switchRenderer(IrisCompatibilityPortalRenderer.instance);
                    case debug -> switchRenderer(IrisCompatibilityPortalRenderer.debugModeInstance);
                    case none -> switchRenderer(IPCGlobal.rendererDummy);
                }
                return;
            }
        }
        
        switch (IPGlobal.renderMode) {
            case normal -> switchRenderer(IPCGlobal.rendererUsingStencil);
            case compatibility -> switchRenderer(IPCGlobal.rendererUsingFrameBuffer);
            case debug -> switchRenderer(IPCGlobal.rendererDebug);
            case none -> switchRenderer(IPCGlobal.rendererDummy);
        }
        
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (IPCGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            IPCGlobal.renderer = renderer;
            
            if (IrisInterface.invoker.isShaders()) {
                IrisInterface.invoker.reloadPipelines();
            }
        }
    }
    
    private static void showPreviewWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (IPGlobal.enableWarning) {
                    Minecraft.getInstance().gui.handleChat(
                        ChatType.CHAT,
                        new TranslatableComponent("imm_ptl.preview_warning").append(
                            McHelper.getLinkText("https://github.com/qouteall/ImmersivePortalsMod/issues")
                        ),
                        UUID.randomUUID()
                    );
                }
            })
        ));
    }
    
    private static void showIntelVideoCardWarning() {
        IPGlobal.clientTaskList.addTask(MyTaskList.withDelayCondition(
            () -> Minecraft.getInstance().level == null,
            MyTaskList.oneShotTask(() -> {
                if (GlUtil.getVendor().toLowerCase().contains("intel")) {
                    CHelper.printChat(new TranslatableComponent("imm_ptl.intel_warning"));
                }
            })
        ));
    }

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        ClientWorldLoader.init();
        

        
        //O_O.loadConfigFabric();
        
        DubiousThings.init();
        
        CrossPortalEntityRenderer.init();
        
        GLResourceCache.init();
        
        CollisionHelper.initClient();
        
        PortalRenderInfo.init();
        
        CloudContext.init();
        
        SharedBlockMeshBuffers.init();
        
        GcMonitor.initClient();

        MinecraftForge.EVENT_BUS.register(ClientDebugCommand.class);
        MinecraftForge.EVENT_BUS.register(ClientWorldLoader.class);

//        showPreviewWarning();
        
        showIntelVideoCardWarning();
        
        PortalAnimationManagement.init();
        
        VisibleSectionDiscovery.init();
        
        MyBuiltChunkStorage.init();
        
        IPFlywheelCompat.init();

//        InvalidateRenderStateCallback.EVENT.register(()->{
//            Helper.log("reload levelrenderer " + Minecraft.getInstance().level.dimension().location());
//        });
    }
    
}
