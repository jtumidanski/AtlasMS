/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.channel.handlers;

import client.MapleClient;
import connection.packets.CMiniRoomBaseDlg;
import connection.packets.CWvsContext;
import constants.game.GameConstants;
import net.AbstractMaplePacketHandler;
import server.maps.MapleHiredMerchant;
import server.maps.MaplePlayerShop;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

/*
 * @author Ronan
 */
public final class OwlWarpHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int ownerId = slea.readInt();
        int mapId = slea.readInt();

        if (ownerId == c.getPlayer().getId()) {
            c.announce(CWvsContext.serverNotice(1, "You cannot visit your own shop."));
            return;
        }

        Optional<MapleHiredMerchant> hm = c.getWorldServer().getHiredMerchant(ownerId);   // if both hired merchant and player shop is on the same map
        if (hm.isEmpty() || hm.get().getMapId() != mapId || !hm.get().hasItem(c.getPlayer().getOwlSearch())) {
            Optional<MaplePlayerShop> ps = c.getWorldServer().getPlayerShop(ownerId);

            if (ps.isEmpty() || ps.get().getMapId() != mapId || !ps.get().hasItem(c.getPlayer().getOwlSearch())) {
                if (hm.isEmpty() && ps.isEmpty()) {
                    c.announce(CWvsContext.getOwlMessage(1));
                } else {
                    c.announce(CWvsContext.getOwlMessage(3));
                }
                return;
            }

            if (!ps.get().isOpen()) {
                //c.announce(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
                c.announce(CWvsContext.getOwlMessage(18));
                return;
            }

            if (!GameConstants.isFreeMarketRoom(mapId)) {
                c.announce(CWvsContext.serverNotice(1, "That shop is currently located outside of the FM area. Current location: Channel " + ps.get().getChannel() + ", '" + c.getPlayer().getMap().getMapName() + "'."));
                return;
            }

            if (ps.get().getChannel() != c.getChannel()) {
                c.announce(CWvsContext.serverNotice(1, "That shop is currently located in another channel. Current location: Channel " + ps.get().getChannel() + ", '" + c.getPlayer().getMap().getMapName() + "'."));
                return;
            }

            c.getPlayer().changeMap(mapId);

            if (!ps.get().isOpen()) {
                //c.announce(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
                c.announce(CWvsContext.getOwlMessage(18));
                return;
            }

            if (ps.get().visitShop(c.getPlayer())) {
                return;
            }

            if (ps.get().isBanned(c.getPlayer().getName())) {
                c.announce(CWvsContext.getOwlMessage(17));
                return;
            }

            c.announce(CWvsContext.getOwlMessage(2));
        } else {
            warpHiredMerchant(c, mapId, hm.get());
        }
    }

    private static void warpHiredMerchant(MapleClient client, int mapId, MapleHiredMerchant hiredMerchant) {
        if (!hiredMerchant.isOpen()) {
            //c.announce(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
            client.announce(CWvsContext.getOwlMessage(18));
            return;
        }

        if (!GameConstants.isFreeMarketRoom(mapId)) {
            client.announce(CWvsContext.serverNotice(1, "That merchant is currently located outside of the FM area. Current location: Channel " + hiredMerchant.getChannel() + ", '" + hiredMerchant.getMap().getMapName() + "'."));
            return;
        }

        if (hiredMerchant.getChannel() != client.getChannel()) {
            client.announce(CWvsContext.serverNotice(1, "That merchant is currently located in another channel. Current location: Channel " + hiredMerchant.getChannel() + ", '" + hiredMerchant.getMap().getMapName() + "'."));
            return;
        }

        client.getPlayer().changeMap(mapId);

        if (!hiredMerchant.isOpen()) {
            //c.announce(MaplePacketCreator.serverNotice(1, "That merchant has either been closed or is under maintenance."));
            client.announce(CWvsContext.getOwlMessage(18));
            return;
        }

        if (!hiredMerchant.addVisitor(client.getPlayer())) {
            //c.announce(MaplePacketCreator.serverNotice(1, hm.getOwner() + "'s merchant is full. Wait awhile before trying again."));
            client.announce(CWvsContext.getOwlMessage(2));
            return;
        }

        client.announce(CMiniRoomBaseDlg.getHiredMerchant(client.getPlayer(), hiredMerchant, false));
        client.getPlayer().setHiredMerchant(hiredMerchant);
    }
}