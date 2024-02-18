package connection.packets;

import connection.constants.SendOpcode;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CUserLocal {
    public static byte[] dojoWarpUp() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DOJO_WARP_UP.getValue());
        mplew.write(0);
        mplew.write(6);
        return mplew.getPacket();
    }

    public static byte[] addQuestTimeLimit(final short quest, final int time) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(6);
        mplew.writeShort(1);//Size but meh, when will there be 2 at the same time? And it won't even replace the old one :)
        mplew.writeShort(quest);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] removeQuestTimeLimit(final short quest) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(7);
        mplew.writeShort(1);//Position
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    public static byte[] updateQuestFinish(short quest, int npc, short nextquest) { //Check
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue()); //0xF2 in v95
        mplew.write(8);//0x0A in v95
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeShort(nextquest);
        return mplew.getPacket();
    }

    public static byte[] questError(short quest) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x0A);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    public static byte[] questFailure(byte type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(type);//0x0B = No meso, 0x0D = Worn by character, 0x0E = Not having the item ?
        return mplew.getPacket();
    }

    public static byte[] questExpire(short quest) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x0F);
        mplew.writeShort(quest);
        return mplew.getPacket();
    }

    /**
     * Sends a player hint.
     *
     * @param hint   The hint it's going to send.
     * @param width  How tall the box is going to be.
     * @param height How long the box is going to be.
     * @return The player hint packet.
     */
    public static byte[] sendHint(String hint, int width, int height) {
        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);
        return mplew.getPacket();
    }

    // MAKER_RESULT packets thanks to Arnah (Vertisy)
    public static byte[] makerResult(boolean success, int itemMade, int itemCount, int mesos, List<Pair<Integer, Integer>> itemsLost, int catalystID, List<Integer> INCBuffGems) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAKER_RESULT.getValue());
        mplew.writeInt(success ? 0 : 1); // 0 = success, 1 = fail
        mplew.writeInt(1); // 1 or 2 doesn't matter, same methods
        mplew.writeBool(!success);
        if (success) {
            mplew.writeInt(itemMade);
            mplew.writeInt(itemCount);
        }
        mplew.writeInt(itemsLost.size()); // Loop
        for (Pair<Integer, Integer> item : itemsLost) {
            mplew.writeInt(item.getLeft());
            mplew.writeInt(item.getRight());
        }
        mplew.writeInt(INCBuffGems.size());
        for (Integer gem : INCBuffGems) {
            mplew.writeInt(gem);
        }
        if (catalystID != -1) {
            mplew.write(1); // stimulator
            mplew.writeInt(catalystID);
        } else {
            mplew.write(0);
        }

        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] makerResultCrystal(int itemIdGained, int itemIdLost) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAKER_RESULT.getValue());
        mplew.writeInt(0); // Always successful!
        mplew.writeInt(3); // Monster Crystal
        mplew.writeInt(itemIdGained);
        mplew.writeInt(itemIdLost);
        return mplew.getPacket();
    }

    public static byte[] makerResultDesynth(int itemId, int mesos, List<Pair<Integer, Integer>> itemsGained) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAKER_RESULT.getValue());
        mplew.writeInt(0); // Always successful!
        mplew.writeInt(4); // Mode Desynth
        mplew.writeInt(itemId); // Item desynthed
        mplew.writeInt(itemsGained.size()); // Loop of items gained, (int, int)
        for (Pair<Integer, Integer> item : itemsGained) {
            mplew.writeInt(item.getLeft());
            mplew.writeInt(item.getRight());
        }
        mplew.writeInt(mesos); // Mesos spent.
        return mplew.getPacket();
    }

    public static byte[] makerEnableActions() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAKER_RESULT.getValue());
        mplew.writeInt(0); // Always successful!
        mplew.writeInt(0); // Monster Crystal
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Sends a UI utility. 0x01 - Equipment Inventory. 0x02 - Stat Window. 0x03
     * - Skill Window. 0x05 - Keyboard Settings. 0x06 - Quest window. 0x09 -
     * Monsterbook Window. 0x0A - Char Info 0x0B - Guild BBS 0x12 - Monster
     * Carnival Window 0x16 - Party Search. 0x17 - Item Creation Window. 0x1A -
     * My Ranking O.O 0x1B - Family Window 0x1C - Family Pedigree 0x1D - GM
     * Story Board /funny shet 0x1E - Envelop saying you got mail from an admin.
     * lmfao 0x1F - Medal Window 0x20 - Maple Event (???) 0x21 - Invalid Pointer
     * Crash
     *
     * @param ui
     * @return
     */
    public static byte[] openUI(byte ui) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.OPEN_UI.getValue());
        mplew.write(ui);
        return mplew.getPacket();
    }

    public static byte[] lockUI(boolean enable) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.LOCK_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] disableUI(boolean enable) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] spawnGuide(boolean spawn) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.SPAWN_GUIDE.getValue());
        if (spawn) {
            mplew.write(1);
        } else {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static byte[] talkGuide(String talk) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TALK_GUIDE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(talk);
        mplew.write(new byte[]{(byte) 0xC8, 0, 0, 0, (byte) 0xA0, (byte) 0x0F, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] guideHint(int hint) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendOpcode.TALK_GUIDE.getValue());
        mplew.write(1);
        mplew.writeInt(hint);
        mplew.writeInt(7000);
        return mplew.getPacket();
    }

    public static byte[] showCombo(int count) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.SHOW_COMBO.getValue());
        mplew.writeInt(count);
        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int sid, int time) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(time);//Int in v97
        return mplew.getPacket();
    }
}
