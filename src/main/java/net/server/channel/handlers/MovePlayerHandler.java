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
import server.movement.Elem;
import server.movement.MovePath;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // dr0
        slea.readInt(); //dr1
        slea.readByte(); //field key
        slea.readInt(); //dr2
        slea.readInt(); //dr3
        slea.readInt(); //crc
        slea.readInt(); //dwKey
        slea.readInt(); //crc32

        final MovePath res = new MovePath();
        res.decode(slea);

        res.Movement()
                .stream()
                .filter(m -> m.getType() == 0)
                .map(m -> m.getPosition((short) 0))
                .forEach(p -> c.getPlayer()
                        .setPosition(p));
        res.Movement()
                .stream()
                .map(Elem::getBMoveAction)
                .forEach(ma -> c.getPlayer()
                        .setStance(ma));

        c.getPlayer()
                .getMap()
                .movePlayer(c.getPlayer(), c.getPlayer()
                        .getPosition());
        if (c.getPlayer()
                .isHidden()) {
            c.getPlayer()
                    .getMap()
                    .broadcastGMMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer()
                            .getId(), res), false);
        } else {
            c.getPlayer()
                    .getMap()
                    .broadcastMessage(c.getPlayer(), MaplePacketCreator.movePlayer(c.getPlayer()
                            .getId(), res), false);
        }
    }
}
