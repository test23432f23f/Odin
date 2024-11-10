package me.odinclient.mixin.mixins.entity;

import me.odinmain.events.impl.MessageSentEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.odinmain.OdinMain.mc;
import me.odinmain.events.impl.MotionUpdateEvent;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Final;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;

import static me.odinmain.utils.Utils.postAndCatch;

@Mixin(value = EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

     @Shadow @Final public NetHandlerPlayClient sendQueue;

    @Shadow protected abstract boolean isCurrentViewEntity();

    @Shadow private boolean serverSneakState;

    @Shadow private boolean serverSprintState;

    @Shadow public abstract boolean isSneaking();

    @Shadow private double lastReportedPosX;

    @Shadow private double lastReportedPosY;

    @Shadow private double lastReportedPosZ;

    @Shadow private float lastReportedPitch;

    @Shadow private float lastReportedYaw;

    @Shadow private int positionUpdateTicks;
    
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (postAndCatch(new MessageSentEvent(message)))
            ci.cancel();
    }

    @Overwrite
    public void onUpdateWalkingPlayer()
    {
        boolean flag = mc.thePlayer.isSprinting();

        MotionUpdateEvent preMotionUpdateEvent = new MotionUpdateEvent(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround);
        
         if(MinecraftForge.EVENT_BUS.post(preMotionUpdateEvent))
             return;
        
        if (flag != this.serverSprintState)
        {
            if (flag)
            {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            }
            else
            {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = flag;
        }

        boolean flag1 = this.isSneaking();

        if (flag1 != this.serverSneakState)
        {
            if (flag1)
            {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
            }
            else
            {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }

            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity())
        {
            double d0 = preMotionUpdateEvent.x - this.lastReportedPosX;
            double d1 = preMotionUpdateEvent.y - this.lastReportedPosY;
            double d2 = preMotionUpdateEvent.z - this.lastReportedPosZ;
            double d3 = (double)(preMotionUpdateEvent.yaw - this.lastReportedYaw);
            double d4 = (double)(preMotionUpdateEvent.pitch - this.lastReportedPitch);
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean flag3 = d3 != 0.0D || d4 != 0.0D;

            if (mc.thePlayer.ridingEntity == null)
            {
                if (flag2 && flag3)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(preMotionUpdateEvent.x,
                            preMotionUpdateEvent.y, preMotionUpdateEvent.z, preMotionUpdateEvent.yaw,
                            preMotionUpdateEvent.pitch, preMotionUpdateEvent.onGround));
                }
                else if (flag2)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                            preMotionUpdateEvent.x, preMotionUpdateEvent.y, preMotionUpdateEvent.z, preMotionUpdateEvent.onGround));
                }
                else if (flag3)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(preMotionUpdateEvent.yaw, preMotionUpdateEvent.pitch, preMotionUpdateEvent.onGround));
                }
                else
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(preMotionUpdateEvent.onGround));
                }
            }
            else
            {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D, this.motionZ, preMotionUpdateEvent.yaw,
                        preMotionUpdateEvent.pitch, preMotionUpdateEvent.onGround));
                flag2 = false;
            }

            ++this.positionUpdateTicks;

            if (flag2)
            {
                this.lastReportedPosX = preMotionUpdateEvent.x;
                this.lastReportedPosY = preMotionUpdateEvent.y;
                this.lastReportedPosZ = preMotionUpdateEvent.z;
                this.positionUpdateTicks = 0;
            }

            if (flag3)
            {
                this.lastReportedYaw = preMotionUpdateEvent.yaw;
                this.lastReportedPitch = preMotionUpdateEvent.pitch;
            }
        }
    }
}
