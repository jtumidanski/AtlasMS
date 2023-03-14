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
package client.command.commands.gm3;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import net.server.Server;
import tools.MapleLogger;
import tools.MaplePacketCreator;

public class MonitorCommand extends Command {
    {
        setDescription("");
    }

    private static void toggleMonitor(MapleCharacter player, MapleCharacter target) {
        boolean monitored = MapleLogger.monitored.contains(target.getId());
        if (monitored) {
            MapleLogger.monitored.remove(target.getId());
        } else {
            MapleLogger.monitored.add(target.getId());
        }
        player.yellowMessage(target.getId() + " is " + (!monitored ? "now being monitored." : "no longer being monitored."));
        String message = player.getName() + (!monitored ? " has started monitoring " : " has stopped monitoring ") + target.getId() + ".";
        Server.getInstance().broadcastGMMessage(player.getWorld(), MaplePacketCreator.serverNotice(5, message));
    }

    private static void monitorCommandError(MapleCharacter player, String targetName) {
        player.message("Player '" + targetName + "' could not be found on this world.");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !monitor <ign>");
            return;
        }

        String targetName = params[0];

        c.getWorldServer()
                .getPlayerStorage()
                .getCharacterByName(targetName)
                .ifPresentOrElse(t -> toggleMonitor(player, t), () -> monitorCommandError(player, targetName));
    }
}
