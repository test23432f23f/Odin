package me.odinclient.mixin.mixins.entity;

import me.odinmain.events.impl.MessageSentEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.odinmain.events.impl.MotionUpdateEvent;
import me.odinmain.events.impl.MotionUpdateEventPost;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Final;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.client.Minecraft;

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

     protected Minecraft mc = Minecraft.getMinecraft();
      /**
     * @author
     * @reason
     */
    @Overwrite
    public void onUpdateWalkingPlayer()
    {
        boolean flag = mc.thePlayer.isSprinting();

        MotionUpdateEvent preMotionUpdateEvent = new MotionUpdateEvent(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround, mc.thePlayer.isSneaking());
        
        if (MinecraftForge.EVENT_BUS.post(preMotionUpdateEvent)) 
        {
             return;
        }

         mc.thePlayer.renderYawOffset = preMotionUpdateEvent.getYaw();
         mc.thePlayer.rotationYawHead = preMotionUpdateEvent.getYaw();
        
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

        boolean flag1 = preMotionUpdateEvent.getSneaking();

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
            double d0 = preMotionUpdateEvent.getX() - this.lastReportedPosX;
            double d1 = preMotionUpdateEvent.getY() - this.lastReportedPosY;
            double d2 = preMotionUpdateEvent.getZ() - this.lastReportedPosZ;
            double d3 = (double)(preMotionUpdateEvent.getYaw() - this.lastReportedYaw);
            double d4 = (double)(preMotionUpdateEvent.getPitch() - this.lastReportedPitch);
            boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean flag3 = d3 != 0.0D || d4 != 0.0D;

            if (mc.thePlayer.ridingEntity == null)
            {
                if (flag2 && flag3)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(preMotionUpdateEvent.getX(),
                            preMotionUpdateEvent.getY(), preMotionUpdateEvent.getZ(), preMotionUpdateEvent.getYaw(),
                            preMotionUpdateEvent.getPitch(), preMotionUpdateEvent.getOnGround()));
                }
                else if (flag2)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                            preMotionUpdateEvent.getX(), preMotionUpdateEvent.getY(), preMotionUpdateEvent.getZ(), preMotionUpdateEvent.getOnGround()));
                }
                else if (flag3)
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(preMotionUpdateEvent.getYaw(), preMotionUpdateEvent.getPitch(), preMotionUpdateEvent.getOnGround()));
                }
                else
                {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(preMotionUpdateEvent.getOnGround()));
                }
            }
            else
            {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.motionX, -999.0D,mc.thePlayer.motionZ, preMotionUpdateEvent.getYaw(),
                        preMotionUpdateEvent.getPitch(), preMotionUpdateEvent.getOnGround()));
                flag2 = false;
            }

            ++this.positionUpdateTicks;

            if (flag2)
            {
                this.lastReportedPosX = preMotionUpdateEvent.getX();
                this.lastReportedPosY = preMotionUpdateEvent.getY();
                this.lastReportedPosZ = preMotionUpdateEvent.getZ();
                this.positionUpdateTicks = 0;
            }

            if (flag3)
            {
                this.lastReportedYaw = preMotionUpdateEvent.getYaw();
                this.lastReportedPitch = preMotionUpdateEvent.getPitch();
            }
        }
          MotionUpdateEventPost postMotionUpdateEvent = new MotionUpdateEventPost(preMotionUpdateEvent.getX(), preMotionUpdateEvent.getY(),  preMotionUpdateEvent.getZ(),
                 preMotionUpdateEvent.getYaw(), preMotionUpdateEvent.getPitch(),  preMotionUpdateEvent.getOnGround(),  preMotionUpdateEvent.getSneaking());

         MinecraftForge.EVENT_BUS.post(postMotionUpdateEvent)
    }
}
