package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CharInfoRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int cid = slea.readInt();
        c.getPlayer().getMap().getCharacterByOid(cid).ifPresent(t -> showInfo(c, t));
    }

    private static void showInfo(MapleClient c, MapleCharacter target) {
        if (c.getPlayer().getId() != target.getId()) {
            target.exportExcludedItems(c);
        }
        c.announce(MaplePacketCreator.charInfo(target));
    }
}
