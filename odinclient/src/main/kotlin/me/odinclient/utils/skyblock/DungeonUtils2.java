package me.odinclient.utils.skyblock;

import me.odinmain.OdinMain.mc;
import me.odinmain.events.impl.DungeonEvents.RoomEnterEvent;
import me.odinmain.events.impl.PacketReceivedEvent;
import me.odinmain.features.impl.dungeon.MapInfo.togglePaul;
import me.odinmain.utils.*;
import me.odinmain.utils.skyblock.*;
import me.odinmain.utils.skyblock.LocationUtils.currentDungeon;
import me.odinmain.utils.skyblock.PlayerUtils.posY;
import me.odinmain.utils.skyblock.dungeon.tiles.Room;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import kotlin.math.ceil;
import kotlin.math.floor;
import kotlin.math.roundToLong;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DungeonUtils {

    public static boolean isInDungeons() {
        return LocationUtils.getCurrentArea().isArea(Island.Dungeon);
    }

    public static int getFloorNumber() {
        return currentDungeon != null && currentDungeon.getFloor() != null ? currentDungeon.getFloor().getFloorNumber() : 0;
    }

    public static Floor getFloor() {
        return currentDungeon != null && currentDungeon.getFloor() != null ? currentDungeon.getFloor() : Floor.E;
    }

    public static boolean isInBoss() {
        return currentDungeon != null && currentDungeon.isInBoss();
    }

    public static int getSecretCount() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getSecretsFound() : 0;
    }

    public static int getKnownSecrets() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getKnownSecrets() : 0;
    }

    public static float getSecretPercentage() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getSecretsPercent() : 0f;
    }

    public static int getTotalSecrets() {
        return getSecretCount() == 0 || getSecretPercentage() == 0f ? 0 : (int) Math.floor(100 / getSecretPercentage() * getSecretCount() + 0.5);
    }

    public static int getDeathCount() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getDeaths() : 0;
    }

    public static int getCryptCount() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getCrypts() : 0;
    }

    public static int getOpenRoomCount() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getOpenedRooms() : 0;
    }

    public static int getCompletedRoomCount() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getCompletedRooms() : 0;
    }

    public static int getPercentCleared() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getPercentCleared() : 0;
    }

    public static int getTotalRooms() {
        return getCompletedRoomCount() == 0 || getPercentCleared() == 0 ? 0 : (int) Math.floor((getCompletedRoomCount() / (getPercentCleared() * 0.01f)) + 0.4);
    }

    public static List<Puzzle> getPuzzles() {
        return currentDungeon != null ? currentDungeon.getPuzzles() : new ArrayList<>();
    }

    public static int getPuzzleCount() {
        return currentDungeon != null && currentDungeon.getPuzzles() != null ? currentDungeon.getPuzzles().size() : 0;
    }

    public static String getDungeonTime() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getElapsedTime() : "00m 00s";
    }

    public static boolean isGhost() {
        return getItemSlot("Haunt", true) != null;
    }

    public static String getCurrentRoomName() {
        return currentDungeon != null && currentDungeon.getCurrentRoom() != null ? currentDungeon.getCurrentRoom().getData().getName() : "Unknown";
    }

    public static ArrayList<DungeonPlayer> getDungeonTeammates() {
        return currentDungeon != null ? currentDungeon.getDungeonTeammates() : new ArrayList<>();
    }

    public static ArrayList<DungeonPlayer> getDungeonTeammatesNoSelf() {
        return currentDungeon != null ? currentDungeon.getDungeonTeammatesNoSelf() : new ArrayList<>();
    }

    public static ArrayList<DungeonPlayer> getLeapTeammates() {
        return currentDungeon != null ? currentDungeon.getLeapTeammates() : new ArrayList<>();
    }

    public static DungeonPlayer getCurrentDungeonPlayer() {
        return getDungeonTeammates().stream()
                .filter(it -> it.getName().equals(mc.thePlayer != null ? mc.thePlayer.getName() : null))
                .findFirst()
                .orElse(new DungeonPlayer(mc.thePlayer != null ? mc.thePlayer.getName() : "Unknown", DungeonClass.Unknown, 0, mc.thePlayer));
    }

    public static String getDoorOpener() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null ? currentDungeon.getDungeonStats().getDoorOpener() : "Unknown";
    }

    public static boolean isMimicKilled() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null && currentDungeon.getDungeonStats().isMimicKilled();
    }

    public static Room getCurrentRoom() {
        return currentDungeon != null ? currentDungeon.getCurrentRoom() : null;
    }

    public static Set<Room> getPassedRooms() {
        return currentDungeon != null ? currentDungeon.getPassedRooms() : Set.of();
    }

    public static boolean isPaul() {
        return currentDungeon != null && currentDungeon.isPaul();
    }

    public static int getBonusScore() {
        int score = Math.min(getCryptCount(), 5);
        if (isMimicKilled()) score += 2;
        if ((isPaul() && togglePaul == 0) || togglePaul == 2) score += 10;
        return score;
    }

    public static boolean isBloodDone() {
        return currentDungeon != null && currentDungeon.getDungeonStats() != null && currentDungeon.getDungeonStats().isBloodDone();
    }

    public static int getScore() {
        float completed = getCompletedRoomCount() + (isBloodDone() ? 0f : 1f) + (isInBoss() ? 0f : 1f);
        float total = getTotalRooms() != 0 ? getTotalRooms() : 36f;

        int exploration = (int) Math.floor(Math.min(Math.max((getSecretPercentage() / getFloor().getSecretPercentage()) / 100f * 40f, 0f), 40f)) +
                (int) Math.floor(Math.min(Math.max(completed / total * 60f, 0f), 60f));

        int skillRooms = (int) Math.floor(Math.min(Math.max(completed / total * 80f, 0f), 80f));
        int puzzlePenalty = (int) getPuzzles().stream().filter(it -> it.getStatus() != PuzzleStatus.Completed).count() * 10;

        return exploration + (20 + skillRooms - puzzlePenalty - (getDeathCount() * 2 - 1));
    }
}
