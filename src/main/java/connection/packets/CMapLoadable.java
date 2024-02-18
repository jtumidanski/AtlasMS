package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CMapLoadable {
    /**
     * Changes the current background effect to either being rendered or not.
     * Data is still missing, so this is pretty binary at the moment in how it
     * behaves.
     *
     * @param remove     whether or not the remove or add the specified layer.
     * @param layer      the targeted layer for removal or addition.
     * @param transition the time it takes to transition the effect.
     * @return a packet to change the background effect of a specified layer.
     */
    public static byte[] changeBackgroundEffect(boolean remove, int layer, int transition) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SET_BACK_EFFECT.getValue());
        mplew.writeBool(remove);
        mplew.writeInt(0); // not sure what this int32 does yet
        mplew.write(layer);
        mplew.writeInt(transition);
        return mplew.getPacket();
    }

    /**
     * Gets a "block" packet (ie. the cash shop is unavailable, etc)
     * <p>
     * Possible values for <code>type</code>:<br> 1: The portal is closed for
     * now.<br> 2: You cannot go to that place.<br> 3: Unable to approach due to
     * the force of the ground.<br> 4: You cannot teleport to or on this
     * map.<br> 5: Unable to approach due to the force of the ground.<br> 6:
     * Only party members can enter this map.<br> 7: The Cash Shop is
     * currently not available. Stay tuned...<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static byte[] blockedMessage(int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.BLOCKED_MAP.getValue());
        mplew.write(type);
        return mplew.getPacket();
    }
}
