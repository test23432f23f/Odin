package me.odinclient.utils.skyblock;

import net.minecraft.util.Vec3;
import me.odinclient.utils.skyblock.AutoRouteUtils;
import org.json.*;
import net.minecraft.client.Minecraft;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraft.util.ChatComponentText;
import java.awt.*;
import me.odinmain.utils.render.Color;
import me.odinmain.utils.skyblock.dungeon.tiles.Rotations;

public class RoutesManager
{
    public static RoutesManager instance;

    public HashMap<String, HashMap<Integer, List<Route>>> loadedRoutes = new HashMap<>();

    public void saveConfig(String config)
    {
        File file = new File(config);
        try
        {
            file.delete();
            file.createNewFile();

            FileWriter writer = new FileWriter(file);

            JSONObject mainObject = new JSONObject();
            JSONObject settingsObject = new JSONObject();

            JSONObject routesObject = new JSONObject();
            

            for(String roomId : loadedRoutes.keySet())
            {
                JSONObject roomObject = new JSONObject();
                for(int id : loadedRoutes.get(roomId).keySet())
                {
                    JSONObject subRoutes = new JSONObject();
                    for(Route route : loadedRoutes.get(roomId).get(id))
                    {
                        JSONObject routeObject = new JSONObject();
                        routeObject.put("type", route.type);
                        routeObject.put("x", route.pos.xCoord);
                        routeObject.put("y", route.pos.yCoord);
                        routeObject.put("z", route.pos.zCoord);
                        routeObject.put("yaw", route.yaw);
                        routeObject.put("pitch", route.pitch);
                        routeObject.put("rotation", route.rotation);
                        subRoutes.put("" + route.subId, routeObject);
                    }
                    roomObject.put("" + id, subRoutes);
                }
                routesObject.put("" + roomId, roomObject);
            }


            mainObject.put("routes", routesObject);

            //settings
            /*settingsObject.put("rotationDelay", AutoRouteUtils.companion.rotationDelay);
            settingsObject.put("etherDelay", AutoRouteUtils.companion.etherDelay);*/

            mainObject.put("settings", settingsObject);

            writer.write(mainObject.toString(4));

            writer.close();
        }
        catch(IOException exception)
        {
            System.out.println("Failed to save config: " + exception.getMessage());
        }
    }

    public void loadConfig(String config)
    {
        File file = new File(config);
        try
        {
            if(!file.exists())
            {
                file.createNewFile();
                return;
            }

            if(Files.readAllLines(file.toPath()).isEmpty()) return;

            JSONObject routesObject = getKey(new JSONObject(this.listToString(Files.readAllLines(file.toPath()))), "routes");
            if(routesObject != null)
            {
                for(String roomKey : routesObject.keySet())
                {
                    HashMap<Integer, List<Route>> roomList = new HashMap<>();
                    JSONObject room = (JSONObject) routesObject.get(roomKey);

                    for(String idKey : room.keySet())
                    {
                        List<Route> routesList = new ArrayList<>();
                        JSONObject routes = (JSONObject) room.get(idKey);
                        for(String subIdKey : routes.keySet())
                        {
                            JSONObject r = routes.getJSONObject(subIdKey);
                            routesList.add(new Route(Route.RouteType.valueOf((String) r.get("type")),
                                    roomKey,
                                    Integer.parseInt(idKey),
                                    Integer.parseInt(subIdKey),
                                    new Vec3(r.getDouble("x"), r.getDouble("y"), r.getDouble("z")),
                                    r.getFloat("yaw"), r.getFloat("pitch"), Rotations.valueOf(r.getString("rotation"))));

                            if(Minecraft.getMinecraft().thePlayer != null)
                                 Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(roomKey + " : " + idKey + " : " + subIdKey));
                                
                        }
                        roomList.put(Integer.parseInt(idKey), routesList);

                    }
                    loadedRoutes.put(roomKey, roomList);
                }
            }

            /*JSONObject settings = getKey(new JSONObject(this.listToString(Files.readAllLines(file.toPath()))), "settings");
            if(settings != null)
            {
              AutoRouteUtils.companion.rotationDelay = settings.getInt("rotationDelay");
                AutoRouteUtils.companion.etherDelay = settings.getInt("etherDelay");
            }*/
        }
        catch(IOException exception)
        {
            System.out.println("Failed to load config: " + exception.getMessage());
        }
    }


    private <T> String listToString(List<T> a)
    {
        String f = "";
        for(T t : a) f += t.toString();
        return f;
    }

    public JSONObject getKey(JSONObject object, String key)
    {
        if(object.has(key)) return object.getJSONObject(key);

        return null;
    }

    public static class Route
    {
        public RouteType type;
        public String roomId;
        public int id;
        public int subId;
        public Vec3 pos;
        public float yaw, pitch;
        public Rotations rotation;

        public Route(RouteType type, String roomId, int id, int subId, Vec3 pos, float yaw, float pitch, Rotations rotation)
        {
            this.type = type;
            this.id = id;
            this.subId = subId;
            this.pos = pos;
            this.roomId = roomId;
            this.yaw = yaw;
            this.pitch = pitch;
            this.rotation = rotation;
        }

        public enum RouteType
        {
            ETHERWARP(me.odinmain.utils.render.Color.PURPLE), TELEPORT(me.odinmain.utils.render.Color.BLUE), WAIT(me.odinmain.utils.render.Color.YELLOW), USE_WAIT(me.odinmain.utils.render.Color.ORANGE), STOP(me.odinmain.utils.render.Color.RED);

            public me.odinmain.utils.render.Color color;
            RouteType(me.odinmain.utils.render.Color color)
            {
                this.color = color;
            }
        }
    }
}
