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
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public final class NPCMoreTalkHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte lastMsg = slea.readByte();
        byte action = slea.readByte(); // 00 = end chat, 01 == follow
        int selection = -1;

        if (lastMsg == 0) {
            // CScriptMan::OnSay
        } else if (lastMsg == 1) {
            // CScriptMan::OnSayImage
        } else if (lastMsg == 2 || lastMsg == 13) {
            // CScriptMan::OnAskYesNo
        } else if (lastMsg == 3 || lastMsg == 14) {
            // CScriptMan::OnAskText
            // CScriptMan::OnAskBoxText
            if (action != 1) {
                if (c.getQM() != null) {
                    c.getQM()
                            .dispose();
                    return;
                }
                c.getCM()
                        .dispose();
                return;
            }

            // Potential decode string
            String text = slea.readMapleAsciiString();
            if (c.getQM() == null) {
                c.getCM()
                        .setGetText(text);
                NPCScriptManager.getInstance()
                        .action(c, action, lastMsg, -1);
                return;
            }

            c.getQM()
                    .setGetText(text);
            if (c.getQM()
                    .isStart()) {
                QuestScriptManager.getInstance()
                        .start(c, action, lastMsg, -1);
                return;
            }

            QuestScriptManager.getInstance()
                    .end(c, action, lastMsg, -1);
            return;
        } else if (lastMsg == 4 || lastMsg == 5 || lastMsg == 15) {
            // CScriptMan::OnAskNumber
            // CScriptMan::OnAskMenu
            // CScriptMan::OnAskSlideMenu
            if (action == 1) {
                selection = slea.readInt();
            }
        } else if (lastMsg == 8) {
            // CScriptMan::OnAskAvatar
        } else if (lastMsg == 9) {
            // CScriptMan::OnAskMembershopAvatar
        } else if (lastMsg == 10 || lastMsg == 11) {
            // CScriptMan::OnAskPet
            // CScriptMan::OnAskPetAll
            // crazy
            return;
        }

        if (c.getQM() != null) {
            if (c.getQM()
                    .isStart()) {
                QuestScriptManager.getInstance()
                        .start(c, action, lastMsg, selection);
                return;
            }

            QuestScriptManager.getInstance()
                    .end(c, action, lastMsg, selection);
            return;
        }

        if (c.getCM() != null) {
            NPCScriptManager.getInstance()
                    .action(c, action, lastMsg, selection);
            return;
        }
    }
}