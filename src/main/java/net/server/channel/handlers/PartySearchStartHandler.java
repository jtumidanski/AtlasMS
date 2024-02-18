/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author XoticStory
 * @author BubblesDev
 * @author Ronan
 */
public class PartySearchStartHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        int min = slea.readInt();
        int max = slea.readInt();

        if (min > max) {
            client.getPlayer().dropMessage(1, "The min. value is higher than the max!");
            client.announce(CWvsContext.enableActions());
            return;
        }

        if (max - min > 30) {
            client.getPlayer().dropMessage(1, "You can only search for party members within a range of 30 levels.");
            client.announce(CWvsContext.enableActions());
            return;
        }

        if (client.getPlayer().getLevel() < min || client.getPlayer().getLevel() > max) {
            client.getPlayer().dropMessage(1, "The range of level for search has to include your own level.");
            client.announce(CWvsContext.enableActions());
            return;
        }

        slea.readInt(); // members
        int jobs = slea.readInt();

        client.getPlayer().getParty()
                .filter(p -> client.getPlayer().isPartyLeader())
                .ifPresent(p -> client.getWorldServer().getPartySearchCoordinator().registerPartyLeader(client.getPlayer(), min, max, jobs));
    }
}