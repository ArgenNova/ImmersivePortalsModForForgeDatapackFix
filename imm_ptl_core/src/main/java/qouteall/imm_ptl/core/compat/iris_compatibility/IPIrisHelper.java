package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

import static org.lwjgl.opengl.GL11.*;

public class IPIrisHelper {
    
    public static void copyDepthStencil(
        RenderTarget from, RenderTarget to,
        boolean copyDepth, boolean copyStencil
    ) {
        from.unbindWrite();
        
        int mask = 0;
        
        if (copyDepth) {
            if (copyStencil) {
                mask = GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT;
            }
            else {
                mask = GL_DEPTH_BUFFER_BIT;
            }
        }
        else {
            if (copyStencil) {
                mask = GL_STENCIL_BUFFER_BIT;
            }
            else {
                throw new RuntimeException();
            }
        }
        
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, from.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId);
        
        GL30.glBlitFramebuffer(
            0, 0, from.width, from.height,
            0, 0, to.width, to.height,
            mask, GL_NEAREST
        );
        
        from.unbindWrite();
    }
}
