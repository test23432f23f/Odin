package me.odinclient.commands.impl;

import me.odinmain.OdinMain;
import me.odinmain.commands.Commodore;
import me.odinmain.utils.SkyblockUtils;
import me.odinclient.utils.skyblock.AutoRouteUtils;
import me.odinclient.utils.skyblock.RoutesManager;
import me.odinclient.utils.skyblock.RoutesManager.Route;

public class OdinClientCommand {
    public static void register() {
        Commodore commodore = new Commodore("route");
        commodore.literal("add").runs((subId, type) -> {
            RoutesManager.Route route = new RoutesManager.Route(RoutesManager.Route.RouteType.valueOf(type),
                        AutoRouteUtils.currentRoom,
                        Integer.parseInt(subId),
                        RoutesManager.instance.loadedRoutes.getOrDefault(AutoRouteUtils.currentRoom, new HashMap<>()).getOrDefault(Integer.parseInt(subId), new ArrayList<>()).size(),
                    new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch);

                HashMap<Integer, List<RoutesManager.Route>> updated = RoutesManager.instance.loadedRoutes.getOrDefault(route.roomId, new HashMap<>());

                List<RoutesManager.Route> updatedList = updated.getOrDefault(route.id, new ArrayList<>());
                updatedList.add(route);

                updated.put(route.id, updatedList);

                RoutesManager.instance.loadedRoutes.put(AutoRouteUtils.currentRoom, updated);
                RoutesManager.instance.saveConfig(Main.ROUTES);
        });
    }
}

