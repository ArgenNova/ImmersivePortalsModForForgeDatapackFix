package qouteall.imm_ptl.core.mixin.client;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEMatrix4f;

//mojang does not provide a method to load numbers into matrix
@Mixin(Matrix4f.class)
public class MixinMatrix4f implements IEMatrix4f {
    @Shadow
    protected
    float m00;
    @Shadow
    protected
    float m01;
    @Shadow
    protected
    float m02;
    @Shadow
    protected
    float m03;
    @Shadow
    protected
    float m10;
    @Shadow
    protected
    float m11;
    @Shadow
    protected
    float m12;
    @Shadow
    protected
    float m13;
    @Shadow
    protected
    float m20;
    @Shadow
    protected
    float m21;
    @Shadow
    protected
    float m22;
    @Shadow
    protected
    float m23;
    @Shadow
    protected
    float m30;
    @Shadow
    protected
    float m31;
    @Shadow
    protected
    float m32;
    @Shadow
    protected
    float m33;
    
    @Override
    public void loadFromArray(float[] arr) {
        m00 = arr[0];
        m01 = arr[1];
        m02 = arr[2];
        m03 = arr[3];
        m10 = arr[4];
        m11 = arr[5];
        m12 = arr[6];
        m13 = arr[7];
        m20 = arr[8];
        m21 = arr[9];
        m22 = arr[10];
        m23 = arr[11];
        m30 = arr[12];
        m31 = arr[13];
        m32 = arr[14];
        m33 = arr[15];
    }
    
    @Override
    public void loadToArray(float[] arr) {
        arr[0] = m00;
        arr[1] = m01;
        arr[2] = m02;
        arr[3] = m03;
        arr[4] = m10;
        arr[5] = m11;
        arr[6] = m12;
        arr[7] = m13;
        arr[8] = m20;
        arr[9] = m21;
        arr[10] = m22;
        arr[11] = m23;
        arr[12] = m30;
        arr[13] = m31;
        arr[14] = m32;
        arr[15] = m33;
    }
}
