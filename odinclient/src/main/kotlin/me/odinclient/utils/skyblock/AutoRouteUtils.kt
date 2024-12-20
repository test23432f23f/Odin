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
import kotlin.concurrent.thread
import java.lang.Thread
import me.odinmain.events.impl.MotionUpdateEvent
import me.odinmain.events.impl.MotionUpdateEventPost
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.render.Color
import me.odinmain.utils.*
import me.odinmain.utils.skyblock.dungeon.tiles.Rotations
import net.minecraft.util.EnumFacing
import me.odinmain.utils.render.RenderUtils



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
    private val tolerance by NumberSetting("Tolerance", default = 1.45, increment = 0.1, min = 0.5, max = 2.0, unit = " blocks", description = "")
    private val offsetX by NumberSetting("Offset X", 0, -1, 1, unit = " blocks", description = "")
    private val offsetZ by NumberSetting("Offset Z", 0, -1, 1, unit = " blocks", description = "")
 
    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        currentRoom = event.room
        val name = event.room?.data?.name
        currentRoomName = name!!

        mc.thePlayer.addChatMessage(ChatComponentText(("Entered room: " + currentRoomName + " : " + currentRoom!!.rotation.name)))
    }
   
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
                        /*Renderer.drawCylinder(
                                route.pos, tolerance, tolerance, .1f, 35,
                                1, 0f, 90f, 90f, if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!
                        )*/
                        
                       Renderer.drawBlock(
                        pos = BlockPos(route.pos),
                        color = if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!,
                        fillAlpha = 0,
                        depth = (renderDepthCheck && route.subId != 0))
                    }
                }
                else
                {
                    if (lastRoute != null && lines)
                    {
                        Renderer.draw3DLine(points = listOf(currentRoom.getRealCoords(lastRoute.pos), currentRoom.getRealCoords(route.pos)),
                                   color = me.odinmain.utils.render.Color.WHITE,
                                   lineWidth = 2f,
                                   depth = renderDepthCheck)
                    }
                    if(boxes)
                    {
                        /*Renderer.drawCylinder(
                                 currentRoom!!.getRealCoords(getOffset(route.pos, route.rotation, currentRoom!!.rotation)), tolerance, tolerance, .1f, 35,
                                1, 0f, 90f, 90f, if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!
                        )*/
                        
                         Renderer.drawBlock(
                         pos = BlockPos(currentRoom!!.getRealCoords(route.pos)),
                         color = if(route.subId == 0) me.odinmain.utils.render.Color.GREEN else route.type.color!!,
                         fillAlpha = 0,
                         depth = (renderDepthCheck && route.subId != 0))
                    }
                }
                
                lastRoute = route
            }
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
       
       
            for (id in RoutesManager.instance.loadedRoutes[currentRoomName!!]!!.keys) {
                val routes = RoutesManager.instance.loadedRoutes[currentRoomName!!]!![id]!!
                    .stream().sorted(Comparator.comparingInt { r: RoutesManager.Route -> r.subId })
                    .collect(Collectors.toList())

                    
                for (i in routes.indices) {
                    if (routes.size < 2) continue
                    val route = routes[i]
                    if ((Vec3(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ
                        ).distanceTo(if(currentRoom == null) route.pos else currentRoom!!.getRealCoords(route.pos)
                                <= tolerance) && i < routes.size && i + 1 < routes.size && ((getSkyBlockID(mc.thePlayer.heldItem)
                                == "ASPECT_OF_THE_VOID") || getDisplayName(mc.thePlayer.heldItem).lowercase()
                            .contains("aspect of the void"))
                    ) {

                        if(route.type == Route.RouteType.STOP)
                        {
                            return
                        }
                        
                        val nextRoute = routes[i + 1]
                        
                        val yaw: Float = getYaw(event.yaw, currentRoom!!.getRealCoords(nextRoute.pos)).toFloat()
                        val pitch: Float = nextRoute.pitch

                        if(silentRotations)
                        {
                            event.yaw = yaw
                            event.pitch = pitch

                           /* mc.thePlayer.addChatMessage(ChatComponentText("dy: " + currentRoom!!.getRealCoords(nextRoute.pos)))
                            mc.thePlayer.addChatMessage(ChatComponentText("yaw: " + yaw + ", pitch: " + pitch))*/
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
                               
                              event.clicked = true
                              //mc.thePlayer.addChatMessage(ChatComponentText("clicked"))
                              clickTimer.reset()
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

      fun getYaw(_yaw: Float, vec: Vec3): Float {
        val diffX = vec.xCoord - Minecraft.getMinecraft().thePlayer.posX
        val diffZ = vec.zCoord - Minecraft.getMinecraft().thePlayer.posZ
        val yaw: Float = (Math.atan2(diffZ.toDouble(), diffX.toDouble()) * 180.0 / Math.PI).toFloat() - 90.0F
        return _yaw + net.minecraft.util.MathHelper.wrapAngleTo180_float(yaw - _yaw)
    }

    fun getPitch(_pitch: Float, vec: Vec3): Float {
        val diffX = vec.xCoord - Minecraft.getMinecraft().thePlayer.posX
        val diffY = vec.yCoord - Minecraft.getMinecraft().thePlayer.posY
        val diffZ = vec.zCoord - Minecraft.getMinecraft().thePlayer.posZ
        val dist = net.minecraft.util.MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ)
        val pitch: Float = -(Math.atan2(diffY.toDouble(), dist.toDouble()) * 180.0 / Math.PI.toDouble()).toFloat()
        return _pitch + net.minecraft.util.MathHelper.wrapAngleTo180_float(pitch - _pitch)
    }

    fun getOffset(vec: Vec3, initial: Rotations, current: Rotations): Vec3 {
        var _vec: Vec3 = Vec3(vec.xCoord, vec.yCoord, vec.zCoord)
    return when (initial) {
        Rotations.NORTH -> when (current) {
            Rotations.NORTH -> vec
            Rotations.WEST ->  vec
            Rotations.SOUTH -> vec
            Rotations.EAST -> vec
            else -> vec
        }
        Rotations.WEST -> when (current) {
            Rotations.NORTH -> vec
            Rotations.WEST -> vec
            Rotations.SOUTH -> vec
            Rotations.EAST -> vec
            else -> vec
        }
        Rotations.SOUTH -> when (current) {
            Rotations.NORTH -> vec
            Rotations.WEST ->  vec
            Rotations.SOUTH -> vec
            Rotations.EAST -> vec
            else -> vec
        }
        Rotations.EAST -> when (current) {
            Rotations.NORTH -> vec
            Rotations.WEST ->  vec
            Rotations.SOUTH -> vec
            Rotations.EAST -> vec
            else -> vec
        }
        else -> vec
    }
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
