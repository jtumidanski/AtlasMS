package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleReactor;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ReactorHitHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int charPos = slea.readInt();
        short stance = slea.readShort();
        slea.skip(4);
        int skillId = slea.readInt();
        c.getPlayer().getMap().getReactorByOid(oid).ifPresent(r -> r.hitReactor(true, charPos, stance, skillId, c));
    }
}
