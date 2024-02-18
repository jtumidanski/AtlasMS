package connection.packets;

import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connection.constants.SendOpcode;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;
import server.MapleShopItem;
import tools.StringUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CShopDlg {
    public static byte[] getNPCShop(MapleClient c, int npcTemplateId, List<MapleShopItem> items) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.OPEN_NPC_SHOP.getValue());
        mplew.writeInt(npcTemplateId); // dwNpcTemplateID
        mplew.writeShort(items.size()); // nCount
        for (MapleShopItem item : items) {
            mplew.writeInt(item.getItemId()); // nItemID
            mplew.writeInt(item.getPrice()); // nTokenItemID
            mplew.write(item.getDiscountRate()); // nDiscountRate
            mplew.writeInt(item.getPrice() == 0 ? item.getPitch() : 0); //Perfect Pitch
            mplew.writeInt(0); //nItemPeriod
            mplew.writeInt(0); //nLevelLimited
            if (ItemConstants.isRechargeable(item.getItemId())) {
                mplew.writeShort(0);
                mplew.writeInt(0);
                mplew.writeShort(doubleToShortBits(ii.getUnitPrice(item.getItemId())));
                mplew.writeShort(ii.getSlotMax(c, item.getItemId()));
            } else {
                mplew.writeShort(1); // nQuantity
                mplew.writeShort(item.getBuyable()); // nMaxPerSlot
            }
        }
        return mplew.getPacket();
    }

    /* 00 = /
     * 01 = You don't have enough in stock
     * 02 = You do not have enough mesos
     * 03 = Please check if your inventory is full or not
     * 05 = You don't have enough in stock
     * 06 = Due to an error, the trade did not happen
     * 07 = Due to an error, the trade did not happen
     * 08 = /
     * 0D = You need more items
     * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
     */
    public static byte[] shopTransaction(byte code) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code);
        return mplew.getPacket();
    }

    // someone thought it was a good idea to handle floating point representation through packets ROFL
    private static int doubleToShortBits(double d) {
        return (int) (Double.doubleToLongBits(d) >> 48);
    }

    public static void addCashItemInformation(final MaplePacketLittleEndianWriter mplew, Item item, int accountId, String giftMessage) {
        boolean isGift = giftMessage != null;
        boolean isRing = false;
        Equip equip = null;
        if (item.getInventoryType()
                .equals(MapleInventoryType.EQUIP)) {
            equip = (Equip) item;
            isRing = equip.getRingId() > -1;
        }
        mplew.writeLong(item.isPet() ? item.getPetId()
                .orElseThrow() : isRing ? equip.getRingId() : item.getCashId());
        if (!isGift) {
            mplew.writeInt(accountId);
            mplew.writeInt(0);
        }
        mplew.writeInt(item.getItemId());
        if (!isGift) {
            mplew.writeInt(item.getSN());
            mplew.writeShort(item.getQuantity());
        }
        mplew.writeAsciiString(StringUtil.getRightPaddedStr(item.getGiftFrom(), '\0', 13));
        if (isGift) {
            mplew.writeAsciiString(StringUtil.getRightPaddedStr(giftMessage, '\0', 73));
            return;
        }
        CCommon.addExpirationTime(mplew, item.getExpiration());
        mplew.writeLong(0);
    }
}
