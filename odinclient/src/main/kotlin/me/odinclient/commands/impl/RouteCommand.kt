package me.odinclient.commands.impl

import me.odinmain.OdinMain.mc
import me.odinmain.commands.commodore
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.smoothRotateTo
import me.odinclient.utils.skyblock.AutoRouteUtils
import me.odinclient.utils.skyblock.RoutesManager
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraft.util.ChatComponentText
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords

val RouteCommand = commodore("route") {
    literal("add").runs { subId: Int, type: String ->
                val route = RoutesManager.Route(
                    RoutesManager.Route.RouteType.valueOf(type),
                    AutoRouteUtils.currentRoomName,
                    subId.toInt(),
                    RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoomName, HashMap()).getOrDefault(subId.toInt(), ArrayList()).size,
                    if(AutoRouteUtils.currentRoom != null) AutoRouteUtils.currentRoom!!.getRelativeCoords(Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).addVector(-1, 0, 0) else Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
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
        
        val route = RoutesManager.Route(
                    updatedList.get(subId.toInt()).type,
                    AutoRouteUtils.currentRoomName,
                    subId.toInt(),
                    id.toInt(),
                    if(AutoRouteUtils.currentRoom != null) AutoRouteUtils.currentRoom!!.getRelativeCoords(Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).addVector(-1, 0, 0) else Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
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
