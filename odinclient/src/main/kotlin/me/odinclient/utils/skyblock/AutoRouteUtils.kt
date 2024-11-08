package me.odinclient.utils.skyblock

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
import me.odinmain.events.impl.DungeonEvents.RoomEnterEvent;
import me.odinmain.utils.skyblock.dungeon.tiles.Room;

class AutoRouteUtils {
    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        currentRoom = event.getRoom()
        val name = if (event.room?.data?.name? != null) event.room.data?.name? else "Unknown"
        currentRoomName = name
    }

    protected val mc = Minecraft.getMinecraft()
    var color = Color(0xFF10FD)
    var tolerance = 0.7
    var rotationQueued = false
    var etherQueued = false
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent?) {
        if (RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoom) == null) {
            return
        }
        var lastRoute: RoutesManager.Route?
        for (id in RoutesManager.instance.loadedRoutes.get(currentRoom)!!.keys) {
            lastRoute = null
            for (route in RoutesManager.instance.loadedRoutes.get(currentRoom)!![id]!!) {
                if (lastRoute != null) RenderUtils.drawLine(
                    currentRoom.getRealCoords(lastRoute.pos), currentRoom.getRealCoords(route.pos),
                    color.brighter()
                )
                RenderUtils.blockBox(
                    BlockPos(currentRoom.getRealCoords(route.pos)),
                    if (route.subId == 0) color.darker().darker() else color
                )
                lastRoute = route
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.getPacket() !is C03PacketPlayer || !cancelling) {
            return
        }
        if (!event.isCanceled()) {
            event.setCanceled(true)
        }
        cancelling = false
    }

    var rotationTimer: Timer = Timer()
    var etherTimer: Timer = Timer()
    @SubscribeEvent
    fun onUpdate(event: ClientTickEvent?) {
        if (mc.thePlayer == null) {
            return
        }
        if (RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoom) == null) {
            return
        }
        for (roomId in RoutesManager.instance.loadedRoutes.keys) {
            for (id in RoutesManager.instance.loadedRoutes[roomId]!!.keys) {
                val routes = RoutesManager.instance.loadedRoutes[roomId]!![id]
                    .stream().sorted(Comparator.comparingInt { r: RoutesManager.Route -> r.subId })
                    .collect(Collectors.toList())
                for (i in routes.indices) {
                    if (routes.size < 2) continue
                    val route = routes[i]
                    if ((Vec3(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ
                        ).distanceTo(currentRoom.getRealCoords(route.pos))
                                <= tolerance) && i < routes.size && i + 1 < routes.size && ((getSkyBlockID(mc.thePlayer.heldItem)
                                == "ASPECT_OF_THE_VOID") || getDisplayName(mc.thePlayer.heldItem).lowercase(Locale.getDefault())
                            .contains("aspect of the void")) && mc.thePlayer.isSneaking
                    ) {
                        val nextRoute = routes[i + 1]
                        var yaw: Float
                        var pitch: Float
                        yaw = route.yaw
                        pitch = route.pitch
                        if (rotationTimer.hasPassed(rotationDelay)) {
                            cancelRotate(yaw, pitch)
                            rotationTimer.reset()
                        }
                        if (etherTimer.hasPassed(etherDelay)) {
                            mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            etherTimer.reset()
                        }
                    }
                }
            }
        }
    }

    var cancelling = false
    fun cancelRotate(yaw: Float, pitch: Float) {
        val player = mc.thePlayer as IEntityPlayerSPAccessor ?: return
        val x = mc.thePlayer.posX - player.lastReportedPosX
        val y = mc.thePlayer.posY - player.lastReportedPosX
        val z = mc.thePlayer.posZ - player.lastReportedPosZ
        val moving = x * x + y * y + z * z > 9.0E-4 || player.positionUpdateTicks >= 20
        if (moving) {
            //ChatLib.sendf("C06");
            mc.netHandler.networkManager.sendPacket(
                C06PacketPlayerPosLook(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    yaw,
                    pitch,
                    mc.thePlayer.onGround
                )
            )
        } else {
            //ChatLib.sendf("C05");
            mc.netHandler.networkManager.sendPacket(
                C05PacketPlayerLook(
                    yaw,
                    pitch,
                    mc.thePlayer.onGround
                )
            )
        }
        cancelling = true
    }

    fun getSortedRoutes(roomId: String?, routeId: Int): List<RoutesManager.Route> {
        return RoutesManager.instance.loadedRoutes.get(roomId)!![routeId]!!.stream()
            .sorted(Comparator.comparingInt { r: RoutesManager.Route -> r.id }).collect(Collectors.toList())
    }

    companion object {
        var currentRoom: Room? = null
        var currentRoomName = "Unknown"
        var rotationDelay = 125
        var etherDelay = 150
        fun getDisplayName(stack: ItemStack?): String {
            return if (stack != null) if (stack.hasDisplayName()) stack.displayName else "" else ""
        }

        fun getSkyBlockID(item: ItemStack?): String {
            if (item != null) {
                val extraAttributes = item.getSubCompound("ExtraAttributes", false)
                if (extraAttributes != null && extraAttributes.hasKey("id")) {
                    return extraAttributes.getString("id")
                }
            }
            return ""
        }
    }
}
