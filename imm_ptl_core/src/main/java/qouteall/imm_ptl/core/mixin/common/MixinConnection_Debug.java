package qouteall.imm_ptl.core.mixin.common;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Connection.class)
public class MixinConnection_Debug {
    
    @Shadow
    private Channel channel;
    
    @Shadow
    @Final
    private static Logger LOGGER;
    
    // This will even trigger without ImmPtl
    // Vanilla seems stops entity tracking after closing the player connection
//    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"))
//    public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, CallbackInfo ci) {
//        if (channel != null && !channel.isOpen()) {
//            LOGGER.error("[ImmPtl] Packet sent after disconnect. This will cause memory leak.");
//
//            LOGGER.error("packet stack trace", new Throwable(""));
//        }
//    }
}
