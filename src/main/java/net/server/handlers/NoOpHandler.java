package net.server.handlers;

import client.MapleClient;
import net.MaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class NoOpHandler implements MaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
    }

    @Override
    public boolean validateState(MapleClient c) {
        return true;
    }
}
