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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class MapleMessenger {

    private final int id;
    private final List<MapleMessengerCharacter> members = new ArrayList<>(3);
    private final boolean[] pos = new boolean[3];

    public MapleMessenger(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Collection<MapleMessengerCharacter> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public Stream<MapleMessengerCharacter> getMembersStream() {
        return getMembers().stream();
    }

    public Stream<MapleMessengerCharacter> getOtherMembersStream(String name) {
        return getMembersStream().filter(m -> !m.name().equals(name));
    }

    public MapleMessengerCharacter addMember(int id, String name, int channelId) {
        return addMember(new MapleMessengerCharacter(id, name, channelId));
    }

    public MapleMessengerCharacter addMember(MapleMessengerCharacter member) {
        int position = getLowestPosition();
        MapleMessengerCharacter newMember = member.setPosition(position);
        members.add(newMember);
        pos[position] = true;
        return newMember;
    }

    public void removeMember(String name) {
        getMemberByName(name).ifPresent(this::removeMember);
    }

    private void removeMember(MapleMessengerCharacter member) {
        int position = member.position();
        pos[position] = false;
        members.remove(member);
    }

    public int getLowestPosition() {
        for (byte i = 0; i < 3; i++) {
            if (!pos[i]) {
                return i;
            }
        }
        return -1;
    }

    public Optional<MapleMessengerCharacter> getMemberByName(String name) {
        return members.stream()
                .filter(c -> c.name().equals(name))
                .findFirst();
    }

    public int getPositionByName(String name) {
        return members.stream()
                .filter(c -> c.name().equals(name))
                .map(MapleMessengerCharacter::position)
                .findFirst()
                .orElse(-1);
    }
}

