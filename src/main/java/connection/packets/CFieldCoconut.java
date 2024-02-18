package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldCoconut {
    public static byte[] hitCoconut(boolean spawn, int id, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.COCONUT_HIT.getValue());
        if (spawn) {
            mplew.writeShort(-1);
            mplew.writeShort(5000);
            mplew.write(0);
        } else {
            mplew.writeShort(id);
            mplew.writeShort(1000);//delay till you can attack again!
            mplew.write(type); // What action to do for the coconut.
        }
        return mplew.getPacket();
    }

    public static byte[] coconutScore(int team1, int team2) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.COCONUT_SCORE.getValue());
        mplew.writeShort(team1);
        mplew.writeShort(team2);
        return mplew.getPacket();
    }
}
