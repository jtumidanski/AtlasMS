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

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.CharacterIdNameBuddyCapacity;
import client.CharacterNameAndId;
import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static client.BuddyList.BuddyOperation.ADDED;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {
    private void nextPendingRequest(MapleClient c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.announce(CWvsContext.requestBuddylistAdd(pendingBuddyRequest.id(), c.getPlayer().getId(), pendingBuddyRequest.name()));
        }
    }

    private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        CharacterIdNameBuddyCapacity ret;
        try (PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                ret = null;
                if (rs.next()) {
                    ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
                }
            }
        }
        con.close();
        return ret;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int mode = slea.readByte();
        MapleCharacter player = c.getPlayer();
        BuddyList buddylist = player.getBuddylist();
        if (mode == 1) { // add
            String addName = slea.readMapleAsciiString();
            String group = slea.readMapleAsciiString();
            addBuddy(c, addName, group);
        } else if (mode == 2) { // accept buddy
            int otherCid = slea.readInt();
            if (!buddylist.isFull()) {
                try {
                    int channel = c.getWorldServer().find(otherCid);//worldInterface.find(otherCid);
                    String otherName = null;
                    Optional<MapleCharacter> otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
                    if (otherChar.isEmpty()) {
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                            ps.setInt(1, otherCid);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    otherName = rs.getString("name");
                                }
                            }
                        }
                        con.close();
                    } else {
                        otherName = otherChar.get().getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, "Default Group", otherCid, channel, true));
                        c.announce(CWvsContext.updateBuddylist(buddylist.getBuddies()));
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            nextPendingRequest(c);
        } else if (mode == 3) { // delete
            int otherCid = slea.readInt();
            player.deleteBuddy(otherCid);
        }
    }

    private void addBuddy(MapleClient c, String addName, String group) {
        if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
            return;
        }
        BuddyList buddyList = c.getPlayer().getBuddylist();
        Optional<BuddylistEntry> buddy = buddyList.get(addName);
        if (buddy.isPresent()) {
            if (!buddy.get().isVisible() && group.equals(buddy.get().getGroup())) {
                c.announce(CWvsContext.serverNotice(1, "You already have \"" + buddy.get().getName() + "\" on your Buddylist"));
                return;
            }


            buddy.get().changeGroup(group);
            c.announce(CWvsContext.updateBuddylist(buddyList.getBuddies()));
            return;
        }

        if (buddyList.isFull()) {
            c.announce(CWvsContext.serverNotice(1, "Your buddylist is already full"));
            return;
        }

        try {
            World world = c.getWorldServer();
            CharacterIdNameBuddyCapacity charWithId;
            int channel;
            Optional<MapleCharacter> otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
            if (otherChar.isPresent()) {
                channel = c.getChannel();
                charWithId = new CharacterIdNameBuddyCapacity(otherChar.get().getId(), otherChar.get().getName(), otherChar.get().getBuddylist().getCapacity());
            } else {
                channel = world.find(addName);
                charWithId = getCharacterIdAndNameFromDatabase(addName);
            }
            if (charWithId != null) {
                BuddyAddResult buddyAddResult = null;
                if (channel != -1) {
                    buddyAddResult = world.requestBuddyAdd(addName, c.getChannel(), c.getPlayer().getId(), c.getPlayer().getName());
                } else {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                    ps.setInt(1, charWithId.id());
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        throw new RuntimeException("Result set expected");
                    } else if (rs.getInt("buddyCount") >= charWithId.buddyCapacity()) {
                        buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                    }
                    rs.close();
                    ps.close();
                    ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                    ps.setInt(1, charWithId.id());
                    ps.setInt(2, c.getPlayer().getId());
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                    }
                    rs.close();
                    ps.close();
                    con.close();
                }
                if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                    c.announce(CWvsContext.serverNotice(1, "\"" + addName + "\"'s Buddylist is full"));
                } else {
                    int displayChannel;
                    displayChannel = -1;
                    int otherCid = charWithId.id();
                    if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                        displayChannel = channel;
                        notifyRemoteChannel(c, channel, otherCid, ADDED);
                    } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)")) {
                            ps.setInt(1, charWithId.id());
                            ps.setInt(2, c.getPlayer().getId());
                            ps.executeUpdate();
                        }
                        con.close();
                    }
                    buddyList.put(new BuddylistEntry(charWithId.name(), group, otherCid, displayChannel, true));
                    c.announce(CWvsContext.updateBuddylist(buddyList.getBuddies()));
                }
            } else {
                c.announce(CWvsContext.serverNotice(1, "A character called \"" + addName + "\" does not exist"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void notifyRemoteChannel(MapleClient c, int remoteChannel, int otherCid, BuddyOperation operation) {
        MapleCharacter player = c.getPlayer();
        if (remoteChannel != -1) {
            c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }
}
