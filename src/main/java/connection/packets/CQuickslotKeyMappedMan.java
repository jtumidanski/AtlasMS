package connection.packets;

import client.keybind.MapleQuickslotBinding;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CQuickslotKeyMappedMan {
    public static byte[] QuickslotMappedInit(MapleQuickslotBinding pQuickslot) {
        final MaplePacketLittleEndianWriter pOutPacket = new MaplePacketLittleEndianWriter();

        pOutPacket.writeShort(SendOpcode.QUICKSLOT_INIT.getValue());
        pQuickslot.Encode(pOutPacket);

        return pOutPacket.getPacket();
    }
}
