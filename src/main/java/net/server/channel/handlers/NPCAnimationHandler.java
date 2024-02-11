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
import net.AbstractMaplePacketHandler;
import net.opcodes.SendOpcode;
import server.movement.MovePath;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;


public final class NPCAnimationHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer()
                .isChangingMaps()) {
            return;
        }

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int length = (int) slea.available();
        int npcId = slea.readInt();
        byte nAction = slea.readByte();
        byte nChatIdx = slea.readByte();

        mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
        mplew.writeInt(npcId);
        mplew.write(nAction);
        mplew.write(nChatIdx);
        if (length == 6) {
            // NPC Talk
            c.announce(mplew.getPacket());
            return;
        }

        // NPC Move
        final MovePath res = new MovePath();
        res.decode(slea);
        res.encode(mplew);
        c.announce(mplew.getPacket());
    }
}
