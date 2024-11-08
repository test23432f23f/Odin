package me.odinclient.utils.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import me.odinmain.events.impl.PacketReceivedEvent
import me.odinmain.events.impl.PacketSentEvent
import me.odinclient.utils.skyblock.RoutesManager
import me.odinclient.utils.skyblock.RoutesManager.Route
import me.odinclient.utils.skyblock.Timer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.features.Module
import net.minecraft.client.entity.EntityPlayerSP
import me.odinmain.features.Category
import me.odinclient.mixin.accessors.IEntityPlayerSPAccessor
import me.odinmain.events.impl.DungeonEvents.RoomEnterEvent
import me.odinmain.utils.skyblock.dungeon.tiles.Room
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import java.awt.Color
import java.util.stream.Collectors
import net.minecraft.util.ChatComponentText



class AutoRouteUtils {
    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        currentRoom = event.room
        val name = event.room?.data?.name
        currentRoomName = name!!
    }

    protected val mc = Minecraft.getMinecraft()
    var color = Color(0xFF10FD)
    var tolerance = 0.7
    var rotationQueued = false
    var etherQueued = false
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent?) {
        if (RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoomName) == null) {
            return
        }
        var lastRoute: RoutesManager.Route?
        for (id in RoutesManager.instance.loadedRoutes.get(currentRoomName)!!.keys) {
            lastRoute = null
            for (route in RoutesManager.instance.loadedRoutes.get(currentRoomName)!![id]!!) {
                if(currentRoom == null)
                {
                    if (lastRoute != null) RenderUtils.drawLine(
                    lastRoute.pos, route.pos,
                    color.brighter()
                )
                RenderUtils.blockBox(
                    BlockPos(route.pos),
                    if (route.subId == 0) color.darker().darker() else color
                )
                }
                else
                {
                    if (lastRoute != null) RenderUtils.drawLine(
                    currentRoom!!.getRealCoords(lastRoute.pos), currentRoom!!.getRealCoords(route.pos),
                    color.brighter()
                )
                RenderUtils.blockBox(
                    BlockPos(currentRoom!!.getRealCoords(route.pos)),
                    if (route.subId == 0) color.darker().darker() else color
                )
                }
                
                lastRoute = route
            }
        }
    }

    var count = 0;
    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet !is C03PacketPlayer) {
            return
        }

        count++;

        if(!cancelling)
        {
            return
        }
        
        if (!event.isCanceled) {
            event.setCanceled(true)
            mc.thePlayer.addChatMessage(ChatComponentText("Cancelled C03"))
        }
        cancelling = false
    }


    var ppsTimer: Timer = Timer()
    @SubscribeEvent
    fun onUpdate2(event: ClientTickEvent?)
    {
        if(mc.thePlayer == null)
        {
            ppsTimer.reset()
            return
        }

        if(ppsTimer.hasPassed(1000L) && count > 0)
        {
             mc.thePlayer.addChatMessage(ChatComponentText("" + count))
             count = 0
            ppsTimer.reset()
        }
    }

    var rotationTimer: Timer = Timer()
    var etherTimer: Timer = Timer()
    @SubscribeEvent
    fun onUpdate(event: ClientTickEvent?) {
        if (mc.thePlayer == null) {
            return
        }
        if (RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoomName!!) == null) {
            return
        }
        for (roomId in RoutesManager.instance.loadedRoutes.keys!!) {
            for (id in RoutesManager.instance.loadedRoutes[roomId]!!.keys) {
                val routes = RoutesManager.instance.loadedRoutes[roomId]!![id]!!
                    .stream().sorted(Comparator.comparingInt { r: RoutesManager.Route -> r.subId })
                    .collect(Collectors.toList())
                for (i in routes.indices) {
                    if (routes.size < 2) continue
                    val route = routes[i]
                    if ((Vec3(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ
                        ).distanceTo(if(currentRoom == null) route.pos else currentRoom!!.getRealCoords(route.pos))
                                <= tolerance) && i < routes.size && i + 1 < routes.size && ((getSkyBlockID(mc.thePlayer.heldItem)
                                == "ASPECT_OF_THE_VOID") || getDisplayName(mc.thePlayer.heldItem).lowercase()
                            .contains("aspect of the void")) && mc.thePlayer.isSneaking
                    ) {
                        val nextRoute = routes[i + 1]
                        var yaw: Float = route.yaw
                        var pitch: Float = route.pitch
                       
                        if (rotationTimer.hasPassed(rotationDelay.toLong())) {
                            cancelRotate(yaw, pitch)
                            rotationTimer.reset()
                        }
                        if (etherTimer.hasPassed(etherDelay.toLong())) {
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
            //ChatLib.sendf("C06")
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
            //ChatLib.sendf("C05")
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
