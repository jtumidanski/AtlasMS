package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.event.EventInstanceManager;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packets.Wedding;

public final class WeddingTalkHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte action = slea.readByte();
        if (action == 1) {
            EventInstanceManager eim = c.getPlayer().getEventInstance().orElse(null);

            if (eim != null && !(c.getPlayer().getId() == eim.getIntProperty("groomId") || c.getPlayer().getId() == eim.getIntProperty("brideId"))) {
                c.announce(Wedding.OnWeddingProgress(false, 0, 0, (byte) 2));
            } else {
                c.announce(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
            }
        } else {
            c.announce(Wedding.OnWeddingProgress(true, 0, 0, (byte) 3));
        }

        c.announce(MaplePacketCreator.enableActions());
    }
}