package me.odinclient.utils.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C0BPacketEntityAction
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
import me.odinmain.features.settings.impl.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.EnumFacing;
import kotlin.concurrent.thread
import java.lang.Thread
import me.odinmain.events.impl.MotionUpdateEvent



class AutoRouteUtils : Module(
    name = "Auto Routes",
    category = Category.DUNGEON,
    description = "idfk"
) {
    private val mode by DualSetting("Rotation Type", "Packet", "Setter", description = "")
    private val rotationDelay by NumberSetting("Rotation Delay", 250L, 50, 750, unit = "ms", description = "Delay between rotations.")
    private val clickDelay by NumberSetting("Click after delay", 5L, 0, 300, unit = "ms", description = "Delay between clicks.")
    private val lines by BooleanSetting("Lines", false, description = "Draw lines?")
    private val boxes by BooleanSetting("Boxes", false, description = "Draw boxes?")
    private val silentRotations by BooleanSetting("Silent Rotations", false, description = "Rotate silently.")
   
    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        currentRoom = event.room
        val name = event.room?.data?.name
        currentRoomName = name!!
    }
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
                    if (lastRoute != null && lines) RenderUtils.drawLine(
                    lastRoute.pos, route.pos, Color.WHITE
                )
                if(boxes)
                {
                    RenderUtils.blockBox(
                        BlockPos(route.pos), if(route.subId == 0) route.type.color.darker() else route.type.color
                    )
                }
                
                }
                else
                {
                    if (lastRoute != null && lines) RenderUtils.drawLine(
                    currentRoom!!.getRealCoords(lastRoute.pos), currentRoom!!.getRealCoords(route.pos), Color.WHITE
                    
                )
                if(boxes)
                {
                    RenderUtils.blockBox(
                        BlockPos(currentRoom!!.getRealCoords(route.pos)), if(route.subId == 0) route.type.color.darker() else route.type.color
                    )
                }
                }
                
                lastRoute = route
            }
        }
    }

  

    @SubscribeEvent
    fun onPacketC08(event: PacketSentEvent)
    {
        if(event.packet is C08PacketPlayerBlockPlacement)
        {
            clickTimer.reset()
        }
    }

    val waitTimer: Timer = Timer()
    val clickTimer: Timer = Timer()
    var doneWaiting = false
    
    @SubscribeEvent
    fun onMotion(event: MotionUpdateEvent) {
         
        if (mc.thePlayer == null) {
            return
        }
        if (RoutesManager.instance.loadedRoutes.isEmpty() || RoutesManager.instance.loadedRoutes.get(currentRoomName!!) == null) {
            return
        }
        Thread()
        {
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
                            .contains("aspect of the void"))
                    ) {
                        val nextRoute = routes[i + 1]
                        var yaw: Float = route.yaw
                        var pitch: Float = route.pitch

                        if(route.type == Route.RouteType.WAIT)
                        {

                             mc.thePlayer.addChatMessage(ChatComponentText("Waiting for route..."))
                            Thread.sleep(500L)
                           
                        }

                        
                        
                        if(silentRotations)
                        {
                            event.yaw = yaw
                            event.pitch = pitch
                        }
                        else
                        {
                            mc.thePlayer.rotationYaw = yaw
                            mc.thePlayer.rotationPitch = pitch
                        }

                        if(route.type == Route.RouteType.ETHERWARP || route.type == Route.RouteType.WAIT)
                        {
                            event.sneaking = true
                        }
                        else if(route.type == Route.RouteType.TELEPORT)
                        {
                            event.sneaking = false
                        }
                        
                       if(clickTimer.hasPassed(clickDelay))
                        {
                            val player = mc.thePlayer as IEntityPlayerSPAccessor
                            mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            clickTimer.reset()
                        }
                    }
                }
            }
        }
    }.start()
    }

    var sneaking = false

   companion object
    {
        var currentRoom: Room? = null
        var currentRoomName = "Unknown"
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
