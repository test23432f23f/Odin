package me.odinclient.utils.skyblock

import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
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
    private val setPosition by BooleanSetting("Set Position", false, description = "SET POS")
    private val click by BooleanSetting("Click", false, description = "CASD")
    
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
                    lastRoute.pos, route.pos, color
                )
                if(boxes)
                {
                    RenderUtils.blockBox(
                        BlockPos(route.pos), color
                    )
                }
                
                }
                else
                {
                    if (lastRoute != null && lines) RenderUtils.drawLine(
                    currentRoom!!.getRealCoords(lastRoute.pos), currentRoom!!.getRealCoords(route.pos), color
                    
                )
                if(boxes)
                {
                    RenderUtils.blockBox(
                        BlockPos(currentRoom!!.getRealCoords(route.pos)), color
                    )
                }
                }
                
                lastRoute = route
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) {
        if (event.packet !is C03PacketPlayer || !cancelling) {
            return
        }
        
        if (!event.isCanceled) {
            event.setCanceled(true)
            mc.thePlayer.addChatMessage(ChatComponentText("Cancelled C03"))
           
        }
        cancelling = false
    }

    @SubscribeEvent
    fun onPacketC08(event: PacketSentEvent)
    {
        if(event.packet is C08PacketPlayerBlockPlacement)
        {
            rotationTimer.reset()
        }
    }

    val rotationTimer: Timer = Timer()
    val clickTimer: Timer = Timer()
    
    @SubscribeEvent
    fun onMotion(event: MotionUpdateEvent) {
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

                        event.yaw = yaw
                        event.pitch = pitch

                       
                       if(clickTimer.hasPassed(clickDelay))
                        {
                            mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            clickTimer.reset()
                        }
                       /*if (rotationTimer.hasPassed(rotationDelay)) 
                        {
                          if(setPosition)
                          {
                               Timer.schedule({mc.thePlayer.setPosition(nextRoute.pos.xCoord.toInt() + 0.5, nextRoute.pos.yCoord + 1.05, nextRoute.pos.zCoord.toInt() + 0.5)}, 0L)
                          }

                          if(!mode)
                          {
                               cancelRotate(yaw, pitch)
                          }
                          else
                          {
                              mc.thePlayer.rotationYaw = yaw
                              mc.thePlayer.rotationPitch = pitch
                          }
                         
                          Timer.schedule({ mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))}, clickDelay.toLong())
                          
    
                          rotationTimer.reset()
                        }*/
                    }
                }
            }
        }
    }
    var clicked: Boolean = false
    var done: Boolean = true;
    fun doEther(yaw: Float, pitch: Float)
    {
        if(!done)
        {
            return
        }

        Timer.schedule(
            {
                if(!mode)
                {
                    cancelRotate(yaw, pitch)
                }
                else
                {
                    mc.thePlayer.rotationYaw = yaw
                    mc.thePlayer.rotationPitch = pitch
                }
                
            }, rotationDelay.toLong())
        done = false;
    }

    var cancelling = false
    
    fun cancelRotate(yaw: Float, pitch: Float) {
       val player = mc.thePlayer as IEntityPlayerSPAccessor ?: return
        val x = mc.thePlayer.posX - player.lastReportedPosX
        val y = mc.thePlayer.posY - player.lastReportedPosX
        val z = mc.thePlayer.posZ - player.lastReportedPosZ
       // val yaw = mc.thePlayer.rotationYaw - player.lastReportedYaw
       // val pitch = mc.thePlayer.rotationPitch - player.lastReportedPitch
        val moving = x * x + y * y + z * z > 9.0E-40 || player.positionUpdateTicks >= 20
       // val rotating = yaw != 0.0f || pitch != 0.0f;
        
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
