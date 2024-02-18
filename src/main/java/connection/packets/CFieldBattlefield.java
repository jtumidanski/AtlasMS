package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldBattlefield {
    public static byte[] sheepRanchInfo(byte wolf, byte sheep) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHEEP_RANCH_INFO.getValue());
        mplew.write(wolf);
        mplew.write(sheep);
        return mplew.getPacket();
    }

    public static byte[] sheepRanchClothes(int id, byte clothes) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHEEP_RANCH_CLOTHES.getValue());
        mplew.writeInt(id); //Character id
        mplew.write(clothes); //0 = sheep, 1 = wolf, 2 = Spectator (wolf without wool)
        return mplew.getPacket();
    }
}
