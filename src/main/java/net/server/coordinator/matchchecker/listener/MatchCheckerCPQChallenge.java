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
import constants.string.LanguageConstants;
import net.server.coordinator.matchchecker.AbstractMatchCheckerListener;
import net.server.coordinator.matchchecker.MatchCheckerListenerRecipe;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ronan
 */
public class MatchCheckerCPQChallenge implements MatchCheckerListenerRecipe {

    public static AbstractMatchCheckerListener loadListener() {
        return (new MatchCheckerCPQChallenge()).getListener();
    }

    private static Optional<MapleCharacter> getChallenger(int leaderid, Set<MapleCharacter> matchPlayers) {
        return matchPlayers.stream()
                .filter(c -> c.getId() == leaderid)
                .filter(c -> c.getClient() != null)
                .findFirst();
    }

    @Override
    public AbstractMatchCheckerListener getListener() {
        return new AbstractMatchCheckerListener() {

            @Override
            public void onMatchCreated(MapleCharacter leader, Set<MapleCharacter> nonLeaderMatchPlayers, String message) {
                NPCConversationManager cm = leader.getClient().getCM();
                int npcid = cm.getNpc();

                Optional<MapleCharacter> ldr = nonLeaderMatchPlayers.stream().findFirst();
                List<MaplePartyCharacter> chrMembers = leader.getParty()
                        .map(MapleParty::getMembers)
                        .orElse(Collections.emptyList()).stream()
                        .filter(MaplePartyCharacter::isOnline)
                        .collect(Collectors.toList());

                if (message.contentEquals("cpq1")) {
                    ldr.ifPresent(c -> NPCScriptManager.getInstance().start("cpqchallenge", c.getClient(), npcid, chrMembers));
                } else {
                    ldr.ifPresent(c -> NPCScriptManager.getInstance().start("cpqchallenge2", c.getClient(), npcid, chrMembers));
                }

                cm.sendOk(LanguageConstants.getMessage(leader, LanguageConstants.CPQChallengeRoomSent));
            }

            @Override
            public void onMatchAccepted(int leaderId, Set<MapleCharacter> matchPlayers, String message) {
                Optional<MapleCharacter> challenger = getChallenger(leaderId, matchPlayers);
                Optional<MapleCharacter> ldr = matchPlayers.stream().findFirst();
                if (message.contentEquals("cpq1")) {
                    challenger.ifPresent(c -> ldr.ifPresent(l -> l.getClient().getCM().startCPQ(c, l.getMapId() + 1)));
                } else {
                    challenger.ifPresent(c -> ldr.ifPresent(l -> l.getClient().getCM().startCPQ2(c, l.getMapId() + 1)));
                }

                ldr.flatMap(MapleCharacter::getParty).ifPresent(p -> challenger.flatMap(MapleCharacter::getParty).ifPresent(p::setEnemy));
                challenger.flatMap(MapleCharacter::getParty).ifPresent(p -> ldr.flatMap(MapleCharacter::getParty).ifPresent(p::setEnemy));
                challenger.ifPresent(c -> c.setChallenged(false));
            }

            @Override
            public void onMatchDeclined(int leaderId, Set<MapleCharacter> matchPlayers, String message) {
                Optional<MapleCharacter> challenger = getChallenger(leaderId, matchPlayers);
                challenger.ifPresent(c -> c.dropMessage(5, LanguageConstants.getMessage(c, LanguageConstants.CPQChallengeRoomDenied)));
            }

            @Override
            public void onMatchDismissed(int leaderId, Set<MapleCharacter> matchPlayers, String message) {
            }
        };
    }
}
