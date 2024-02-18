/*
    This file is part of the HeavenMS MapleStory Server
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
package net.server.coordinator.matchchecker.listener;

import client.MapleCharacter;
import config.YamlConfig;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.server.Server;
import net.server.coordinator.matchchecker.AbstractMatchCheckerListener;
import net.server.coordinator.matchchecker.MatchCheckerListenerRecipe;
import net.server.guild.MapleGuild;
import net.server.world.MapleParty;

import java.util.Optional;
import java.util.Set;

/**
 * @author Ronan
 */
public class MatchCheckerGuildCreation implements MatchCheckerListenerRecipe {

    private static void broadcastGuildCreationDismiss(Set<MapleCharacter> nonLeaderMatchPlayers) {
        for (MapleCharacter chr : nonLeaderMatchPlayers) {
            if (chr.isLoggedinWorld()) {
                chr.announce(CWvsContext.genericGuildMessage((byte) 0x26));
            }
        }
    }

    public static AbstractMatchCheckerListener loadListener() {
        return (new MatchCheckerGuildCreation()).getListener();
    }

    @Override
    public AbstractMatchCheckerListener getListener() {
        return new AbstractMatchCheckerListener() {

            @Override
            public void onMatchCreated(MapleCharacter leader, Set<MapleCharacter> nonLeaderMatchPlayers, String message) {
                byte[] createGuildPacket = CWvsContext.createGuildMessage(leader.getName(), message);

                for (MapleCharacter chr : nonLeaderMatchPlayers) {
                    if (chr.isLoggedinWorld()) {
                        chr.announce(createGuildPacket);
                    }
                }
            }

            @Override
            public void onMatchAccepted(int leaderid, Set<MapleCharacter> matchPlayers, String message) {
                MapleCharacter leader = null;
                for (MapleCharacter chr : matchPlayers) {
                    if (chr.getId() == leaderid) {
                        leader = chr;
                        break;
                    }
                }

                if (leader == null || !leader.isLoggedinWorld()) {
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                matchPlayers.remove(leader);

                if (leader.getGuildId() > 0) {
                    leader.dropMessage(1, "You cannot create a new Guild while in one.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                Optional<Integer> partyId = leader.getPartyId();
                if (partyId.isEmpty() || !leader.isPartyLeader()) {
                    leader.dropMessage(1, "You cannot establish the creation of a new Guild without leading a party.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                if (leader.getMapId() != 200000301) {
                    leader.dropMessage(1, "You cannot establish the creation of a new Guild outside of the Guild Headquarters.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                for (MapleCharacter chr : matchPlayers) {
                    if (leader.getMap().getCharacterById(chr.getId()).isEmpty()) {
                        leader.dropMessage(1, "You cannot establish the creation of a new Guild if one of the members is not present here.");
                        broadcastGuildCreationDismiss(matchPlayers);
                        return;
                    }
                }
                if (leader.getMeso() < YamlConfig.config.server.CREATE_GUILD_COST) {
                    leader.dropMessage(1, "You do not have " + GameConstants.numberWithCommas(YamlConfig.config.server.CREATE_GUILD_COST) + " mesos to create a Guild.");
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }

                int gid = Server.getInstance().createGuild(leader.getId(), message);
                if (gid == 0) {
                    leader.announce(CWvsContext.genericGuildMessage((byte) 0x23));
                    broadcastGuildCreationDismiss(matchPlayers);
                    return;
                }
                leader.gainMeso(-YamlConfig.config.server.CREATE_GUILD_COST, true, false, true);

                leader.getMGC().ifPresent(mgc -> mgc.setGuildId(gid));
                Optional<MapleGuild> guild = Server.getInstance().getGuild(leader.getGuildId(), leader.getWorld(), leader);  // initialize guild structure
                Server.getInstance().changeRank(gid, leader.getId(), 1);

                leader.announce(CWvsContext.showGuildInfo(leader));
                leader.dropMessage(1, "You have successfully created a Guild.");

                for (MapleCharacter chr : matchPlayers) {
                    boolean cofounder = chr.getPartyId().orElse(-1).equals(partyId.get());
                    chr.getMGC().ifPresent(mgc -> {
                        mgc.setGuildId(gid);
                        mgc.setGuildRank(cofounder ? 2 : 5);
                        mgc.setAllianceRank(5);

                        Server.getInstance().addGuildMember(mgc, chr);

                        if (chr.isLoggedinWorld()) {
                            chr.announce(CWvsContext.showGuildInfo(chr));

                            if (cofounder) {
                                chr.dropMessage(1, "You have successfully cofounded a Guild.");
                            } else {
                                chr.dropMessage(1, "You have successfully joined the new Guild.");
                            }
                        }

                        chr.saveGuildStatus(); // update database
                    });
                }

                guild.ifPresent(MapleGuild::broadcastNameChanged);
                guild.ifPresent(MapleGuild::broadcastEmblemChanged);
            }

            @Override
            public void onMatchDeclined(int leaderid, Set<MapleCharacter> matchPlayers, String message) {
                for (MapleCharacter chr : matchPlayers) {
                    if (chr.getId() == leaderid && chr.getClient() != null) {
                        chr.getParty().ifPresent(p -> MapleParty.leaveParty(p, chr.getClient()));
                    }

                    if (chr.isLoggedinWorld()) {
                        chr.announce(CWvsContext.genericGuildMessage((byte) 0x26));
                    }
                }
            }

            @Override
            public void onMatchDismissed(int leaderId, Set<MapleCharacter> matchPlayers, String message) {

                Optional<MapleCharacter> leader = matchPlayers.stream()
                        .filter(c -> c.getId() == leaderId)
                        .findFirst();

                String msg;
                if (leader.isPresent() && leader.flatMap(MapleCharacter::getParty).isEmpty()) {
                    msg = "The Guild creation has been dismissed since the leader left the founding party.";
                } else {
                    msg = "The Guild creation has been dismissed since a member was already in a party when they answered.";
                }

                for (MapleCharacter chr : matchPlayers) {
                    if (chr.getId() == leaderId && chr.getClient() != null) {
                        chr.getParty().ifPresent(p -> MapleParty.leaveParty(p, chr.getClient()));
                    }

                    if (chr.isLoggedinWorld()) {
                        chr.message(msg);
                        chr.announce(CWvsContext.genericGuildMessage((byte) 0x26));
                    }
                }
            }
        };
    }
}
