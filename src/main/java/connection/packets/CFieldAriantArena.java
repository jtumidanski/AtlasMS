package connection.packets;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.LinkedHashMap;
import java.util.Map;

public class CFieldAriantArena {
    public static byte[] updateAriantPQRanking(Map<MapleCharacter, Integer> playerScore) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ARIANT_ARENA_USER_SCORE.getValue());
        mplew.write(playerScore.size());
        for (Map.Entry<MapleCharacter, Integer> e : playerScore.entrySet()) {
            mplew.writeMapleAsciiString(e.getKey()
                    .getName());
            mplew.writeInt(e.getValue());
        }
        return mplew.getPacket();
    }

    public static byte[] updateAriantPQRanking(final MapleCharacter chr, final int score) {
        return updateAriantPQRanking(new LinkedHashMap<>() {{
            put(chr, score);
        }});
    }
}
