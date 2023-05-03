/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm5;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import net.server.Server;
import scripting.event.EventInstanceManager;
import server.TimerManager;
import server.life.MapleMonster;
import server.life.SpawnPoint;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MaplePortal;
import server.maps.MapleReactor;

import java.awt.*;
import java.util.List;
import java.util.stream.IntStream;

public class DebugCommand extends Command {
    private final static String[] debugTypes = {"monster", "packet", "portal", "spawnpoint", "pos", "map", "mobsp", "event", "areas", "reactors", "servercoupons", "playercoupons", "timer", "marriage", "buff", ""};

    {
        setDescription("");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();

        if (params.length < 1) {
            player.yellowMessage("Syntax: !debug <type>");
            return;
        }

        switch (params[0]) {
            case "type":
            case "help":
                debugHelp(c);
                break;

            case "monster":
                debugMonster(player);
                break;

            case "packet":
                //player.getMap().broadcastMessage(MaplePacketCreator.customPacket(joinStringFrom(params, 1)));
                break;

            case "portal":
                debugPortal(player);
                break;

            case "spawnpoint":
                debugSpawnPoint(player);
                break;

            case "pos":
                player.dropMessage(6, "Current map position: (" + player.getPosition().getX() + ", " + player.getPosition().getY() + ").");
                break;

            case "map":
                player.dropMessage(6, "Current map id " + player.getMap().getId() + ", event: '" + player.getMap().getEventInstance().map(EventInstanceManager::getName).orElse ("null") + "'; Players: " + player.getMap().getAllPlayers().size() + ", Mobs: " + player.getMap().countMonsters() + ", Reactors: " + player.getMap().countReactors() + ", Items: " + player.getMap().countItems() + ", Objects: " + player.getMap().getMapObjects().size() + ".");
                break;

            case "mobsp":
                player.getMap().reportMonsterSpawnPoints(player);
                break;

            case "event":
                debugEvent(player);
                break;

            case "areas":
                debugAreas(player);
                break;

            case "reactors":
                debugReactors(player);
                break;

            case "servercoupons":
            case "coupons":
                debugCoupons(player);
                break;

            case "playercoupons":
                debugPlayerCoupons(player);
                break;

            case "timer":
                debugTimer(player);
                break;

            case "marriage":
                c.getChannelServer().debugMarriageStatus();
                break;

            case "buff":
                c.getPlayer().debugListAllBuffs();
                break;
        }
    }

    private static void debugTimer(MapleCharacter player) {
        TimerManager tMan = TimerManager.getInstance();
        player.dropMessage(6, "Total Task: " + tMan.getTaskCount() + " Current Task: " + tMan.getQueuedTasks() + " Active Task: " + tMan.getActiveCount() + " Completed Task: " + tMan.getCompletedTaskCount());
    }

    private static void debugPlayerCoupons(MapleCharacter player) {
        String message = player.getActiveCoupons().stream()
                .reduce(new StringBuilder("Currently active PLAYER coupons: "),
                        (sb, i) -> sb.append(i).append(" "),
                        StringBuilder::append)
                .toString();
        player.dropMessage(6, message);
    }

    private static void debugCoupons(MapleCharacter player) {
        String message = Server.getInstance().getActiveCoupons().stream()
                .reduce(new StringBuilder("Currently active SERVER coupons: "),
                        (sb, i) -> sb.append(i).append(" "),
                        StringBuilder::append)
                .toString();
        player.dropMessage(6, message);
    }

    private static void debugReactors(MapleCharacter player) {
        player.dropMessage(6, "Current reactor states on map " + player.getMapId() + ":");
        player.getMap()
                .getReactors().stream()
                .map(o -> (MapleReactor) o)
                .map(DebugCommand::reactorDebugMessage)
                .forEach(message -> player.dropMessage(6, message));
    }

    private static String reactorDebugMessage(MapleReactor r) {
        return String.format("Id: %d Oid: %d name: %s -> Type: %d State: %d Event State: %d Position: x %f y %f.", r.getId(), r.getObjectId(), r.getName(), r.getReactorType(), r.getState(), r.getEventState(), r.getPosition().getX(), r.getPosition().getY());
    }

    private static void debugAreas(MapleCharacter player) {
        player.dropMessage(6, "Configured areas on map " + player.getMapId() + ":");

        byte index = 0;
        for (Rectangle rect : player.getMap().getAreas()) {
            player.dropMessage(6, "Id: " + index + " -> posX: " + rect.getX() + " posY: '" + rect.getY() + "' dX: " + rect.getWidth() + " dY: " + rect.getHeight() + ".");
            index++;
        }
    }

    private static void debugEvent(MapleCharacter player) {
        if (player.getEventInstance().isEmpty()) {
            player.dropMessage(6, "Player currently not in an event.");
        } else {
            player.dropMessage(6, "Current event name: " + player.getEventInstance().get().getName() + ".");
        }
    }

    private static void debugSpawnPoint(MapleCharacter player) {
        SpawnPoint sp = player.getMap().findClosestSpawnpoint(player.getPosition());
        if (sp != null) {
            player.dropMessage(6, "Closest mob spawn point: " + " Position: x " + sp.getPosition().getX() + " y " + sp.getPosition().getY() + " Spawns mobid: '" + sp.getMonsterId() + "' --> canSpawn: " + !sp.getDenySpawn() + " canSpawnRightNow: " + sp.shouldSpawn() + ".");
        } else {
            player.dropMessage(6, "There is no mob spawn point on this map.");
        }
    }

    private static void debugPortal(MapleCharacter player) {
        MaplePortal portal = player.getMap().findClosestPortal(player.getPosition());
        if (portal != null) {
            player.dropMessage(6, "Closest portal: " + portal.getId() + " '" + portal.getName() + "' Type: " + portal.getType() + " --> toMap: " + portal.getTargetMapId() + " scriptname: '" + portal.getScriptName() + "' state: " + (portal.getPortalState() ? 1 : 0) + ".");
        } else {
            player.dropMessage(6, "There is no portal on this map.");
        }
    }

    private static void debugMonster(MapleCharacter player) {
        player.getMap()
                .getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, List.of(MapleMapObjectType.MONSTER)).stream()
                .map(o -> (MapleMonster) o)
                .map(m -> "Monster ID: " + m.getId() + " Aggro target: " + ((m.getController().isPresent()) ? m.getController().get().getName() + " Has aggro: " + m.isControllerHasAggro() + " Knowns aggro: " + m.isControllerKnowsAboutAggro() : "<none>"))
                .forEach(player::message);
    }

    private static void debugHelp(MapleClient c) {
        String message = IntStream.range(0, debugTypes.length)
                .mapToObj(i -> String.format("#L%d#%s#l\r\n", i, debugTypes[i]))
                .reduce(new StringBuilder("Available #bdebug types#k:\r\n\r\n"),
                        StringBuilder::append,
                        StringBuilder::append)
                .toString();
        c.getAbstractPlayerInteraction().npcTalk(9201143, message);
    }
}
