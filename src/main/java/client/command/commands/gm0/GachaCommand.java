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
package client.command.commands.gm0;

import client.MapleClient;
import client.command.Command;
import server.ItemInformationProvider;
import server.gachapon.MapleGachapon;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class GachaCommand extends Command {
    {
        setDescription("");
    }

    @Override
    public void execute(MapleClient c, String[] params) {
        String search = c.getPlayer().getLastCommandMessage();
        String[] names = {"Henesys", "Ellinia", "Perion", "Kerning City", "Sleepywood", "Mushroom Shrine", "Showa Spa Male", "Showa Spa Female", "New Leaf City", "Nautilus Harbor"};
        int[] ids = {9100100, 9100101, 9100102, 9100103, 9100104, 9100105, 9100106, 9100107, 9100109, 9100117};

        Optional<Integer> index = IntStream.range(0, names.length)
                .filter(i -> search.equalsIgnoreCase(names[i]))
                .boxed()
                .findFirst();
        Optional<MapleGachapon.Gachapon> gachapon = index.map(i -> ids[i]).flatMap(MapleGachapon.Gachapon::getByNpcId);
        Optional<String> gachaponName = index.map(i -> names[i]);

        if (gachapon.isEmpty()) {
            c.getPlayer().yellowMessage("Please use @gacha <name> where name corresponds to one of the below:");
            Arrays.stream(names).forEach(n -> c.getPlayer().yellowMessage(n));
            return;
        }

        String message = IntStream.range(0, 2)
                .flatMap(i -> Arrays.stream(gachapon.get().getItems(i)))
                .mapToObj(id -> ItemInformationProvider.getInstance().getName(id))
                .reduce(new StringBuilder(String.format("The #b%s#k Gachapon contains the following items.\\r\\n\\r\\n", gachaponName)),
                        (sb, name) -> sb.append("-").append(name).append("\r\n"),
                        StringBuilder::append)
                .append("\r\nPlease keep in mind that there are items that are in all gachapons and are not listed here.")
                .toString();
        c.getAbstractPlayerInteraction().npcTalk(9010000, message);
    }
}
