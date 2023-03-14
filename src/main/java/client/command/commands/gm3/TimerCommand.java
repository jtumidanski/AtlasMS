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
   @Author: MedicOP - Add clock commands
*/
package client.command.commands.gm3;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;
import tools.MaplePacketCreator;

public class TimerCommand extends Command {
    {
        setDescription("");
    }

    private static void setTimer(MapleCharacter player, String time, MapleCharacter victim) {
        if (time.equalsIgnoreCase("remove")) {
            victim.announce(MaplePacketCreator.removeClock());
        } else {
            try {
                victim.announce(MaplePacketCreator.getClock(Integer.parseInt(time)));
            } catch (NumberFormatException e) {
                player.yellowMessage("Syntax: !timer <playername> <seconds>|remove");
            }
        }
    }

    private static void timerError(MapleCharacter player, String targetName) {
        player.message("Player '" + targetName + "' could not be found.");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !timer <playername> <seconds>|remove");
            return;
        }

        String targetName = params[0];
        String time = params[1];

        c.getWorldServer()
                .getPlayerStorage()
                .getCharacterByName(targetName)
                .ifPresentOrElse(t -> setTimer(player, time, t), () -> timerError(player, targetName));
    }
}
