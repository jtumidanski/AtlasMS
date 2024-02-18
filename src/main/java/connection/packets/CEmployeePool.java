package connection.packets;

import connection.constants.SendOpcode;
import server.maps.MapleHiredMerchant;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CEmployeePool {
    public static byte[] spawnHiredMerchantBox(MapleHiredMerchant hm) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writeShort((short) hm.getPosition()
                .getX());
        mplew.writeShort((short) hm.getPosition()
                .getY());
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(hm.getOwner());
        mplew.write(0x05);
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(hm.getItemId() % 100);
        mplew.write(new byte[]{1, 4});
        return mplew.getPacket();
    }

    public static byte[] removeHiredMerchantBox(int id) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static byte[] updateHiredMerchantBox(MapleHiredMerchant hm) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());

        updateHiredMerchantBoxInfo(mplew, hm);
        return mplew.getPacket();
    }

    private static void updateHiredMerchantBoxInfo(MaplePacketLittleEndianWriter mplew, MapleHiredMerchant hm) {
        byte[] roomInfo = hm.getShopRoomInfo();

        mplew.write(5);
        mplew.writeInt(hm.getObjectId());
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(hm.getItemId() % 100);
        mplew.write(roomInfo);    // visitor capacity here, thanks GabrielSin
    }
}
