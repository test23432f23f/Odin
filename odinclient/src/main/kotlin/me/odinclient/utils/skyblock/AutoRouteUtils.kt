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
import java.util.stream.Collectors
import net.minecraft.util.ChatComponentText
import me.odinmain.features.settings.impl.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.EnumFacing;
import kotlin.concurrent.thread
import java.lang.Thread
import me.odinmain.events.impl.MotionUpdateEvent
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.render.Color
import me.odinmain.utils.*
import me.odinmain.utils.skyblock.dungeon.tiles.Rotations



class AutoRouteUtils : Module(
    name = "Auto Routes",
    category = Category.DUNGEON,
    description = "idfk"
) {
    private val clickDelay by NumberSetting("Click delay", 250L, 0, 1000, unit = "ms", description = "Delay between clicks.")
    private val waitDelay by NumberSetting("Wait delay", 750L, 0, 3000, unit = "ms", description = "")
    private val silentRotations by BooleanSetting("Silent Rotations", false, description = "Rotate silently.")
    private val lines by BooleanSetting("Lines", false, description = "Draw lines?")
    private val boxes by BooleanSetting("Boxes", false, description = "Draw boxes?")
    private val renderDepthCheck by BooleanSetting("Render Depth Check", false, description = "Depth check")
    private val editMode by BooleanSetting("Edit Mode", false, description = "Doesn't execute routes.")
  
    
   
    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        currentRoom = event.room
        val name = event.room?.data?.name
        currentRoomName = name!!

        mc.thePlayer.addChatMessage(ChatComponentText(("Entered room: " + currentRoomName + " : " + currentRoom!!.rotation.name)))
    }
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
                    if (lastRoute != null && lines)
                    {
                         Renderer.draw3DLine(points = listOf(lastRoute.pos, route.pos),
                                   color = me.odinmain.utils.render.Color.WHITE,
                                   lineWidth = 2f,
                                   depth = renderDepthCheck)
                    }
                    if(boxes)
                    {
                        Renderer.drawBlock(
                        pos = BlockPos(route.pos),
                        color = if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!,
                        fillAlpha = 0,
                        depth = renderDepthCheck)
                    }
                }
                else
                {
                    if (lastRoute != null && lines)
                    {
                        Renderer.draw3DLine(points = listOf(currentRoom!!.getRealCoords(lastRoute.pos), currentRoom!!.getRealCoords(route.pos)),
                                   color = me.odinmain.utils.render.Color.WHITE,
                                   lineWidth = 2f,
                                   depth = renderDepthCheck)
                    }
                    if(boxes)
                    {
                         Renderer.drawBlock(
                         pos = BlockPos(currentRoom!!.getRealCoords(route.pos)),
                         color = if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!,
                         fillAlpha = 0,
                         depth = renderDepthCheck)
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
         
        if (mc.thePlayer == null || editMode) {
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
                            .contains("aspect of the void"))
                    ) {

                        if(route.type == Route.RouteType.STOP)
                        {
                            return
                        }
                        
                        val nextRoute = routes[i + 1]
                        val dy: Int = Math.round((getYaw(currentRoom!!.getRealCoords(nextRoute.pos)) - 180.0f - route.yaw).toFloat())
                        val yaw: Float = route.yaw + dy.toFloat()
                        val pitch: Float = route.pitch
                       

                        if(silentRotations)
                        {
                            event.yaw = yaw
                            event.pitch = pitch

                            mc.thePlayer.addChatMessage(ChatComponentText("dy: " + dy))
                        }
                        else
                        {
                            mc.thePlayer.rotationYaw = yaw
                            mc.thePlayer.rotationPitch = pitch
                        }

                        if(route.type == Route.RouteType.ETHERWARP || route.type == Route.RouteType.WAIT || route.type == Route.RouteType.USE_WAIT)
                        {
                            event.sneaking = true
                        }
                        else if(route.type == Route.RouteType.TELEPORT)
                        {
                            event.sneaking = false
                        }
                        
                       if(clickTimer.hasPassed(clickDelay + (if(route.type==Route.RouteType.WAIT||route.type==Route.RouteType.USE_WAIT) waitDelay else 0L)) && 
                               ((getSkyBlockID(mc.thePlayer.heldItem) == "ASPECT_OF_THE_VOID") || getDisplayName(mc.thePlayer.heldItem).lowercase().contains("aspect of the void")))
                        {
                            val player = mc.thePlayer as IEntityPlayerSPAccessor
                            mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                            clickTimer.reset()
                        }
                    }
                }
            }
        }
    }

   

   companion object
   {
        var currentRoom: Room? = null
        var currentRoomName = "Unknown"
        
        fun getDisplayName(stack: ItemStack?): String 
       {
            return if (stack != null) if (stack.hasDisplayName()) stack.displayName else "" else ""
        }

        fun getSkyBlockID(item: ItemStack?): String 
       {
            if (item != null) 
           {
                val extraAttributes = item.getSubCompound("ExtraAttributes", false)
                if (extraAttributes != null && extraAttributes.hasKey("id")) 
               {
                    return extraAttributes.getString("id")
                }
            }
            return ""   
        }

       fun getYaw(end: Vec3): Float {
            return Minecraft.getMinecraft().thePlayer.rotationYaw + net.minecraft.util.MathHelper.wrapAngleTo180_float(
            (Math.atan2(end.xCoord - Minecraft.getMinecraft().thePlayer.posX, end.zCoord -
            Minecraft.getMinecraft().thePlayer.posZ).toFloat() * 180.0f / Math.PI - 90.0F).toFloat() - Minecraft.getMinecraft().thePlayer.rotationYaw
        )
    }

       /*fun pleaseKillMe(yaw: Float, vec3: Vec3): Float {
    return when {
        tolerates(yaw - getYaw(vec3), 85.0f, 95.0f) -> yaw
        tolerates(yaw + 90.0f - getYaw(vec3), 85.0f, 95.0f)-> yaw + 90.0f
        tolerates(yaw + 180.0f - getYaw(vec3), 85.0f, 95.0f) -> yaw + 180.0f
        tolerates(yaw + 90.0f - getYaw(vec3), 85.0f, 95.0f) == getYaw(vec3) -> yaw - 90.0f
        else -> yaw
    }
}*/


        /*fun searchFor(item: Item): Int 
       {
            for (i in 0 until 9) 
           {
                if (mc.thePlayer.inventory.getStackInSlot(i).item == item) {
                    
                    return i
                }
            }
            return -1
        }

        fun searchForId(item: String): Int 
       {
            for (i in 0 until 9) 
           {
                if (getSkyBlockID(mc.thePlayer.inventory.getStackInSlot(i)) == item) 
                {
                    return i
                }
            }
            return -1
        }*/
    }
}
