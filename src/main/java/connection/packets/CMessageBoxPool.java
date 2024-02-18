package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class CMessageBoxPool {
    public static byte[] sendCannotSpawnKite() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANNOT_SPAWN_KITE.getValue());
        return mplew.getPacket();
    }

    public static byte[] spawnKite(int oid, int itemid, String name, String msg, Point pos, int ft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_KITE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(msg);
        mplew.writeMapleAsciiString(name);
        mplew.writeShort(pos.x);
        mplew.writeShort(ft);
        return mplew.getPacket();
    }

    public static byte[] removeKite(int objectid, int animationType) {    // thanks to Arnah (Vertisy)
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_KITE.getValue());
        mplew.write(animationType); // 0 is 10/10, 1 just vanishes
        mplew.writeInt(objectid);
        return mplew.getPacket();
    }
}
