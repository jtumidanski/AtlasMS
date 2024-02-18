package connection.packets;

import connection.constants.SendOpcode;
import server.maps.MapleReactor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class CReactorPool {
    // is there a way to trigger reactors without performing the hit animation?
    public static byte[] triggerReactor(MapleReactor reactor, int stance) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(pos);
        mplew.write(stance);
        mplew.writeShort(0);
        mplew.write(5); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it
        return mplew.getPacket();
    }

    // is there a way to spawn reactors non-animated?
    public static byte[] spawnReactor(MapleReactor reactor) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getId());
        mplew.write(reactor.getState());
        mplew.writePos(pos);
        mplew.write(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Point pos = reactor.getPosition();
        mplew.writeShort(SendOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(pos);
        return mplew.getPacket();
    }
}
