package connection.packets;

import connection.constants.SendOpcode;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Set;

public class CNpcTemplate {
    public static byte[] setNPCScriptable(Set<Pair<Integer, String>> scriptNpcDescriptions) {  // thanks to GabrielSin
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_NPC_SCRIPTABLE.getValue());
        mplew.write(scriptNpcDescriptions.size());
        for (Pair<Integer, String> p : scriptNpcDescriptions) {
            mplew.writeInt(p.getLeft());
            mplew.writeMapleAsciiString(p.getRight());
            mplew.writeInt(0); // start time
            mplew.writeInt(Integer.MAX_VALUE); // end time
        }
        return mplew.getPacket();
    }
}
