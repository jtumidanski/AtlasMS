package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleMapObject;
import tools.FilePrinter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;

public final class ItemPickupHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        slea.readInt(); //Timestamp
        slea.readByte();
        slea.readPos(); //cpos
        int oid = slea.readInt();
        c.getPlayer().getMap().getMapObject(oid).ifPresent(o -> performPickup(c, o));
    }

    private static void performPickup(MapleClient c, MapleMapObject ob) {
        Point charPos = c.getPlayer().getPosition();
        Point obPos = ob.getPosition();
        if (Math.abs(charPos.getX() - obPos.getX()) > 800 || Math.abs(charPos.getY() - obPos.getY()) > 600) {
            FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to pick up an item too far away. Mapid: " + c.getPlayer().getMapId() + " Player pos: " + charPos + " Object pos: " + obPos);
            return;
        }

        c.getPlayer().pickupItem(ob);
    }
}
