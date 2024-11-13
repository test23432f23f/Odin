package me.odinclient.commands.impl

import me.odinmain.OdinMain.mc
import me.odinmain.commands.commodore
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.smoothRotateTo
import me.odinclient.utils.skyblock.AutoRouteUtils
import me.odinclient.utils.skyblock.RoutesManager
import net.minecraft.util.Vec3
import net.minecraft.util.ChatComponentText
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import me.odinmain.utils.skyblock.dungeon.tiles.Rotations
import me.odinmain.utils.skyblock.EtherWarpHelper
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition


fun method0(var0: Float, var1: Float, var2: Float): MovingObjectPosition {
    val var3 = mc.thePlayer.getPositionEyes(1.0F)
    val var4 = tr(var0, var1)
    val var5 = var3.addVector(var4.xCoord * var2.toDouble(), var4.yCoord * var2.toDouble(), var4.zCoord * var2.toDouble())
    return mc.theWorld.rayTraceBlocks(var3, var5, true, true, false)
}

fun tr(var0: Float, var1: Float): Vec3 {
    val var2 = MathHelper.cos(-var0 * 0.017453292F - 3.1415927F)
    val var3 = MathHelper.sin(-var0 * 0.017453292F - 3.1415927F)
    val var4 = -MathHelper.cos(-var1 * 0.017453292F)
    val var5 = MathHelper.sin(-var1 * 0.017453292F)
    return Vec3(var3 * var4.toDouble(), var5.toDouble(), var2 * var4.toDouble())
}

fun multiply(a: Vec3, m: Double): Vec3 {
    return Vec3(a.xCoord * m, a.yCoord * m, a.zCoord * m)
}

val RouteCommand = commodore("route") {


    
    literal("add").runs { subId: Int, type: String, yOffset: Double ->

                val mop: MovingObjectPosition = method0(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 64.0F)
                if(mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
                {
                    mc.thePlayer.addChatMessage(ChatComponentText("UNKNOWN POSITION"))
                }
        
                val route = RoutesManager.Route(
                    RoutesManager.Route.RouteType.valueOf(type),
                    AutoRouteUtils.currentRoomName,
                    subId.toInt(),
                    RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoomName, HashMap()).getOrDefault(subId.toInt(), ArrayList()).size,
                    if(AutoRouteUtils.currentRoom != null) AutoRouteUtils.currentRoom!!.getRelativeCoords(mop.hitVec.addVector(0.0, yOffset, 0.0)) else mop.hitVec.addVector(0.0, yOffset, 0.0),
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    mc.thePlayer.rotationPitch
                )

                val updated = RoutesManager.instance.loadedRoutes.getOrDefault(route.roomId, HashMap())
                val updatedList: MutableList<RoutesManager.Route> = updated.getOrDefault(route.id, ArrayList())
                updatedList.add(route)

                updated.put(route.id, updatedList) 
                RoutesManager.instance.loadedRoutes.put(AutoRouteUtils.currentRoomName, updated)
                RoutesManager.instance.saveConfig("./config/routes.abc")
                mc.thePlayer.addChatMessage(ChatComponentText("Added " + route.roomId + ", " + route.id + ", " + route.subId))

                
        
    }

    literal("addhere").runs { subId: Int, type: String ->
                val route = RoutesManager.Route(
                    RoutesManager.Route.RouteType.valueOf(type),
                    AutoRouteUtils.currentRoomName,
                    subId.toInt(),
                    RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoomName, HashMap()).getOrDefault(subId.toInt(), ArrayList()).size,
                    if(AutoRouteUtils.currentRoom != null) AutoRouteUtils.currentRoom!!.getRelativeCoords(Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)) else Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    mc.thePlayer.rotationPitch
                )

                val updated = RoutesManager.instance.loadedRoutes.getOrDefault(route.roomId, HashMap())
                val updatedList: MutableList<RoutesManager.Route> = updated.getOrDefault(route.id, ArrayList())
                updatedList.add(route)

                updated.put(route.id, updatedList) 
                RoutesManager.instance.loadedRoutes.put(AutoRouteUtils.currentRoomName, updated)
                RoutesManager.instance.saveConfig("./config/routes.abc")
                mc.thePlayer.addChatMessage(ChatComponentText("Added " + route.roomId + ", " + route.id + ", " + route.subId))

                
        
    }

    literal("set").runs { id: Int, subId: Int ->
        val updated = RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoomName, HashMap())
        val updatedList: MutableList<RoutesManager.Route> = updated.getOrDefault(id.toInt(), ArrayList())

         val mop: MovingObjectPosition = method0(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 61.0F)
                if(mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK)
                {
                    mc.thePlayer.addChatMessage(ChatComponentText("UNKNOWN POSITION"))

                    
                }
        
        val route = RoutesManager.Route(
                    updatedList.get(subId.toInt()).type,
                    AutoRouteUtils.currentRoomName,
                    subId.toInt(),
                    id.toInt(),
                    if(AutoRouteUtils.currentRoom != null) AutoRouteUtils.currentRoom!!.getRelativeCoords(mop.hitVec) else mop.hitVec,
                       MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    mc.thePlayer.rotationPitch
                )

                
                updatedList.removeAt(subId.toInt())
                updatedList.add(route)

                updated.put(route.id, updatedList) 
                RoutesManager.instance.loadedRoutes.put(AutoRouteUtils.currentRoomName, updated)
                RoutesManager.instance.saveConfig("./config/routes.abc")
                mc.thePlayer.addChatMessage(ChatComponentText("Set " + route.roomId + ", " + route.id + ", " + route.subId))
    }
    literal("remove").runs { id: Int, subId: Int ->
        RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoomName, HashMap()).getOrDefault(id, ArrayList()).removeAt(subId)
        RoutesManager.instance.saveConfig("./config/routes.abc")
        mc.thePlayer.addChatMessage(ChatComponentText("Removed " + id + " " + subId))
    }

    literal("reset").runs {
        AutoRouteUtils.currentRoom = null
        AutoRouteUtils.currentRoomName = "Unknown"
        mc.thePlayer.addChatMessage(ChatComponentText("Reset current room"))
    }

    literal("load").runs {
        RoutesManager.instance.loadConfig("./config/routes.abc")
        mc.thePlayer.addChatMessage(ChatComponentText("Loaded routes config"))
    }

    literal("save").runs {
        RoutesManager.instance.saveConfig("./config/routes.abc")
        mc.thePlayer.addChatMessage(ChatComponentText("Saved routes config"))
    }

    literal("rl").runs {
        RoutesManager.instance.saveConfig("./config/routes.abc")
        RoutesManager.instance.loadConfig("./config/routes.abc")
    }
}
