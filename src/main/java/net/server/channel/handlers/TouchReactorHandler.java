package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.reactor.ReactorScriptManager;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

public final class TouchReactorHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        byte mode = slea.readByte();
        c.getPlayer().getMap().getReactorByOid(oid).ifPresent(r -> touchReactor(c, mode, r));
    }

    private static void touchReactor(MapleClient c, byte mode, MapleReactor reactor) {
        if (mode != 0) {
            ReactorScriptManager.getInstance().touch(c, reactor);
        } else {
            ReactorScriptManager.getInstance().untouch(c, reactor);
        }
    }
}
