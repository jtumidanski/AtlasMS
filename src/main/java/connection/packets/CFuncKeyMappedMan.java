package connection.packets;

import client.keybind.MapleKeyBinding;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Map;

public class CFuncKeyMappedMan {
    public static byte[] getKeymap(Map<Integer, MapleKeyBinding> keybindings) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.KEYMAP.getValue());
        mplew.write(0);
        for (int x = 0; x < 90; x++) {
            MapleKeyBinding binding = keybindings.get(x);
            if (binding != null) {
                mplew.write(binding.type());
                mplew.writeInt(binding.action());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] sendAutoHpPot(int itemId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.AUTO_HP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static byte[] sendAutoMpPot(int itemId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.AUTO_MP_POT.getValue());
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }
}
