package connection.packets;

import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Collection;

public class CTrunkDlg {
    static byte[] getStorage(int npcId, byte slots, Collection<Item> items, int meso) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        mplew.writeShort(0);
        mplew.write((byte) items.size());
        for (Item item : items) {
            CCommon.addItemInfo(mplew, item, true);
        }
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    /*
     * 0x0A = Inv full
     * 0x0B = You do not have enough mesos
     * 0x0C = One-Of-A-Kind error
     */
    static byte[] getStorageError(byte i) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(i);
        return mplew.getPacket();
    }

    static byte[] mesoStorage(byte slots, int meso) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    static byte[] storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(0xD);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (Item item : items) {
            CCommon.addItemInfo(mplew, item, true);
        }
        return mplew.getPacket();
    }

    static byte[] takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(0x9);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (Item item : items) {
            CCommon.addItemInfo(mplew, item, true);
        }
        return mplew.getPacket();
    }

    static byte[] arrangeStorage(byte slots, Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.STORAGE.getValue());
        mplew.write(0xF);
        mplew.write(slots);
        mplew.write(124);
        mplew.skip(10);
        mplew.write(items.size());
        for (Item item : items) {
            CCommon.addItemInfo(mplew, item, true);
        }
        mplew.write(0);
        return mplew.getPacket();
    }
}
