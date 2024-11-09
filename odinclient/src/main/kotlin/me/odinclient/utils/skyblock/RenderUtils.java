package me.odinclient.utils.skyblock;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class RenderUtils
{
    protected static Minecraft mc = Minecraft.getMinecraft();
    public static void drawLine(Vec3 pos1, Vec3 pos2)
    {
            GL11.glPushMatrix();
          GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
           GL11.glLineWidth(2.5F);
           GL11.glDisable(3553);
         GL11.glDisable(2929);
             GL11.glDepthMask(false);

            
             GL11.glTranslated(-(mc.getRenderManager()).viewerPosX,
                     -(mc.getRenderManager()).viewerPosY,
                     -(mc.getRenderManager()).viewerPosZ);
             GL11.glBegin(1);

           GL11.glVertex3d(pos1.xCoord, pos1.yCoord, pos1.zCoord);
           GL11.glVertex3d(pos2.xCoord, pos2.yCoord, pos2.zCoord);

            GL11.glEnd();
           GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
           GL11.glDisable(3042);
            GL11.glPopMatrix();
           }

    public static void blockBox(BlockPos block) {
        /* 536 */     GL11.glBlendFunc(770, 771);
        /* 537 */     GL11.glEnable(3042);
        /* 538 */     GL11.glLineWidth(2.0F);
        /* 539 */     GL11.glDisable(3553);
        /* 540 */     GL11.glDisable(2929);
        /* 541 */     GL11.glDepthMask(false);
        /* 542 */     
        /* 543 */     RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(block

                .getX() -

                      (mc.getRenderManager()).viewerPosX, block
                         .getY() -

                         (mc.getRenderManager()).viewerPosY, block
                         .getZ() -

                          (mc.getRenderManager()).viewerPosZ, (block
                          .getX() + 1) -

                        (mc.getRenderManager()).viewerPosX, (block
                          .getY() + 1) -

                         (mc.getRenderManager()).viewerPosY, (block
                /* 560 */           .getZ() + 1) -
                /*     */
                /* 562 */           (mc.getRenderManager()).viewerPosZ));
        /* 563 */     GL11.glEnable(3553);
        /* 564 */     GL11.glEnable(2929);
        /* 565 */     GL11.glDepthMask(true);
        /* 566 */     GL11.glDisable(3042);
        /*     */   }

}
