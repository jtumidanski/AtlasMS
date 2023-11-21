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
package net.server.world;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import net.server.audit.LockCollector;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.MonitoredReentrantLock;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.coordinator.matchchecker.MapleMatchCheckerCoordinator;
import net.server.coordinator.matchchecker.MatchCheckerListenerFactory.MatchCheckerType;
import scripting.event.EventInstanceManager;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.partyquest.MonsterCarnival;
import tools.MaplePacketCreator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class MapleParty {

    private int id;
    private MapleParty enemy = null;
    private int leaderId;
    private List<MaplePartyCharacter> members = new LinkedList<>();
    private List<MaplePartyCharacter> pqMembers = null;

    private Map<Integer, Integer> histMembers = new HashMap<>();
    private int nextEntry = 0;

    private Map<Integer, MapleDoor> doors = new HashMap<>();

    private MonitoredReentrantLock lock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.PARTY, true);

    public MapleParty(int id, MaplePartyCharacter chrfor) {
        this.leaderId = chrfor.getId();
        this.id = id;
    }

    public static boolean createParty(MapleCharacter player, boolean silentCheck) {
        if (player.getParty().isPresent()) {
            if (!silentCheck) {
                player.announce(MaplePacketCreator.partyStatusMessage(16));
            }
            return false;
        }

        if (player.getLevel() < 10 && !YamlConfig.config.server.USE_PARTY_FOR_STARTERS) {
            player.announce(MaplePacketCreator.partyStatusMessage(10));
            return false;
        }

        if (player.getAriantColiseum() != null) {
            player.dropMessage(5, "You cannot request a party creation while participating the Ariant Battle Arena.");
            return false;
        }

        MaplePartyCharacter partyplayer = new MaplePartyCharacter(player);
        MapleParty party = player.getWorldServer().createParty(partyplayer);
        player.setParty(party);
        player.setMPC(partyplayer);
        player.getMap().addPartyMember(player, party.getId());
        player.silentPartyUpdate();

        player.updatePartySearchAvailability(false);
        player.partyOperationUpdate(party, null);

        player.announce(MaplePacketCreator.partyCreated(party, partyplayer.getId()));
        return true;
    }

    public static boolean joinParty(MapleCharacter player, int partyId, boolean silentCheck) {
        World world = player.getWorldServer();
        if (player.getParty().isPresent()) {
            if (!silentCheck) {
                player.announce(MaplePacketCreator.serverNotice(5, "You can't join the party as you are already in one."));
            }
            return false;
        }

        if (world.getParty(partyId).isEmpty()) {
            player.announce(MaplePacketCreator.serverNotice(5, "You couldn't join the party since it had already been disbanded."));
            return false;
        }

        MapleParty party = world.getParty(partyId).get();
        if (party.getMembers().size() >= 6) {
            if (!silentCheck) {
                player.announce(MaplePacketCreator.partyStatusMessage(17));
            }
            return false;
        }

        player.getMap().addPartyMember(player, party.getId());

        world.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(player));
        player.receivePartyMemberHP();
        player.updatePartyMemberHP();

        player.resetPartySearchInvite(party.getLeaderId());
        player.updatePartySearchAvailability(false);
        player.partyOperationUpdate(party, null);
        return true;
    }

    public static void leaveParty(MapleParty party, MapleClient c) {
        World world = c.getWorldServer();
        MapleCharacter player = c.getPlayer();
        MaplePartyCharacter partyplayer = player.getMPC();

        if (party != null && partyplayer != null) {
            if (partyplayer.getId() == party.getLeaderId()) {
                c.getWorldServer().removeMapPartyMembers(party.getId());

                MonsterCarnival mcpq = player.getMonsterCarnival();
                if (mcpq != null) {
                    mcpq.leftParty(player.getId());
                }

                world.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);

                player.getEventInstance().ifPresent(EventInstanceManager::disbandParty);
            } else {
                MapleMap map = player.getMap();
                if (map != null) {
                    map.removePartyMember(player, party.getId());
                }

                MonsterCarnival mcpq = player.getMonsterCarnival();
                if (mcpq != null) {
                    mcpq.leftParty(player.getId());
                }

                world.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);

                player.getEventInstance().ifPresent(ei -> ei.leftParty(player));
            }

            player.setParty(null);

            MapleMatchCheckerCoordinator mmce = c.getWorldServer().getMatchCheckerCoordinator();
            if (mmce.getMatchConfirmationLeaderid(player.getId()) == player.getId() && mmce.getMatchConfirmationType(player.getId()) == MatchCheckerType.GUILD_CREATION) {
                mmce.dismissMatchConfirmation(player.getId());
            }
        }
    }

    public static void expelFromParty(MapleParty party, MapleClient c, int expelCid) {
        World world = c.getWorldServer();
        MapleCharacter player = c.getPlayer();
        MaplePartyCharacter partyplayer = player.getMPC();

        if (party != null && partyplayer != null) {
            if (partyplayer.equals(party.getLeader())) {
                Optional<MaplePartyCharacter> expelled = party.getMemberById(expelCid);
                if (expelled.isPresent()) {

                    if (expelled.get().getPlayer().isEmpty()) {
                        world.updateParty(party.getId(), PartyOperation.EXPEL, expelled.get());
                        return;
                    }

                    MapleCharacter emc = expelled.get().getPlayer().get();
                    List<MapleCharacter> partyMembers = emc.getPartyMembersOnline();

                    MapleMap map = emc.getMap();
                    if (map != null) {
                        map.removePartyMember(emc, party.getId());
                    }

                    MonsterCarnival mcpq = player.getMonsterCarnival();
                    if (mcpq != null) {
                        mcpq.leftParty(emc.getId());
                    }

                    emc.getEventInstance().ifPresent(ei -> ei.leftParty(emc));
                    emc.setParty(null);
                    world.updateParty(party.getId(), PartyOperation.EXPEL, expelled.get());

                    emc.updatePartySearchAvailability(true);
                    emc.partyOperationUpdate(party, partyMembers);
                }
            }
        }
    }

    public boolean containsMembers(MaplePartyCharacter member) {
        lock.lock();
        try {
            return members.contains(member);
        } finally {
            lock.unlock();
        }
    }

    public void addMember(MaplePartyCharacter member) {
        lock.lock();
        try {
            histMembers.put(member.getId(), nextEntry);
            nextEntry++;

            members.add(member);
        } finally {
            lock.unlock();
        }
    }

    public void removeMember(MaplePartyCharacter member) {
        lock.lock();
        try {
            histMembers.remove(member.getId());

            members.remove(member);
        } finally {
            lock.unlock();
        }
    }

    public void updateMember(MaplePartyCharacter member) {
        lock.lock();
        try {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getId() == member.getId()) {
                    members.set(i, member);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<MaplePartyCharacter> getMemberById(int id) {
        lock.lock();
        try {
            return members.stream().filter(c -> c.getId() == id).findFirst();
        } finally {
            lock.unlock();
        }
    }

    public Collection<MaplePartyCharacter> getMembers() {
        lock.lock();
        try {
            return new LinkedList<>(members);
        } finally {
            lock.unlock();
        }
    }

    public List<MaplePartyCharacter> getPartyMembers() {
        lock.lock();
        try {
            return new LinkedList<>(members);
        } finally {
            lock.unlock();
        }
    }

    public List<MaplePartyCharacter> getPartyMembersOnline() {
        lock.lock();
        try {
            List<MaplePartyCharacter> ret = new LinkedList<>();

            for (MaplePartyCharacter mpc : members) {
                if (mpc.isOnline()) {
                    ret.add(mpc);
                }
            }

            return ret;
        } finally {
            lock.unlock();
        }
    }

    // used whenever entering PQs: will draw every party member that can attempt a target PQ while ingnoring those unfit.
    public Collection<MaplePartyCharacter> getEligibleMembers() {
        return Collections.unmodifiableList(pqMembers);
    }

    public void setEligibleMembers(List<MaplePartyCharacter> eliParty) {
        pqMembers = eliParty;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public MaplePartyCharacter getLeader() {
        lock.lock();
        try {
            for (MaplePartyCharacter mpc : members) {
                if (mpc.getId() == leaderId) {
                    return mpc;
                }
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    public void setLeader(MaplePartyCharacter victim) {
        this.leaderId = victim.getId();
    }

    public MapleParty getEnemy() {
        return enemy;
    }

    public void setEnemy(MapleParty enemy) {
        this.enemy = enemy;
    }

    public List<Integer> getMembersSortedByHistory() {
        List<Entry<Integer, Integer>> histList;

        lock.lock();
        try {
            histList = new LinkedList<>(histMembers.entrySet());
        } finally {
            lock.unlock();
        }

        histList.sort(Entry.comparingByValue());

        List<Integer> histSort = new LinkedList<>();
        for (Entry<Integer, Integer> e : histList) {
            histSort.add(e.getKey());
        }

        return histSort;
    }

    public byte getPartyDoor(int cid) {
        List<Integer> histList = getMembersSortedByHistory();
        byte slot = 0;
        for (Integer e : histList) {
            if (e == cid) {
                break;
            }
            slot++;
        }

        return slot;
    }

    public void addDoor(Integer owner, MapleDoor door) {
        lock.lock();
        try {
            this.doors.put(owner, door);
        } finally {
            lock.unlock();
        }
    }

    public void removeDoor(Integer owner) {
        lock.lock();
        try {
            this.doors.remove(owner);
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, MapleDoor> getDoors() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(doors);
        } finally {
            lock.unlock();
        }
    }

    public void assignNewLeader(MapleClient c) {
        World world = c.getWorldServer();
        MaplePartyCharacter newLeadr = null;

        lock.lock();
        try {
            for (MaplePartyCharacter mpc : members) {
                if (mpc.getId() != leaderId && (newLeadr == null || newLeadr.getLevel() < mpc.getLevel())) {
                    newLeadr = mpc;
                }
            }
        } finally {
            lock.unlock();
        }

        if (newLeadr != null) {
            world.updateParty(this.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
        }
    }

    public void disposeLocks() {
        LockCollector.getInstance().registerDisposeAction(this::emptyLocks);
    }

    private void emptyLocks() {
        lock = lock.dispose();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    public MaplePartyCharacter getMemberByPos(int pos) {
        int i = 0;
        for (MaplePartyCharacter chr : members) {
            if (pos == i) {
                return chr;
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MapleParty other = (MapleParty) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
}
