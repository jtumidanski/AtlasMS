package connection.packets;

import client.MapleCharacter;
import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CMapleTVMan {
    /**
     * Sends MapleTV
     *
     * @param chr      The character shown in TV
     * @param messages The message sent with the TV
     * @param type     The type of TV
     * @param partner  The partner shown with chr
     * @return the SEND_TV packet
     */
    public static byte[] sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SEND_TV.getValue());
        mplew.write(partner != null ? 3 : 1);
        mplew.write(type); //Heart = 2  Star = 1  Normal = 0
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        if (partner != null) {
            mplew.writeMapleAsciiString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4)
                    .length() > 15) {
                mplew.writeMapleAsciiString(messages.get(4)
                        .substring(0, 15));
            } else {
                mplew.writeMapleAsciiString(messages.get(i));
            }
        }
        mplew.writeInt(1337); // time limit shit lol 'Your thing still start in blah blah seconds'
        if (partner != null) {
            CCommon.addCharLook(mplew, partner, false);
        }
        return mplew.getPacket();
    }

    /**
     * Removes TV
     *
     * @return The Remove TV Packet
     */
    public static byte[] removeTV() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendOpcode.REMOVE_TV.getValue());
        return mplew.getPacket();
    }

    public static byte[] enableTV() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.ENABLE_TV.getValue());
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }
}
