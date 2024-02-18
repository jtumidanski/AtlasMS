package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldWitchtower {
    public static byte[] updateWitchTowerScore(int score) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.WITCH_TOWER_SCORE_UPDATE.getValue());
        mplew.write(score);
        return mplew.getPacket();
    }
}
