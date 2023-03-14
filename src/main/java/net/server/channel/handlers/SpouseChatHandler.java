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
import config.YamlConfig;
import net.AbstractMaplePacketHandler;
import tools.LogHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpouseChatHandler extends AbstractMaplePacketHandler {
    private static void spouseChat(MapleClient c, String msg, MapleCharacter spouse) {
        spouse.announce(MaplePacketCreator.OnCoupleMessage(c.getPlayer().getName(), msg, true));
        c.announce(MaplePacketCreator.OnCoupleMessage(c.getPlayer().getName(), msg, true));
        if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
            LogHelper.logChat(c, "Spouse", msg);
        }
    }

    private static void spouseChatError(MapleClient c) {
        c.getPlayer().dropMessage(5, "Your spouse is currently offline.");
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readMapleAsciiString();//recipient
        String msg = slea.readMapleAsciiString();

        int partnerId = c.getPlayer().getPartnerId();
        if (partnerId <= 0) {
            c.getPlayer().dropMessage(5, "You don't have a spouse.");
            return;
        }

        c.getWorldServer()
                .getPlayerStorage()
                .getCharacterById(partnerId)
                .ifPresentOrElse(s -> spouseChat(c, msg, s), () -> spouseChatError(c));
    }
}
