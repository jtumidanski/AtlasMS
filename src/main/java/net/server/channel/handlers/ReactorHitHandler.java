package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ReactorHitHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int charPos = slea.readInt();

        int dwHitOption = slea.readInt();// bMoveAction & 1 | 2 * (m_pfh != 0), if on ground, left/right
        short bMoveAction = (short) (dwHitOption & 1);
        short m_pfh = (short) ((dwHitOption >> 1) & 1);

        slea.readShort(); // tDelay
        int skillId = slea.readInt();
        c.getPlayer().getMap().getReactorByOid(oid).ifPresent(r -> r.hitReactor(true, charPos, bMoveAction, skillId, c));
    }
}
