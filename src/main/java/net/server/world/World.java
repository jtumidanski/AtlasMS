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

import client.AbstractMapleCharacterObject;
import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleFamily;
import config.YamlConfig;
import constants.game.GameConstants;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.audit.LockCollector;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.MonitoredReadLock;
import net.server.audit.locks.MonitoredReentrantLock;
import net.server.audit.locks.MonitoredReentrantReadWriteLock;
import net.server.audit.locks.MonitoredWriteLock;
import net.server.audit.locks.factory.MonitoredReadLockFactory;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import net.server.audit.locks.factory.MonitoredWriteLockFactory;
import net.server.channel.Channel;
import net.server.channel.CharacterIdChannelPair;
import net.server.coordinator.matchchecker.MapleMatchCheckerCoordinator;
import net.server.coordinator.partysearch.MaplePartySearchCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.guild.MapleGuildSummary;
import net.server.services.BaseService;
import net.server.services.ServicesManager;
import net.server.services.type.WorldServices;
import net.server.task.CharacterAutosaverTask;
import net.server.task.FamilyDailyResetTask;
import net.server.task.FishingTask;
import net.server.task.HiredMerchantTask;
import net.server.task.MapOwnershipTask;
import net.server.task.MountTirednessTask;
import net.server.task.PartySearchTask;
import net.server.task.PetFullnessTask;
import net.server.task.ServerMessageTask;
import net.server.task.TimedMapObjectTask;
import net.server.task.TimeoutTask;
import net.server.task.WeddingReservationTask;
import scripting.event.EventInstanceManager;
import server.MapleStorage;
import server.TimerManager;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleHiredMerchant;
import server.maps.MapleMiniDungeon;
import server.maps.MapleMiniDungeonInfo;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packets.Fishing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class World {

    private final MonitoredReentrantReadWriteLock chnLock = new MonitoredReentrantReadWriteLock(MonitoredLockType.WORLD_CHANNELS, true);
    private final MonitoredReentrantReadWriteLock suggestLock = new MonitoredReentrantReadWriteLock(MonitoredLockType.WORLD_SUGGEST, true);
    private int id, flag, exprate, droprate, bossdroprate, mesorate, questrate, travelrate, fishingrate;
    private String eventmsg;
    private List<Channel> channels = new ArrayList<>();
    private Map<Integer, Byte> pnpcStep = new HashMap<>();
    private Map<Integer, Short> pnpcPodium = new HashMap<>();
    private Map<Integer, MapleMessenger> messengers = new HashMap<>();
    private AtomicInteger runningMessengerId = new AtomicInteger();
    private Map<Integer, MapleFamily> families = new LinkedHashMap<>();
    private Map<Integer, Integer> relationships = new HashMap<>();
    private Map<Integer, Pair<Integer, Integer>> relationshipCouples = new HashMap<>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<>();
    private PlayerStorage players = new PlayerStorage();
    private ServicesManager services = new ServicesManager(WorldServices.SAVE_CHARACTER);
    private MapleMatchCheckerCoordinator matchChecker = new MapleMatchCheckerCoordinator();
    private MaplePartySearchCoordinator partySearch = new MaplePartySearchCoordinator();
    private MonitoredReadLock chnRLock = MonitoredReadLockFactory.createLock(chnLock);
    private MonitoredWriteLock chnWLock = MonitoredWriteLockFactory.createLock(chnLock);
    private Map<Integer, SortedMap<Integer, MapleCharacter>> accountChars = new HashMap<>();
    private Map<Integer, MapleStorage> accountStorages = new HashMap<>();
    private MonitoredReentrantLock accountCharsLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_CHARS, true);
    private Set<Integer> queuedGuilds = new HashSet<>();
    private Map<Integer, Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>>> queuedMarriages = new HashMap<>();
    private Map<Integer, Set<Integer>> marriageGuests = new ConcurrentHashMap<>();
    private Map<Integer, Integer> partyChars = new HashMap<>();
    private Map<Integer, MapleParty> parties = new HashMap<>();
    private AtomicInteger runningPartyId = new AtomicInteger();
    private MonitoredReentrantLock partyLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_PARTY, true);
    private Map<Integer, Integer> owlSearched = new LinkedHashMap<>();
    private List<Map<Integer, Integer>> cashItemBought = new ArrayList<>(9);
    private MonitoredReadLock suggestRLock = MonitoredReadLockFactory.createLock(suggestLock);
    private MonitoredWriteLock suggestWLock = MonitoredWriteLockFactory.createLock(suggestLock);

    private Map<Integer, Integer> disabledServerMessages = new HashMap<>();    // reuse owl lock
    private MonitoredReentrantLock srvMessagesLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_SRVMESSAGES);
    private ScheduledFuture<?> srvMessagesSchedule;

    private MonitoredReentrantLock activePetsLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_PETS, true);
    private Map<Integer, Integer> activePets = new LinkedHashMap<>();
    private ScheduledFuture<?> petsSchedule;
    private long petUpdate;

    private MonitoredReentrantLock activeMountsLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_MOUNTS, true);
    private Map<Integer, Integer> activeMounts = new LinkedHashMap<>();
    private ScheduledFuture<?> mountsSchedule;
    private long mountUpdate;

    private MonitoredReentrantLock activePlayerShopsLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_PSHOPS, true);
    private Map<Integer, MaplePlayerShop> activePlayerShops = new LinkedHashMap<>();

    private MonitoredReentrantLock activeMerchantsLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_MERCHS, true);
    private Map<Integer, Pair<MapleHiredMerchant, Integer>> activeMerchants = new LinkedHashMap<>();
    private ScheduledFuture<?> merchantSchedule;
    private long merchantUpdate;

    private Map<Runnable, Long> registeredTimedMapObjects = new LinkedHashMap<>();
    private ScheduledFuture<?> timedMapObjectsSchedule;
    private MonitoredReentrantLock timedMapObjectLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.WORLD_MAPOBJS, true);

    private Map<MapleCharacter, Integer> fishingAttempters = Collections.synchronizedMap(new WeakHashMap<>());

    private ScheduledFuture<?> charactersSchedule;
    private ScheduledFuture<?> marriagesSchedule;
    private ScheduledFuture<?> mapOwnershipSchedule;
    private ScheduledFuture<?> fishingSchedule;
    private ScheduledFuture<?> partySearchSchedule;
    private ScheduledFuture<?> timeoutSchedule;

    public World(int world, int flag, String eventmsg, int exprate, int droprate, int bossdroprate, int mesorate, int questrate, int travelrate, int fishingrate) {
        this.id = world;
        this.flag = flag;
        this.eventmsg = eventmsg;
        this.exprate = exprate;
        this.droprate = droprate;
        this.bossdroprate = bossdroprate;
        this.mesorate = mesorate;
        this.questrate = questrate;
        this.travelrate = travelrate;
        this.fishingrate = fishingrate;
        runningPartyId.set(1000000001); // partyid must not clash with charid to solve update item looting issues, found thanks to Vcoc
        runningMessengerId.set(1);

        petUpdate = Server.getInstance().getCurrentTime();
        mountUpdate = petUpdate;

        for (int i = 0; i < 9; i++) {
            cashItemBought.add(new LinkedHashMap<>());
        }

        TimerManager tman = TimerManager.getInstance();
        petsSchedule = tman.register(new PetFullnessTask(this), 60 * 1000, 60 * 1000);
        srvMessagesSchedule = tman.register(new ServerMessageTask(this), 10 * 1000, 10 * 1000);
        mountsSchedule = tman.register(new MountTirednessTask(this), 60 * 1000, 60 * 1000);
        merchantSchedule = tman.register(new HiredMerchantTask(this), 10 * 60 * 1000, 10 * 60 * 1000);
        timedMapObjectsSchedule = tman.register(new TimedMapObjectTask(this), 60 * 1000, 60 * 1000);
        charactersSchedule = tman.register(new CharacterAutosaverTask(this), 60 * 60 * 1000, 60 * 60 * 1000);
        marriagesSchedule = tman.register(new WeddingReservationTask(this), (long) YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL * 60 * 1000, (long) YamlConfig.config.server.WEDDING_RESERVATION_INTERVAL * 60 * 1000);
        mapOwnershipSchedule = tman.register(new MapOwnershipTask(this), 20 * 1000, 20 * 1000);
        fishingSchedule = tman.register(new FishingTask(this), 10 * 1000, 10 * 1000);
        partySearchSchedule = tman.register(new PartySearchTask(this), 10 * 1000, 10 * 1000);
        timeoutSchedule = tman.register(new TimeoutTask(this), 10 * 1000, 10 * 1000);

        if (YamlConfig.config.server.USE_FAMILY_SYSTEM) {
            long timeLeft = Server.getTimeLeftForNextDay();
            FamilyDailyResetTask.resetEntitlementUsage(this);
            tman.register(new FamilyDailyResetTask(this), 24 * 60 * 60 * 1000, timeLeft);
        }
    }

    private static List<Entry<Integer, SortedMap<Integer, MapleCharacter>>> getSortedAccountCharacterView(Map<Integer, SortedMap<Integer, MapleCharacter>> map) {
        List<Entry<Integer, SortedMap<Integer, MapleCharacter>>> list = new ArrayList<>(map.size());
        list.addAll(map.entrySet());
        list.sort(Comparator.comparingInt(Entry::getKey));
        return list;
    }

    private static Integer getPetKey(MapleCharacter chr, byte petSlot) {    // assuming max 3 pets
        return (chr.getId() << 2) + petSlot;
    }

    private static void executePlayerNpcMapDataUpdate(Connection con, boolean isPodium, Map<Integer, ?> pnpcData, int value, int worldid, int mapid) throws SQLException {
        PreparedStatement ps;
        if (pnpcData.containsKey(mapid)) {
            ps = con.prepareStatement("UPDATE playernpcs_field SET " + (isPodium ? "podium" : "step") + " = ? WHERE world = ? AND map = ?");
        } else {
            ps = con.prepareStatement("INSERT INTO playernpcs_field (" + (isPodium ? "podium" : "step") + ", world, map) VALUES (?, ?, ?)");
        }

        ps.setInt(1, value);
        ps.setInt(2, worldid);
        ps.setInt(3, mapid);
        ps.executeUpdate();
        ps.close();
    }

    private static Pair<Integer, Pair<Integer, Integer>> getRelationshipCoupleFromDb(int id, boolean usingMarriageId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            Integer mid = null, hid = null, wid = null;

            PreparedStatement ps;
            if (usingMarriageId) {
                ps = con.prepareStatement("SELECT * FROM marriages WHERE marriageid = ?");
                ps.setInt(1, id);
            } else {
                ps = con.prepareStatement("SELECT * FROM marriages WHERE husbandid = ? OR wifeid = ?");
                ps.setInt(1, id);
                ps.setInt(2, id);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mid = rs.getInt("marriageid");
                hid = rs.getInt("husbandid");
                wid = rs.getInt("wifeid");
            }

            rs.close();
            ps.close();
            con.close();

            return (mid == null) ? null : new Pair<>(mid, new Pair<>(hid, wid));
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    private static int addRelationshipToDb(int groomId, int brideId) {
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("INSERT INTO marriages (husbandid, wifeid) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, groomId);
            ps.setInt(2, brideId);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int ret = rs.getInt(1);

            rs.close();
            ps.close();
            con.close();
            return ret;
        } catch (SQLException se) {
            se.printStackTrace();
            return -1;
        }
    }

    private static void deleteRelationshipFromDb(int playerId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM marriages WHERE marriageid = ?");
            ps.setInt(1, playerId);
            ps.executeUpdate();

            ps.close();
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public int getChannelsSize() {
        chnRLock.lock();
        try {
            return channels.size();
        } finally {
            chnRLock.unlock();
        }
    }

    public List<Channel> getChannels() {
        chnRLock.lock();
        try {
            return new ArrayList<>(channels);
        } finally {
            chnRLock.unlock();
        }
    }

    public Optional<Channel> getChannel(int channel) {
        chnRLock.lock();
        try {
            if ((channel - 1) < 0 || (channel - 1) >= channels.size()) {
                return Optional.empty();
            }
            return Optional.ofNullable(channels.get(channel - 1));
        } finally {
            chnRLock.unlock();
        }
    }

    public boolean addChannel(Channel channel) {
        chnWLock.lock();
        try {
            if (channel.getId() == channels.size() + 1) {
                channels.add(channel);
                return true;
            } else {
                return false;
            }
        } finally {
            chnWLock.unlock();
        }
    }

    public int removeChannel() {
        Channel ch;
        int chIdx;

        chnRLock.lock();
        try {
            chIdx = channels.size() - 1;
            if (chIdx < 0) {
                return -1;
            }

            ch = channels.get(chIdx);
        } finally {
            chnRLock.unlock();
        }

        if (ch == null || !ch.canUninstall()) {
            return -1;
        }

        chnWLock.lock();
        try {
            if (chIdx == channels.size() - 1) {
                channels.remove(chIdx);
            } else {
                return -1;
            }
        } finally {
            chnWLock.unlock();
        }

        ch.shutdown();
        return ch.getId();
    }

    public boolean canUninstall() {
        if (players.getSize() > 0) {
            return false;
        }

        for (Channel ch : this.getChannels()) {
            if (!ch.canUninstall()) {
                return false;
            }
        }

        return true;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(byte b) {
        this.flag = b;
    }

    public String getEventMessage() {
        return eventmsg;
    }

    public int getExpRate() {
        return exprate;
    }

    public void setExpRate(int exp) {
        Collection<MapleCharacter> list = getPlayerStorage().getAllCharacters();

        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.exprate = exp;
        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getDropRate() {
        return droprate;
    }

    public void setDropRate(int drop) {
        Collection<MapleCharacter> list = getPlayerStorage().getAllCharacters();

        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.droprate = drop;
        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getBossDropRate() {  // boss rate concept thanks to Lapeiro
        return bossdroprate;
    }

    public void setBossDropRate(int bossdrop) {
        bossdroprate = bossdrop;
    }

    public int getMesoRate() {
        return mesorate;
    }

    public void setMesoRate(int meso) {
        Collection<MapleCharacter> list = getPlayerStorage().getAllCharacters();

        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.revertWorldRates();
        }
        this.mesorate = meso;
        for (MapleCharacter chr : list) {
            if (!chr.isLoggedin()) {
                continue;
            }
            chr.setWorldRates();
        }
    }

    public int getQuestRate() {
        return questrate;
    }

    public void setQuestRate(int quest) {
        this.questrate = quest;
    }

    public int getTravelRate() {
        return travelrate;
    }

    public void setTravelRate(int travel) {
        this.travelrate = travel;
    }

    public int getTransportationTime(int travelTime) {
        return (int) Math.ceil(travelTime / travelrate);
    }

    public int getFishingRate() {
        return fishingrate;
    }

    public void setFishingRate(int quest) {
        this.fishingrate = quest;
    }

    public void loadAccountCharactersView(Integer accountId, List<MapleCharacter> chars) {
        SortedMap<Integer, MapleCharacter> charsMap = new TreeMap<>();
        for (MapleCharacter chr : chars) {
            charsMap.put(chr.getId(), chr);
        }

        accountCharsLock.lock();    // accountCharsLock should be used after server's lgnWLock for compliance
        try {
            accountChars.put(accountId, charsMap);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void registerAccountCharacterView(Integer accountId, MapleCharacter chr) {
        accountCharsLock.lock();
        try {
            accountChars.get(accountId).put(chr.getId(), chr);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void unregisterAccountCharacterView(Integer accountId, Integer chrId) {
        accountCharsLock.lock();
        try {
            accountChars.get(accountId).remove(chrId);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void clearAccountCharacterView(Integer accountId) {
        accountCharsLock.lock();
        try {
            SortedMap<Integer, MapleCharacter> accChars = accountChars.remove(accountId);
            if (accChars != null) {
                accChars.clear();
            }
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void loadAccountStorage(Integer accountId) {
        if (getAccountStorage(accountId) == null) {
            registerAccountStorage(accountId);
        }
    }

    private void registerAccountStorage(Integer accountId) {
        MapleStorage storage = MapleStorage.loadOrCreateFromDB(accountId, this.id);
        accountCharsLock.lock();
        try {
            accountStorages.put(accountId, storage);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public void unregisterAccountStorage(Integer accountId) {
        accountCharsLock.lock();
        try {
            accountStorages.remove(accountId);
        } finally {
            accountCharsLock.unlock();
        }
    }

    public MapleStorage getAccountStorage(Integer accountId) {
        return accountStorages.get(accountId);
    }

    public List<MapleCharacter> loadAndGetAllCharactersView() {
        Server.getInstance().loadAllAccountsCharactersView();
        return getAllCharactersView();
    }

    public List<MapleCharacter> getAllCharactersView() {
        List<MapleCharacter> chrList = new LinkedList<>();
        Map<Integer, SortedMap<Integer, MapleCharacter>> accChars;

        accountCharsLock.lock();
        try {
            accChars = new HashMap<>(accountChars);
        } finally {
            accountCharsLock.unlock();
        }

        for (Entry<Integer, SortedMap<Integer, MapleCharacter>> e : getSortedAccountCharacterView(accChars)) {
            chrList.addAll(e.getValue().values());
        }

        return chrList;
    }

    public List<MapleCharacter> getAccountCharactersView(Integer accountId) {
        List<MapleCharacter> chrList;

        accountCharsLock.lock();
        try {
            SortedMap<Integer, MapleCharacter> accChars = accountChars.get(accountId);

            if (accChars != null) {
                chrList = new LinkedList<>(accChars.values());
            } else {
                accountChars.put(accountId, new TreeMap<>());
                chrList = null;
            }
        } finally {
            accountCharsLock.unlock();
        }

        return chrList;
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public MapleMatchCheckerCoordinator getMatchCheckerCoordinator() {
        return matchChecker;
    }

    public MaplePartySearchCoordinator getPartySearchCoordinator() {
        return partySearch;
    }

    public void addPlayer(MapleCharacter chr) {
        players.addPlayer(chr);
    }

    public void removePlayer(MapleCharacter chr) {
        Channel cserv = chr.getClient().getChannelServer();

        if (cserv != null) {
            if (!cserv.removePlayer(chr)) {
                for (Channel ch : getChannels()) {
                    if (ch.removePlayer(chr)) {
                        break;
                    }
                }
            }
        }

        players.removePlayer(chr.getId());
    }

    public int getId() {
        return id;
    }

    public void addFamily(int id, MapleFamily f) {
        synchronized (families) {
            if (!families.containsKey(id)) {
                families.put(id, f);
            }
        }
    }

    public void removeFamily(int id) {
        synchronized (families) {
            families.remove(id);
        }
    }

    public MapleFamily getFamily(int id) {
        synchronized (families) {
            if (families.containsKey(id)) {
                return families.get(id);
            }
            return null;
        }
    }

    public Collection<MapleFamily> getFamilies() {
        synchronized (families) {
            return Collections.unmodifiableCollection(families.values());
        }
    }

    public Optional<MapleGuild> getGuild(MapleGuildCharacter mgc) {
        if (mgc == null) {
            return Optional.empty();
        }

        int gid = mgc.getGuildId();
        Optional<MapleGuild> g = Server.getInstance().getGuild(gid, mgc.getWorld(), mgc.getCharacter());
        if (g.isPresent() && gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g.get()));
        }
        return g;
    }

    public boolean isWorldCapacityFull() {
        return getWorldCapacityStatus() == 2;
    }

    public int getWorldCapacityStatus() {
        int worldCap = getChannelsSize() * YamlConfig.config.server.CHANNEL_LOAD;
        int num = players.getSize();

        int status;
        if (num >= worldCap) {
            status = 2;
        } else if (num >= worldCap * .8) { // More than 80 percent o___o
            status = 1;
        } else {
            status = 0;
        }

        return status;
    }

    public MapleGuildSummary getGuildSummary(int gid, int wid) {
        if (!gsStore.containsKey(gid)) {
            Server.getInstance().getGuild(gid, wid, null).ifPresent(g -> gsStore.put(gid, new MapleGuildSummary(g)));
        }
        return gsStore.get(gid);
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        Server server = Server.getInstance();
        for (int i : gsStore.keySet()) {
            Optional<MapleGuild> g = server.getGuild(i, getId(), null);
            if (g.isPresent()) {
                gsStore.put(i, new MapleGuildSummary(g.get()));
            } else {
                gsStore.remove(i);
            }
        }
    }

    public void setGuildAndRank(List<Integer> characterIds, int guildId, int rank, int exception) {
        characterIds.stream()
                .filter(id -> id != exception)
                .forEach(id -> setGuildAndRank(id, guildId, rank));
    }

    public void setOfflineGuildStatus(int guildid, int guildrank, int cid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
                ps.setInt(1, guildid);
                ps.setInt(2, guildrank);
                ps.setInt(3, cid);
                ps.execute();
            }

            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void setGuildAndRank(int cid, int guildid, int rank) {
        getPlayerStorage().getCharacterById(cid).ifPresent(c -> setGuildAndRank(c, guildid, rank));
    }

    private void setGuildAndRank(MapleCharacter character, int guildId, int rank) {
        boolean bDifferentGuild;
        if (guildId == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildId != character.getGuildId();
            character.getMGC().ifPresent(mgc -> mgc.setGuildId(guildId));
            character.getMGC().ifPresent(mgc -> mgc.setGuildRank(rank));

            if (bDifferentGuild) {
                character.getMGC().ifPresent(mgc -> mgc.setAllianceRank(5));
            }

            character.saveGuildStatus();
        }
        if (bDifferentGuild) {
            if (character.isLoggedinWorld()) {
                Optional<MapleGuild> guild = Server.getInstance().getGuild(guildId);
                if (guild.isPresent()) {
                    character.getMap().broadcastMessage(character, MaplePacketCreator.guildNameChanged(character.getId(), guild.get().getName()));
                    character.getMap().broadcastMessage(character, MaplePacketCreator.guildMarkChanged(character.getId(), guild.get()));
                } else {
                    character.getMap().broadcastMessage(character, MaplePacketCreator.guildNameChanged(character.getId(), ""));
                }
            }
        }
    }

    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) {
        updateGuildSummary(gid, mgs);
        sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        setGuildAndRank(affectedPlayers, -1, -1, -1);    //respawn player
    }

    public void sendPacket(List<Integer> targetIds, final byte[] packet, int exception) {
        targetIds.stream()
                .filter(i -> i != exception)
                .map(i -> getPlayerStorage().getCharacterById(i))
                .flatMap(Optional::stream)
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public boolean isGuildQueued(int guildId) {
        return queuedGuilds.contains(guildId);
    }

    public void putGuildQueued(int guildId) {
        queuedGuilds.add(guildId);
    }

    public void removeGuildQueued(int guildId) {
        queuedGuilds.remove(guildId);
    }

    public boolean isMarriageQueued(int marriageId) {
        return queuedMarriages.containsKey(marriageId);
    }

    public Pair<Boolean, Boolean> getMarriageQueuedLocation(int marriageId) {
        Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>> qm = queuedMarriages.get(marriageId);
        return (qm != null) ? qm.getLeft() : null;
    }

    public Pair<Integer, Integer> getMarriageQueuedCouple(int marriageId) {
        Pair<Pair<Boolean, Boolean>, Pair<Integer, Integer>> qm = queuedMarriages.get(marriageId);
        return (qm != null) ? qm.getRight() : null;
    }

    public void putMarriageQueued(int marriageId, boolean cathedral, boolean premium, int groomId, int brideId) {
        queuedMarriages.put(marriageId, new Pair<>(new Pair<>(cathedral, premium), new Pair<>(groomId, brideId)));
        marriageGuests.put(marriageId, new HashSet<>());
    }

    public Pair<Boolean, Set<Integer>> removeMarriageQueued(int marriageId) {
        Boolean type = queuedMarriages.remove(marriageId).getLeft().getRight();
        Set<Integer> guests = marriageGuests.remove(marriageId);

        return new Pair<>(type, guests);
    }

    public boolean addMarriageGuest(int marriageId, int playerId) {
        Set<Integer> guests = marriageGuests.get(marriageId);
        if (guests != null) {
            if (guests.contains(playerId)) {
                return false;
            }

            guests.add(playerId);
            return true;
        }

        return false;
    }

    public Pair<Integer, Integer> getWeddingCoupleForGuest(int guestId, Boolean cathedral) {
        for (Channel ch : getChannels()) {
            Pair<Integer, Integer> p = ch.getWeddingCoupleForGuest(guestId, cathedral);
            if (p != null) {
                return p;
            }
        }

        List<Integer> possibleWeddings = new LinkedList<>();
        for (Entry<Integer, Set<Integer>> mg : new HashSet<>(marriageGuests.entrySet())) {
            if (mg.getValue().contains(guestId)) {
                Pair<Boolean, Boolean> loc = getMarriageQueuedLocation(mg.getKey());
                if (loc != null && cathedral.equals(loc.getLeft())) {
                    possibleWeddings.add(mg.getKey());
                }
            }
        }

        int pwSize = possibleWeddings.size();
        if (pwSize == 0) {
            return null;
        } else if (pwSize > 1) {
            int selectedPw = -1;
            int selectedPos = Integer.MAX_VALUE;

            for (Integer pw : possibleWeddings) {
                for (Channel ch : getChannels()) {
                    int pos = ch.getWeddingReservationStatus(pw, cathedral);
                    if (pos != -1) {
                        if (pos < selectedPos) {
                            selectedPos = pos;
                            selectedPw = pw;
                            break;
                        }
                    }
                }
            }

            if (selectedPw == -1) {
                return null;
            }

            possibleWeddings.clear();
            possibleWeddings.add(selectedPw);
        }

        return getMarriageQueuedCouple(possibleWeddings.get(0));
    }

    public void debugMarriageStatus() {
        System.out.println("Queued marriages: " + queuedMarriages);
        System.out.println("Guest list: " + marriageGuests);
    }

    private void registerCharacterParty(Integer chrid, Integer partyid) {
        partyLock.lock();
        try {
            partyChars.put(chrid, partyid);
        } finally {
            partyLock.unlock();
        }
    }

    private void unregisterCharacterPartyInternal(Integer chrid) {
        partyChars.remove(chrid);
    }

    private void unregisterCharacterParty(Integer chrid) {
        partyLock.lock();
        try {
            unregisterCharacterPartyInternal(chrid);
        } finally {
            partyLock.unlock();
        }
    }

    public Integer getCharacterPartyid(Integer chrid) {
        partyLock.lock();
        try {
            return partyChars.get(chrid);
        } finally {
            partyLock.unlock();
        }
    }

    public MapleParty createParty(MaplePartyCharacter chrfor) {
        int partyid = runningPartyId.getAndIncrement();
        MapleParty party = new MapleParty(partyid, chrfor);

        partyLock.lock();
        try {
            parties.put(party.getId(), party);
            registerCharacterParty(chrfor.getId(), partyid);
        } finally {
            partyLock.unlock();
        }

        party.addMember(chrfor);
        return party;
    }

    public Optional<MapleParty> getParty(int partyid) {
        partyLock.lock();
        try {
            return Optional.ofNullable(parties.get(partyid));
        } finally {
            partyLock.unlock();
        }
    }

    private MapleParty disbandParty(int partyid) {
        partyLock.lock();
        try {
            return parties.remove(partyid);
        } finally {
            partyLock.unlock();
        }
    }

    private void updateCharacterParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target, Collection<MaplePartyCharacter> partyMembers) {
        switch (operation) {
            case JOIN -> registerCharacterParty(target.getId(), party.getId());
            case LEAVE, EXPEL -> unregisterCharacterParty(target.getId());
            case DISBAND -> disbandCharacterParty(partyMembers);
        }
    }

    private void disbandCharacterParty(Collection<MaplePartyCharacter> partyMembers) {
        partyLock.lock();
        try {
            partyMembers.stream().map(MaplePartyCharacter::getId).forEach(this::unregisterCharacterParty);
        } finally {
            partyLock.unlock();
        }
    }

    private void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) {
        Collection<MaplePartyCharacter> partyMembers = party.getMembers();
        updateCharacterParty(party, operation, target, partyMembers);

        for (MaplePartyCharacter partychar : partyMembers) {
            Optional<MapleCharacter> chr = getPlayerStorage().getCharacterById(partychar.getId());
            if (chr.isPresent()) {
                if (operation == PartyOperation.DISBAND) {
                    chr.get().setParty(null);
                    chr.get().setMPC(null);
                } else {
                    chr.get().setParty(party);
                    chr.get().setMPC(partychar);
                }
                chr.get().announce(MaplePacketCreator.updateParty(chr.get().getClient().getChannel(), party, operation, target));
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                Optional<MapleCharacter> chr = getPlayerStorage().getCharacterById(target.getId());
                if (chr.isPresent()) {
                    chr.get().announce(MaplePacketCreator.updateParty(chr.get().getClient().getChannel(), party, operation, target));
                    chr.get().setParty(null);
                    chr.get().setMPC(null);
                }
            default:
                break;
        }
    }

    public void updateParty(int partyId, PartyOperation operation, MaplePartyCharacter target) {
        Optional<MapleParty> party = getParty(partyId);
        if (party.isEmpty()) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.ifPresent(p -> p.addMember(target));
                break;
            case EXPEL:
            case LEAVE:
                party.ifPresent(p -> p.removeMember(target));
                break;
            case DISBAND:
                disbandParty(partyId);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.ifPresent(p -> p.updateMember(target));
                break;
            case CHANGE_LEADER:
                Optional<MapleCharacter> mc = party.map(MapleParty::getLeader).flatMap(MaplePartyCharacter::getPlayer);
                if (mc.isPresent()) {
                    EventInstanceManager eim = mc.get().getEventInstance().orElse(null);

                    if (eim != null && eim.isEventLeader(mc.get())) {
                        eim.changedLeader(target);
                    } else {
                        int oldLeaderMapid = mc.get().getMapId();

                        if (MapleMiniDungeonInfo.isDungeonMap(oldLeaderMapid)) {
                            if (oldLeaderMapid != target.getMapId()) {
                                MapleMiniDungeon mmd = mc.get().getClient().getChannelServer().getMiniDungeon(oldLeaderMapid);
                                if (mmd != null) {
                                    mmd.close();
                                }
                            }
                        }
                    }
                    party.get().setLeader(target);
                }
                break;
            default:
                System.out.println("Unhandled updateParty operation " + operation.name());
        }
        party.ifPresent(p -> updateParty(p, operation, target));
    }

    public void removeMapPartyMembers(int partyId) {
        Optional<MapleParty> party = getParty(partyId);
        if (party.isEmpty()) {
            return;
        }

        party.map(MapleParty::getMembers)
                .orElse(Collections.emptyList()).stream()
                .map(MaplePartyCharacter::getPlayer)
                .flatMap(Optional::stream)
                .map(AbstractMapleCharacterObject::getMap)
                .filter(Objects::nonNull)
                .forEach(m -> m.removeParty(partyId));
    }

    public int find(String name) {
        return getPlayerStorage().getCharacterByName(name)
                .map(MapleCharacter::getClient)
                .map(MapleClient::getChannel)
                .orElse(-1);
    }

    public int find(int id) {
        return getPlayerStorage().getCharacterById(id)
                .map(MapleCharacter::getClient)
                .map(MapleClient::getChannel)
                .orElse(-1);
    }

    public void partyChat(MapleParty party, String chatText, String nameFrom) {
        byte[] packet = MaplePacketCreator.multiChat(nameFrom, chatText, 1);
        party.getMembers().stream()
                .map(MaplePartyCharacter::getName)
                .filter(name -> !name.equals(nameFrom))
                .map(name -> getPlayerStorage().getCharacterByName(name))
                .flatMap(Optional::stream)
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chatText) {
        byte[] packet = MaplePacketCreator.multiChat(nameFrom, chatText, 0);
        Arrays.stream(recipientCharacterIds)
                .mapToObj(id -> getPlayerStorage().getCharacterById(id))
                .flatMap(Optional::stream)
                .filter(c -> c.getBuddylist().containsVisible(cidFrom))
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (Channel ch : getChannels()) {
            for (int charid : ch.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, ch.getId()));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[0]);
    }

    public Optional<MapleMessenger> getMessenger(int messengerId) {
        return Optional.ofNullable(messengers.get(messengerId));
    }

    public void leaveMessenger(int messengerId, String name) {
        Optional<MapleMessenger> messenger = getMessenger(messengerId);
        if (messenger.isEmpty()) {
            throw new IllegalArgumentException("No messenger with the specified messengerId exists");
        }
        leaveMessenger(messenger.get(), name);
    }

    private void leaveMessenger(MapleMessenger messenger, String name) {
        int position = messenger.getPositionByName(name);
        messenger.removeMember(name);
        removeMessengerPlayer(messenger, position);
    }

    public void messengerInvite(String sender, int messengerId, String target, int fromChannel) {
        if (isConnected(target)) {
            Optional<MapleCharacter> targetChr = getPlayerStorage().getCharacterByName(target);
            if (targetChr.isPresent()) {
                Optional<MapleMessenger> messenger = targetChr.get().getMessenger();
                Optional<MapleCharacter> from = getChannel(fromChannel).flatMap(c -> c.getPlayerStorage().getCharacterByName(sender));
                if (messenger.isEmpty()) {
                    if (from.isPresent()) {
                        if (MapleInviteCoordinator.createInvite(InviteType.MESSENGER, from.get(), messengerId, targetChr.get().getId())) {
                            targetChr.get().announce(MaplePacketCreator.messengerInvite(sender, messengerId));
                            from.get().announce(MaplePacketCreator.messengerNote(target, 4, 1));
                        } else {
                            from.get().announce(MaplePacketCreator.messengerChat(sender + " : " + target + " is already managing a Maple Messenger invitation"));
                        }
                    }
                } else {
                    from.ifPresent(character -> character.announce(MaplePacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger")));
                }
            }
        }
    }

    public void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            Optional<MapleCharacter> chr = getPlayerStorage().getCharacterByName(messengerchar.name());
            if (chr.isEmpty()) {
                continue;
            }
            if (!messengerchar.name().equals(namefrom)) {
                Optional<MapleCharacter> from = getChannel(fromchannel).flatMap(c -> c.getPlayerStorage().getCharacterByName(namefrom));
                chr.get().announce(MaplePacketCreator.addMessengerPlayer(namefrom, from.get(), position, (byte) (fromchannel - 1)));
                from.get().announce(MaplePacketCreator.addMessengerPlayer(chr.get().getName(), chr.get(), messengerchar.position(), (byte) (messengerchar.channelId() - 1)));
            } else {
                chr.get().announce(MaplePacketCreator.joinMessenger(messengerchar.position()));
            }
        }
    }

    public void removeMessengerPlayer(MapleMessenger messenger, int position) {
        byte[] packet = MaplePacketCreator.removeMessengerPlayer(position);
        messenger.getMembersStream()
                .map(MapleMessengerCharacter::name)
                .map(name -> getPlayerStorage().getCharacterByName(name))
                .flatMap(Optional::stream)
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public void messengerChat(MapleMessenger messenger, String text, String nameFrom) {
        byte[] packet = MaplePacketCreator.messengerChat(text);
        messenger.getOtherMembersStream(nameFrom)
                .map(MapleMessengerCharacter::name)
                .map(name -> getPlayerStorage().getCharacterByName(name))
                .flatMap(Optional::stream)
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public void declineChat(String senderName, MapleCharacter player) {
        if (isConnected(senderName)) {
            Optional<MapleCharacter> senderChr = getPlayerStorage().getCharacterByName(senderName);
            Optional<MapleMessenger> messenger = senderChr.flatMap(MapleCharacter::getMessenger);
            if (senderChr.isPresent() && messenger.isPresent()) {
                if (MapleInviteCoordinator.answerInvite(InviteType.MESSENGER, player.getId(), messenger.get().getId(), false).result == InviteResult.DENIED) {
                    senderChr.get().announce(MaplePacketCreator.messengerNote(player.getName(), 5, 0));
                }
            }
        }
    }

    public void updateMessenger(int messengerId, String nameFrom, int fromChannel) {
        getMessenger(messengerId).ifPresent(m -> updateMessenger(m, nameFrom, fromChannel));
    }

    private void updateMessenger(MapleMessenger messenger, String nameFrom, int fromChannel) {
        int position = messenger.getPositionByName(nameFrom);
        updateMessenger(messenger, nameFrom, position, fromChannel);
    }

    public void updateMessenger(MapleMessenger messenger, String nameFrom, int position, int fromChannelId) {
        Optional<Channel> fromChannel = getChannel(fromChannelId);
        if (fromChannel.isEmpty()) {
            return;
        }

        Optional<MapleCharacter> fromCharacter = fromChannel.flatMap(c -> c.getPlayerStorage().getCharacterByName(nameFrom));
        if (fromCharacter.isEmpty()) {
            return;
        }

        byte[] packet = MaplePacketCreator.updateMessengerPlayer(nameFrom, fromCharacter.get(), position, (byte) (fromChannelId - 1));
        messenger.getOtherMembersStream(nameFrom)
                .map(MapleMessengerCharacter::name)
                .map(n -> fromChannel.flatMap(c -> c.getPlayerStorage().getCharacterByName(n)))
                .flatMap(Optional::stream)
                .forEach(MapleCharacter.announcePacket(packet));
    }

    public void joinMessenger(int messengerId, int targetId, String targetName, String inviteFromName, int fromChannel) {
        Optional<MapleMessenger> messenger = getMessenger(messengerId);
        if (messenger.isEmpty()) {
            throw new IllegalArgumentException("No messenger with the specified messengerId exists");
        }

        MapleMessengerCharacter messengerCharacter = messenger.get().addMember(targetId, targetName, fromChannel);
        addMessengerPlayer(messenger.get(), inviteFromName, fromChannel, messengerCharacter.position());
    }

    public void silentJoinMessenger(int messengerId, int targetId, String targetName, int channelId) {
        Optional<MapleMessenger> messenger = getMessenger(messengerId);
        if (messenger.isEmpty()) {
            throw new IllegalArgumentException("No messenger with the specified messengerId exists");
        }
        messenger.get().addMember(targetId, targetName, channelId);
    }

    public MapleMessenger createMessenger(int creatorId, String creatorName, int channelId) {
        int messengerId = runningMessengerId.getAndIncrement();
        MapleMessenger messenger = new MapleMessenger(messengerId);
        messenger.addMember(creatorId, creatorName, channelId);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public boolean isConnected(String charName) {
        return getPlayerStorage().getCharacterByName(charName).isPresent();
    }

    public void whisper(String sender, String target, int channel, String message) {
        if (isConnected(target)) {
            getPlayerStorage().getCharacterByName(target)
                    .ifPresent(c -> c.announce(MaplePacketCreator.getWhisper(sender, channel, message)));
        }
    }

    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        Optional<MapleCharacter> addChar = getPlayerStorage().getCharacterByName(addName);
        if (addChar.isEmpty()) {
            return BuddyAddResult.OK;
        }
        return requestBuddyAdd(addChar.get(), channelFrom, cidFrom, nameFrom);
    }

    private BuddyAddResult requestBuddyAdd(MapleCharacter character, int channelFrom, int characterIdFrom, String nameFrom) {
        BuddyList buddylist = character.getBuddylist();
        if (buddylist.isFull()) {
            return BuddyAddResult.BUDDYLIST_FULL;
        }
        if (!buddylist.contains(characterIdFrom)) {
            buddylist.addBuddyRequest(character.getClient(), characterIdFrom, nameFrom, channelFrom);
            return BuddyAddResult.OK;
        }
        if (buddylist.containsVisible(characterIdFrom)) {
            return BuddyAddResult.ALREADY_ON_LIST;
        }
        return BuddyAddResult.OK;
    }

    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        getPlayerStorage().getCharacterById(cid).ifPresent(c -> buddyChanged(c, cidFrom, name, channel, operation));
    }

    private void buddyChanged(MapleCharacter character, int characterIdFrom, String name, int channelId, BuddyOperation operation) {
        switch (operation) {
            case ADDED -> buddyAdded(characterIdFrom, name, channelId, character);
            case DELETED -> buddyDeleted(characterIdFrom, name, character);
        }
    }

    private static void buddyDeleted(int cidFrom, String name, MapleCharacter addChar) {
        if (addChar.getBuddylist().contains(cidFrom)) {
            Optional<BuddylistEntry> entry = addChar.getBuddylist().get(cidFrom);
            addChar.getBuddylist().put(new BuddylistEntry(name, "Default Group", cidFrom, (byte) -1, entry.map(BuddylistEntry::isVisible).orElse(false)));
            addChar.announce(MaplePacketCreator.updateBuddyChannel(cidFrom, (byte) -1));
        }
    }

    private static void buddyAdded(int cidFrom, String name, int channel, MapleCharacter addChar) {
        if (addChar.getBuddylist().contains(cidFrom)) {
            addChar.getBuddylist().put(new BuddylistEntry(name, "Default Group", cidFrom, channel, true));
            addChar.announce(MaplePacketCreator.updateBuddyChannel(cidFrom, (byte) (channel - 1)));
        }
    }

    public void loggedOff(int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, true);
    }

    public void loggedOn(int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        for (int buddy : buddies) {
            Optional<MapleCharacter> chr = getPlayerStorage().getCharacterById(buddy);
            if (chr.isPresent()) {
                Optional<BuddylistEntry> ble = chr.get().getBuddylist().get(characterId);
                if (ble.isEmpty() || !ble.get().isVisible()) {
                    continue;
                }

                int mcChannel;
                if (offline) {
                    ble.get().setChannel((byte) -1);
                    mcChannel = -1;
                } else {
                    ble.get().setChannel(channel);
                    mcChannel = (byte) (channel - 1);
                }
                chr.get().getBuddylist().put(ble.get());
                chr.get().announce(MaplePacketCreator.updateBuddyChannel(ble.get().getCharacterId(), mcChannel));
            }
        }
    }

    public void addOwlItemSearch(Integer itemid) {
        suggestWLock.lock();
        try {
            owlSearched.merge(itemid, 1, Integer::sum);
        } finally {
            suggestWLock.unlock();
        }
    }

    public List<OwlSearchResult> getOwlSearchedItems() {
        if (YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            return Collections.emptyList();
        }

        suggestRLock.lock();
        try {
            return owlSearched.entrySet().stream()
                    .map(e -> new OwlSearchResult(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } finally {
            suggestRLock.unlock();
        }
    }

    public void addCashItemBought(Integer snid) {
        suggestWLock.lock();
        try {
            Map<Integer, Integer> tabItemBought = cashItemBought.get(snid / 10000000);
            tabItemBought.merge(snid, 1, Integer::sum);
        } finally {
            suggestWLock.unlock();
        }
    }

    private List<List<Pair<Integer, Integer>>> getBoughtCashItems() {
        if (YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            List<List<Pair<Integer, Integer>>> boughtCounts = new ArrayList<>(9);

            // thanks GabrielSin for pointing out an issue here
            for (int i = 0; i < 9; i++) {
                List<Pair<Integer, Integer>> tabCounts = new ArrayList<>(0);
                boughtCounts.add(tabCounts);
            }

            return boughtCounts;
        }

        suggestRLock.lock();
        try {
            List<List<Pair<Integer, Integer>>> boughtCounts = new ArrayList<>(cashItemBought.size());

            for (Map<Integer, Integer> tab : cashItemBought) {
                List<Pair<Integer, Integer>> tabItems = new LinkedList<>();
                boughtCounts.add(tabItems);

                for (Entry<Integer, Integer> e : tab.entrySet()) {
                    tabItems.add(new Pair<>(e.getKey(), e.getValue()));
                }
            }

            return boughtCounts;
        } finally {
            suggestRLock.unlock();
        }
    }

    private List<Integer> getMostSellerOnTab(List<Pair<Integer, Integer>> tabSellers) {
        Comparator<Pair<Integer, Integer>> comparator = Comparator.comparing(Pair::getRight);
        return tabSellers.stream()
                .sorted(comparator.reversed())
                .limit(Math.min(tabSellers.size(), 5))
                .map(Pair::getLeft)
                .collect(Collectors.toList());
    }

    public List<List<Integer>> getMostSellerCashItems() {
        List<List<Pair<Integer, Integer>>> mostSellers = this.getBoughtCashItems();
        List<List<Integer>> cashLeaderboards = new ArrayList<>(9);
        List<Integer> tabLeaderboards;
        List<Integer> allLeaderboards = null;

        for (List<Pair<Integer, Integer>> tabSellers : mostSellers) {
            if (tabSellers.size() < 5) {
                if (allLeaderboards == null) {
                    List<Pair<Integer, Integer>> allSellers = new LinkedList<>();
                    for (List<Pair<Integer, Integer>> tabItems : mostSellers) {
                        allSellers.addAll(tabItems);
                    }

                    allLeaderboards = getMostSellerOnTab(allSellers);
                }

                tabLeaderboards = new LinkedList<>();
                if (allLeaderboards.size() < 5) {
                    for (int i : GameConstants.CASH_DATA) {
                        tabLeaderboards.add(i);
                    }
                } else {
                    tabLeaderboards.addAll(allLeaderboards);
                }
            } else {
                tabLeaderboards = getMostSellerOnTab(tabSellers);
            }

            cashLeaderboards.add(tabLeaderboards);
        }

        return cashLeaderboards;
    }

    public void registerPetHunger(MapleCharacter chr, byte petSlot) {
        if (chr.isGM() && YamlConfig.config.server.GM_PETS_NEVER_HUNGRY || YamlConfig.config.server.PETS_NEVER_HUNGRY) {
            return;
        }

        Integer key = getPetKey(chr, petSlot);

        activePetsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - petUpdate > 55000) {
                initProc = YamlConfig.config.server.PET_EXHAUST_COUNT - 2;
            } else {
                initProc = YamlConfig.config.server.PET_EXHAUST_COUNT - 1;
            }

            activePets.put(key, initProc);
        } finally {
            activePetsLock.unlock();
        }
    }

    public void unregisterPetHunger(MapleCharacter chr, byte petSlot) {
        Integer key = getPetKey(chr, petSlot);

        activePetsLock.lock();
        try {
            activePets.remove(key);
        } finally {
            activePetsLock.unlock();
        }
    }

    public void runPetSchedule() {
        Map<Integer, Integer> deployedPets;

        activePetsLock.lock();
        try {
            petUpdate = Server.getInstance().getCurrentTime();
            deployedPets = new HashMap<>(activePets);   // exception here found thanks to MedicOP
        } finally {
            activePetsLock.unlock();
        }

        for (Map.Entry<Integer, Integer> dp : deployedPets.entrySet()) {
            Optional<MapleCharacter> chr = this.getPlayerStorage().getCharacterById(dp.getKey() / 4);
            if (chr.isEmpty() || !chr.get().isLoggedinWorld()) {
                continue;
            }

            int dpVal = dp.getValue() + 1;
            if (dpVal == YamlConfig.config.server.PET_EXHAUST_COUNT) {
                chr.get().runFullnessSchedule(dp.getKey() % 4);
                dpVal = 0;
            }

            activePetsLock.lock();
            try {
                activePets.put(dp.getKey(), dpVal);
            } finally {
                activePetsLock.unlock();
            }
        }
    }

    public void registerMountHunger(MapleCharacter chr) {
        if (chr.isGM() && YamlConfig.config.server.GM_PETS_NEVER_HUNGRY || YamlConfig.config.server.PETS_NEVER_HUNGRY) {
            return;
        }

        Integer key = chr.getId();
        activeMountsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - mountUpdate > 45000) {
                initProc = YamlConfig.config.server.MOUNT_EXHAUST_COUNT - 2;
            } else {
                initProc = YamlConfig.config.server.MOUNT_EXHAUST_COUNT - 1;
            }

            activeMounts.put(key, initProc);
        } finally {
            activeMountsLock.unlock();
        }
    }

    public void unregisterMountHunger(MapleCharacter chr) {
        Integer key = chr.getId();

        activeMountsLock.lock();
        try {
            activeMounts.remove(key);
        } finally {
            activeMountsLock.unlock();
        }
    }

    public void runMountSchedule() {
        Map<Integer, Integer> deployedMounts;
        activeMountsLock.lock();
        try {
            mountUpdate = Server.getInstance().getCurrentTime();
            deployedMounts = new HashMap<>(activeMounts);
        } finally {
            activeMountsLock.unlock();
        }

        for (Map.Entry<Integer, Integer> dp : deployedMounts.entrySet()) {
            Optional<MapleCharacter> chr = this.getPlayerStorage().getCharacterById(dp.getKey());
            if (chr.isEmpty() || !chr.get().isLoggedinWorld()) {
                continue;
            }

            int dpVal = dp.getValue() + 1;
            if (dpVal == YamlConfig.config.server.MOUNT_EXHAUST_COUNT) {
                if (!chr.get().runTirednessSchedule()) {
                    continue;
                }
                dpVal = 0;
            }

            activeMountsLock.lock();
            try {
                activeMounts.put(dp.getKey(), dpVal);
            } finally {
                activeMountsLock.unlock();
            }
        }
    }

    public void registerPlayerShop(MaplePlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.put(ps.getOwner().getId(), ps);
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public void unregisterPlayerShop(MaplePlayerShop ps) {
        activePlayerShopsLock.lock();
        try {
            activePlayerShops.remove(ps.getOwner().getId());
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public List<MaplePlayerShop> getActivePlayerShops() {
        activePlayerShopsLock.lock();
        try {
            return new ArrayList<>(activePlayerShops.values());
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public Optional<MaplePlayerShop> getPlayerShop(int ownerid) {
        activePlayerShopsLock.lock();
        try {
            return Optional.ofNullable(activePlayerShops.get(ownerid));
        } finally {
            activePlayerShopsLock.unlock();
        }
    }

    public void registerHiredMerchant(MapleHiredMerchant hm) {
        activeMerchantsLock.lock();
        try {
            int initProc;
            if (Server.getInstance().getCurrentTime() - merchantUpdate > 5 * 60 * 1000) {
                initProc = 1;
            } else {
                initProc = 0;
            }

            activeMerchants.put(hm.getOwnerId(), new Pair<>(hm, initProc));
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void unregisterHiredMerchant(MapleHiredMerchant hm) {
        activeMerchantsLock.lock();
        try {
            activeMerchants.remove(hm.getOwnerId());
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void runHiredMerchantSchedule() {
        Map<Integer, Pair<MapleHiredMerchant, Integer>> deployedMerchants;
        activeMerchantsLock.lock();
        try {
            merchantUpdate = Server.getInstance().getCurrentTime();
            deployedMerchants = new LinkedHashMap<>(activeMerchants);

            for (Map.Entry<Integer, Pair<MapleHiredMerchant, Integer>> dm : deployedMerchants.entrySet()) {
                int timeOn = dm.getValue().getRight();
                MapleHiredMerchant hm = dm.getValue().getLeft();

                if (timeOn <= 144) {   // 1440 minutes == 24hrs
                    activeMerchants.put(hm.getOwnerId(), new Pair<>(dm.getValue().getLeft(), timeOn + 1));
                } else {
                    hm.forceClose();
                    getChannel(hm.getChannel()).ifPresent(c -> c.removeHiredMerchant(hm.getOwnerId()));

                    activeMerchants.remove(dm.getKey());
                }
            }
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public List<MapleHiredMerchant> getActiveMerchants() {
        activeMerchantsLock.lock();
        try {
            return activeMerchants.values().stream()
                    .map(Pair::getLeft)
                    .filter(MapleHiredMerchant::isOpen)
                    .collect(Collectors.toList());
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public Optional<MapleHiredMerchant> getHiredMerchant(int ownerId) {
        activeMerchantsLock.lock();
        try {
            if (activeMerchants.containsKey(ownerId)) {
                return Optional.ofNullable(activeMerchants.get(ownerId).getLeft());
            }

            return Optional.empty();
        } finally {
            activeMerchantsLock.unlock();
        }
    }

    public void registerTimedMapObject(Runnable r, long duration) {
        timedMapObjectLock.lock();
        try {
            long expirationTime = Server.getInstance().getCurrentTime() + duration;
            registeredTimedMapObjects.put(r, expirationTime);
        } finally {
            timedMapObjectLock.unlock();
        }
    }

    public void runTimedMapObjectSchedule() {
        List<Runnable> toRemove;

        timedMapObjectLock.lock();
        try {
            long timeNow = Server.getInstance().getCurrentTime();
            toRemove = registeredTimedMapObjects.entrySet().stream()
                    .filter(e -> e.getValue() <= timeNow)
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
            toRemove.forEach(registeredTimedMapObjects::remove);
        } finally {
            timedMapObjectLock.unlock();
        }

        toRemove.forEach(Runnable::run);
    }

    public void resetDisabledServerMessages() {
        srvMessagesLock.lock();
        try {
            disabledServerMessages.clear();
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public boolean registerDisabledServerMessage(int characterId) {
        srvMessagesLock.lock();
        try {
            boolean alreadyDisabled = disabledServerMessages.containsKey(characterId);
            disabledServerMessages.put(characterId, 0);

            return alreadyDisabled;
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public boolean unregisterDisabledServerMessage(int characterId) {
        srvMessagesLock.lock();
        try {
            return disabledServerMessages.remove(characterId) != null;
        } finally {
            srvMessagesLock.unlock();
        }
    }

    public void runDisabledServerMessagesSchedule() {
        List<Integer> toRemove = new LinkedList<>();

        srvMessagesLock.lock();
        try {
            for (Entry<Integer, Integer> dsm : disabledServerMessages.entrySet()) {
                int b = dsm.getValue();
                if (b >= 4) {   // ~35sec duration, 10sec update
                    toRemove.add(dsm.getKey());
                } else {
                    disabledServerMessages.put(dsm.getKey(), ++b);
                }
            }

            for (Integer chrid : toRemove) {
                disabledServerMessages.remove(chrid);
            }
        } finally {
            srvMessagesLock.unlock();
        }

        if (!toRemove.isEmpty()) {
            toRemove.stream()
                    .map(id -> players.getCharacterById(id))
                    .flatMap(Optional::stream)
                    .filter(MapleCharacter::isLoggedinWorld)
                    .forEach(c -> c.announce(MaplePacketCreator.serverMessage(c.getClient().getChannelServer().getServerMessage())));
        }
    }

    public void setPlayerNpcMapStep(int mapId, int step) {
        setPlayerNpcMapData(mapId, step, -1, false);
    }

    public void setPlayerNpcMapPodiumData(int mapId, int podium) {
        setPlayerNpcMapData(mapId, -1, podium, false);
    }

    public void setPlayerNpcMapData(int mapId, int step, int podium) {
        setPlayerNpcMapData(mapId, step, podium, true);
    }

    private void setPlayerNpcMapData(int mapId, int step, int podium, boolean silent) {
        if (!silent) {
            try {
                Connection con = DatabaseConnection.getConnection();

                if (step != -1) {
                    executePlayerNpcMapDataUpdate(con, false, pnpcStep, step, id, mapId);
                }

                if (podium != -1) {
                    executePlayerNpcMapDataUpdate(con, true, pnpcPodium, podium, id, mapId);
                }

                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (step != -1) {
            pnpcStep.put(mapId, (byte) step);
        }
        if (podium != -1) {
            pnpcPodium.put(mapId, (short) podium);
        }
    }

    public int getPlayerNpcMapStep(int mapId) {
        return pnpcStep.get(mapId);
    }

    public int getPlayerNpcMapPodiumData(int mapId) {
        return pnpcPodium.get(mapId);
    }

    public void resetPlayerNpcMapData() {
        pnpcStep.clear();
        pnpcPodium.clear();
    }

    public void setServerMessage(String msg) {
        getChannels().forEach(c -> c.setServerMessage(msg));
    }

    public void broadcastPacket(final byte[] packet) {
        players.getAllCharacters().forEach(MapleCharacter.announcePacket(packet));
    }

    public List<Pair<MaplePlayerShopItem, AbstractMapleMapObject>> getAvailableItemBundles(int itemid) {
        List<Pair<MaplePlayerShopItem, AbstractMapleMapObject>> hmsAvailable = new ArrayList<>();

        for (MapleHiredMerchant hm : getActiveMerchants()) {
            List<MaplePlayerShopItem> itemBundles = hm.sendAvailableBundles(itemid);

            for (MaplePlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, hm));
            }
        }

        for (MaplePlayerShop ps : getActivePlayerShops()) {
            List<MaplePlayerShopItem> itemBundles = ps.sendAvailableBundles(itemid);

            for (MaplePlayerShopItem mpsi : itemBundles) {
                hmsAvailable.add(new Pair<>(mpsi, ps));
            }
        }

        hmsAvailable.sort(Comparator.comparingInt(p -> p.getLeft().getPrice()));

        hmsAvailable.subList(0, Math.min(hmsAvailable.size(), 200));    //truncates the list to have up to 200 elements
        return hmsAvailable;
    }

    private void pushRelationshipCouple(Pair<Integer, Pair<Integer, Integer>> couple) {
        int mid = couple.getLeft(), hid = couple.getRight().getLeft(), wid = couple.getRight().getRight();
        relationshipCouples.put(mid, couple.getRight());
        relationships.put(hid, mid);
        relationships.put(wid, mid);
    }

    public Pair<Integer, Integer> getRelationshipCouple(int relationshipId) {
        Pair<Integer, Integer> rc = relationshipCouples.get(relationshipId);

        if (rc == null) {
            Pair<Integer, Pair<Integer, Integer>> couple = getRelationshipCoupleFromDb(relationshipId, true);
            if (couple == null) {
                return null;
            }

            pushRelationshipCouple(couple);
            rc = couple.getRight();
        }

        return rc;
    }

    public int getRelationshipId(int playerId) {
        Integer ret = relationships.get(playerId);

        if (ret == null) {
            Pair<Integer, Pair<Integer, Integer>> couple = getRelationshipCoupleFromDb(playerId, false);
            if (couple == null) {
                return -1;
            }

            pushRelationshipCouple(couple);
            ret = couple.getLeft();
        }

        return ret;
    }

    public int createRelationship(int groomId, int brideId) {
        int ret = addRelationshipToDb(groomId, brideId);

        pushRelationshipCouple(new Pair<>(ret, new Pair<>(groomId, brideId)));
        return ret;
    }

    public void deleteRelationship(int playerId, int partnerId) {
        int relationshipId = relationships.get(playerId);
        deleteRelationshipFromDb(relationshipId);

        relationshipCouples.remove(relationshipId);
        relationships.remove(playerId);
        relationships.remove(partnerId);
    }

    public void dropMessage(int type, String message) {
        getPlayerStorage().getAllCharacters().forEach(c -> c.dropMessage(type, message));
    }

    public boolean registerFisherPlayer(MapleCharacter chr, int baitLevel) {
        synchronized (fishingAttempters) {
            if (fishingAttempters.containsKey(chr)) {
                return false;
            }

            fishingAttempters.put(chr, baitLevel);
            return true;
        }
    }

    public int unregisterFisherPlayer(MapleCharacter chr) {
        Integer baitLevel = fishingAttempters.remove(chr);
        return Objects.requireNonNullElse(baitLevel, 0);
    }

    public void runCheckFishingSchedule() {
        double[] fishingLikelihoods = Fishing.fetchFishingLikelihood();
        double yearLikelihood = fishingLikelihoods[0], timeLikelihood = fishingLikelihoods[1];

        if (!fishingAttempters.isEmpty()) {
            List<MapleCharacter> fishingAttemptersList;

            synchronized (fishingAttempters) {
                fishingAttemptersList = new ArrayList<>(fishingAttempters.keySet());
            }

            for (MapleCharacter chr : fishingAttemptersList) {
                int baitLevel = unregisterFisherPlayer(chr);
                Fishing.doFishing(chr, baitLevel, yearLikelihood, timeLikelihood);
            }
        }
    }

    public void runPartySearchUpdateSchedule() {
        partySearch.updatePartySearchStorage();
        partySearch.runPartySearch();
    }

    public BaseService getServiceAccess(WorldServices sv) {
        return services.getAccess(sv).getService();
    }

    private void closeWorldServices() {
        services.shutdown();
    }

    private void clearWorldData() {
        List<MapleParty> pList;
        partyLock.lock();
        try {
            pList = new ArrayList<>(parties.values());
        } finally {
            partyLock.unlock();
        }

        for (MapleParty p : pList) {
            p.disposeLocks();
        }

        closeWorldServices();
        disposeLocks();
    }

    private void disposeLocks() {
        LockCollector.getInstance().registerDisposeAction(this::emptyLocks);
    }

    private void emptyLocks() {
        accountCharsLock = accountCharsLock.dispose();
        partyLock = partyLock.dispose();
        srvMessagesLock = srvMessagesLock.dispose();
        activePetsLock = activePetsLock.dispose();
        activeMountsLock = activeMountsLock.dispose();
        activePlayerShopsLock = activePlayerShopsLock.dispose();
        activeMerchantsLock = activeMerchantsLock.dispose();
        timedMapObjectLock = timedMapObjectLock.dispose();
    }

    public final void shutdown() {
        for (Channel ch : getChannels()) {
            ch.shutdown();
        }

        if (petsSchedule != null) {
            petsSchedule.cancel(false);
            petsSchedule = null;
        }

        if (srvMessagesSchedule != null) {
            srvMessagesSchedule.cancel(false);
            srvMessagesSchedule = null;
        }

        if (mountsSchedule != null) {
            mountsSchedule.cancel(false);
            mountsSchedule = null;
        }

        if (merchantSchedule != null) {
            merchantSchedule.cancel(false);
            merchantSchedule = null;
        }

        if (timedMapObjectsSchedule != null) {
            timedMapObjectsSchedule.cancel(false);
            timedMapObjectsSchedule = null;
        }

        if (charactersSchedule != null) {
            charactersSchedule.cancel(false);
            charactersSchedule = null;
        }

        if (marriagesSchedule != null) {
            marriagesSchedule.cancel(false);
            marriagesSchedule = null;
        }

        if (mapOwnershipSchedule != null) {
            mapOwnershipSchedule.cancel(false);
            mapOwnershipSchedule = null;
        }

        if (fishingSchedule != null) {
            fishingSchedule.cancel(false);
            fishingSchedule = null;
        }

        if (partySearchSchedule != null) {
            partySearchSchedule.cancel(false);
            partySearchSchedule = null;
        }

        if (timeoutSchedule != null) {
            timeoutSchedule.cancel(false);
            timeoutSchedule = null;
        }

        players.disconnectAll();
        players = null;

        clearWorldData();
        System.out.println("Finished shutting down world " + id + "\r\n");
    }
}
