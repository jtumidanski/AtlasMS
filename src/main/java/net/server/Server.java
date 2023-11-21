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
package net.server;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleFamily;
import client.SkillFactory;
import client.command.CommandsExecutor;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.manipulator.MapleCashidGenerator;
import client.newyear.NewYearCardRecord;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import constants.net.OpcodeConstants;
import constants.net.ServerConstants;
import net.MapleServerHandler;
import net.mina.MapleCodecFactory;
import net.server.audit.ThreadTracker;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.MonitoredReadLock;
import net.server.audit.locks.MonitoredReentrantReadWriteLock;
import net.server.audit.locks.MonitoredWriteLock;
import net.server.audit.locks.factory.MonitoredReadLockFactory;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.audit.locks.factory.MonitoredWriteLockFactory;
import net.server.channel.Channel;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.task.BossLogTask;
import net.server.task.CharacterDiseaseTask;
import net.server.task.CouponTask;
import net.server.task.DueyFredrickTask;
import net.server.task.EventRecallCoordinatorTask;
import net.server.task.InvitationTask;
import net.server.task.LoginCoordinatorTask;
import net.server.task.LoginStorageTask;
import net.server.task.RankingCommandTask;
import net.server.task.RankingLoginTask;
import net.server.task.ReleaseLockTask;
import net.server.task.RespawnTask;
import net.server.world.World;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.CashShop.CashItemFactory;
import server.MapleSkillbookInformationProvider;
import server.ThreadManager;
import server.TimerManager;
import server.expeditions.MapleExpeditionBossLog;
import server.life.MaplePlayerNPCFactory;
import server.quest.MapleQuest;
import tools.AutoJCE;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Security;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class Server {

    private static final Set<Integer> activeFly = new HashSet<>();
    private static final Map<Integer, Integer> couponRates = new HashMap<>(30);
    private static final List<Integer> activeCoupons = new LinkedList<>();
    public static long uptime = System.currentTimeMillis();
    private static Server instance = null;
    private final Properties subnetInfo = new Properties();
    private final Map<Integer, Set<Integer>> accountChars = new HashMap<>();
    private final Map<Integer, Short> accountCharacterCount = new HashMap<>();
    private final Map<Integer, Integer> worldChars = new HashMap<>();
    private final Map<String, Integer> transitioningChars = new HashMap<>();
    private final Map<Integer, MapleGuild> guilds = new HashMap<>(100);
    private final Map<MapleClient, Long> inLoginState = new HashMap<>(100);
    private final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
    private final Map<Integer, MapleAlliance> alliances = new HashMap<>(100);
    private final Map<Integer, NewYearCardRecord> newyears = new HashMap<>();
    private final List<MapleClient> processDiseaseAnnouncePlayers = new LinkedList<>();
    private final List<MapleClient> registeredDiseaseAnnouncePlayers = new LinkedList<>();
    private final List<List<Pair<String, Integer>>> playerRanking = new LinkedList<>();
    private final Lock srvLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.SERVER);
    private final Lock disLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.SERVER_DISEASES);
    private final MonitoredReentrantReadWriteLock wldLock = new MonitoredReentrantReadWriteLock(MonitoredLockType.SERVER_WORLDS, true);
    private final MonitoredReadLock wldRLock = MonitoredReadLockFactory.createLock(wldLock);
    private final MonitoredWriteLock wldWLock = MonitoredWriteLockFactory.createLock(wldLock);
    private final MonitoredReentrantReadWriteLock lgnLock = new MonitoredReentrantReadWriteLock(MonitoredLockType.SERVER_LOGIN, true);
    private final MonitoredReadLock lgnRLock = MonitoredReadLockFactory.createLock(lgnLock);
    private final MonitoredWriteLock lgnWLock = MonitoredWriteLockFactory.createLock(lgnLock);
    private final AtomicLong currentTime = new AtomicLong(0);
    private IoAcceptor acceptor;
    private List<Map<Integer, String>> channels = new LinkedList<>();
    private List<World> worlds = new ArrayList<>();
    private List<Pair<Integer, String>> worldRecommendedList = new LinkedList<>();
    private long serverCurrentTime = 0;

    private boolean availableDeveloperRoom = false;
    private boolean online = false;

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    private static long getTimeLeftForNextHour() {
        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR, 1);
        nextHour.set(Calendar.MINUTE, 0);
        nextHour.set(Calendar.SECOND, 0);

        return Math.max(0, nextHour.getTimeInMillis() - System.currentTimeMillis());
    }

    public static long getTimeLeftForNextDay() {
        Calendar nextDay = Calendar.getInstance();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        nextDay.set(Calendar.HOUR_OF_DAY, 0);
        nextDay.set(Calendar.MINUTE, 0);
        nextDay.set(Calendar.SECOND, 0);

        return Math.max(0, nextDay.getTimeInMillis() - System.currentTimeMillis());
    }

    public static void cleanNxcodeCoupons(Connection con) throws SQLException {
        if (!YamlConfig.config.server.USE_CLEAR_OUTDATED_COUPONS) {
            return;
        }

        long timeClear = System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000;

        PreparedStatement ps = con.prepareStatement("SELECT * FROM nxcode WHERE expiration <= ?");
        ps.setLong(1, timeClear);
        ResultSet rs = ps.executeQuery();

        if (!rs.isLast()) {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM nxcode_items WHERE codeid = ?");
            while (rs.next()) {
                ps2.setInt(1, rs.getInt("id"));
                ps2.addBatch();
            }
            ps2.executeBatch();
            ps2.close();

            ps2 = con.prepareStatement("DELETE FROM nxcode WHERE expiration <= ?");
            ps2.setLong(1, timeClear);
            ps2.executeUpdate();
            ps2.close();
        }

        rs.close();
        ps.close();
    }

    private static List<Pair<Integer, List<Pair<String, Integer>>>> updatePlayerRankingFromDB(int worldid) {
        List<Pair<Integer, List<Pair<String, Integer>>>> rankSystem = new ArrayList<>();
        List<Pair<String, Integer>> rankUpdate = new ArrayList<>(0);

        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();

            String worldQuery;
            if (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
                if (worldid >= 0) {
                    worldQuery = (" AND `characters`.`world` = " + worldid);
                } else {
                    worldQuery = (" AND `characters`.`world` >= 0 AND `characters`.`world` <= " + -worldid);
                }
            } else {
                worldQuery = (" AND `characters`.`world` >= 0 AND `characters`.`world` <= " + Math.abs(worldid));
            }

            ps = con.prepareStatement("SELECT `characters`.`name`, `characters`.`level`, `characters`.`world` FROM `characters` LEFT JOIN accounts ON accounts.id = characters.accountid WHERE `characters`.`gm` < 2 AND `accounts`.`banned` = '0'" + worldQuery + " ORDER BY " + (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING ? "world, " : "") + "level DESC, exp DESC, lastExpGainTime ASC LIMIT 50");
            rs = ps.executeQuery();

            if (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
                int currentWorld = -1;
                while (rs.next()) {
                    int rsWorld = rs.getInt("world");
                    if (currentWorld < rsWorld) {
                        currentWorld = rsWorld;
                        rankUpdate = new ArrayList<>(50);
                        rankSystem.add(new Pair<>(rsWorld, rankUpdate));
                    }

                    rankUpdate.add(new Pair<>(rs.getString("name"), rs.getInt("level")));
                }
            } else {
                rankUpdate = new ArrayList<>(50);
                rankSystem.add(new Pair<>(0, rankUpdate));

                while (rs.next()) {
                    rankUpdate.add(new Pair<>(rs.getString("name"), rs.getInt("level")));
                }
            }

            ps.close();
            rs.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return rankSystem;
    }

    public static void main(String[] args) {
        // Mute GraalVM warning: "The polyglot context is using an implementation that does not support runtime compilation."
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        System.setProperty("wzpath", "wz");
        Security.setProperty("crypto.policy", "unlimited");
        AutoJCE.removeCryptographyRestrictions();
        Server.getInstance().init();
    }

    private static Pair<Short, List<List<MapleCharacter>>> loadAccountCharactersViewFromDb(int accId, int wlen) {
        short characterCount = 0;
        List<List<MapleCharacter>> wchars = new ArrayList<>(wlen);
        for (int i = 0; i < wlen; i++) wchars.add(i, new LinkedList<>());

        List<MapleCharacter> chars = new LinkedList<>();
        int curWorld = 0;
        try {
            List<Pair<Item, Integer>> accEquips = ItemFactory.loadEquippedItems(accId, true, true);
            Map<Integer, List<Item>> accPlayerEquips = new HashMap<>();

            for (Pair<Item, Integer> ae : accEquips) {
                List<Item> playerEquips = accPlayerEquips.computeIfAbsent(ae.getRight(), k -> new LinkedList<>());

                playerEquips.add(ae.getLeft());
            }

            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY world, id")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        characterCount++;

                        int cworld = rs.getByte("world");
                        if (cworld >= wlen) {
                            continue;
                        }

                        if (cworld > curWorld) {
                            wchars.add(curWorld, chars);

                            curWorld = cworld;
                            chars = new LinkedList<>();
                        }

                        Integer cid = rs.getInt("id");
                        chars.add(MapleCharacter.loadCharacterEntryFromDB(rs, accPlayerEquips.get(cid)));
                    }
                }
            }
            con.close();

            wchars.add(curWorld, chars);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        return new Pair<>(characterCount, wchars);
    }

    private static void applyAllNameChanges() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM namechanges WHERE completionTime IS NULL")) {
            ResultSet rs = ps.executeQuery();
            List<Pair<String, String>> changedNames = new LinkedList<>(); //logging only
            while (rs.next()) {
                con.setAutoCommit(false);
                int nameChangeId = rs.getInt("id");
                int characterId = rs.getInt("characterId");
                String oldName = rs.getString("old");
                String newName = rs.getString("new");
                boolean success = MapleCharacter.doNameChange(con, characterId, oldName, newName, nameChangeId);
                if (!success) {
                    con.rollback(); //discard changes
                } else {
                    changedNames.add(new Pair<>(oldName, newName));
                }
                con.setAutoCommit(true);
            }
            //log
            for (Pair<String, String> namePair : changedNames) {
                FilePrinter.print(FilePrinter.CHANGE_CHARACTER_NAME, "Name change applied : from \"" + namePair.getLeft() + "\" to \"" + namePair.getRight() + "\" at " + Calendar.getInstance().getTime());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.CHANGE_CHARACTER_NAME, e, "Failed to retrieve list of pending name changes.");
        }
    }

    private static void applyAllWorldTransfers() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM worldtransfers WHERE completionTime IS NULL", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = ps.executeQuery();
            List<Integer> removedTransfers = new LinkedList<>();
            while (rs.next()) {
                int nameChangeId = rs.getInt("id");
                int characterId = rs.getInt("characterId");
                int oldWorld = rs.getInt("from");
                int newWorld = rs.getInt("to");
                String reason = MapleCharacter.checkWorldTransferEligibility(con, characterId, oldWorld, newWorld); //check if character is still eligible
                if (reason != null) {
                    removedTransfers.add(nameChangeId);
                    FilePrinter.print(FilePrinter.WORLD_TRANSFER, "World transfer cancelled : Character ID " + characterId + " at " + Calendar.getInstance().getTime() + ", Reason : " + reason);
                    try (PreparedStatement delPs = con.prepareStatement("DELETE FROM worldtransfers WHERE id = ?")) {
                        delPs.setInt(1, nameChangeId);
                        delPs.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        FilePrinter.printError(FilePrinter.WORLD_TRANSFER, e, "Failed to delete world transfer for character ID " + characterId);
                    }
                }
            }
            rs.beforeFirst();
            List<Pair<Integer, Pair<Integer, Integer>>> worldTransfers = new LinkedList<>(); //logging only <charid, <oldWorld, newWorld>>
            while (rs.next()) {
                con.setAutoCommit(false);
                int nameChangeId = rs.getInt("id");
                if (removedTransfers.contains(nameChangeId)) {
                    continue;
                }
                int characterId = rs.getInt("characterId");
                int oldWorld = rs.getInt("from");
                int newWorld = rs.getInt("to");
                boolean success = MapleCharacter.doWorldTransfer(con, characterId, oldWorld, newWorld, nameChangeId);
                if (!success) {
                    con.rollback();
                } else {
                    worldTransfers.add(new Pair<>(characterId, new Pair<>(oldWorld, newWorld)));
                }
                con.setAutoCommit(true);
            }
            //log
            for (Pair<Integer, Pair<Integer, Integer>> worldTransferPair : worldTransfers) {
                int charId = worldTransferPair.getLeft();
                int oldWorld = worldTransferPair.getRight().getLeft();
                int newWorld = worldTransferPair.getRight().getRight();
                FilePrinter.print(FilePrinter.WORLD_TRANSFER, "World transfer applied : Character ID " + charId + " from World " + oldWorld + " to World " + newWorld + " at " + Calendar.getInstance().getTime());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            FilePrinter.printError(FilePrinter.WORLD_TRANSFER, e, "Failed to retrieve list of pending world transfers.");
        }
    }

    private static String getRemoteHost(MapleClient client) {
        return MapleSessionCoordinator.getSessionRemoteHost(client.getSession());
    }

    public int getCurrentTimestamp() {
        return (int) (Server.getInstance().getCurrentTime() - Server.uptime);
    }

    public long getCurrentTime() {  // returns a slightly delayed time value, under frequency of UPDATE_INTERVAL
        return serverCurrentTime;
    }

    public void updateCurrentTime() {
        serverCurrentTime = currentTime.addAndGet(YamlConfig.config.server.UPDATE_INTERVAL);
    }

    public long forceUpdateCurrentTime() {
        long timeNow = System.currentTimeMillis();
        serverCurrentTime = timeNow;
        currentTime.set(timeNow);

        return timeNow;
    }

    public boolean isOnline() {
        return online;
    }

    public List<Pair<Integer, String>> worldRecommendedList() {
        return worldRecommendedList;
    }

    public void setNewYearCard(NewYearCardRecord nyc) {
        newyears.put(nyc.getId(), nyc);
    }

    public Optional<NewYearCardRecord> getNewYearCard(int cardid) {
        return Optional.ofNullable(newyears.get(cardid));
    }

    public NewYearCardRecord removeNewYearCard(int cardid) {
        return newyears.remove(cardid);
    }

    public void setAvailableDeveloperRoom() {
        availableDeveloperRoom = true;
    }

    public boolean canEnterDeveloperRoom() {
        return availableDeveloperRoom;
    }

    private void loadPlayerNpcMapStepFromDb() {
        try {
            List<World> wlist = this.getWorlds();

            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM playernpcs_field");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int world = rs.getInt("world"), map = rs.getInt("map"), step = rs.getInt("step"), podium = rs.getInt("podium");

                World w = wlist.get(world);
                if (w != null) {
                    w.setPlayerNpcMapData(map, step, podium);
                }
            }

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<World> getWorld(int id) {
        wldRLock.lock();
        try {
            if (id < 0 || id >= worlds.size()) {
                return Optional.empty();
            }
            return Optional.ofNullable(worlds.get(id));
        } finally {
            wldRLock.unlock();
        }
    }

    public List<World> getWorlds() {
        wldRLock.lock();
        try {
            return Collections.unmodifiableList(worlds);
        } finally {
            wldRLock.unlock();
        }
    }

    public int getWorldsSize() {
        wldRLock.lock();
        try {
            return worlds.size();
        } finally {
            wldRLock.unlock();
        }
    }

    public Optional<Channel> getChannel(int world, int channel) {
        return getWorld(world)
                .flatMap(w -> w.getChannel(channel));
    }

    public List<Channel> getChannelsFromWorld(int world) {
        return getWorld(world)
                .map(World::getChannels)
                .orElse(Collections.emptyList());
    }

    public List<Channel> getAllChannels() {
        return getWorlds().stream()
                .map(World::getChannels)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Set<Integer> getOpenChannels(int world) {
        wldRLock.lock();
        try {
            return new HashSet<>(channels.get(world).keySet());
        } finally {
            wldRLock.unlock();
        }
    }

    private String getIP(int world, int channel) {
        wldRLock.lock();
        try {
            return channels.get(world).get(channel);
        } finally {
            wldRLock.unlock();
        }
    }

    public String[] getInetSocket(int world, int channel) {
        try {
            return getIP(world, channel).split(":");
        } catch (Exception e) {
            return null;
        }
    }

    private void dumpData() {
        wldRLock.lock();
        try {
            System.out.println(worlds);
            System.out.println(channels);
            System.out.println(worldRecommendedList);
            System.out.println();
            System.out.println("---------------------");
        } finally {
            wldRLock.unlock();
        }
    }

    public int addChannel(int worldId) {
        World world;
        Map<Integer, String> channelInfo;
        int channelid;

        wldRLock.lock();
        try {
            if (worldId >= worlds.size()) {
                return -3;
            }

            channelInfo = channels.get(worldId);
            if (channelInfo == null) {
                return -3;
            }

            channelid = channelInfo.size();
            if (channelid >= YamlConfig.config.server.CHANNEL_SIZE) {
                return -2;
            }

            channelid++;
            Optional<World> wr = getWorld(worldId);
            if (wr.isEmpty()) {
                return -3;
            }

            world = wr.get();
        } finally {
            wldRLock.unlock();
        }

        Channel channel = new Channel(worldId, channelid, getCurrentTime());
        channel.setServerMessage(YamlConfig.config.worlds.get(worldId).why_am_i_recommended);

        if (world.addChannel(channel)) {
            wldWLock.lock();
            try {
                channelInfo.put(channelid, channel.getIP());
            } finally {
                wldWLock.unlock();
            }
        }

        return channelid;
    }

    public int addWorld() {
        int newWorld = initWorld();
        if (newWorld > -1) {
            installWorldPlayerRanking(newWorld);

            Set<Integer> accounts;
            lgnRLock.lock();
            try {
                accounts = new HashSet<>(accountChars.keySet());
            } finally {
                lgnRLock.unlock();
            }

            accounts.forEach(id -> loadAccountCharactersView(id, 0, newWorld));
        }

        return newWorld;
    }

    private int initWorld() {
        int i;

        wldRLock.lock();
        try {
            i = worlds.size();

            if (i >= YamlConfig.config.server.WLDLIST_SIZE) {
                return -1;
            }
        } finally {
            wldRLock.unlock();
        }

        System.out.println("Starting world " + i);

        int exprate = YamlConfig.config.worlds.get(i).exp_rate;
        int mesorate = YamlConfig.config.worlds.get(i).meso_rate;
        int droprate = YamlConfig.config.worlds.get(i).drop_rate;
        int bossdroprate = YamlConfig.config.worlds.get(i).boss_drop_rate;
        int questrate = YamlConfig.config.worlds.get(i).quest_rate;
        int travelrate = YamlConfig.config.worlds.get(i).travel_rate;
        int fishingrate = YamlConfig.config.worlds.get(i).fishing_rate;

        int flag = YamlConfig.config.worlds.get(i).flag;
        String event_message = YamlConfig.config.worlds.get(i).event_message;
        String why_am_i_recommended = YamlConfig.config.worlds.get(i).why_am_i_recommended;

        World world = new World(i,
                flag,
                event_message,
                exprate, droprate, bossdroprate, mesorate, questrate, travelrate, fishingrate);
        boolean canDeploy = world.getId() == worlds.size();

        worldRecommendedList.add(new Pair<>(i, why_am_i_recommended));
        worlds.add(world);

        Map<Integer, String> channelInfo = new HashMap<>();
        long bootTime = getCurrentTime();
        for (int channelid = 1; channelid <= YamlConfig.config.worlds.get(i).channels; channelid++) {
            Channel channel = new Channel(i, channelid, bootTime);
            world.addChannel(channel);
            channelInfo.put(channelid, channel.getIP());
        }


        wldWLock.lock();    // thanks Ashen for noticing a deadlock issue when trying to deploy a channel
        try {
            if (canDeploy) {
                channels.add(i, channelInfo);
            }
        } finally {
            wldWLock.unlock();
        }

        if (canDeploy) {
            world.setServerMessage(YamlConfig.config.worlds.get(i).server_message);

            System.out.println("Finished loading world " + i + "\r\n");
            return i;
        } else {
            System.out.println("Could not load world " + i + "...\r\n");
            world.shutdown();
            return -2;
        }
    }

    public boolean removeChannel(int worldid) {   //lol don't!
        World world;

        wldRLock.lock();
        try {
            if (worldid >= worlds.size()) {
                return false;
            }
            world = worlds.get(worldid);
        } finally {
            wldRLock.unlock();
        }

        if (world != null) {
            int channel = world.removeChannel();
            wldWLock.lock();
            try {
                Map<Integer, String> m = channels.get(worldid);
                if (m != null) {
                    m.remove(channel);
                }
            } finally {
                wldWLock.unlock();
            }

            return channel > -1;
        }

        return false;
    }

    public boolean removeWorld() {   //lol don't!
        World w;
        int worldid;

        wldRLock.lock();
        try {
            worldid = worlds.size() - 1;
            if (worldid < 0) {
                return false;
            }

            w = worlds.get(worldid);
        } finally {
            wldRLock.unlock();
        }

        if (w == null || !w.canUninstall()) {
            return false;
        }

        removeWorldPlayerRanking();
        w.shutdown();

        wldWLock.lock();
        try {
            if (worldid == worlds.size() - 1) {
                worlds.remove(worldid);
                channels.remove(worldid);
                worldRecommendedList.remove(worldid);
            }
        } finally {
            wldWLock.unlock();
        }

        return true;
    }

    private void resetServerWorlds() {  // thanks maple006 for noticing proprietary lists assigned to null
        wldWLock.lock();
        try {
            worlds.clear();
            channels.clear();
            worldRecommendedList.clear();
        } finally {
            wldWLock.unlock();
        }
    }

    public Map<Integer, Integer> getCouponRates() {
        return couponRates;
    }

    private void loadCouponRates(Connection c) throws SQLException {
        PreparedStatement ps = c.prepareStatement("SELECT couponid, rate FROM nxcoupons");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            int cid = rs.getInt("couponid");
            int rate = rs.getInt("rate");

            couponRates.put(cid, rate);
        }

        rs.close();
        ps.close();
    }

    public List<Integer> getActiveCoupons() {
        synchronized (activeCoupons) {
            return activeCoupons;
        }
    }

    public void commitActiveCoupons() {
        getWorlds().stream()
                .map(World::getPlayerStorage)
                .map(PlayerStorage::getAllCharacters)
                .flatMap(Collection::stream)
                .filter(MapleCharacter::isLoggedin)
                .forEach(MapleCharacter::updateCouponRates);
    }

    public void toggleCoupon(Integer couponId) {
        if (ItemConstants.isRateCoupon(couponId)) {
            synchronized (activeCoupons) {
                if (activeCoupons.contains(couponId)) {
                    activeCoupons.remove(couponId);
                } else {
                    activeCoupons.add(couponId);
                }

                commitActiveCoupons();
            }
        }
    }

    public void updateActiveCoupons() throws SQLException {
        synchronized (activeCoupons) {
            activeCoupons.clear();
            Calendar c = Calendar.getInstance();

            int weekDay = c.get(Calendar.DAY_OF_WEEK);
            int hourDay = c.get(Calendar.HOUR_OF_DAY);

            Connection con = null;
            try {
                con = DatabaseConnection.getConnection();

                int weekdayMask = (1 << weekDay);
                PreparedStatement ps = con.prepareStatement("SELECT couponid FROM nxcoupons WHERE (activeday & ?) = ? AND starthour <= ? AND endhour > ?");
                ps.setInt(1, weekdayMask);
                ps.setInt(2, weekdayMask);
                ps.setInt(3, hourDay);
                ps.setInt(4, hourDay);

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    activeCoupons.add(rs.getInt("couponid"));
                }

                rs.close();
                ps.close();

                con.close();
            } catch (SQLException ex) {
                ex.printStackTrace();

                try {
                    if (con != null && !con.isClosed()) {
                        con.close();
                    }
                } catch (SQLException ex2) {
                    ex2.printStackTrace();
                }
            }
        }
    }

    public void runAnnouncePlayerDiseasesSchedule() {
        List<MapleClient> processDiseaseAnnounceClients;
        disLock.lock();
        try {
            processDiseaseAnnounceClients = new LinkedList<>(processDiseaseAnnouncePlayers);
            processDiseaseAnnouncePlayers.clear();
        } finally {
            disLock.unlock();
        }

        while (!processDiseaseAnnounceClients.isEmpty()) {
            MapleClient c = processDiseaseAnnounceClients.remove(0);
            MapleCharacter player = c.getPlayer();
            if (player != null && player.isLoggedinWorld()) {
                player.announceDiseases();
                player.collectDiseases();
            }
        }

        disLock.lock();
        try {
            // this is to force the system to wait for at least one complete tick before releasing disease info for the registered clients
            while (!registeredDiseaseAnnouncePlayers.isEmpty()) {
                MapleClient c = registeredDiseaseAnnouncePlayers.remove(0);
                processDiseaseAnnouncePlayers.add(c);
            }
        } finally {
            disLock.unlock();
        }
    }

    public void registerAnnouncePlayerDiseases(MapleClient c) {
        disLock.lock();
        try {
            registeredDiseaseAnnouncePlayers.add(c);
        } finally {
            disLock.unlock();
        }
    }

    public List<Pair<String, Integer>> getWorldPlayerRanking(int worldid) {
        wldRLock.lock();
        try {
            return new ArrayList<>(playerRanking.get(!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING ? worldid : 0));
        } finally {
            wldRLock.unlock();
        }
    }

    private void installWorldPlayerRanking(int worldid) {
        List<Pair<Integer, List<Pair<String, Integer>>>> ranking = updatePlayerRankingFromDB(worldid);
        if (!ranking.isEmpty()) {
            wldWLock.lock();
            try {
                if (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
                    for (int i = playerRanking.size(); i <= worldid; i++) {
                        playerRanking.add(new ArrayList<>(0));
                    }

                    playerRanking.add(worldid, ranking.get(0).getRight());
                } else {
                    playerRanking.add(0, ranking.get(0).getRight());
                }
            } finally {
                wldWLock.unlock();
            }
        }
    }

    private void removeWorldPlayerRanking() {
        if (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
            wldWLock.lock();
            try {
                if (playerRanking.size() < worlds.size()) {
                    return;
                }

                playerRanking.remove(playerRanking.size() - 1);
            } finally {
                wldWLock.unlock();
            }
        } else {
            List<Pair<Integer, List<Pair<String, Integer>>>> ranking = updatePlayerRankingFromDB(-1 * (this.getWorldsSize() - 2));  // update ranking list

            wldWLock.lock();
            try {
                playerRanking.add(0, ranking.get(0).getRight());
            } finally {
                wldWLock.unlock();
            }
        }
    }

    public void updateWorldPlayerRanking() {
        List<Pair<Integer, List<Pair<String, Integer>>>> rankUpdates = updatePlayerRankingFromDB(-1 * (this.getWorldsSize() - 1));
        if (!rankUpdates.isEmpty()) {
            wldWLock.lock();
            try {
                if (!YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
                    for (int i = playerRanking.size(); i <= rankUpdates.get(rankUpdates.size() - 1).getLeft(); i++) {
                        playerRanking.add(new ArrayList<>(0));
                    }

                    for (Pair<Integer, List<Pair<String, Integer>>> wranks : rankUpdates) {
                        playerRanking.set(wranks.getLeft(), wranks.getRight());
                    }
                } else {
                    playerRanking.set(0, rankUpdates.get(0).getRight());
                }
            } finally {
                wldWLock.unlock();
            }
        }
    }

    private void initWorldPlayerRanking() {
        if (YamlConfig.config.server.USE_WHOLE_SERVER_RANKING) {
            playerRanking.add(new ArrayList<>(0));
        }
        updateWorldPlayerRanking();
    }

    public void init() {
        System.out.println("HeavenMS v" + ServerConstants.VERSION + " starting up.\r\n");

        if (YamlConfig.config.server.SHUTDOWNHOOK) {
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown(false)));
        }

        TimeZone.setDefault(TimeZone.getTimeZone(YamlConfig.config.server.TIMEZONE));

        Connection c;
        try {
            c = DatabaseConnection.getConnection();
            PreparedStatement ps = c.prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
            ps = c.prepareStatement("UPDATE characters SET HasMerchant = 0");
            ps.executeUpdate();
            ps.close();

            cleanNxcodeCoupons(c);
            loadCouponRates(c);
            updateActiveCoupons();

            c.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        applyAllNameChanges(); // -- name changes can be missed by INSTANT_NAME_CHANGE --
        applyAllWorldTransfers();
        //MaplePet.clearMissingPetsFromDb();    // thanks Optimist for noticing this taking too long to run
        MapleCashidGenerator.loadExistentCashIdsFromDb();

        ThreadManager.getInstance().start();
        initializeTimelyTasks();    // aggregated method for timely tasks thanks to lxconan

        long timeToTake = System.currentTimeMillis();
        SkillFactory.loadAllSkills();
        System.out.println("Skills loaded in " + ((System.currentTimeMillis() - timeToTake) / 1000.0) + " seconds");

        timeToTake = System.currentTimeMillis();

        CashItemFactory.getSpecialCashItems();
        System.out.println("Items loaded in " + ((System.currentTimeMillis() - timeToTake) / 1000.0) + " seconds");

        timeToTake = System.currentTimeMillis();
        MapleQuest.loadAllQuest();
        System.out.println("Quest loaded in " + ((System.currentTimeMillis() - timeToTake) / 1000.0) + " seconds\r\n");

        NewYearCardRecord.startPendingNewYearCardRequests();

        if (YamlConfig.config.server.USE_THREAD_TRACKER) {
            ThreadTracker.getInstance().registerThreadTrackerTask();
        }

        try {
            int worldCount = Math.min(GameConstants.WORLD_NAMES.length, YamlConfig.config.server.WORLDS);

            for (int i = 0; i < worldCount; i++) {
                initWorld();
            }
            initWorldPlayerRanking();

            MaplePlayerNPCFactory.loadFactoryMetadata();
            loadPlayerNpcMapStepFromDb();
        } catch (Exception e) {
            e.printStackTrace();//For those who get errors
            System.out.println("[SEVERE] Syntax error in 'world.ini'.");
            System.exit(0);
        }

        System.out.println();

        if (YamlConfig.config.server.USE_FAMILY_SYSTEM) {
            timeToTake = System.currentTimeMillis();
            MapleFamily.loadAllFamilies();
            System.out.println("Families loaded in " + ((System.currentTimeMillis() - timeToTake) / 1000.0) + " seconds\r\n");
        }

        System.out.println();

        IoBuffer.setUseDirectBuffer(false);     // join IO operations performed by lxconan
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
        acceptor.setHandler(new MapleServerHandler());
        try {
            acceptor.bind(new InetSocketAddress(8484));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Listening on port 8484\r\n\r\n");

        System.out.println("HeavenMS is now online.\r\n");
        online = true;

        MapleSkillbookInformationProvider.getInstance();
        OpcodeConstants.generateOpcodeNames();
        CommandsExecutor.getInstance();

        getAllChannels().forEach(Channel::reloadEventScriptManager);
    }

    private void initializeTimelyTasks() {
        TimerManager tMan = TimerManager.getInstance();
        tMan.start();
        tMan.register(tMan.purge(), YamlConfig.config.server.PURGING_INTERVAL);//Purging ftw...
        disconnectIdlesOnLoginTask();

        long timeLeft = getTimeLeftForNextHour();
        tMan.register(new CharacterDiseaseTask(), YamlConfig.config.server.UPDATE_INTERVAL, YamlConfig.config.server.UPDATE_INTERVAL);
        tMan.register(new ReleaseLockTask(), 2 * 60 * 1000, 2 * 60 * 1000);
        tMan.register(new CouponTask(), YamlConfig.config.server.COUPON_INTERVAL, timeLeft);
        tMan.register(new RankingCommandTask(), 5 * 60 * 1000, 5 * 60 * 1000);
        tMan.register(new RankingLoginTask(), YamlConfig.config.server.RANKING_INTERVAL, timeLeft);
        tMan.register(new LoginCoordinatorTask(), 60 * 60 * 1000, timeLeft);
        tMan.register(new EventRecallCoordinatorTask(), 60 * 60 * 1000, timeLeft);
        tMan.register(new LoginStorageTask(), 2 * 60 * 1000, 2 * 60 * 1000);
        tMan.register(new DueyFredrickTask(), 60 * 60 * 1000, timeLeft);
        tMan.register(new InvitationTask(), 30 * 1000, 30 * 1000);
        tMan.register(new RespawnTask(), YamlConfig.config.server.RESPAWN_INTERVAL, YamlConfig.config.server.RESPAWN_INTERVAL);

        timeLeft = getTimeLeftForNextDay();
        MapleExpeditionBossLog.resetBossLogTable();
        tMan.register(new BossLogTask(), 24 * 60 * 60 * 1000, timeLeft);
    }

    public Properties getSubnetInfo() {
        return subnetInfo;
    }

    public Optional<MapleAlliance> getAlliance(int id) {
        synchronized (alliances) {
            if (alliances.containsKey(id)) {
                return Optional.ofNullable(alliances.get(id));
            }
            return Optional.empty();
        }
    }

    public void addAlliance(int id, MapleAlliance alliance) {
        synchronized (alliances) {
            if (!alliances.containsKey(id)) {
                alliances.put(id, alliance);
            }
        }
    }

    public void disbandAlliance(int id) {
        synchronized (alliances) {
            MapleAlliance alliance = alliances.get(id);
            if (alliance != null) {
                alliance.getGuilds().stream()
                        .map(guilds::get)
                        .forEach(g -> g.setAllianceId(0));
                alliances.remove(id);
            }
        }
    }

    public void allianceMessage(int id, final byte[] packet, int exception, int guildex) {
        MapleAlliance alliance = alliances.get(id);
        if (alliance != null) {
            alliance.getGuilds().stream()
                    .filter(guildId -> guildId != guildex)
                    .map(guilds::get)
                    .filter(Objects::nonNull)
                    .forEach(g -> g.broadcast(packet, exception));
        }
    }

    public boolean addGuildtoAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.addGuild(guildId);
            guilds.get(guildId).setAllianceId(aId);
            return true;
        }
        return false;
    }

    public boolean removeGuildFromAlliance(int aId, int guildId) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.removeGuild(guildId);
            guilds.get(guildId).setAllianceId(0);
            return true;
        }
        return false;
    }

    public boolean setAllianceRanks(int aId, String[] ranks) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setRankTitle(ranks);
            return true;
        }
        return false;
    }

    public boolean setAllianceNotice(int aId, String notice) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.setNotice(notice);
            return true;
        }
        return false;
    }

    public boolean increaseAllianceCapacity(int aId, int inc) {
        MapleAlliance alliance = alliances.get(aId);
        if (alliance != null) {
            alliance.increaseCapacity(inc);
            return true;
        }
        return false;
    }

    public int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }

    public Optional<MapleGuild> getGuildByName(String name) {
        synchronized (guilds) {
            return guilds.values().stream()
                    .filter(g -> g.getName().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

    public Optional<MapleGuild> getGuild(int id) {
        synchronized (guilds) {
            return Optional.ofNullable(guilds.get(id));
        }
    }

    public Optional<MapleGuild> getGuild(int id, int world) {
        return getGuild(id, world, null);
    }

    public Optional<MapleGuild> getGuild(int id, int world, MapleCharacter mc) {
        synchronized (guilds) {
            MapleGuild g = guilds.get(id);
            if (g != null) {
                return Optional.of(g);
            }

            g = new MapleGuild(id, world);
            if (g.getId() == -1) {
                return Optional.empty();
            }

            if (mc != null) {
                MapleGuildCharacter mgc = g.getMGC(mc.getId());
                if (mgc != null) {
                    mc.setMGC(mgc);
                    mgc.setCharacter(mc);
                } else {
                    FilePrinter.printError(FilePrinter.GUILD_CHAR_ERROR, "Could not find " + mc.getName() + " when loading guild " + id + ".");
                }

                g.setOnline(mc.getId(), true, mc.getClient().getChannel());
            }

            guilds.put(id, g);
            return Optional.of(g);
        }
    }

    public void setGuildMemberOnline(MapleCharacter mc, boolean bOnline, int channel) {
        getGuild(mc.getGuildId(), mc.getWorld(), mc).ifPresent(g -> g.setOnline(mc.getId(), bOnline, channel));
    }

    public int addGuildMember(MapleGuildCharacter mgc, MapleCharacter chr) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mgc, chr);
        }
        return 0;
    }

    public void setGuildAllianceId(int guildId, int allianceId) {
        getGuild(guildId).ifPresent(g -> g.setAllianceId(allianceId));
    }

    public void resetAllianceGuildPlayersRank(int guildId) {
        getGuild(guildId).ifPresent(MapleGuild::resetAllianceGuildPlayersRank);
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public void guildChat(int guildId, String name, int senderId, String message) {
        getGuild(guildId).ifPresent(g -> g.guildChat(name, senderId, message));
    }

    public void changeRank(int guildId, int targetId, int rank) {
        getGuild(guildId).ifPresent(g -> g.changeRank(targetId, rank));
    }

    public void expelMember(MapleGuildCharacter initiator, String targetName, int targetId) {
        getGuild(initiator.getGuildId()).ifPresent(g -> g.expelMember(initiator, targetName, targetId));
    }

    public void setGuildNotice(int guildId, String notice) {
        getGuild(guildId).ifPresent(g -> g.setGuildNotice(notice));
    }

    public void memberLevelJobUpdate(MapleGuildCharacter character) {
        getGuild(character.getGuildId()).ifPresent(g -> g.memberLevelJobUpdate(character));
    }

    public void changeRankTitle(int guildId, String[] ranks) {
        getGuild(guildId).ifPresent(g -> g.changeRankTitle(ranks));
    }

    public void setGuildEmblem(int guildId, short bg, byte bgColor, short logo, byte logoColor) {
        getGuild(guildId).ifPresent(g -> g.setGuildEmblem(bg, bgColor, logo, logoColor));
    }

    public void disbandGuild(int gid) {
        synchronized (guilds) {
            MapleGuild g = guilds.get(gid);
            g.disbandGuild();
            guilds.remove(gid);
        }
    }

    public boolean increaseGuildCapacity(int gid) {
        MapleGuild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public void gainGP(int guildId, int amount) {
        getGuild(guildId).ifPresent(g -> g.gainGP(amount));
    }

    public void guildMessage(int guildId, byte[] packet) {
        guildMessage(guildId, packet, -1);
    }

    public void guildMessage(int guildId, byte[] packet, int exception) {
        getGuild(guildId).ifPresent(g -> g.broadcast(packet, exception));
    }

    public PlayerBuffStorage getPlayerBuffStorage() {
        return buffStorage;
    }

    public void deleteGuildCharacter(MapleCharacter mc) {
        setGuildMemberOnline(mc, false, (byte) -1);
        if (mc.getMGC().getGuildRank() > 1) {
            leaveGuild(mc.getMGC());
        } else {
            disbandGuild(mc.getMGC().getGuildId());
        }
    }

    public void deleteGuildCharacter(MapleGuildCharacter mgc) {
        if (mgc.getCharacter() != null) {
            setGuildMemberOnline(mgc.getCharacter(), false, (byte) -1);
        }
        if (mgc.getGuildRank() > 1) {
            leaveGuild(mgc);
        } else {
            disbandGuild(mgc.getGuildId());
        }
    }

    public void reloadGuildCharacters(int worldId) {
        getWorld(worldId).ifPresent(this::reloadGuildCharacters);
    }

    private void reloadGuildCharacters(World world) {
        for (MapleCharacter mc : world.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildId() > 0) {
                setGuildMemberOnline(mc, true, world.getId());
                memberLevelJobUpdate(mc.getMGC());
            }
        }
        world.reloadGuildSummary();
    }

    public void broadcastMessage(int world, final byte[] packet) {
        getChannelsFromWorld(world).forEach(c -> c.broadcastPacket(packet));
    }

    public void broadcastGMMessage(int world, final byte[] packet) {
        getChannelsFromWorld(world).forEach(c -> c.broadcastGMPacket(packet));
    }

    public boolean isGmOnline(int world) {
        return getChannelsFromWorld(world).stream()
                .map(Channel::getPlayerStorage)
                .map(PlayerStorage::getAllCharacters)
                .flatMap(Collection::stream)
                .anyMatch(MapleCharacter::isGM);
    }

    public void changeFly(Integer accountid, boolean canFly) {
        if (canFly) {
            activeFly.add(accountid);
        } else {
            activeFly.remove(accountid);
        }
    }

    public boolean canFly(Integer accountid) {
        return activeFly.contains(accountid);
    }

    public Optional<Integer> getCharacterWorld(int chrid) {
        lgnRLock.lock();
        try {
            return Optional.ofNullable(worldChars.get(chrid));
        } finally {
            lgnRLock.unlock();
        }
    }

    public boolean haveCharacterEntry(Integer accountid, Integer chrid) {
        lgnRLock.lock();
        try {
            Set<Integer> accChars = accountChars.get(accountid);
            return accChars.contains(chrid);
        } finally {
            lgnRLock.unlock();
        }
    }

    public short getAccountCharacterCount(Integer accountId) {
        lgnRLock.lock();
        try {
            return accountCharacterCount.get(accountId);
        } finally {
            lgnRLock.unlock();
        }
    }

    public short getAccountWorldCharacterCount(Integer accountId, Integer worldId) {
        lgnRLock.lock();
        try {
            return (short) accountChars.get(accountId).stream()
                    .map(this::getCharacterWorld)
                    .flatMap(Optional::stream)
                    .filter(id -> id.equals(worldId))
                    .count();
        } finally {
            lgnRLock.unlock();
        }
    }

    private Set<Integer> getAccountCharacterEntries(Integer accountId) {
        lgnRLock.lock();
        try {
            return new HashSet<>(accountChars.get(accountId));
        } finally {
            lgnRLock.unlock();
        }
    }
    
    /*
    public void deleteAccountEntry(Integer accountid) { is this even a thing?
        lgnWLock.lock();
        try {
            accountCharacterCount.remove(accountid);
            accountChars.remove(accountid);
        } finally {
            lgnWLock.unlock();
        }
    
        for (World wserv : this.getWorlds()) {
            wserv.clearAccountCharacterView(accountid);
            wserv.unregisterAccountStorage(accountid);
        }
    }
    */

    public void updateCharacterEntry(MapleCharacter chr) {
        MapleCharacter chrView = chr.generateCharacterEntry();

        lgnWLock.lock();
        try {
            getWorld(chrView.getWorld()).ifPresent(w -> w.registerAccountCharacterView(chrView.getAccountID(), chrView));
        } finally {
            lgnWLock.unlock();
        }
    }

    public void createCharacterEntry(MapleCharacter chr) {
        lgnWLock.lock();
        try {
            accountCharacterCount.put(chr.getAccountID(), (short) (accountCharacterCount.get(chr.getAccountID()) + 1));

            Set<Integer> accChars = accountChars.get(chr.getAccountID());
            accChars.add(chr.getId());

            worldChars.put(chr.getId(), chr.getWorld());

            MapleCharacter chrView = chr.generateCharacterEntry();
            getWorld(chrView.getWorld()).ifPresent(w -> w.registerAccountCharacterView(chrView.getAccountID(), chrView));
        } finally {
            lgnWLock.unlock();
        }
    }

    public void deleteCharacterEntry(Integer accountId, Integer characterId) {
        lgnWLock.lock();
        try {
            accountCharacterCount.put(accountId, (short) (accountCharacterCount.get(accountId) - 1));

            Set<Integer> accChars = accountChars.get(accountId);
            accChars.remove(characterId);

            Integer worldId = worldChars.remove(characterId);
            getWorld(worldId).ifPresent(w -> w.unregisterAccountCharacterView(accountId, characterId));
        } finally {
            lgnWLock.unlock();
        }
    }

    public void transferWorldCharacterEntry(MapleCharacter chr, Integer toWorld) { // used before setting the new worldid on the character object
        lgnWLock.lock();
        try {
            getCharacterWorld(chr.getId())
                    .flatMap(this::getWorld)
                    .ifPresent(w -> w.unregisterAccountCharacterView(chr.getAccountID(), chr.getId()));

            worldChars.put(chr.getId(), toWorld);
            MapleCharacter chrView = chr.generateCharacterEntry();
            getWorld(toWorld).ifPresent(w -> w.registerAccountCharacterView(chrView.getAccountID(), chrView));
        } finally {
            lgnWLock.unlock();
        }
    }

    public Pair<Pair<Integer, List<MapleCharacter>>, List<Pair<Integer, List<MapleCharacter>>>> loadAccountCharlist(Integer accountId, int visibleWorlds) {
        List<World> wlist = this.getWorlds();
        if (wlist.size() > visibleWorlds) {
            wlist = wlist.subList(0, visibleWorlds);
        }

        List<Pair<Integer, List<MapleCharacter>>> accChars = new ArrayList<>(wlist.size() + 1);
        int chrTotal = 0;
        List<MapleCharacter> lastwchars = null;

        lgnRLock.lock();
        try {
            for (World w : wlist) {
                List<MapleCharacter> wchars = w.getAccountCharactersView(accountId);
                if (wchars == null) {
                    if (!accountChars.containsKey(accountId)) {
                        accountCharacterCount.put(accountId, (short) 0);
                        accountChars.put(accountId, new HashSet<>());    // not advisable at all to write on the map on a read-protected environment
                    }                                                           // yet it's known there's no problem since no other point in the source does
                } else if (!wchars.isEmpty()) {                                  // this action.
                    lastwchars = wchars;

                    accChars.add(new Pair<>(w.getId(), wchars));
                    chrTotal += wchars.size();
                }
            }
        } finally {
            lgnRLock.unlock();
        }

        return new Pair<>(new Pair<>(chrTotal, lastwchars), accChars);
    }

    public void loadAllAccountsCharactersView() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int accountId = rs.getInt("id");
                if (isFirstAccountLogin(accountId)) {
                    loadAccountCharactersView(accountId, 0, 0);
                }
            }

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private boolean isFirstAccountLogin(Integer accId) {
        lgnRLock.lock();
        try {
            return !accountChars.containsKey(accId);
        } finally {
            lgnRLock.unlock();
        }
    }

    public void loadAccountCharacters(MapleClient c) {
        Integer accId = c.getAccID();
        if (!isFirstAccountLogin(accId)) {
            Set<Integer> accWorlds;

            lgnRLock.lock();
            try {
                accWorlds = getAccountCharacterEntries(accId).stream()
                        .map(this::getCharacterWorld)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toSet());
            } finally {
                lgnRLock.unlock();
            }

            int gmLevel = accWorlds.stream()
                    .map(this::getWorld)
                    .flatMap(Optional::stream)
                    .map(World::getAllCharactersView)
                    .flatMap(Collection::stream)
                    .mapToInt(MapleCharacter::gmLevel)
                    .max()
                    .orElse(0);

            c.setGMLevel(gmLevel);
            return;
        }

        int gmLevel = loadAccountCharactersView(c.getAccID(), 0, 0);
        c.setGMLevel(gmLevel);
    }

    private int loadAccountCharactersView(Integer accId, int gmLevel, int fromWorldid) {    // returns the maximum gmLevel found
        List<World> wlist = this.getWorlds();
        Pair<Short, List<List<MapleCharacter>>> accCharacters = loadAccountCharactersViewFromDb(accId, wlist.size());

        lgnWLock.lock();
        try {
            List<List<MapleCharacter>> accChars = accCharacters.getRight();
            accountCharacterCount.put(accId, accCharacters.getLeft());

            Set<Integer> chars = accountChars.get(accId);
            if (chars == null) {
                chars = new HashSet<>(5);
            }

            for (int wid = fromWorldid; wid < wlist.size(); wid++) {
                World w = wlist.get(wid);
                List<MapleCharacter> wchars = accChars.get(wid);
                w.loadAccountCharactersView(accId, wchars);

                for (MapleCharacter chr : wchars) {
                    int cid = chr.getId();
                    if (gmLevel < chr.gmLevel()) {
                        gmLevel = chr.gmLevel();
                    }

                    chars.add(cid);
                    worldChars.put(cid, wid);
                }
            }

            accountChars.put(accId, chars);
        } finally {
            lgnWLock.unlock();
        }

        return gmLevel;
    }

    public void loadAccountStorages(MapleClient c) {
        int accountId = c.getAccID();
        Set<Integer> accWorlds;
        lgnWLock.lock();
        try {
            accWorlds = accountChars.get(accountId).stream()
                    .map(this::getCharacterWorld)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
        } finally {
            lgnWLock.unlock();
        }

        List<World> worldList = this.getWorlds();
        for (Integer worldid : accWorlds) {
            if (worldid < worldList.size()) {
                World wserv = worldList.get(worldid);
                wserv.loadAccountStorage(accountId);
            }
        }
    }

    public void setCharacteridInTransition(MapleClient client, int charId) {
        String remoteIp = getRemoteHost(client);

        lgnWLock.lock();
        try {
            transitioningChars.put(remoteIp, charId);
        } finally {
            lgnWLock.unlock();
        }
    }

    public boolean validateCharacteridInTransition(MapleClient client, int charId) {
        if (!YamlConfig.config.server.USE_IP_VALIDATION) {
            return true;
        }

        String remoteIp = getRemoteHost(client);

        lgnWLock.lock();
        try {
            Integer cid = transitioningChars.remove(remoteIp);
            return cid != null && cid.equals(charId);
        } finally {
            lgnWLock.unlock();
        }
    }

    public Optional<Integer> freeCharacteridInTransition(MapleClient client) {
        if (!YamlConfig.config.server.USE_IP_VALIDATION) {
            return Optional.empty();
        }

        String remoteIp = getRemoteHost(client);

        lgnWLock.lock();
        try {
            return Optional.of(transitioningChars.remove(remoteIp));
        } finally {
            lgnWLock.unlock();
        }
    }

    public boolean hasCharacteridInTransition(MapleClient client) {
        if (!YamlConfig.config.server.USE_IP_VALIDATION) {
            return true;
        }

        String remoteIp = getRemoteHost(client);

        lgnRLock.lock();
        try {
            return transitioningChars.containsKey(remoteIp);
        } finally {
            lgnRLock.unlock();
        }
    }

    public void registerLoginState(MapleClient c) {
        srvLock.lock();
        try {
            inLoginState.put(c, System.currentTimeMillis() + 600000);
        } finally {
            srvLock.unlock();
        }
    }

    public void unregisterLoginState(MapleClient c) {
        srvLock.lock();
        try {
            inLoginState.remove(c);
        } finally {
            srvLock.unlock();
        }
    }

    private void disconnectIdlesOnLoginState() {
        List<MapleClient> toDisconnect;

        srvLock.lock();
        try {
            long timeNow = System.currentTimeMillis();

            toDisconnect = inLoginState.entrySet().stream()
                    .filter(e -> timeNow > e.getValue())
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
            toDisconnect.forEach(inLoginState::remove);
        } finally {
            srvLock.unlock();
        }

        for (MapleClient c : toDisconnect) {    // thanks Lei for pointing a deadlock issue with srvLock
            if (c.isLoggedIn()) {
                c.disconnect(false, false);
            } else {
                MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
            }
        }
    }

    private void disconnectIdlesOnLoginTask() {
        TimerManager.getInstance().register(this::disconnectIdlesOnLoginState, 300000);
    }

    public final Runnable shutdown(final boolean restart) {//no player should be online when trying to shutdown!
        return () -> shutdownInternal(restart);
    }

    private synchronized void shutdownInternal(boolean restart) {
        System.out.println((restart ? "Restarting" : "Shutting down") + " the server!\r\n");
        if (getWorlds() == null) {
            return;
        }
        getWorlds().forEach(World::shutdown);

        if (YamlConfig.config.server.USE_THREAD_TRACKER) {
            ThreadTracker.getInstance().cancelThreadTrackerTask();
        }

        List<Channel> allChannels = getAllChannels();
        for (Channel ch : allChannels) {
            while (!ch.finishedShutdown()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    System.err.println("FUCK MY LIFE");
                }
            }
        }

        resetServerWorlds();

        ThreadManager.getInstance().stop();
        TimerManager.getInstance().purge();
        TimerManager.getInstance().stop();

        System.out.println("Worlds + Channels are offline.");
        acceptor.unbind();
        acceptor = null;
        if (!restart) {  // shutdown hook deadlocks if System.exit() method is used within its body chores, thanks MIKE for pointing that out
            new Thread(() -> System.exit(0)).start();
        } else {
            System.out.println("\r\nRestarting the server....\r\n");
            try {
                instance.finalize();//FUU I CAN AND IT'S FREE
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            instance = null;
            System.gc();
            getInstance().init();//DID I DO EVERYTHING?! D:
        }
    }
}
