package me.odinclient.commands.impl

import me.odinmain.OdinMain.mc
import me.odinmain.commands.commodore
import me.odinmain.utils.skyblock.modMessage
import me.odinmain.utils.smoothRotateTo
import me.odinclient.utils.skyblock.AutoRouteUtils
import me.odinclient.utils.skyblock.RoutesManager
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3

val RouteCommand = commodore("route") {
    literal("add").runs { subId: Int, type: String ->
                val route = RoutesManager.Route(
                    RoutesManager.Route.RouteType.valueOf(type),
                    AutoRouteUtils.currentRoom,
                    subId.toInt(),
                    RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoom, HashMap()).getOrDefault(subId.toInt(), ArrayList()).size,
                    Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ),
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    mc.thePlayer.rotationPitch
                )

                val updated = RoutesManager.instance.loadedRoutes.getOrDefault(route.roomId, HashMap())
                val updatedList: MutableList<RoutesManager.Route> = updated.getOrDefault(route.id, ArrayList())
                updatedList.add(route)

                updated.put(route.id, updatedList) 
                RoutesManager.instance.loadedRoutes.put(AutoRouteUtils.currentRoom, updated)
                RoutesManager.instance.saveConfig("./config/routes.abc")
                mc.thePlayer.addChatMessage(ChatComponentText("Added " + route.roomId + ", " + route.id + ", " + route.subId))
        
    }
    literal("remove").runs { id: Int, subId Int ->
        RoutesManager.instnace.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoom, HashMap()).getOrDefault(id).remove(subId)
        mc.thePlayer.addChatMessage(ChatComponentText("Removed " + id + " " + subId))
    }
}
