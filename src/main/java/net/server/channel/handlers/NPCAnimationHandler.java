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
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;


public final class NPCAnimationHandler extends AbstractMaplePacketHandler {
    private static void processItem(SeekableLittleEndianAccessor slea, MaplePacketLittleEndianWriter mplew) {
        byte op = slea.readByte();
        mplew.write(op);

        short tx = -1;
        short ty = -1;
        short vx = -1;
        short fh = -1;
        short vy = -1;
        short fhFallStart = -1;
        short xOffset = -1;
        short yOffset = -1;
        switch (op) {
            case 0:
            case 5:
            case 15:
            case 17:
                tx = slea.readShort();
                ty = slea.readShort();
                vx = slea.readShort();
                vy = slea.readShort();
                fh = slea.readShort();
                mplew.writeShort(tx);
                mplew.writeShort(ty);
                mplew.writeShort(vx);
                mplew.writeShort(vy);
                mplew.writeShort(fh);
                if (op == 15) {
                    fhFallStart = slea.readShort();
                    mplew.writeShort(fhFallStart);
                }
                xOffset = slea.readShort();
//                mplew.writeShort(xOffset);
                yOffset = slea.readShort();
//                mplew.writeShort(yOffset);
                break;
            case 1:
            case 2:
            case 6:
            case 12:
            case 13:
            case 16:
            case 18:
            case 19:
            case 20:
            case 22:
            case 24:
                vx = slea.readShort();
                vy = slea.readShort();
                mplew.writeShort(vx);
                mplew.writeShort(vy);
                break;
            case 3:
            case 4:
            case 7:
            case 8:
            case 9:
            case 11:
                tx = slea.readShort();
                ty = slea.readShort();
                fh = slea.readShort();
                mplew.writeShort(tx);
                mplew.writeShort(ty);
                mplew.writeShort(fh);
                break;
            case 10:
                byte bStat = slea.readByte();
                mplew.write(bStat);
                return;
            case 14:
                vx = slea.readShort();
                vy = slea.readShort();
                fhFallStart = slea.readShort();
                mplew.writeShort(vx);
                mplew.writeShort(vy);
                mplew.writeShort(fhFallStart);
                break;
            case 23:
                tx = slea.readShort();
                ty = slea.readShort();
                vx = slea.readShort();
                vy = slea.readShort();
                mplew.writeShort(tx);
                mplew.writeShort(ty);
                mplew.writeShort(vx);
                mplew.writeShort(vy);
                break;
            default:
        }

        byte moveAction = slea.readByte();
        short tElapse = slea.readShort();
//        short usRandCnt = slea.readShort();
//        short usActualRandCnt = slea.readShort();
        mplew.write(moveAction);
        mplew.writeShort(tElapse);

        System.out.printf("NPCAnimationHandler::handlePacket op=[%d] tx=[%d], ty=[%d], vx=[%d], vy=[%d], fh=[%d], fhFallStart=[%d] xOffset=[%d], yOffset=[%d], moveAction=[%d], tElapse=[%d]\r\n", op, tx, ty, vx, vy, fh, fhFallStart, xOffset, yOffset, moveAction, tElapse);
//        mplew.writeShort(usRandCnt);
//        mplew.writeShort(usActualRandCnt);
    }

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
        short x = slea.readShort();
        short y = slea.readShort();
        byte items = slea.readByte();

        //byte[] bytes = slea.read((int) (slea.available() - 9));
        mplew.writeShort(x);
        mplew.writeShort(y);
        mplew.write(items);

        System.out.printf("NPCAnimationHandler::handlePacket NPCOID=[%d], nAction=[%d], nChatIdx=[%d], x=[%d], y=[%d], items=[%d]\r\n", npcId, nAction, nChatIdx, x, y, items);

        for (byte i = 0; i < items; i++) {
            processItem(slea, mplew);
        }

        //mplew.write(bytes);
        c.announce(mplew.getPacket());
    }
}
