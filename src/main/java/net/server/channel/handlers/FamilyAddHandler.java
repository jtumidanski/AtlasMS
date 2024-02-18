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
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class FamilyAddHandler extends AbstractMaplePacketHandler {
    private static void handlePacket(MapleClient c, MapleCharacter addChr) {
        MapleCharacter chr = c.getPlayer();
        if (addChr == chr) {
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (addChr.getMap() != chr.getMap() || (addChr.isHidden()) && chr.gmLevel() < addChr.gmLevel()) {
            c.announce(CWvsContext.sendFamilyMessage(69, 0));
            return;
        }

        if (addChr.getLevel() <= 10) {
            c.announce(CWvsContext.sendFamilyMessage(77, 0));
            return;
        }

        if (Math.abs(addChr.getLevel() - chr.getLevel()) > 20) {
            c.announce(CWvsContext.sendFamilyMessage(72, 0));
            return;
        }

        if (addChr.getFamily().isPresent() && addChr.getFamily() == chr.getFamily()) {
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (MapleInviteCoordinator.hasInvite(InviteType.FAMILY, addChr.getId())) {
            c.announce(CWvsContext.sendFamilyMessage(73, 0));
            return;
        }

        if (chr.getFamily().isPresent() && addChr.getFamily().isPresent() && addChr.getFamily().get().getTotalGenerations() + chr.getFamily().get().getTotalGenerations() > YamlConfig.config.server.FAMILY_MAX_GENERATIONS) {
            c.announce(CWvsContext.sendFamilyMessage(76, 0));
            return;
        }

        MapleInviteCoordinator.createInvite(InviteType.FAMILY, chr, addChr, addChr.getId());
        addChr.announce(CWvsContext.sendFamilyInvite(chr.getId(), chr.getName()));
        chr.dropMessage("The invite has been sent.");
        c.announce(CWvsContext.enableActions());
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!YamlConfig.config.server.USE_FAMILY_SYSTEM) {
            return;
        }
        String toAdd = slea.readMapleAsciiString();
        Optional<MapleCharacter> addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(toAdd);
        if (addChr.isEmpty()) {
            c.announce(CWvsContext.sendFamilyMessage(65, 0));
            return;
        }

        handlePacket(c, addChr.get());
    }
}
