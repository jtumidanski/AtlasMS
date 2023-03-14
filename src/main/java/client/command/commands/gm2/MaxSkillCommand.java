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

import client.*;
import client.command.Command;
import provider.MapleData;
import provider.MapleDataProviderFactory;

import java.io.File;

public class MaxSkillCommand extends Command {
    {
        setDescription("");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        MapleCharacter player = c.getPlayer();
        for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                SkillFactory.getSkill(Integer.parseInt(skill_.getName()))
                        .ifPresent(s -> player.changeSkillLevel(s, (byte) s.getMaxLevel(), s.getMaxLevel(), -1));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
                break;
            }
        }

        if (player.getJob().isA(MapleJob.ARAN1) || player.getJob().isA(MapleJob.LEGEND)) {
            SkillFactory.getSkill(5001005)
                    .ifPresent(s -> player.changeSkillLevel(s, (byte) -1, -1, -1));
        } else {
            SkillFactory.getSkill(21001001)
                    .ifPresent(s -> player.changeSkillLevel(s, (byte) -1, -1, -1));
        }

        player.yellowMessage("Skills maxed out.");
    }
}
