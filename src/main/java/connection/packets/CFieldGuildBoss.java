package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldGuildBoss {
    private static byte[] GuildBoss_HealerMove(short nY) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_BOSS_HEALER_MOVE.getValue());
        mplew.writeShort(nY); //New Y Position
        return mplew.getPacket();
    }

    private static byte[] GuildBoss_PulleyStateChange(byte nState) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_BOSS_PULLEY_STATE_CHANGE.getValue());
        mplew.write(nState);
        return mplew.getPacket();
    }
}
