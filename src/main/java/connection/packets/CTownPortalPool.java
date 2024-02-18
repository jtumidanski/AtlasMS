package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class CTownPortalPool {
    /**
     * Gets a packet to spawn a door.
     *
     * @param ownerid  The door's owner ID.
     * @param pos      The position of the door.
     * @param launched Already deployed the door.
     * @return The remove door packet.
     */
    public static byte[] spawnDoor(int ownerid, Point pos, boolean launched) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendOpcode.SPAWN_DOOR.getValue());
        mplew.writeBool(launched);
        mplew.writeInt(ownerid);
        mplew.writePos(pos);
        return mplew.getPacket();
    }
}
