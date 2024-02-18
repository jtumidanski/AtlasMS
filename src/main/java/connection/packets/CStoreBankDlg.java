package connection.packets;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventoryType;
import connection.constants.SendOpcode;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.SQLException;
import java.util.List;

public class CStoreBankDlg {
    public static byte[] fredrickMessage(byte operation) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FREDRICK_MESSAGE.getValue());
        mplew.write(operation);
        return mplew.getPacket();
    }

    public static byte[] getFredrick(byte op) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FREDRICK.getValue());
        mplew.write(op);

        switch (op) {
            case 0x24:
                mplew.skip(8);
                break;
            default:
                mplew.write(0);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] getFredrick(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.FREDRICK.getValue());
        mplew.write(0x23);
        mplew.writeInt(9030000); // Fredrick
        mplew.writeInt(32272); //id
        mplew.skip(5);
        mplew.writeInt(chr.getMerchantNetMeso());
        mplew.write(0);
        try {
            List<Pair<Item, MapleInventoryType>> items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);
            mplew.write(items.size());

            for (int i = 0; i < items.size(); i++) {
                CCommon.addItemInfo(mplew, items.get(i)
                        .getLeft(), true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mplew.skip(3);
        return mplew.getPacket();
    }
}
