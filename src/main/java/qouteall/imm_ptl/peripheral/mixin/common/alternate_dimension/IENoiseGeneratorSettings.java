package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseGeneratorSettings.class)
public interface IENoiseGeneratorSettings {
    @Invoker("floatingIslands")
    public static NoiseGeneratorSettings ip_floatingIslands(){
        throw new RuntimeException();
    }
    
    @Invoker("end")
    public static NoiseGeneratorSettings ip_end(){
        throw new RuntimeException();
    }
    
}
