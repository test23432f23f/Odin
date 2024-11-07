package me.odinclient.utils.skyblock;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import me.odinmain.events.impl.PacketReceivedEvent;
import me.odinmain.events.impl.PacketSentEvent;
import me.odinclient.utils.skyblock.RoutesManager;
import me.odinclient.utils.skyblock.Timer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import me.odinmain.features.Module;
import net.minecraft.client.entity.EntityPlayerSP;
import me.odinmain.features.Category;
import me.odinclient.mixin.accessors.IEntityPlayerSPAccessor;
import me.odinmain.events.impl.RoomEnterEvents;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import me.odinmain.utils.skyblock.dungeon.DungeonUtils;

public class AutoRouteUtils
{
    public static String currentRoom = "Unknown";
    @SubscribeEvent
    public void onRoom(RoomEnterEvent event)
    {
        String name = event.getRoom().getData() != null ? event.getRoom().getData().getName() : "Unknown";
        currentRoom = name;
    }
    
    protected final Minecraft mc = Minecraft.getMinecraft();
    public Color color = new Color(0xFF10FD);
    public double tolerance = 0.7;

    public static int rotationDelay = 125, etherDelay = 150;
    public boolean rotationQueued, etherQueued;

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event)
    {
        if(RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoom) == null)
        {
            return;
        }

        RoutesManager.Route lastRoute;
        for(int id : RoutesManager.instance.loadedRoutes.get(currentRoom).keySet())
        {
            lastRoute = null;
            for(RoutesManager.Route route : RoutesManager.instance.loadedRoutes.get(currentRoom).get(id))
            {
                if(lastRoute != null)
                    RenderUtils.drawLine(
                            lastRoute.pos, route.pos,
                            color.brighter());

                    RenderUtils.blockBox(new BlockPos(route.pos), route.subId == 0 ? color.darker().darker() : color);

                lastRoute = route;
            }
        }
    }

    @SubscribeEvent
    public void onPacket(PacketSentEvent event)
    {
        if (!(event.getPacket() instanceof C03PacketPlayer) || !cancelling)
        {
            return;
        }
        if (!event.isCanceled())
        {
            event.setCanceled(true);
        }
        cancelling = false;
    }


    Timer rotationTimer = new Timer();
    Timer etherTimer = new Timer();

    @SubscribeEvent
    public void onUpdate(TickEvent.ClientTickEvent event)
    {
        if(mc.thePlayer == null)
        {
            return;
        }
        if(RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoom) == null)
        {
            return;
        }
        for(String roomId : RoutesManager.instance.loadedRoutes.keySet())
        {
            for(int id : RoutesManager.instance.loadedRoutes.get(roomId).keySet())
            {
                List<RoutesManager.Route> routes = RoutesManager.instance.loadedRoutes.get(roomId).get(id)
                        .stream().sorted(Comparator.comparingInt(r -> r.subId)).collect(Collectors.toList());
                for(int i = 0; i < routes.size(); i++)
                {
                    if(routes.size() < 2) continue;

                    RoutesManager.Route route = routes.get(i);

                    if(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).distanceTo(route.pos)
                            <= tolerance && i < routes.size() && i + 1 < routes.size() && (getSkyBlockID(mc.thePlayer.getHeldItem())
                            .equals("ASPECT_OF_THE_VOID") || getDisplayName(mc.thePlayer.getHeldItem()).toLowerCase().contains("aspect of the void")) && mc.thePlayer.isSneaking())
                    {
                        RoutesManager.Route nextRoute = routes.get(i + 1);

                        float yaw;
                        float pitch;

                        yaw = route.yaw;
                        pitch = route.pitch;

                        if(rotationTimer.hasPassed(rotationDelay))
                        {
                            cancelRotate(yaw, pitch);
                        
                            rotationTimer.reset();
                        }

                        if(etherTimer.hasPassed(etherDelay))
                        {
                            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            etherTimer.reset();
                        }
                    }
                }
            }
        }
    }

    public boolean cancelling = false;

    public void cancelRotate(float yaw, float pitch)
    {
        IEntityPlayerSPAccessor player = (IEntityPlayerSPAccessor) mc.thePlayer;
        if (player == null)
        {
            return;
        }
        double x = mc.thePlayer.posX - player.getLastReportedPosX();
        double y = mc.thePlayer.posY - player.getLastReportedPosX();
        double z = mc.thePlayer.posZ - player.getLastReportedPosZ();
        boolean moving = x * x + y * y + z * z > 9.0E-4 || player.getPositionUpdateTicks() >= 20;
        if (moving) {
            //ChatLib.sendf("C06");
            mc.getNetHandler().getNetworkManager().sendPacket(
                    new C03PacketPlayer.C06PacketPlayerPosLook(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ,
                            yaw,
                            pitch,
                            mc.thePlayer.onGround
                    )
            );
        } else {
            //ChatLib.sendf("C05");
            mc.getNetHandler().getNetworkManager().sendPacket(new C03PacketPlayer.C05PacketPlayerLook(
                    yaw,
                    pitch,
                    mc.thePlayer.onGround
            ));
        }
        cancelling = true;
    }
    
    public List<RoutesManager.Route> getSortedRoutes(String roomId, int routeId)
    {
        return RoutesManager.instance.loadedRoutes.get(roomId).get(routeId).stream().sorted(Comparator.comparingInt(r -> r.id)).collect(Collectors.toList());
    }

      public static String getDisplayName(ItemStack stack)
    {
        return stack != null ? stack.hasDisplayName() ? stack.getDisplayName() : "" : "";
    }

    public static String getSkyBlockID(ItemStack item)
    {
        if(item != null)
        {
            NBTTagCompound extraAttributes = item.getSubCompound("ExtraAttributes", false);
            if(extraAttributes != null && extraAttributes.hasKey("id")) {
                return extraAttributes.getString("id");
            }
        }
        return "";
    }
}
