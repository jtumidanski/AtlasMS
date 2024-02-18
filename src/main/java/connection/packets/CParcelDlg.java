package connection.packets;

import connection.constants.SendOpcode;
import server.DueyPackage;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CParcelDlg {
    public static byte[] removeItemFromDuey(boolean remove, int Package) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(0x17);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);
        return mplew.getPacket();
    }

    public static byte[] sendDueyParcelReceived(String from, boolean quick) {    // thanks inhyuk
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(0x19);
        mplew.writeMapleAsciiString(from);
        mplew.writeBool(quick);
        return mplew.getPacket();
    }

    public static byte[] sendDueyParcelNotification(boolean quick) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(0x1B);
        mplew.writeBool(quick);  // 0 : package received, 1 : quick delivery package
        return mplew.getPacket();
    }

    public static byte[] sendDuey(int operation, List<DueyPackage> packages) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PARCEL.getValue());
        mplew.write(operation);
        if (operation == 8) {
            mplew.write(0);
            mplew.write(packages.size());
            for (DueyPackage dp : packages) {
                mplew.writeInt(dp.getPackageId());
                mplew.writeAsciiString(dp.getSender());
                for (int i = dp.getSender()
                        .length(); i < 13; i++) {
                    mplew.write(0);
                }

                mplew.writeInt(dp.getMesos());
                mplew.writeLong(CCommon.getTime(dp.sentTimeInMilliseconds()));

                String msg = dp.getMessage();
                if (msg != null) {
                    mplew.writeInt(1);
                    mplew.writeAsciiString(msg);
                    for (int i = msg.length(); i < 200; i++) {
                        mplew.write(0);
                    }
                } else {
                    mplew.writeInt(0);
                    mplew.skip(200);
                }

                mplew.write(0);
                if (dp.getItem() != null) {
                    mplew.write(1);
                    CCommon.addItemInfo(mplew, dp.getItem(), true);
                } else {
                    mplew.write(0);
                }
            }
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] sendDueyMSG(byte operation) {
        return sendDuey(operation, null);
    }
}
