package connection.packets;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleRing;
import client.Skill;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.newyear.NewYearCardRecord;
import connection.constants.SendOpcode;
import constants.game.GameConstants;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import server.CashShop;
import server.maps.MapleMap;
import tools.Randomizer;
import tools.StringUtil;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CStage {
    /**
     * Gets character info for a character.
     *
     * @param chr The character to get info about.
     * @return The character info packet.
     */
    public static byte[] getCharInfo(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_FIELD.getValue());
        mplew.writeShort(0);// decode opt, loop with 2 decode 4s
        mplew.writeInt(chr.getClient()
                .getChannel() - 1);
        mplew.write(1); // sNotifierMessage
        mplew.write(1); // bCharacterData
        mplew.writeShort(0); // nNotifierCheck
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(Randomizer.nextInt());
        }
        addCharacterInfo(mplew, chr);
        setLogutGiftConfig(chr, mplew);
        mplew.writeLong(CCommon.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to change maps.
     *
     * @param to         The <code>MapleMap</code> to warp to.
     * @param spawnPoint The spawn portal number to spawn at.
     * @param chr        The character warping to <code>to</code>
     * @return The map change packet.
     */
    public static byte[] getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_FIELD.getValue());
        mplew.writeShort(0);// decode opt, loop with 2 decode 4s
        for (int i = 0; i < 0; i++) {
            mplew.writeInt(i + 1); // dwType
            mplew.writeInt(0); // idk?
        }
        mplew.writeInt(chr.getClient()
                .getChannel() - 1);
        mplew.write(0); // sNotifierMessage
        mplew.write(0); // bCharacterData
        mplew.writeShort(0); // nNotifierCheck
        if (0 > 0) {
            mplew.writeMapleAsciiString("");
            for (int j = 0; j < 0; j++) {
                mplew.writeMapleAsciiString("");
            }
        }
        if (0 != 0) { // bCharacterData

        } else {
            mplew.write(0); // revive
            mplew.writeInt(to.getId());
            mplew.write(spawnPoint);
            mplew.writeShort(chr.getHp());
            mplew.writeBool(false);
            if (false) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        mplew.writeLong(CCommon.getTime(Server.getInstance()
                    .getCurrentTime()));
        return mplew.getPacket();
    }

    public static byte[] getWarpToMap(MapleMap to, int spawnPoint, Point spawnPosition, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_FIELD.getValue());
        mplew.writeShort(0);// decode opt, loop with 2 decode 4s
        mplew.writeInt(chr.getClient()
                .getChannel() - 1);
        mplew.write(0); // sNotifierMessage
        mplew.write(0); // bCharacterData
        mplew.writeShort(0); // nNotifierCheck
        mplew.write(0);// revive
        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeShort(chr.getHp());
        mplew.writeBool(true);
        mplew.writeInt(spawnPosition.x);    // spawn position placement thanks to Arnah (Vertisy)
        mplew.writeInt(spawnPosition.y);
        mplew.writeLong(CCommon.getTime(Server.getInstance()
                .getCurrentTime()));
        return mplew.getPacket();
    }

    public static byte[] openCashShop(MapleClient c, boolean mts) throws Exception {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(mts ? SendOpcode.SET_ITC.getValue() : SendOpcode.SET_CASH_SHOP.getValue());

        addCharacterInfo(mplew, c.getPlayer());

        if (!mts) {
            mplew.write(1);
        }

        mplew.writeMapleAsciiString(c.getAccountName());
        if (mts) {
            mplew.write(new byte[]{(byte) 0x88, 19, 0, 0, 7, 0, 0, 0, (byte) 0xF4, 1, 0, 0, (byte) 0x18, 0, 0, 0, (byte) 0xA8, 0, 0, 0, (byte) 0x70, (byte) 0xAA, (byte) 0xA7, (byte) 0xC5, (byte) 0x4E, (byte) 0xC1, (byte) 0xCA, 1});
        } else {
            mplew.writeInt(0);
            java.util.List<CashShop.SpecialCashItem> lsci = CashShop.CashItemFactory.getSpecialCashItems();
            mplew.writeShort(lsci.size());//Guess what
            for (CashShop.SpecialCashItem sci : lsci) {
                mplew.writeInt(sci.getSN());
                mplew.writeInt(sci.getModifier());
                mplew.write(sci.getInfo());
            }
            mplew.skip(121);

            java.util.List<java.util.List<Integer>> mostSellers = c.getWorldServer()
                    .getMostSellerCashItems();
            for (int i = 1; i <= 8; i++) {
                List<Integer> mostSellersTab = mostSellers.get(i);

                for (int j = 0; j < 2; j++) {
                    for (Integer snid : mostSellersTab) {
                        mplew.writeInt(i);
                        mplew.writeInt(j);
                        mplew.writeInt(snid);
                    }
                }
            }

            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(75);
        }
        return mplew.getPacket();
    }

    private static void addCharacterInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeLong(-1); // dbcharFlag
        mplew.write(0); // something about SN, I believe this is size of list
        CCommon.addCharStats(mplew, chr);
        mplew.write(chr.getBuddylist()
                .getCapacity());

        if (chr.getLinkedName() == null) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getLinkedName());
        }

        mplew.writeInt(chr.getMeso());
        addInventoryInfo(mplew, chr);
        addSkillInfo(mplew, chr);
        addQuestInfo(mplew, chr);
        addMiniGameInfo(mplew, chr);
        addRingInfo(mplew, chr);
        addTeleportInfo(mplew, chr);
        addMonsterBookInfo(mplew, chr);
        addNewYearInfo(mplew, chr);
        addAreaInfo(mplew, chr);//assuming it stayed here xd
        mplew.writeShort(0);
    }

    private static void addNewYearInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Set<NewYearCardRecord> received = chr.getReceivedNewYearRecords();

        mplew.writeShort(received.size());
        for (NewYearCardRecord nyc : received) {
            CCommon.encodeNewYearCard(nyc, mplew);
        }
    }

    private static void addTeleportInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<Integer> tele = chr.getTrockMaps();
        final List<Integer> viptele = chr.getVipTrockMaps();
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(tele.get(i));
        }
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(viptele.get(i));
        }
    }

    private static void addMiniGameInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(0);
                /*for (int m = size; m > 0; m--) {//nexon does this :P
                 mplew.writeInt(0);
                 mplew.writeInt(0);
                 mplew.writeInt(0);
                 mplew.writeInt(0);
                 mplew.writeInt(0);
                 }*/
    }

    private static void addAreaInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Map<Short, String> areaInfos = chr.getAreaInfos();
        mplew.writeShort(areaInfos.size());
        for (Short area : areaInfos.keySet()) {
            mplew.writeShort(area);
            mplew.writeMapleAsciiString(areaInfos.get(area));
        }
    }

    private static void addQuestInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        List<MapleQuestStatus> started = chr.getStartedQuests();
        int startedSize = 0;
        for (MapleQuestStatus qs : started) {
            if (qs.getInfoNumber() > 0) {
                startedSize++;
            }
            startedSize++;
        }
        mplew.writeShort(startedSize);
        for (MapleQuestStatus qs : started) {
            mplew.writeShort(qs.getQuest()
                    .getId());
            mplew.writeMapleAsciiString(qs.getProgressData());

            short infoNumber = qs.getInfoNumber();
            if (infoNumber > 0) {
                MapleQuestStatus iqs = chr.getQuest(infoNumber);
                mplew.writeShort(infoNumber);
                mplew.writeMapleAsciiString(iqs.getProgressData());
            }
        }
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus qs : completed) {
            mplew.writeShort(qs.getQuest()
                    .getId());
            mplew.writeLong(CCommon.getTime(qs.getCompletionTime()));
        }
    }

    private static void addInventoryInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        for (byte i = 1; i <= 5; i++) {
            byte limit = MapleInventoryType.getByType(i)
                    .map(chr::getInventory)
                    .map(MapleInventory::getSlotLimit)
                    .orElse((byte) 0);
            mplew.write(limit);
        }
        mplew.writeLong(CCommon.getTime(-2));
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        Collection<Item> equippedC = iv.list();
        List<Item> equipped = new ArrayList<>(equippedC.size());
        List<Item> equippedCash = new ArrayList<>(equippedC.size());
        for (Item item : equippedC) {
            if (item.getPosition() <= -100) {
                equippedCash.add(item);
            } else {
                equipped.add(item);
            }
        }
        for (Item item : equipped) {    // equipped doesn't actually need sorting, thanks Pllsz
            CCommon.addItemInfo(mplew, item);
        }
        mplew.writeShort(0); // start of equip cash
        for (Item item : equippedCash) {
            CCommon.addItemInfo(mplew, item);
        }
        mplew.writeShort(0); // start of equip inventory
        for (Item item : chr.getInventory(MapleInventoryType.EQUIP)
                .list()) {
            CCommon.addItemInfo(mplew, item);
        }
        mplew.writeInt(0);
        for (Item item : chr.getInventory(MapleInventoryType.USE)
                .list()) {
            CCommon.addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (Item item : chr.getInventory(MapleInventoryType.SETUP)
                .list()) {
            CCommon.addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (Item item : chr.getInventory(MapleInventoryType.ETC)
                .list()) {
            CCommon.addItemInfo(mplew, item);
        }
        mplew.write(0);
        for (Item item : chr.getInventory(MapleInventoryType.CASH)
                .list()) {
            CCommon.addItemInfo(mplew, item);
        }
    }

    private static void addSkillInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(0); // start of skills
        Map<Skill, MapleCharacter.SkillEntry> skills = chr.getSkills();
        int skillsSize = skills.size();
        // We don't want to include any hidden skill in this, so subtract them from the size list and ignore them.
        for (Iterator<Map.Entry<Skill, MapleCharacter.SkillEntry>> it = skills.entrySet()
                .iterator(); it.hasNext(); ) {
            Map.Entry<Skill, MapleCharacter.SkillEntry> skill = it.next();
            if (GameConstants.isHiddenSkills(skill.getKey()
                    .id())) {
                skillsSize--;
            }
        }
        mplew.writeShort(skillsSize);
        for (Iterator<Map.Entry<Skill, MapleCharacter.SkillEntry>> it = skills.entrySet()
                .iterator(); it.hasNext(); ) {
            Map.Entry<Skill, MapleCharacter.SkillEntry> skill = it.next();
            if (GameConstants.isHiddenSkills(skill.getKey()
                    .id())) {
                continue;
            }
            mplew.writeInt(skill.getKey()
                    .id());
            mplew.writeInt(skill.getValue().skillevel);
            CCommon.addExpirationTime(mplew, skill.getValue().expiration);
            if (skill.getKey()
                    .isFourthJob()) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }
        mplew.writeShort(chr.getAllCooldowns()
                .size());
        for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
            mplew.writeInt(cooling.skillId);
            int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
            mplew.writeShort(timeLeft / 1000);
        }
    }

    private static void addMonsterBookInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMonsterBookCover()); // cover
        mplew.write(0);
        Map<Integer, Integer> cards = chr.getMonsterBook()
                .getCards();
        mplew.writeShort(cards.size());
        for (Map.Entry<Integer, Integer> all : cards.entrySet()) {
            mplew.writeShort(all.getKey() % 10000); // Id
            mplew.write(all.getValue()); // Level
        }
    }

    private static void setLogutGiftConfig(MapleCharacter chr, LittleEndianWriter lew) {
        lew.writeInt(0);// bPredictQuit
        for (int i = 0; i < 3; i++) {
            lew.writeInt(0);//
        }
    }

    private static void addRingInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(chr.getCrushRings()
                .size());
        for (MapleRing ring : chr.getCrushRings()) {
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(CCommon.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            mplew.writeInt(ring.getRingId());
            mplew.writeInt(0);
            mplew.writeInt(ring.getPartnerRingId());
            mplew.writeInt(0);
        }
        mplew.writeShort(chr.getFriendshipRings()
                .size());
        for (MapleRing ring : chr.getFriendshipRings()) {
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(CCommon.getRightPaddedStr(ring.getPartnerName(), '\0', 13));
            mplew.writeInt(ring.getRingId());
            mplew.writeInt(0);
            mplew.writeInt(ring.getPartnerRingId());
            mplew.writeInt(0);
            mplew.writeInt(ring.getItemId());
        }

        if (chr.getPartnerId() > 0) {
            mplew.writeShort(1);
            mplew.writeInt(chr.getRelationshipId());
            mplew.writeInt(chr.getGender() == 0 ? chr.getId() : chr.getPartnerId());
            mplew.writeInt(chr.getGender() == 0 ? chr.getPartnerId() : chr.getId());

            Optional<MapleRing> marriageRing = chr.getMarriageRing();
            mplew.writeShort((marriageRing.isPresent()) ? 3 : 1);
            mplew.writeInt(marriageRing.map(MapleRing::getItemId)
                    .orElse(1112803));
            mplew.writeInt(marriageRing.map(MapleRing::getItemId)
                    .orElse(1112803));
            String spouse = MapleCharacter.getNameById(chr.getPartnerId())
                    .orElseThrow();
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(chr.getGender() == 0 ? chr.getName() : spouse, '\0', 13));
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(chr.getGender() == 0 ? spouse : chr.getName(), '\0', 13));
        } else {
            mplew.writeShort(0);
        }
    }
}
