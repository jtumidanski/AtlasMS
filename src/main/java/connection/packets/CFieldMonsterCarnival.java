package connection.packets;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldMonsterCarnival {
    public static byte[] startMonsterCarnival(MapleCharacter chr, int team, int oposition) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(25);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_START.getValue());
        mplew.write(team); // team
        mplew.writeShort(chr.getCP()); // Obtained CP - Used CP
        mplew.writeShort(chr.getTotalCP()); // Total Obtained CP
        mplew.writeShort(chr.getMonsterCarnival()
                .getCP(team)); // Obtained CP - Used CP of the team
        mplew.writeShort(chr.getMonsterCarnival()
                .getTotalCP(team)); // Total Obtained CP of the team
        mplew.writeShort(chr.getMonsterCarnival()
                .getCP(oposition)); // Obtained CP - Used CP of the team
        mplew.writeShort(chr.getMonsterCarnival()
                .getTotalCP(oposition)); // Total Obtained CP of the team
        mplew.writeShort(0); // Probably useless nexon shit
        mplew.writeLong(0); // Probably useless nexon shit
        return mplew.getPacket();
    }

    public static byte[] CPUpdate(boolean party, int curCP, int totalCP, int team) { // CPQ
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (!party) {
            mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        } else {
            mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
            mplew.write(team); // team?
        }
        mplew.writeShort(curCP);
        mplew.writeShort(totalCP);
        return mplew.getPacket();
    }

    public static byte[] playerSummoned(String name, int tab, int number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(tab);
        mplew.write(number);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] CPQMessage(byte message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        mplew.write(message); // Message
        return mplew.getPacket();
    }

    public static byte[] playerDiedMessage(String name, int lostCP, int team) { // CPQ
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendOpcode.MONSTER_CARNIVAL_DIED.getValue());
        mplew.write(team); // team
        mplew.writeMapleAsciiString(name);
        mplew.write(lostCP);
        return mplew.getPacket();
    }
}
