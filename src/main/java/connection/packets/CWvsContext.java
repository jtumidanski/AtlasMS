package connection.packets;

import client.BuddylistEntry;
import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleFamilyEntitlement;
import client.MapleFamilyEntry;
import client.MapleMount;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MonsterBook;
import client.SkillMacro;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import client.newyear.NewYearCardRecord;
import connection.constants.SendOpcode;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import constants.net.ShowStatusInfoMessageType;
import constants.skills.Buccaneer;
import constants.skills.Corsair;
import constants.skills.ThunderBreaker;
import net.server.Server;
import net.server.guild.MapleAlliance;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import server.ItemInformationProvider;
import server.life.MobSkill;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleDoor;
import server.maps.MapleDoorObject;
import server.maps.MapleHiredMerchant;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import tools.Pair;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CWvsContext {
    public static byte[] modifyInventory(boolean updateTick, final List<ModifyInventory> mods) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.INVENTORY_OPERATION.getValue());
        mplew.writeBool(updateTick);
        mplew.write(mods.size());
        //mplew.write(0); v104 :)
        int addMovement = -1;
        for (ModifyInventory mod : mods) {
            mplew.write(mod.getMode());
            mplew.write(mod.getInventoryType());
            mplew.writeShort(mod.getMode() == 2 ? mod.getOldPosition() : mod.getPosition());
            switch (mod.getMode()) {
                case 0: {//add item
                    CCommon.addItemInfo(mplew, mod.getItem(), true);
                    break;
                }
                case 1: {//update quantity
                    mplew.writeShort(mod.getQuantity());
                    break;
                }
                case 2: {//move
                    mplew.writeShort(mod.getPosition());
                    if (mod.getPosition() < 0 || mod.getOldPosition() < 0) {
                        addMovement = mod.getOldPosition() < 0 ? 1 : 2;
                    }
                    break;
                }
                case 3: {//remove
                    if (mod.getPosition() < 0) {
                        addMovement = 2;
                    }
                    break;
                }
            }
            mod.clear();
        }
        if (addMovement > -1) {
            mplew.write(addMovement);
        }
        return mplew.getPacket();
    }

    public static byte[] updateInventorySlotLimit(int type, int newLimit) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.INVENTORY_GROW.getValue());
        mplew.write(type);
        mplew.write(newLimit);
        return mplew.getPacket();
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats         The list of stats to update.
     * @param enableActions Allows actions after the update.
     * @param chr           The update target.
     * @return The stat update packet.
     */
    public static byte[] updatePlayerStats(List<Pair<MapleStat, Integer>> stats, boolean enableActions, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STAT_CHANGED.getValue());
        mplew.write(enableActions ? 1 : 0);
        int updateMask = 0;
        for (Pair<MapleStat, Integer> statupdate : stats) {
            updateMask |= statupdate.getLeft()
                    .getValue();
        }

        Comparator<Pair<MapleStat, Integer>> comparator = (o1, o2) -> {
            int val1 = o1.getLeft()
                    .getValue();
            int val2 = o2.getLeft()
                    .getValue();
            return (Integer.compare(val1, val2));
        };

        mplew.writeInt(updateMask);
        stats.stream()
                .sorted(comparator)
                .forEach(stat -> updatePlayerStat(mplew, chr, stat));
        return mplew.getPacket();
    }

    public static byte[] petStatUpdate(MapleCharacter chr) {
        // this actually does nothing... packet structure and stats needs to be uncovered

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STAT_CHANGED.getValue());
        int mask = 0;
        mask |= MapleStat.PETSN.getValue();
        mplew.write(0);
        mplew.writeInt(mask);
        MaplePet[] pets = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.writeLong(pets[i].getUniqueId());
            } else {
                mplew.writeLong(0);
            }
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * It is important that statups is in the correct order (see declaration
     * order in MapleBuffStat) since this method doesn't do automagical
     * reordering.
     *
     * @param buffid
     * @param bufflength
     * @param statups
     * @return
     */
    //1F 00 00 00 00 00 03 00 00 40 00 00 00 E0 00 00 00 00 00 00 00 00 E0 01 8E AA 4F 00 00 C2 EB 0B E0 01 8E AA 4F 00 00 C2 EB 0B 0C 00 8E AA 4F 00 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B 00 00 E0 7A 1D 00 8E AA 4F 00 00 00 00 00 00 00 00 03
    public static byte[] giveBuff(int buffid, int bufflength, List<Pair<MapleBuffStat, Integer>> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        boolean special = false;
        CCommon.writeLongMask(mplew, statups);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            if (statup.getLeft()
                    .equals(MapleBuffStat.MONSTER_RIDING) || statup.getLeft()
                    .equals(MapleBuffStat.HOMING_BEACON)) {
                special = true;
            }
            mplew.writeShort(statup.getRight()
                    .shortValue());
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(statups.get(0)
                .getRight()); //Homing beacon ...

        if (special) {
            mplew.skip(3);
        }
        return mplew.getPacket();
    }

    public static byte[] giveDebuff(List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        CCommon.writeLongMaskD(mplew, statups);
        for (Pair<MapleDisease, Integer> statup : statups) {
            mplew.writeShort(statup.getRight()
                    .shortValue());
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
            mplew.writeInt((int) skill.getDuration());
        }
        mplew.writeShort(0); // ??? wk charges have 600 here o.o
        mplew.writeShort(900);//Delay
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] givePirateBuff(List<Pair<MapleBuffStat, Integer>> statups, int buffid, int duration) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        boolean infusion = buffid == Buccaneer.SPEED_INFUSION || buffid == ThunderBreaker.SPEED_INFUSION || buffid == Corsair.SPEED_INFUSION;
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        CCommon.writeLongMask(mplew, statups);
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> stat : statups) {
            mplew.writeInt(stat.getRight()
                    .shortValue());
            mplew.writeInt(buffid);
            mplew.skip(infusion ? 10 : 5);
            mplew.writeShort(duration);
        }
        mplew.skip(3);
        return mplew.getPacket();
    }

    public static byte[] giveFinalAttack(int skillid, int time) { // packets found thanks to lailainoob
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(0);//some 80 and 0 bs DIRECTION
        mplew.write(0x80);//let's just do 80, then 0
        mplew.writeInt(0);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(time);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] cancelBuff(List<MapleBuffStat> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        CCommon.writeLongMaskFromList(mplew, statups);
        mplew.write(1);//?
        return mplew.getPacket();
    }

    public static byte[] cancelDebuff(long mask) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(19);
        mplew.writeShort(SendOpcode.CANCEL_BUFF.getValue());
        mplew.writeLong(0);
        mplew.writeLong(mask);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] aranGodlyStats() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FORCED_STAT_SET.getValue());
        mplew.write(new byte[]{(byte) 0x1F, (byte) 0x0F, 0, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xFF, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0x78, (byte) 0x8C});
        return mplew.getPacket();
    }

    public static byte[] resetForcedStats() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendOpcode.FORCED_STAT_RESET.getValue());
        return mplew.getPacket();
    }

    public static byte[] updateSkill(int skillid, int level, int masterlevel, long expiration) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        CCommon.addExpirationTime(mplew, expiration);
        mplew.write(4);
        return mplew.getPacket();
    }

    public static byte[] giveFameResponse(int mode, String charname, int newfame) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);
        mplew.writeShort(newfame);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * status can be: <br> 0: ok, use giveFameResponse<br> 1: the username is
     * incorrectly entered<br> 2: users under level 15 are unable to toggle with
     * fame.<br> 3: can't raise or drop fame anymore today.<br> 4: can't raise
     * or drop fame for this character for this month anymore.<br> 5: received
     * fame, use receiveFame()<br> 6: level of fame neither has been raised nor
     * dropped due to an unexpected error
     *
     * @param status
     * @return
     */
    public static byte[] giveFameErrorResponse(int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);
        return mplew.getPacket();
    }

    public static byte[] receiveFame(int mode, String charnameFrom) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show an EXP increase.
     *
     * @param gain   The amount of EXP gained.
     * @param inChat In the chat box?
     * @param white  White text or yellow?
     * @return The exp gained packet.
     */
    public static byte[] getShowExpGain(int gain, int equip, int party, boolean inChat, boolean white) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_INCREASE_EXP.getMessageType());
        mplew.writeBool(white);
        mplew.writeInt(gain);
        mplew.writeBool(inChat);
        mplew.writeInt(0); // bonus event exp
        mplew.write(0); // third monster kill event
        mplew.write(0); // RIP byte, this is always a 0
        mplew.writeInt(0); //wedding bonus
        if (0 > 0) {
            mplew.write(0); // nPlayTimeHour
        }
        if (inChat) { // quest bonus rate stuff
            mplew.write(0); // nQuestBonusRate
            if (0 > 0) {
                mplew.write(0); // nQuestBonusRemainCount
            }
        }

        mplew.write(0); //0 = party bonus, 100 = 1x Bonus EXP, 200 = 2x Bonus EXP
        mplew.writeInt(party); // party bonus
        mplew.writeInt(equip); //equip bonus
        mplew.writeInt(0); //Internet Cafe Bonus
        mplew.writeInt(0); //Rainbow Week Bonus
        mplew.writeInt(0); // nPartyExpRingExp
        mplew.writeInt(0); // nCakePieEventBonus
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a fame gain.
     *
     * @param gain How many fame gained.
     * @return The meso gain packet.
     */
    public static byte[] getShowFameGain(int gain) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_INCREASE_FAME.getMessageType());
        mplew.writeInt(gain);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain How many mesos gained.
     * @return The meso gain packet.
     */
    public static byte[] getShowMesoGain(int gain) {
        return getShowMesoGain(gain, false);
    }

    /**
     * Gets a packet telling the client to show a meso gain.
     *
     * @param gain   How many mesos gained.
     * @param inChat Show in the chat window?
     * @return The meso gain packet.
     */
    public static byte[] getShowMesoGain(int gain, boolean inChat) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            //TODO does this actually work?
            mplew.write(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
            mplew.writeShort(1); //v83
        } else {
            mplew.write(ShowStatusInfoMessageType.ON_INCREASE_MONEY.getMessageType());
        }
        mplew.writeInt(gain);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client to show an item gain.
     *
     * @param itemId   The ID of the item gained.
     * @param quantity The number of items gained.
     * @param inChat   Show in the chat window?
     * @return The item gain packet.
     */
    public static byte[] getShowItemGain(int itemId, short quantity, boolean inChat) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (inChat) {
            mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        } else {
            mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    /**
     * @param c
     * @param quest
     * @return
     */
    public static byte[] forfeitQuest(short quest) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());
        mplew.writeShort(quest);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * @param c
     * @param quest
     * @return
     */
    public static byte[] completeQuest(short quest, long time) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());
        mplew.writeShort(quest);
        mplew.write(2);
        mplew.writeLong(CCommon.getTime(time));
        return mplew.getPacket();
    }

    /**
     * @param c
     * @param quest
     * @param npc
     * @param progress
     * @return
     */

    public static byte[] updateQuestInfo(short quest, int npc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(8); //0x0A in v95
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] updateQuest(MapleCharacter chr, MapleQuestStatus qs, boolean infoUpdate) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD.getMessageType());

        if (infoUpdate) {
            MapleQuestStatus iqs = chr.getQuest(qs.getInfoNumber());
            mplew.writeShort(iqs.getQuestID());
            mplew.write(1);
            mplew.writeMapleAsciiString(iqs.getProgressData());
        } else {
            mplew.writeShort(qs.getQuest()
                    .getId());
            mplew.write(qs.getStatus()
                    .getId());
            mplew.writeMapleAsciiString(qs.getProgressData());
        }
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] getShowInventoryStatus(int mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_DROP_PICK_UP.getMessageType());
        mplew.write(mode);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] updateAreaInfo(int area, String info) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType()); //0x0B in v95
        mplew.writeShort(area);//infoNumber
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static byte[] getGPMessage(int gpChange) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_INCREASE_GUILD_POINT.getMessageType());
        mplew.writeInt(gpChange);
        return mplew.getPacket();
    }

    public static byte[] getItemMessage(int itemid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_GIVE_BUFF.getMessageType());
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] showInfoText(String text) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static byte[] getDojoInfo(String info) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType());
        mplew.write(new byte[]{(byte) 0xB7, 4});//QUEST ID f5
        mplew.writeMapleAsciiString(info);
        return mplew.getPacket();
    }

    public static byte[] getDojoInfoMessage(String message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static byte[] updateDojoStats(MapleCharacter chr, int belt) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_QUEST_RECORD_EX.getMessageType());
        mplew.write(new byte[]{(byte) 0xB7, 4}); //?
        mplew.writeMapleAsciiString("pt=" + chr.getDojoPoints() + ";belt=" + belt + ";tuto=" + (chr.getFinishedDojoTutorial() ? "1" : "0"));
        return mplew.getPacket();
    }

    public static byte[] itemExpired(int itemid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_CASH_ITEM_EXPIRE.getMessageType());
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] bunnyPacket() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ShowStatusInfoMessageType.ON_SYSTEM.getMessageType());
        mplew.writeAsciiString("Protect the Moon Bunny!!!");
        return mplew.getPacket();
    }

    public static byte[] noteSendMsg() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(4);
        return mplew.getPacket();
    }

    /*
     *  0 = Player online, use whisper
     *  1 = Check player's name
     *  2 = Receiver inbox full
     */
    public static byte[] noteError(byte error) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(5);
        mplew.write(error);
        return mplew.getPacket();
    }

    public static byte[] showNotes(ResultSet notes, int count) throws SQLException {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MEMO_RESULT.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from") + " ");//Stupid nexon forgot space lol
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(CCommon.getTime(notes.getLong("timestamp")));
            mplew.write(notes.getByte("fame"));//FAME :D
            notes.next();
        }
        return mplew.getPacket();
    }

    public static byte[] trockRefreshMapList(MapleCharacter chr, boolean delete, boolean vip) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAP_TRANSFER_RESULT.getValue());
        mplew.write(delete ? 2 : 3);
        if (vip) {
            mplew.write(1);
            List<Integer> map = chr.getVipTrockMaps();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map.get(i));
            }
        } else {
            mplew.write(0);
            List<Integer> map = chr.getTrockMaps();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map.get(i));
            }
        }
        return mplew.getPacket();
    }

    public static byte[] enableReport() { // thanks to snow
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.CLAIM_STATUS_CHANGED.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] updateMount(int charid, MapleMount mount, boolean levelup) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_TAMING_MOB_INFO.getValue());
        mplew.writeInt(charid);
        mplew.writeInt(mount.getLevel());
        mplew.writeInt(mount.getExp());
        mplew.writeInt(mount.getTiredness());
        mplew.write(levelup ? (byte) 1 : (byte) 0);
        return mplew.getPacket();
    }

    public static byte[] getShowQuestCompletion(int id) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.QUEST_CLEAR.getValue());
        mplew.writeShort(id);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantBox() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x07);
        return mplew.getPacket();
    }

    public static byte[] retrieveFirstMessage() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x09);
        return mplew.getPacket();
    }

    public static byte[] remoteChannelChange(byte ch) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ENTRUSTED_SHOP_CHECK_RESULT.getValue()); // header.
        mplew.write(0x10);
        mplew.writeInt(0);//No idea yet
        mplew.write(ch);
        return mplew.getPacket();
    }

    public static byte[] skillBookResult(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SKILL_LEARN_ITEM_RESULT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] finishedSort(int inv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.GATHER_ITEM_RESULT.getValue());
        mplew.write(0);
        mplew.write(inv);
        return mplew.getPacket();
    }

    public static byte[] finishedSort2(int inv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.SORT_ITEM_RESULT.getValue());
        mplew.write(0);
        mplew.write(inv);
        return mplew.getPacket();
    }

    /**
     * Sends a report response
     * <p>
     * Possible values for <code>mode</code>:<br> 0: You have succesfully
     * reported the user.<br> 1: Unable to locate the user.<br> 2: You may only
     * report users 10 times a day.<br> 3: You have been reported to the GM's by
     * a user.<br> 4: Your request did not go through for unknown reasons.
     * Please try again later.<br>
     *
     * @param mode The mode
     * @return Report Reponse packet
     */
    public static byte[] reportResponse(byte mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SUE_CHARACTER_RESULT.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] sendMesoLimit() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TRADE_MONEY_LIMIT.getValue()); //Players under level 15 can only trade 1m per day
        return mplew.getPacket();
    }

    public static byte[] updateGender(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.SET_GENDER.getValue());
        mplew.write(chr.getGender());
        return mplew.getPacket();
    }

    public static byte[] BBSThreadList(ResultSet rs, int start) throws SQLException {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_BBS_PACKET.getValue());
        mplew.write(0x06);
        if (!rs.last()) {
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        int threadCount = rs.getRow();
        if (rs.getInt("localthreadid") == 0) { //has a notice
            mplew.write(1);
            addThread(mplew, rs);
            threadCount--; //one thread didn't count (because it's a notice)
        } else {
            mplew.write(0);
        }
        if (!rs.absolute(start + 1)) { //seek to the thread before where we start
            rs.first(); //uh, we're trying to start at a place past possible
            start = 0;
        }
        mplew.writeInt(threadCount);
        mplew.writeInt(Math.min(10, threadCount - start));
        for (int i = 0; i < Math.min(10, threadCount - start); i++) {
            addThread(mplew, rs);
            rs.next();
        }
        return mplew.getPacket();
    }

    public static byte[] showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_BBS_PACKET.getValue());
        mplew.write(0x07);
        mplew.writeInt(localthreadid);
        mplew.writeInt(threadRS.getInt("postercid"));
        mplew.writeLong(CCommon.getTime(threadRS.getLong("timestamp")));
        mplew.writeMapleAsciiString(threadRS.getString("name"));
        mplew.writeMapleAsciiString(threadRS.getString("startpost"));
        mplew.writeInt(threadRS.getInt("icon"));
        if (repliesRS != null) {
            int replyCount = threadRS.getInt("replycount");
            mplew.writeInt(replyCount);
            int i;
            for (i = 0; i < replyCount && repliesRS.next(); i++) {
                mplew.writeInt(repliesRS.getInt("replyid"));
                mplew.writeInt(repliesRS.getInt("postercid"));
                mplew.writeLong(CCommon.getTime(repliesRS.getLong("timestamp")));
                mplew.writeMapleAsciiString(repliesRS.getString("content"));
            }
            if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    /**
     * @param chr
     * @param isSelf
     * @return
     */
    public static byte[] charInfo(MapleCharacter chr) {
        //3D 00 0A 43 01 00 02 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob()
                .getId());
        mplew.writeShort(chr.getFame());
        mplew.write(chr.getMarriageRing()
                .isPresent() ? 1 : 0);
        String guildName = "";
        String allianceName = "";
        if (chr.getGuildId() > 0) {
            guildName = Server.getInstance()
                    .getGuild(chr.getGuildId())
                    .map(MapleGuild::getName)
                    .orElse("");

            allianceName = chr.getGuild()
                    .map(MapleGuild::getAllianceId)
                    .flatMap(id -> Server.getInstance()
                            .getAlliance(id))
                    .map(MapleAlliance::getName)
                    .orElse("");
        }
        mplew.writeMapleAsciiString(guildName);
        mplew.writeMapleAsciiString(allianceName);  // does not seem to work
        mplew.write(0); // pMedalInfo, thanks to Arnah (Vertisy)

        MaplePet[] pets = chr.getPets();
        Item inv = chr.getInventory(MapleInventoryType.EQUIPPED)
                .getItem((short) -114);
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.write(pets[i].getUniqueId());
                mplew.writeInt(pets[i].getItemId()); // petid
                mplew.writeMapleAsciiString(pets[i].getName());
                mplew.write(pets[i].getLevel()); // pet level
                mplew.writeShort(pets[i].getCloseness()); // pet closeness
                mplew.write(pets[i].getFullness()); // pet fullness
                mplew.writeShort(0);
                mplew.writeInt(inv != null ? inv.getItemId() : 0);
            }
        }
        mplew.write(0); //end of pets

        Item mount;     //mounts can potentially crash the client if the player's level is not properly checked
        if (chr.getMount()
                .isPresent() && (mount = chr.getInventory(MapleInventoryType.EQUIPPED)
                .getItem((short) -18)) != null && ItemInformationProvider.getInstance()
                .getEquipLevelReq(mount.getItemId()) <= chr.getLevel()) {
            mplew.write(chr.getMount()
                    .map(MapleMount::getId)
                    .orElse(0));
            mplew.writeInt(chr.getMount()
                    .map(MapleMount::getLevel)
                    .orElse(0));
            mplew.writeInt(chr.getMount()
                    .map(MapleMount::getExp)
                    .orElse(0));
            mplew.writeInt(chr.getMount()
                    .map(MapleMount::getTiredness)
                    .orElse(0));
        } else {
            mplew.write(0);
        }
        mplew.write(chr.getCashShop()
                .getWishList()
                .size());
        for (int sn : chr.getCashShop()
                .getWishList()) {
            mplew.writeInt(sn);
        }

        MonsterBook book = chr.getMonsterBook();
        mplew.writeInt(book.getBookLevel());
        mplew.writeInt(book.getNormalCard());
        mplew.writeInt(book.getSpecialCard());
        mplew.writeInt(book.getTotalCards());
        mplew.writeInt(chr.getMonsterBookCover() > 0 ? ItemInformationProvider.getInstance()
                .getCardMobId(chr.getMonsterBookCover()) : 0);
        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED)
                .getItem((short) -49);
        if (medal != null) {
            mplew.writeInt(medal.getItemId());
        } else {
            mplew.writeInt(0);
        }
        ArrayList<Short> medalQuests = new ArrayList<>();
        List<MapleQuestStatus> completed = chr.getCompletedQuests();
        for (MapleQuestStatus qs : completed) {
            if (qs.getQuest()
                    .getId() >= 29000) { // && q.getQuest().getId() <= 29923
                medalQuests.add(qs.getQuest()
                        .getId());
            }
        }

        Collections.sort(medalQuests);
        mplew.writeShort(medalQuests.size());
        for (Short s : medalQuests) {
            mplew.writeShort(s);
        }

        List<Integer> chairs = new ArrayList<>();
        for (Item item : chr.getInventory(MapleInventoryType.SETUP)
                .list()) {
            if (ItemConstants.isChair(item.getItemId())) {
                chairs.add(item.getItemId());
            }
        }
        mplew.writeInt(chairs.size());
        for (int itemid : chairs) {
            mplew.writeInt(itemid);
        }
        return mplew.getPacket();
    }

    public static byte[] partyCreated(MapleParty party, int partycharid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeInt(party.getId());

        Map<Integer, MapleDoor> partyDoors = party.getDoors();
        if (partyDoors.size() > 0) {
            MapleDoor door = partyDoors.get(partycharid);

            if (door != null) {
                MapleDoorObject mdo = door.getAreaDoor();
                mplew.writeInt(mdo.getTo()
                        .getId());
                mplew.writeInt(mdo.getFrom()
                        .getId());
                mplew.writeInt(mdo.getPosition().x);
                mplew.writeInt(mdo.getPosition().y);
            } else {
                mplew.writeInt(999999999);
                mplew.writeInt(999999999);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        } else {
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] partyInvite(MapleCharacter from) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getPartyId()
                .orElse(-1));
        mplew.writeMapleAsciiString(from.getName());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] partySearchInvite(MapleCharacter from) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getPartyId()
                .orElse(-1));
        mplew.writeMapleAsciiString("PS: " + from.getName());
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * 10: A beginner can't create a party. 1/5/6/11/14/19: Your request for a
     * party didn't work due to an unexpected error. 12: Quit as leader of the
     * party. 13: You have yet to join a party.
     * 16: Already have joined a party. 17: The party you're trying to join is
     * already in full capacity. 19: Unable to find the requested character in
     * this channel. 25: Cannot kick another user in this map. 28/29: Leadership
     * can only be given to a party member in the vicinity. 30: Change leadership
     * only on same channel.
     *
     * @param message
     * @return
     */
    public static byte[] partyStatusMessage(int message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    /**
     * 21: Player is blocking any party invitations, 22: Player is taking care of
     * another invitation, 23: Player have denied request to the party.
     *
     * @param message
     * @param charname
     * @return
     */
    public static byte[] partyStatusMessage(int message, String charname) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);
        mplew.writeMapleAsciiString(charname);
        return mplew.getPacket();
    }

    public static byte[] updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                mplew.write(0x0C);
                mplew.writeInt(party.getId());
                mplew.writeInt(target.getId());
                if (op == PartyOperation.DISBAND) {
                    mplew.write(0);
                    mplew.writeInt(party.getId());
                } else {
                    mplew.write(1);
                    if (op == PartyOperation.EXPEL) {
                        mplew.write(1);
                    } else {
                        mplew.write(0);
                    }
                    mplew.writeMapleAsciiString(target.getName());
                    addPartyStatus(forChannel, party, mplew, false);
                }
                break;
            case JOIN:
                mplew.write(0xF);
                mplew.writeInt(party.getId());
                mplew.writeMapleAsciiString(target.getName());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                mplew.write(0x7);
                mplew.writeInt(party.getId());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case CHANGE_LEADER:
                mplew.write(0x1B);
                mplew.writeInt(target.getId());
                mplew.write(0);
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] partyPortal(int townId, int targetId, Point position) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARTY_OPERATION.getValue());
        mplew.writeShort(0x23);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writePos(position);
        return mplew.getPacket();
    }

    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(7);
        mplew.write(buddylist.size());
        for (BuddylistEntry buddy : buddylist) {
            if (buddy.isVisible()) {
                mplew.writeInt(buddy.getCharacterId()); // cid
                mplew.writeAsciiString(CCommon.getRightPaddedStr(buddy.getName(), '\0', 13));
                mplew.write(0); // opposite status
                mplew.writeInt(buddy.getChannel() - 1);
                mplew.writeAsciiString(CCommon.getRightPaddedStr(buddy.getGroup(), '\0', 13));
                mplew.writeInt(0);//mapid?
            }
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);//mapid?
        }
        return mplew.getPacket();
    }

    public static byte[] buddylistMessage(byte message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(message);
        return mplew.getPacket();
    }

    public static byte[] requestBuddylistAdd(int cidFrom, int cid, String nameFrom) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(cidFrom);
        mplew.writeAsciiString(CCommon.getRightPaddedStr(nameFrom, '\0', 11));
        mplew.write(0x09);
        mplew.write(0xf0);
        mplew.write(0x01);
        mplew.writeInt(0x0f);
        mplew.writeNullTerminatedAsciiString("Default Group");
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static byte[] updateBuddyChannel(int characterid, int channel) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0);
        mplew.writeInt(channel);
        return mplew.getPacket();
    }

    public static byte[] updateBuddyCapacity(int capacity) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static byte[] showGuildInfo(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x1A); //signature for showing guild info
        if (chr == null) { //show empty guild (used for leaving, expelled)
            mplew.write(0);
            return mplew.getPacket();
        }
        Optional<MapleGuild> g = chr.getMGC()
                .flatMap(mgc -> chr.getClient()
                        .getWorldServer()
                        .getGuild(mgc));
        if (g.isEmpty()) { //failed to read from DB - don't show a guild
            mplew.write(0);
            return mplew.getPacket();
        }
        mplew.write(1); //bInGuild
        mplew.writeInt(g.get()
                .getId());
        mplew.writeMapleAsciiString(g.get()
                .getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(g.get()
                    .getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = g.get()
                .getMembers();
        mplew.write(members.size()); //then it is the size of all the members
        for (MapleGuildCharacter mgc : members) {//and each of their character ids o_O
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(g.get()
                    .getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(g.get()
                .getCapacity());
        mplew.writeShort(g.get()
                .getLogoBG());
        mplew.write(g.get()
                .getLogoBGColor());
        mplew.writeShort(g.get()
                .getLogo());
        mplew.write(g.get()
                .getLogoColor());
        mplew.writeMapleAsciiString(g.get()
                .getNotice());
        mplew.writeInt(g.get()
                .getGP());
        mplew.writeInt(g.get()
                .getAllianceId());
        return mplew.getPacket();
    }

    public static byte[] guildMemberOnline(int gid, int cid, boolean bOnline) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3d);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.write(bOnline ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] guildInvite(int gid, String charName) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(charName);
        return mplew.getPacket();
    }

    public static byte[] createGuildMessage(String masterName, String guildName) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(masterName);
        mplew.writeMapleAsciiString(guildName);
        return mplew.getPacket();
    }

    /**
     * Gets a Heracle/guild message packet.
     * <p>
     * Possible values for <code>code</code>:<br> 28: guild name already in use<br>
     * 31: problem in locating players during agreement<br> 33/40: already joined a guild<br>
     * 35: Cannot make guild<br> 36: problem in player agreement<br> 38: problem during forming guild<br>
     * 41: max number of players in joining guild<br> 42: character can't be found this channel<br>
     * 45/48: character not in guild<br> 52: problem in disbanding guild<br> 56: admin cannot make guild<br>
     * 57: problem in increasing guild size<br>
     *
     * @param code The response code.
     * @return The guild message packet.
     */
    public static byte[] genericGuildMessage(byte code) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    /**
     * Gets a guild message packet appended with target name.
     * <p>
     * 53: player not accepting guild invites<br>
     * 54: player already managing an invite<br> 55: player denied an invite<br>
     *
     * @param code       The response code.
     * @param targetName The initial player target of the invitation.
     * @return The guild message packet.
     */
    public static byte[] responseGuildMessage(byte code, String targetName) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);
        mplew.writeMapleAsciiString(targetName);
        return mplew.getPacket();
    }

    public static byte[] newGuildMember(MapleGuildCharacter mgc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x27);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        mplew.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        mplew.writeInt(1); //? could be guild signature, but doesn't seem to matter
        mplew.writeInt(3);
        return mplew.getPacket();
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static byte[] memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(bExpelled ? 0x2f : 0x2c);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleAsciiString(mgc.getName());
        return mplew.getPacket();
    }

    //rank change
    public static byte[] changeRank(MapleGuildCharacter mgc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x40);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());
        return mplew.getPacket();
    }

    public static byte[] guildNotice(int gid, String notice) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x44);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(notice);
        return mplew.getPacket();
    }

    public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3C);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());
        return mplew.getPacket();
    }

    public static byte[] rankTitleChange(int gid, String[] ranks) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3E);
        mplew.writeInt(gid);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] guildDisband(int gid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x32);
        mplew.writeInt(gid);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] guildQuestWaitingNotice(byte channel, int waitingPos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x4C);
        mplew.write(channel - 1);
        mplew.write(waitingPos);
        return mplew.getPacket();
    }

    public static byte[] guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x42);
        mplew.writeInt(gid);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);
        return mplew.getPacket();
    }

    public static byte[] guildCapacityChange(int gid, int capacity) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3A);
        mplew.writeInt(gid);
        mplew.write(capacity);
        return mplew.getPacket();
    }

    public static byte[] showGuildRanks(int npcid, ResultSet rs) throws SQLException {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (!rs.last()) { //no guilds o.o
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(rs.getRow()); //number of entries
        rs.beforeFirst();
        while (rs.next()) {
            mplew.writeMapleAsciiString(rs.getString("name"));
            mplew.writeInt(rs.getInt("GP"));
            mplew.writeInt(rs.getInt("logo"));
            mplew.writeInt(rs.getInt("logoColor"));
            mplew.writeInt(rs.getInt("logoBG"));
            mplew.writeInt(rs.getInt("logoBGColor"));
        }
        return mplew.getPacket();
    }

    public static byte[] showPlayerRanks(int npcid, List<Pair<String, Integer>> worldRanking) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x49);
        mplew.writeInt(npcid);
        if (worldRanking.isEmpty()) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(worldRanking.size());
        for (Pair<String, Integer> wr : worldRanking) {
            mplew.writeMapleAsciiString(wr.getLeft());
            mplew.writeInt(wr.getRight());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] updateGP(int gid, int GP) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.writeInt(GP);
        return mplew.getPacket();
    }

    public static byte[] getAllianceInfo(MapleAlliance alliance) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0C);
        mplew.write(1);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds()
                .size());
        mplew.writeInt(alliance.getCapacity()); // probably capacity
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeMapleAsciiString(alliance.getNotice());
        return mplew.getPacket();
    }

    public static byte[] updateAllianceInfo(MapleAlliance alliance, int world) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0F);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds()
                .size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(alliance.getCapacity()); // probably capacity
        mplew.writeShort(0);
        alliance.getGuilds()
                .stream()
                .map(id -> Server.getInstance()
                        .getGuild(id, world))
                .flatMap(Optional::stream)
                .forEach(g -> getGuildInfo(mplew, g));
        return mplew.getPacket();
    }

    public static byte[] getGuildAlliances(MapleAlliance alliance, int worldId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0D);
        mplew.writeInt(alliance.getGuilds()
                .size());
        alliance.getGuilds()
                .stream()
                .map(id -> Server.getInstance()
                        .getGuild(id, worldId))
                .flatMap(Optional::stream)
                .forEach(g -> getGuildInfo(mplew, g));
        return mplew.getPacket();
    }

    public static byte[] addGuildToAlliance(MapleAlliance alliance, int newGuild, MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x12);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds()
                .size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(alliance.getCapacity());
        mplew.writeMapleAsciiString(alliance.getNotice());
        mplew.writeInt(newGuild);
        getGuildInfo(mplew, Server.getInstance()
                .getGuild(newGuild, c.getWorld(), null)
                .orElseThrow());
        return mplew.getPacket();
    }

    public static byte[] allianceMemberOnline(MapleCharacter mc, boolean online) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0E);
        mplew.writeInt(mc.getGuild()
                .map(MapleGuild::getAllianceId)
                .orElse(0));
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.write(online ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] allianceNotice(int id, String notice) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1C);
        mplew.writeInt(id);
        mplew.writeMapleAsciiString(notice);
        return mplew.getPacket();
    }

    public static byte[] changeAllianceRankTitle(int alliance, String[] ranks) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1A);
        mplew.writeInt(alliance);
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString(ranks[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] updateAllianceJobLevel(MapleCharacter mc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(mc.getGuild()
                .map(MapleGuild::getAllianceId)
                .orElse(0));
        mplew.writeInt(mc.getGuildId());
        mplew.writeInt(mc.getId());
        mplew.writeInt(mc.getLevel());
        mplew.writeInt(mc.getJob()
                .getId());
        return mplew.getPacket();
    }

    public static byte[] removeGuildFromAlliance(MapleAlliance alliance, int expelledGuild, int worldId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRankTitle(i));
        }
        mplew.write(alliance.getGuilds()
                .size());
        for (Integer guild : alliance.getGuilds()) {
            mplew.writeInt(guild);
        }
        mplew.writeInt(alliance.getCapacity());
        mplew.writeMapleAsciiString(alliance.getNotice());
        mplew.writeInt(expelledGuild);
        getGuildInfo(mplew, Server.getInstance()
                .getGuild(expelledGuild, worldId, null)
                .orElseThrow());
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static byte[] disbandAlliance(int alliance) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1D);
        mplew.writeInt(alliance);
        return mplew.getPacket();
    }

    public static byte[] allianceInvite(int allianceid, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x03);
        mplew.writeInt(allianceid);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] sendShowInfo(int allianceid, int playerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x02);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        return mplew.getPacket();
    }

    private static byte[] sendInvitation(int allianceid, int playerid, final String guildname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeMapleAsciiString(guildname);
        return mplew.getPacket();
    }

    private static byte[] sendChangeGuild(int allianceid, int playerid, int guildid, int option) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x07);
        mplew.writeInt(allianceid);
        mplew.writeInt(guildid);
        mplew.writeInt(playerid);
        mplew.write(option);
        return mplew.getPacket();
    }

    private static byte[] sendChangeLeader(int allianceid, int playerid, int victim) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x08);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeInt(victim);
        return mplew.getPacket();
    }

    private static byte[] sendChangeRank(int allianceid, int playerid, int int1, byte byte1) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x09);
        mplew.writeInt(allianceid);
        mplew.writeInt(playerid);
        mplew.writeInt(int1);
        mplew.writeInt(byte1);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a portal.
     *
     * @param townId   The ID of the town the portal goes to.
     * @param targetId The ID of the target.
     * @param pos      Where to put the portal.
     * @return The portal spawn packet.
     */
    public static byte[] spawnPortal(int townId, int targetId, Point pos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(14);
        mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writePos(pos);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a door.
     *
     * @param ownerid The door's owner ID.
     * @param town
     * @return The remove door packet.
     */
    public static byte[] removeDoor(int ownerid, boolean town) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);
        if (town) {
            mplew.writeShort(SendOpcode.SPAWN_PORTAL.getValue());
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
        } else {
            mplew.writeShort(SendOpcode.REMOVE_DOOR.getValue());
            mplew.write(0);
            mplew.writeInt(ownerid);
        }
        return mplew.getPacket();
    }

    /**
     * Gets a server message packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
     * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
     * 5: Pink Text<br> 6: Lightblue Text<br> 7: BroadCasting NPC
     *
     * @param type          The type of the notice.
     * @param channel       The channel this notice was sent on.
     * @param message       The message to convey.
     * @param servermessage Is this a scrolling ticker?
     * @return The server notice packet.
     */
    static byte[] serverMessage(int type, int channel, String message, boolean servermessage, boolean megaEar, int npc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (servermessage) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);
        if (type == 3) {
            mplew.write(channel - 1); // channel
            mplew.writeBool(megaEar);
        } else if (type == 6) {
            mplew.writeInt(0);
        } else if (type == 7) { // npc
            mplew.writeInt(npc);
        }
        return mplew.getPacket();
    }

    /**
     * Sends the Gachapon green message when a user uses a gachapon ticket.
     *
     * @param item
     * @param town
     * @param player
     * @return
     */
    public static byte[] gachaponMessage(Item item, String town, MapleCharacter player) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0B);
        mplew.writeMapleAsciiString(player.getName() + " : got a(n)");
        mplew.writeInt(0); //random?
        mplew.writeMapleAsciiString(town);
        CCommon.addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    public static byte[] itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleAsciiString(msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);
        if (item == null) {
            mplew.write(0);
        } else {
            mplew.write(item.getPosition());
            CCommon.addItemInfo(mplew, item, true);
        }
        return mplew.getPacket();
    }

    public static byte[] getMultiMegaphone(String[] messages, int channel, boolean showEar) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0A);
        if (messages[0] != null) {
            mplew.writeMapleAsciiString(messages[0]);
        }
        mplew.write(messages.length);
        for (int i = 1; i < messages.length; i++) {
            if (messages[i] != null) {
                mplew.writeMapleAsciiString(messages[i]);
            }
        }
        for (int i = 0; i < 10; i++) {
            mplew.write(channel - 1);
        }
        mplew.write(showEar ? 1 : 0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] incubatorResult() {//lol
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendOpcode.INCUBATOR_RESULT.getValue());
        mplew.skip(6);
        return mplew.getPacket();
    }

    public static byte[] owlOfMinerva(MapleClient c, int itemid, List<Pair<MaplePlayerShopItem, AbstractMapleMapObject>> hmsAvailable) {
        byte itemType = ItemConstants.getInventoryType(itemid)
                .getType();

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOP_SCANNER_RESULT.getValue()); // header.
        mplew.write(6);
        mplew.writeInt(0);
        mplew.writeInt(itemid);
        mplew.writeInt(hmsAvailable.size());
        for (Pair<MaplePlayerShopItem, AbstractMapleMapObject> hme : hmsAvailable) {
            MaplePlayerShopItem item = hme.getLeft();
            AbstractMapleMapObject mo = hme.getRight();

            if (mo instanceof MaplePlayerShop) {
                MaplePlayerShop ps = (MaplePlayerShop) mo;
                MapleCharacter owner = ps.getOwner();

                mplew.writeMapleAsciiString(owner.getName());
                mplew.writeInt(owner.getMapId());
                mplew.writeMapleAsciiString(ps.getDescription());
                mplew.writeInt(item.getBundles());
                mplew.writeInt(item.getItem()
                        .getQuantity());
                mplew.writeInt(item.getPrice());
                mplew.writeInt(owner.getId());
                mplew.write(owner.getClient()
                        .getChannel() - 1);
            } else {
                MapleHiredMerchant hm = (MapleHiredMerchant) mo;

                mplew.writeMapleAsciiString(hm.getOwner());
                mplew.writeInt(hm.getMapId());
                mplew.writeMapleAsciiString(hm.getDescription());
                mplew.writeInt(item.getBundles());
                mplew.writeInt(item.getItem()
                        .getQuantity());
                mplew.writeInt(item.getPrice());
                mplew.writeInt(hm.getOwnerId());
                mplew.write(hm.getChannel() - 1);
            }

            mplew.write(itemType);
            if (itemType == MapleInventoryType.EQUIP.getType()) {
                CCommon.addItemInfo(mplew, item.getItem(), true);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getOwlOpen(List<Integer> owlLeaderboards) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.SHOP_SCANNER_RESULT.getValue());
        mplew.write(7);
        mplew.write(owlLeaderboards.size());
        for (Integer i : owlLeaderboards) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();
    }

    // 0: Success
    // 1: The room is already closed.
    // 2: You can't enter the room due to full capacity.
    // 3: Other requests are being fulfilled this minute.
    // 4: You can't do it while you're dead.
    // 7: You are not allowed to trade other items at this point.
    // 17: You may not enter this store.
    // 18: The owner of the store is currently undergoing store maintenance. Please try again in a bit.
    // 23: This can only be used inside the Free Market.
    // default: This character is unable to do it.
    public static byte[] getOwlMessage(int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendOpcode.SHOP_LINK_RESULT.getValue());
        mplew.write(msg); // depending on the byte sent, a different message is sent.

        return mplew.getPacket();
    }

    public static byte[] sendYellowTip(String tip) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_WEEK_EVENT_MESSAGE.getValue());
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(tip);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] catchMessage(int message) { // not done, I guess
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BRIDLE_MOB_CATCH_FAIL.getValue());
        mplew.write(message); // 1 = too strong, 2 = Elemental Rock
        mplew.writeInt(0);//Maybe itemid?
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] addCard(boolean full, int cardid, int level) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendOpcode.MONSTER_BOOK_SET_CARD.getValue());
        mplew.write(full ? 0 : 1);
        mplew.writeInt(cardid);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static byte[] changeCover(int cardid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.MONSTER_BOOK_SET_COVER.getValue());
        mplew.writeInt(cardid);
        return mplew.getPacket();
    }

    public static byte[] getEnergy(String info, int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SESSION_VALUE.getValue());
        mplew.writeMapleAsciiString(info);
        mplew.writeMapleAsciiString(Integer.toString(amount));
        return mplew.getPacket();
    }

    public static byte[] showPedigree(MapleFamilyEntry entry) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_CHART_RESULT.getValue());
        mplew.writeInt(entry.getChrId()); //ID of viewed player's pedigree, can't be leader?
        List<MapleFamilyEntry> superJuniors = new ArrayList<>(4);
        boolean hasOtherJunior = false;
        int entryCount = 2; //2 guaranteed, leader and self
        entryCount += Math.min(2, entry.getTotalSeniors());
        //needed since MaplePacketLittleEndianWriter doesn't have any seek functionality
        if (entry.getSenior() != null) {
            if (entry.getSenior()
                    .getJuniorCount() == 2) {
                entryCount++;
                hasOtherJunior = true;
            }
        }
        for (MapleFamilyEntry junior : entry.getJuniors()) {
            if (junior == null) {
                continue;
            }
            entryCount++;
            for (MapleFamilyEntry superJunior : junior.getJuniors()) {
                if (superJunior == null) {
                    continue;
                }
                entryCount++;
                superJuniors.add(superJunior);
            }
        }
        //write entries
        boolean missingEntries = entryCount == 2; //pedigree requires at least 3 entries to show leader, might only have 2 if leader's juniors leave
        if (missingEntries) {
            entryCount++;
        }
        mplew.writeInt(entryCount); //player count
        addPedigreeEntry(mplew, entry.getFamily()
                .getLeader());
        if (entry.getSenior() != null) {
            if (entry.getSenior()
                    .getSenior() != null) {
                addPedigreeEntry(mplew, entry.getSenior()
                        .getSenior());
            }
            addPedigreeEntry(mplew, entry.getSenior());
        }
        addPedigreeEntry(mplew, entry);
        if (hasOtherJunior) { //must be sent after own entry
            entry.getSenior()
                    .getOtherJunior(entry)
                    .ifPresent(oj -> addPedigreeEntry(mplew, oj));
        }
        if (missingEntries) {
            addPedigreeEntry(mplew, entry);
        }
        for (MapleFamilyEntry junior : entry.getJuniors()) {
            if (junior == null) {
                continue;
            }
            addPedigreeEntry(mplew, junior);
            for (MapleFamilyEntry superJunior : junior.getJuniors()) {
                if (superJunior != null) {
                    addPedigreeEntry(mplew, superJunior);
                }
            }
        }
        mplew.writeInt(2 + superJuniors.size()); //member info count
        // 0 = total seniors, -1 = total members, otherwise junior count of ID
        mplew.writeInt(-1);
        mplew.writeInt(entry.getFamily()
                .getTotalMembers());
        mplew.writeInt(0);
        mplew.writeInt(entry.getTotalSeniors()); //client subtracts provided seniors
        for (MapleFamilyEntry superJunior : superJuniors) {
            mplew.writeInt(superJunior.getChrId());
            mplew.writeInt(superJunior.getTotalJuniors());
        }
        mplew.writeInt(0); //another loop count (entitlements used)
        //mplew.writeInt(1); //entitlement index
        //mplew.writeInt(2); //times used
        mplew.writeShort(entry.getJuniorCount() >= 2 ? 0 : 2); //0 disables Add button (only if viewing own pedigree)
        return mplew.getPacket();
    }

    public static byte[] getFamilyInfo(MapleFamilyEntry f) {
        if (f == null) {
            return getEmptyFamilyInfo();
        }
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_INFO_RESULT.getValue());
        mplew.writeInt(f.getReputation()); // cur rep left
        mplew.writeInt(f.getTotalReputation()); // tot rep left
        mplew.writeInt(f.getTodaysRep()); // todays rep
        mplew.writeShort(f.getJuniorCount()); // juniors added
        mplew.writeShort(2); // juniors allowed
        mplew.writeShort(0); //Unknown
        mplew.writeInt(f.getFamily()
                .getLeader()
                .getChrId()); // Leader ID (Allows setting message)
        mplew.writeMapleAsciiString(f.getFamily()
                .getName());
        mplew.writeMapleAsciiString(f.getFamily()
                .getMessage()); //family message
        mplew.writeInt(MapleFamilyEntitlement.values().length); //Entitlement info count
        for (MapleFamilyEntitlement entitlement : MapleFamilyEntitlement.values()) {
            mplew.writeInt(entitlement.ordinal()); //ID
            mplew.writeInt(f.isEntitlementUsed(entitlement) ? 1 : 0); //Used count
        }
        return mplew.getPacket();
    }

    private static byte[] getEmptyFamilyInfo() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_INFO_RESULT.getValue());
        mplew.writeInt(0); // cur rep left
        mplew.writeInt(0); // tot rep left
        mplew.writeInt(0); // todays rep
        mplew.writeShort(0); // juniors added
        mplew.writeShort(2); // juniors allowed
        mplew.writeShort(0); //Unknown
        mplew.writeInt(0); // Leader ID (Allows setting message)
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString(""); //family message
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Family Result Message
     * <p>
     * Possible values for <code>type</code>:<br>
     * 64: You cannot add this character as a junior.
     * 65: The name could not be found or is not online.
     * 66: You belong to the same family.
     * 67: You do not belong to the same family.<br>
     * 69: The character you wish to add as\r\na Junior must be in the same
     * map.<br>
     * 70: This character is already a Junior of another character.<br>
     * 71: The Junior you wish to add\r\nmust be at a lower rank.<br>
     * 72: The gap between you and your\r\njunior must be within 20 levels.<br>
     * 73: Another character has requested to add this character.\r\nPlease try
     * again later.<br>
     * 74: Another character has requested a summon.\r\nPlease try again
     * later.<br>
     * 75: The summons has failed. Your current location or state does not allow
     * a summons.<br>
     * 76: The family cannot extend more than 1000 generations from above and
     * below.<br>
     * 77: The Junior you wish to add\r\nmust be over Level 10.<br>
     * 78: You cannot add a Junior \r\nthat has requested to change worlds.<br>
     * 79: You cannot add a Junior \r\nsince you've requested to change
     * worlds.<br>
     * 80: Separation is not possible due to insufficient Mesos.\r\nYou will
     * need %d Mesos to\r\nseparate with a Senior.<br>
     * 81: Separation is not possible due to insufficient Mesos.\r\nYou will
     * need %d Mesos to\r\nseparate with a Junior.<br>
     * 82: The Entitlement does not apply because your level does not match the
     * corresponding area.<br>
     *
     * @param type The type
     * @return Family Result packet
     */
    public static byte[] sendFamilyMessage(int type, int mesos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.FAMILY_RESULT.getValue());
        mplew.writeInt(type);
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] sendFamilyInvite(int playerId, String inviter) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_REQUEST.getValue());
        mplew.writeInt(playerId);
        mplew.writeMapleAsciiString(inviter);
        return mplew.getPacket();
    }

    public static byte[] sendFamilyJoinResponse(boolean accepted, String added) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_REQUEST_RESULT.getValue());
        mplew.write(accepted ? 1 : 0);
        mplew.writeMapleAsciiString(added);
        return mplew.getPacket();
    }

    public static byte[] getSeniorMessage(String name) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_JOIN_ACCEPTED.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] loadFamily(MapleCharacter player) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_PRIVILEGE_LIST.getValue());
        mplew.writeInt(MapleFamilyEntitlement.values().length);
        for (int i = 0; i < MapleFamilyEntitlement.values().length; i++) {
            MapleFamilyEntitlement entitlement = MapleFamilyEntitlement.values()[i];
            mplew.write(i <= 1 ? 1 : 2); //type
            mplew.writeInt(entitlement.getRepCost());
            mplew.writeInt(entitlement.getUsageLimit());
            mplew.writeMapleAsciiString(entitlement.getName());
            mplew.writeMapleAsciiString(entitlement.getDescription());
        }
        return mplew.getPacket();
    }

    public static byte[] sendGainRep(int gain, String from) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_REP_GAIN.getValue());
        mplew.writeInt(gain);
        mplew.writeMapleAsciiString(from);
        return mplew.getPacket();
    }

    public static byte[] sendFamilyLoginNotice(String name, boolean loggedIn) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_NOTIFY_LOGIN_OR_LOGOUT.getValue());
        mplew.writeBool(loggedIn);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] sendFamilySummonRequest(String familyName, String from) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAMILY_SUMMON_REQUEST.getValue());
        mplew.writeMapleAsciiString(from);
        mplew.writeMapleAsciiString(familyName);
        return mplew.getPacket();
    }

    /**
     * Sends a "levelup" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Family> ? has reached Lv.
     * ?.<br> - The Reps you have received from ? will be reduced in half. 1:
     * <Family> ? has reached Lv. ?.<br> 2: <Guild> ? has reached Lv. ?.<br>
     *
     * @param type The type
     * @return The "levelup" packet.
     */
    public static byte[] levelUpMessage(int type, int level, String charname) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NOTIFY_LEVELUP.getValue());
        mplew.write(type);
        mplew.writeInt(level);
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    /**
     * Sends a "married" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Guild ? is now married.
     * Please congratulate them.<br> 1: <Family ? is now married. Please
     * congratulate them.<br>
     *
     * @param type The type
     * @return The "married" packet.
     */
    public static byte[] marriageMessage(int type, String charname) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NOTIFY_MARRIAGE.getValue());
        mplew.write(type);  // 0: guild, 1: family
        mplew.writeMapleAsciiString("> " + charname); //To fix the stupid packet lol

        return mplew.getPacket();
    }

    /**
     * Sends a "job advance" packet to the guild or family.
     * <p>
     * Possible values for <code>type</code>:<br> 0: <Guild ? has advanced to
     * a(an) ?.<br> 1: <Family ? has advanced to a(an) ?.<br>
     *
     * @param type The type
     * @return The "job advance" packet.
     */
    public static byte[] jobMessage(int type, int job, String charname) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NOTIFY_JOB_CHANGE.getValue());
        mplew.write(type);
        mplew.writeInt(job); //Why fking int?
        mplew.writeMapleAsciiString("> " + charname); //To fix the stupid packet lol

        return mplew.getPacket();
    }

    /**
     * Sends a Avatar Super Megaphone packet.
     *
     * @param chr     The character name.
     * @param medal   The medal text.
     * @param channel Which channel.
     * @param itemId  Which item used.
     * @param message The message sent.
     * @param ear     Whether or not the ear is shown for whisper.
     * @return
     */
    public static byte[] getAvatarMega(MapleCharacter chr, String medal, int channel, int itemId, List<String> message, boolean ear) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_AVATAR_MEGAPHONE.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(medal + chr.getName());
        for (String s : message) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeInt(channel - 1); // channel
        mplew.writeBool(ear);
        CCommon.addCharLook(mplew, chr, true);
        return mplew.getPacket();
    }

    /*
     * Sends a packet to remove the tiger megaphone
     * @return
     */
    public static byte[] byeAvatarMega() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CLEAR_AVATAR_MEGAPHONE.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] showNameChangeCancel(boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_NAME_CHANGE_RESULT.getValue());
        mplew.writeBool(success);
        if (!success) {
            mplew.write(0);
        }
        //mplew.writeMapleAsciiString("Custom message."); //only if ^ != 0
        return mplew.getPacket();
    }

    public static byte[] showWorldTransferCancel(boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_TRANSFER_WORLD_RESULT.getValue());
        mplew.writeBool(success);
        if (!success) {
            mplew.write(0);
        }
        //mplew.writeMapleAsciiString("Custom message."); //only if ^ != 0
        return mplew.getPacket();
    }

    public static byte[] sendPolice() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FAKE_GM_NOTICE.getValue());
        mplew.write(0);//doesn't even matter what value
        return mplew.getPacket();
    }

    public static byte[] onNewYearCardRes(MapleCharacter user, NewYearCardRecord newyear, int mode, int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NEW_YEAR_CARD_RES.getValue());
        mplew.write(mode);
        switch (mode) {
            case 4: // Successfully sent a New Year Card\r\n to %s.
            case 6: // Successfully received a New Year Card.
                CCommon.encodeNewYearCard(newyear, mplew);
                break;

            case 8: // Successfully deleted a New Year Card.
                mplew.writeInt(newyear.getId());
                break;

            case 5: // Nexon's stupid and makes 4 modes do the same operation..
            case 7:
            case 9:
            case 0xB:
                // 0x10: You have no free slot to store card.\r\ntry later on please.
                // 0x11: You have no card to send.
                // 0x12: Wrong inventory information !
                // 0x13: Cannot find such character !
                // 0x14: Incoherent Data !
                // 0x15: An error occured during DB operation.
                // 0x16: An unknown error occured !
                // 0xF: You cannot send a card to yourself !
                mplew.write(msg);
                break;

            case 0xA:   // GetUnreceivedList_Done
                int nSN = 1;
                mplew.writeInt(nSN);
                if ((nSN - 1) <= 98 && nSN > 0) {//lol nexon are you kidding
                    for (int i = 0; i < nSN; i++) {
                        mplew.writeInt(newyear.getId());
                        mplew.writeInt(newyear.getSenderId());
                        mplew.writeMapleAsciiString(newyear.getSenderName());
                    }
                }
                break;

            case 0xC:   // NotiArrived
                mplew.writeInt(newyear.getId());
                mplew.writeMapleAsciiString(newyear.getSenderName());
                break;

            case 0xD:   // BroadCast_AddCardInfo
                mplew.writeInt(newyear.getId());
                mplew.writeInt(user.getId());
                break;

            case 0xE:   // BroadCast_RemoveCardInfo
                mplew.writeInt(newyear.getId());
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] setExtraPendantSlot(boolean toggleExtraSlot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_EXTRA_PENDANT_SLOT.getValue());
        mplew.writeBool(toggleExtraSlot);
        return mplew.getPacket();
    }

    public static byte[] earnTitleMessage(String msg) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SCRIPT_PROGRESS_MESSAGE.getValue());
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] sendPolice(String text) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DATA_CRC_CHECK_FAILED.getValue());
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MACRO_SYS_DATA_INIT.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count);
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }
        return mplew.getPacket();
    }

    private static void addPedigreeEntry(MaplePacketLittleEndianWriter mplew, MapleFamilyEntry entry) {
        MapleCharacter chr = entry.getChr();
        boolean isOnline = chr != null;
        mplew.writeInt(entry.getChrId()); //ID
        mplew.writeInt(entry.getSenior() != null ? entry.getSenior()
                .getChrId() : 0); //parent ID
        mplew.writeShort(entry.getJob()
                .getId()); //job id
        mplew.write(entry.getLevel()); //level
        mplew.writeBool(isOnline); //isOnline
        mplew.writeInt(entry.getReputation()); //current rep
        mplew.writeInt(entry.getTotalReputation()); //total rep
        mplew.writeInt(entry.getRepsToSenior()); //reps recorded to senior
        mplew.writeInt(entry.getTodaysRep());
        mplew.writeInt(isOnline ? ((chr.isAwayFromWorld() || chr.getCashShop()
                .isOpened()) ? -1 : chr.getClient()
                .getChannel() - 1) : 0);
        mplew.writeInt(isOnline ? (int) (chr.getLoggedInTime() / 60000) : 0); //time online in minutes
        mplew.writeMapleAsciiString(entry.getName()); //name
    }

    private static void addPartyStatus(int forchannel, MapleParty party, LittleEndianWriter lew, boolean leaving) {
        List<MaplePartyCharacter> partymembers = new ArrayList<>(party.getMembers());
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(CCommon.getRightPaddedStr(partychar.getName(), '\0', 13));
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party.getLeader()
                .getId());
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapId());
            } else {
                lew.writeInt(0);
            }
        }

        Map<Integer, MapleDoor> partyDoors = party.getDoors();
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                if (partyDoors.size() > 0) {
                    MapleDoor door = partyDoors.get(partychar.getId());
                    if (door != null) {
                        MapleDoorObject mdo = door.getTownDoor();
                        lew.writeInt(mdo.getTown()
                                .getId());
                        lew.writeInt(mdo.getArea()
                                .getId());
                        lew.writeInt(mdo.getPosition().x);
                        lew.writeInt(mdo.getPosition().y);
                    } else {
                        lew.writeInt(999999999);
                        lew.writeInt(999999999);
                        lew.writeInt(0);
                        lew.writeInt(0);
                    }
                } else {
                    lew.writeInt(999999999);
                    lew.writeInt(999999999);
                    lew.writeInt(0);
                    lew.writeInt(0);
                }
            } else {
                lew.writeInt(999999999);
                lew.writeInt(999999999);
                lew.writeInt(0);
                lew.writeInt(0);
            }
        }
    }

    private static void getGuildInfo(final MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
        mplew.writeInt(guild.getId());
        mplew.writeMapleAsciiString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(guild.getRankTitle(i));
        }
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.write(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : members) {
            mplew.writeAsciiString(CCommon.getRightPaddedStr(mgc.getName(), '\0', 13));
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(guild.getSignature());
            mplew.writeInt(mgc.getAllianceRank());
        }
        mplew.writeInt(guild.getCapacity());
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        mplew.writeMapleAsciiString(guild.getNotice());
        mplew.writeInt(guild.getGP());
        mplew.writeInt(guild.getAllianceId());
    }

    private static void updatePlayerStat(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, Pair<MapleStat, Integer> statupdate) {
        switch (statupdate.getLeft()) {
            case SKIN:
            case LEVEL:
                mplew.write(statupdate.getRight()
                        .byteValue());
                break;
            case JOB:
            case STR:
            case DEX:
            case INT:
            case LUK:
            case HP:
            case MAXHP:
            case MP:
            case MAXMP:
            case AVAILABLEAP:
            case FAME:
                mplew.writeShort(statupdate.getRight()
                        .shortValue());
                break;
            case AVAILABLESP:
                if (GameConstants.hasExtendedSPTable(chr.getJob())) {
                    //TODO Evan
//                    mplew.write(chr.getRemainingSpSize());
//                    for(int i = 0; i < chr.getRemainingSps().length; i++){
//                        if(chr.getRemainingSpBySkill(i) > 0){
//                            mplew.write(i);
//                            mplew.write(chr.getRemainingSpBySkill(i));
//                        }
//                    }
                } else {
                    mplew.writeShort(statupdate.getRight()
                            .shortValue());
                }
                break;
            case FACE:
            case HAIR:
            case EXP:
            case MESO:
            case GACHAEXP:
                mplew.writeInt(statupdate.getRight());
                break;
            case PETSN:
                mplew.writeLong(0);
                break;
            case PETSN2:
                mplew.writeLong(0);
                break;
            case PETSN3:
                mplew.writeLong(0);
                break;
        }
    }

    /**
     * Gets a server message packet.
     *
     * @param message The message to convey.
     * @return The server message packet.
     */
    public static byte[] serverMessage(String message) {
        return serverMessage(4, (byte) 0, message, true, false, 0);
    }

    /**
     * Gets a server notice packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
     * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
     * 5: Pink Text<br> 6: Lightblue Text
     *
     * @param type    The type of the notice.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static byte[] serverNotice(int type, String message) {
        return serverMessage(type, (byte) 0, message, false, false, 0);
    }

    /**
     * Gets a server notice packet.
     * <p>
     * Possible values for <code>type</code>:<br> 0: [Notice]<br> 1: Popup<br>
     * 2: Megaphone<br> 3: Super Megaphone<br> 4: Scrolling message at top<br>
     * 5: Pink Text<br> 6: Lightblue Text
     *
     * @param type    The type of the notice.
     * @param channel The channel this notice was sent on.
     * @param message The message to convey.
     * @return The server notice packet.
     */
    public static byte[] serverNotice(int type, String message, int npc) {
        return serverMessage(type, 0, message, false, false, npc);
    }

    public static byte[] serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false, false, 0);
    }

    public static byte[] serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, false, smegaEar, 0);
    }

    /**
     * Gets a packet telling the client to show a item gain.
     *
     * @param itemId   The ID of the item gained.
     * @param quantity How many items gained.
     * @return The item gain packet.
     */
    public static byte[] getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static byte[] onNewYearCardRes(MapleCharacter user, int cardId, int mode, int msg) {
        NewYearCardRecord newyear = user.getNewYearRecord(cardId)
                .orElseThrow();
        return onNewYearCardRes(user, newyear, mode, msg);
    }

    public static void addThread(final MaplePacketLittleEndianWriter mplew, ResultSet rs) throws SQLException {
        mplew.writeInt(rs.getInt("localthreadid"));
        mplew.writeInt(rs.getInt("postercid"));
        mplew.writeMapleAsciiString(rs.getString("name"));
        mplew.writeLong(CCommon.getTime(rs.getLong("timestamp")));
        mplew.writeInt(rs.getInt("icon"));
        mplew.writeInt(rs.getInt("replycount"));
    }

    public static byte[] getInventoryFull() {
        return modifyInventory(true, Collections.emptyList());
    }

    public static byte[] getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static byte[] showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    /**
     * Gets an empty stat update.
     *
     * @return The empty stat update packet.
     */
    public static byte[] enableActions() {
        return updatePlayerStats(Collections.emptyList(), true, null);
    }
}
