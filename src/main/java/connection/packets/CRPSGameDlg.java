package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CRPSGameDlg {
    // RPS_GAME packets thanks to Arnah (Vertisy)
    public static byte[] openRPSNPC() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(8);// open npc
        mplew.writeInt(9000019);
        return mplew.getPacket();
    }

    public static byte[] rpsMesoError(int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(0x06);
        if (mesos != -1) {
            mplew.writeInt(mesos);
        }
        return mplew.getPacket();
    }

    public static byte[] rpsSelection(byte selection, byte answer) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(0x0B);// 11l
        mplew.write(selection);
        mplew.write(answer);
        return mplew.getPacket();
    }

    public static byte[] rpsMode(byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RPS_GAME.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }
}
