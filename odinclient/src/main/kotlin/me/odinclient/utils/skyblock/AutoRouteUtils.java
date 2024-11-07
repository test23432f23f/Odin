package me.odinclient.utils.skyblock;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import me.odinmain.events.impl.PacketEvents;
import me.odinclient.utils.skyblock.RoutesManager;
import me.odinclient.utils.skyblock.Timer;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AutoRouteUtils
{
    protected final Minecraft mc = Minecraft.getMinecraft();
    public static int currentRoom = 0;
    public Color color = new Color(0xFF10FD);
    public double tolerance = 0.7;


    public static int rotationDelay = 125, etherDelay = 150;
    public boolean rotationQueued, etherQueued;

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event)
    {
        if(RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(DungeonUtils.getRoomId()) == null)
            return;

        if(currentRoom != DungeonUtils.getRoomId())
            ChatLib.sendf("Loaded routes for roomId %s", DungeonUtils.getRoomId());

        currentRoom = DungeonUtils.getRoomId();

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
        if (!(event.packet instanceof C03PacketPlayer) || !cancelling)
            return;
        if (!event.isCanceled())
        {
            ChatLib.sendf("Cancelled C03");
            event.setCanceled(true);
        }
        cancelling = false;
    }


    Timer rotationTimer = new Timer();
    Timer etherTimer = new Timer();

    @SubscribeEvent
    public void onUpdate(UpdateEvent event)
    {
        if(
        
        if(RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(DungeonUtils.getRoomId()) == null)
            return;

        for(int roomId : RoutesManager.instance.loadedRoutes.keySet())
        {
            for(int id : RoutesManager.instance.loadedRoutes.get(roomId).keySet())
            {
                List<RoutesManager.Route> routes = RoutesManager.instance.loadedRoutes.get(roomId).get(id)
                        .stream().sorted(Comparator.comparingInt(r -> r.subId)).collect(Collectors.toList());
                for(int i = 0; i < routes.size(); i++)
                {
                    if(routes.size() < 2) continue;

                    RoutesManager.Route route = routes.get(i);

                    if(new Vec3(Main.mc.thePlayer.posX, Main.mc.thePlayer.posY, Main.mc.thePlayer.posZ).distanceTo(route.pos)
                            <= tolerance && i < routes.size() && i + 1 < routes.size() && (ItemUtils.getSkyBlockID(mc.thePlayer.getHeldItem())
                            .equals("ASPECT_OF_THE_VOID") || ItemUtils.getDisplayName(mc.thePlayer.getHeldItem()).toLowerCase().contains("aspect of the void")) && mc.thePlayer.isSneaking())
                    {
                        RoutesManager.Route nextRoute = routes.get(i + 1);

                        float yaw = BlockUtils.getAngles(nextRoute.pos)[0];
                        float pitch = BlockUtils.getAngles(nextRoute.pos.add(new Vec3(0, -1.6, 0)))[1];

                        yaw = route.yaw;
                        pitch = route.pitch;

                        if(rotationTimer.hasPassed(rotationDelay))
                        {
                            cancelRotate(yaw, pitch);
                            ChatLib.sendf("%.2f, %.2f", yaw, pitch);
                            rotationTimer.reset();
                        }

                        if(etherTimer.hasPassed(etherDelay))
                        {
                            NetworkUtils.sendPacket(new C08PacketPlayerBlockPlacement(Main.mc.thePlayer.getHeldItem()));
                            ChatLib.sendf("Etherwarp");
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
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) 
            return;
        double x = mc.thePlayer.posX - player.lastReportedPosX;
        double y = mc.thePlayer.posY - player.lastReportedPosY;
        double z = mc.thePlayer.posZ - player.lastReportedPosZ;
        boolean moving = x * x + y * y + z * z > 9.0E-4 || player.positionUpdateTicks >= 20;
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
    public List<RoutesManager.Route> getSortedRoutes(int roomId, int routeId)
    {
        return RoutesManager.instance.loadedRoutes.get(roomId).get(routeId).stream().sorted(Comparator.comparingInt(r -> r.id)).collect(Collectors.toList());
    }
}
