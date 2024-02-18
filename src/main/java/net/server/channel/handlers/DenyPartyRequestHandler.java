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

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DenyPartyRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        String[] characterName = slea.readMapleAsciiString().split("PS: ");

        c.getChannelServer().getPlayerStorage().getCharacterByName(characterName[characterName.length - 1])
                .ifPresent(characterFrom -> denyPartyRequest(c.getPlayer(), characterFrom));
    }

    private static void denyPartyRequest(MapleCharacter character, MapleCharacter characterFrom) {
        if (MapleInviteCoordinator.answerInvite(InviteType.PARTY, character.getId(), characterFrom.getPartyId().orElse(-1), false).result == InviteResult.DENIED) {
            character.updatePartySearchAvailability(!character.hasParty());
            characterFrom.announce(CWvsContext.partyStatusMessage(23, character.getName()));
        }
    }
}
