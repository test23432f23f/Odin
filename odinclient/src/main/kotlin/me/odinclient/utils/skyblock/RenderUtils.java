package me.odinclient.utils.skyblock;

import net.minecraft.client.renderer.GlStateManager;
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
    public static void drawLine(Vec3 pos1, Vec3 pos2, Color color)
    {
        GL11.glPushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.5F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        setColor(color);
        GL11.glTranslated(-(mc.getRenderManager()).viewerPosX, -(mc.getRenderManager()).viewerPosY, -(mc.getRenderManager()).viewerPosZ);
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

    public static void blockBox(BlockPos block, Color color)
    {
        GlStateManager.pushMatrix();
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        setColor(color);
        RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(
                block.getX() - (mc.getRenderManager()).viewerPosX,
                block.getY() - (mc.getRenderManager()).viewerPosY,
                block.getZ() - (mc.getRenderManager()).viewerPosZ,
                (block.getX() + 1) - (mc.getRenderManager()).viewerPosX,
                (block.getY() + 1) - (mc.getRenderManager()).viewerPosY,
                (block.getZ() + 1) - (mc.getRenderManager()).viewerPosZ));
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GlStateManager.popMatrix();
    }

    public static void setColor(Color c)
    {
       GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
    }
}
