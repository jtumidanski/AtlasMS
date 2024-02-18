package connection.packets;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import server.maps.MapleMapItem;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class CDropPool {
    public static byte[] updateMapItemObject(MapleMapItem drop, boolean giveOwnership) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(2);
        mplew.writeInt(drop.getObjectId());
        mplew.writeBool(drop.getMeso() > 0);
        mplew.writeInt(drop.getItemId());
        mplew.writeInt(giveOwnership ? 0 : -1);
        mplew.write(drop.hasExpiredOwnershipTime() ? 2 : drop.getDropType());
        mplew.writePos(drop.getPosition());
        mplew.writeInt(giveOwnership ? 0 : -1);

        if (drop.getMeso() == 0) {
            CCommon.addExpirationTime(mplew, drop.getItem()
                    .getExpiration());
        }
        mplew.write(drop.isPlayerDrop() ? 0 : 1);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] dropItemFromMapObject(MapleCharacter player, MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
        int dropType = drop.getDropType();
        if (drop.hasClientsideOwnership(player) && dropType < 3) {
            dropType = 2;
        }

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod);
        mplew.writeInt(drop.getObjectId());
        mplew.writeBool(drop.getMeso() > 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.getClientsideOwnerId()); // owner charid/partyid :)
        mplew.write(dropType); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        mplew.writePos(dropto);
        mplew.writeInt(drop.getDropper()
                .getObjectId()); // dropper oid, found thanks to Li Jixue

        if (mod != 2) {
            mplew.writePos(dropfrom);
            mplew.writeShort(0);//Fh?
        }
        if (drop.getMeso() == 0) {
            CCommon.addExpirationTime(mplew, drop.getItem()
                    .getExpiration());
        }
        mplew.write(drop.isPlayerDrop() ? 0 : 1); //pet EQP pickup
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
     * explode<br/> cid is ignored for 0 and 1.<br /><br />Flagging pet as true
     * will make a pet pick up the item.
     *
     * @param oid
     * @param animation
     * @param cid
     * @param pet
     * @param slot
     * @return
     */
    public static byte[] removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // expire
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (pet) {
                mplew.write(slot);
            }
        }
        return mplew.getPacket();
    }

    /**
     * animation: 0 - expire<br/> 1 - without animation<br/> 2 - pickup<br/> 4 -
     * explode<br/> cid is ignored for 0 and 1
     *
     * @param oid
     * @param animation
     * @param cid
     * @return
     */
    public static byte[] removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, false, 0);
    }

    public static byte[] silentRemoveItemFromMap(int oid) {
        return removeItemFromMap(oid, 1, 0);
    }
}
