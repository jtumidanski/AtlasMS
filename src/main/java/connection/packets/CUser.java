package connection.packets;

import client.MapleCharacter;
import client.inventory.Equip;
import client.inventory.MaplePet;
import connection.constants.SendOpcode;
import server.maps.MapleDragon;
import server.maps.MaplePlayerShop;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class CUser {
    /**
     * Gets a general chat packet.
     *
     * @param cidfrom The character ID who sent the chat.
     * @param text    The text of the chat.
     * @param whiteBG
     * @param show
     * @return The general chat packet.
     */
    public static byte[] getChatText(int cidfrom, String text, boolean gm, int show) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.writeBool(gm);
        mplew.writeMapleAsciiString(text);
        mplew.write(show);
        return mplew.getPacket();
    }

    public static byte[] useChalkboard(MapleCharacter chr, boolean close) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHALKBOARD.getValue());
        mplew.writeInt(chr.getId());
        if (close) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getChalkboard()
                    .orElse(""));
        }
        return mplew.getPacket();
    }

    public static byte[] updatePlayerShopBox(MaplePlayerShop shop) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(shop.getOwner()
                .getId());

        updatePlayerShopBoxInfo(mplew, shop);
        return mplew.getPacket();
    }

    public static byte[] removePlayerShopBox(MaplePlayerShop shop) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(shop.getOwner()
                .getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] addOmokBox(MapleCharacter chr, int ammount, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(chr.getId());
        CCommon.addAnnounceBox(mplew, chr.getMiniGame(), ammount, type);
        return mplew.getPacket();
    }

    public static byte[] addMatchCardBox(MapleCharacter chr, int ammount, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(chr.getId());
        CCommon.addAnnounceBox(mplew, chr.getMiniGame(), ammount, type);
        return mplew.getPacket();
    }

    public static byte[] removeMinigameBox(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chr, Equip.ScrollResult scrollSuccess, boolean legendarySpirit, boolean whiteScroll) {   // thanks to Rien dev team
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.writeBool(scrollSuccess == Equip.ScrollResult.SUCCESS);
        mplew.writeBool(scrollSuccess == Equip.ScrollResult.CURSE);
        mplew.writeBool(legendarySpirit);
        mplew.writeBool(whiteScroll);
        return mplew.getPacket();
    }

    public static byte[] showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_PET.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getPetIndex(pet));
        if (remove) {
            mplew.write(0);
            mplew.write(hunger ? 1 : 0);
        } else {
            CCommon.addPetInfo(mplew, pet, true);
        }
        return mplew.getPacket();
    }

    public static byte[] spawnDragon(MapleDragon dragon) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_DRAGON.getValue());
        mplew.writeInt(dragon.getOwner()
                .getId());//objectid = owner id
        mplew.writeShort(dragon.getPosition().x);
        mplew.writeShort(0);
        mplew.writeShort(dragon.getPosition().y);
        mplew.writeShort(0);
        mplew.write(dragon.getStance());
        mplew.write(0);
        mplew.writeShort(dragon.getOwner()
                .getJob()
                .getId());
        return mplew.getPacket();
    }

    public static byte[] moveDragon(MapleDragon dragon, Point startPos, SeekableLittleEndianAccessor movementSlea, long movementDataLength) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_DRAGON.getValue());
        mplew.writeInt(dragon.getOwner()
                .getId());
        mplew.writePos(startPos);
        CCommon.rebroadcastMovementList(mplew, movementSlea, movementDataLength);
        return mplew.getPacket();
    }

    /**
     * Sends a request to remove Mir<br>
     *
     * @param charid - Needs the specific Character ID
     * @return The packet
     */
    public static byte[] removeDragon(int chrid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_DRAGON.getValue());
        mplew.writeInt(chrid);
        return mplew.getPacket();
    }

    public static byte[] showHpHealed(int cid, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x0A); //Type
        mplew.write(amount);
        return mplew.getPacket();
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid, byte direction) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effectid); //buff level
        mplew.writeInt(skillid);
        mplew.write(direction);
        mplew.write(1);
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] showBuffeffect(int cid, int skillid, int skilllv, int effectid, byte direction) {   // updated packet structure found thanks to Rien dev team
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(0);
        mplew.write(skilllv);
        mplew.write(direction);

        return mplew.getPacket();
    }

    public static byte[] showBerserk(int cid, int skilllevel, boolean Berserk) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] showPetLevelUp(MapleCharacter chr, byte index) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(4);
        mplew.write(0);
        mplew.write(index);
        return mplew.getPacket();
    }

    public static byte[] showForeignCardEffect(int id) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(id);
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static byte[] showForeignInfo(int cid, String path) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x17);
        mplew.writeMapleAsciiString(path);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static byte[] showForeignBuybackEffect(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(11);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] showForeignMakerEffect(int cid, boolean makerSucceeded) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(16);
        mplew.writeInt(makerSucceeded ? 0 : 1);
        return mplew.getPacket();
    }

    public static byte[] showForeignEffect(int effect) {
        return showForeignEffect(-1, effect);
    }

    public static byte[] showForeignEffect(int cid, int effect) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static byte[] showRecovery(int cid, byte amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x0A);
        mplew.write(amount);
        return mplew.getPacket();
    }

    public static byte[] showOwnBuffEffect(int skillid, int effectid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(0xA9);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] showOwnBerserk(int skilllevel, boolean Berserk) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(1);
        mplew.writeInt(1320006);
        mplew.write(0xA9);
        mplew.write(skilllevel);
        mplew.write(Berserk ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] showOwnPetLevelUp(byte index) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(4);
        mplew.write(0);
        mplew.write(index); // Pet Index
        return mplew.getPacket();
    }

    public static byte[] showGainCard() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0D);
        return mplew.getPacket();
    }

    public static byte[] showIntro(String path) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(path);
        return mplew.getPacket();
    }

    public static byte[] showInfo(String path) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x17);
        mplew.writeMapleAsciiString(path);
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static byte[] showBuybackEffect() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(11);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /**
     * 0 = Levelup 6 = Exp did not drop (Safety Charms) 7 = Enter portal sound
     * 8 = Job change 9 = Quest complete 10 = Recovery 11 = Buff effect
     * 14 = Monster book pickup 15 = Equipment levelup 16 = Maker Skill Success
     * 17 = Buff effect w/ sfx 19 = Exp card [500, 200, 50] 21 = Wheel of destiny
     * 26 = Spirit Stone
     *
     * @param effect
     * @return
     */
    public static byte[] showSpecialEffect(int effect) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);
        return mplew.getPacket();
    }

    public static byte[] showMakerEffect(boolean makerSucceeded) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(16);
        mplew.writeInt(makerSucceeded ? 0 : 1);
        return mplew.getPacket();
    }

    public static byte[] showOwnRecovery(byte heal) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x0A);
        mplew.write(heal);
        return mplew.getPacket();
    }

    public static byte[] showWheelsLeft(int left) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x15);
        mplew.write(left);
        return mplew.getPacket();
    }

    private static void updatePlayerShopBoxInfo(final MaplePacketLittleEndianWriter mplew, MaplePlayerShop shop) {
        byte[] roomInfo = shop.getShopRoomInfo();

        mplew.write(4);
        mplew.writeInt(shop.getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        mplew.write(0);                 // pw
        mplew.write(shop.getItemId() % 100);
        mplew.write(roomInfo[0]);       // curPlayers
        mplew.write(roomInfo[1]);       // maxPlayers
        mplew.write(0);
    }

    public static byte[] playPortalSound() {
        return showSpecialEffect(7);
    }

    public static byte[] showMonsterBookPickup() {
        return showSpecialEffect(14);
    }

    public static byte[] showEquipmentLevelUp() {
        return showSpecialEffect(15);
    }

    public static byte[] showItemLevelup() {
        return showSpecialEffect(15);
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid) {
        return showBuffeffect(cid, skillid, effectid, (byte) 3);
    }
}
