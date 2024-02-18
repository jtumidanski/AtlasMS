package net.server.channel.handlers;

import client.MapleClient;
import client.MapleFamily;
import connection.packets.CWvsContext;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public class FamilyPreceptsHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        Optional<MapleFamily> family = c.getPlayer().getFamily();
        if (family.isEmpty()) {
            return;
        }
        if (family.get().getLeader().getChr() != c.getPlayer()) {
            return; //only the leader can set the precepts
        }
        String newPrecepts = slea.readMapleAsciiString();
        if (newPrecepts.length() > 200) {
            return;
        }
        family.get().setMessage(newPrecepts, true);
        //family.broadcastFamilyInfoUpdate(); //probably don't need to broadcast for this?
        c.announce(CWvsContext.getFamilyInfo(c.getPlayer().getFamilyEntry()));
    }

}
