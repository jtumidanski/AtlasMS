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
package client.command.commands.gm2;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;

public class UnJailCommand extends Command {
    {
        setDescription("");
    }

    private static void unjail(MapleCharacter player, MapleCharacter target) {
        if (target.getJailExpirationTimeLeft() <= 0) {
            player.message("This player is already free.");
            return;
        }
        target.removeJailExpirationTime();
        target.message("By lack of concrete proof you are now unjailed. Enjoy freedom!");
        player.message(target.getName() + " was unjailed.");
    }

    private static void unjailError(MapleCharacter player, String targetName) {
        player.message("Player '" + targetName + "' could not be found.");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: !unjail <playername>");
            return;
        }
        String targetName = params[0];

        c.getWorldServer()
                .getPlayerStorage()
                .getCharacterByName(targetName)
                .ifPresentOrElse(t -> unjail(c.getPlayer(), t), () -> unjailError(c.getPlayer(), targetName));
    }
}
