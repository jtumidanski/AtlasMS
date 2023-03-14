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
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class PartyOperationHandler extends AbstractMaplePacketHandler {

    private static void changePartyLeader(MapleClient client, int newLeader) {
        client.getPlayer().getParty().ifPresent(p -> changePartyLeader(client, p, newLeader));
    }

    private static void changePartyLeader(MapleClient client, MapleParty party, int characterId) {
        MaplePartyCharacter newLeader = party.getMemberById(characterId);
        client.getWorldServer().updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newLeader);
    }

    private static void expelFromParty(MapleClient client, int characterId) {
        client.getPlayer().getParty().ifPresent(p -> MapleParty.expelFromParty(p, client, characterId));
    }

    private static void inviteToParty(MapleClient client, String name) {
        MapleCharacter character = client.getPlayer();
        MapleCharacter invited = client.getWorldServer().getPlayerStorage().getCharacterByName(name).orElse(null);
        if (invited == null) {
            client.announce(MaplePacketCreator.partyStatusMessage(19));
            return;
        }

        if (invited.getLevel() < 10 && (!YamlConfig.config.server.USE_PARTY_FOR_STARTERS || character.getLevel() >= 10)) { //min requirement is level 10
            client.announce(MaplePacketCreator.serverNotice(5, "The player you have invited does not meet the requirements."));
            return;
        }
        if (YamlConfig.config.server.USE_PARTY_FOR_STARTERS && invited.getLevel() >= 10 && character.getLevel() < 10) {    //trying to invite high level
            client.announce(MaplePacketCreator.serverNotice(5, "The player you have invited does not meet the requirements."));
            return;
        }

        if (invited.getParty().isPresent()) {
            client.announce(MaplePacketCreator.partyStatusMessage(16));
            return;
        }

        Optional<MapleParty> party = character.getParty();
        if (party.isEmpty()) {
            if (!MapleParty.createParty(character, false)) {
                return;
            }

            party = character.getParty();
        }

        if (party.isEmpty()) {
            client.announce(MaplePacketCreator.partyStatusMessage(1));
            return;
        }

        if (party.get().getMembers().size() >= 6) {
            client.announce(MaplePacketCreator.partyStatusMessage(17));
            return;
        }

        if (MapleInviteCoordinator.createInvite(InviteType.PARTY, character, party.get().getId(), invited.getId())) {
            invited.getClient().announce(MaplePacketCreator.partyInvite(character));
        } else {
            client.announce(MaplePacketCreator.partyStatusMessage(22, invited.getName()));
        }
    }

    private static void joinParty(MapleClient client, int partyId) {
        MapleCharacter character = client.getPlayer();
        MapleInviteResult inviteRes = MapleInviteCoordinator.answerInvite(InviteType.PARTY, character.getId(), partyId, true);
        InviteResult res = inviteRes.result;
        if (res == InviteResult.ACCEPTED) {
            MapleParty.joinParty(character, partyId, false);
        } else {
            client.announce(MaplePacketCreator.serverNotice(5, "You couldn't join the party due to an expired invitation request."));
        }
    }

    private static void leaveOrDisbandParty(MapleClient client) {
        client.getPlayer().getParty().ifPresent(p -> leaveOrDisbandParty(client, p));
    }

    private static void leaveOrDisbandParty(MapleClient client, MapleParty party) {
        MapleCharacter character = client.getPlayer();
        MapleParty.leaveParty(party, client);
        character.updatePartySearchAvailability(true);
        character.partyOperationUpdate(party, character.getPartyMembersOnline());
    }

    private static void createParty(MapleClient client) {
        MapleParty.createParty(client.getPlayer(), false);
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        int operation = slea.readByte();
        switch (operation) {
            case 1 -> createParty(client);
            case 2 -> leaveOrDisbandParty(client);
            case 3 -> {
                int partyId = slea.readInt();
                joinParty(client, partyId);
            }
            case 4 -> {
                String name = slea.readMapleAsciiString();
                inviteToParty(client, name);
            }
            case 5 -> {
                int characterId = slea.readInt();
                expelFromParty(client, characterId);
            }
            case 6 -> {
                int newLeader = slea.readInt();
                changePartyLeader(client, newLeader);
            }
        }
    }
}