package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.event.EventInstanceManager;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packets.Wedding;

public final class WeddingTalkMoreHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        EventInstanceManager eim = c.getPlayer().getEventInstance().orElse(null);
        if (eim != null && !(c.getPlayer().getId() == eim.getIntProperty("groomId") || c.getPlayer().getId() == eim.getIntProperty("brideId"))) {
            eim.gridInsert(c.getPlayer(), 1);
            c.getPlayer().dropMessage(5, "High Priest John: Your blessings have been added to their love. What a noble act for a lovely couple!");
        }

        c.announce(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
        c.announce(MaplePacketCreator.enableActions());
    }
}