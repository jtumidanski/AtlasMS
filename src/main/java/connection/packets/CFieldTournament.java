package connection.packets;

import connection.constants.SendOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldTournament {
    private static byte[] Tournament__Tournament(byte nState, byte nSubState) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TOURNAMENT.getValue());
        mplew.write(nState);
        mplew.write(nSubState);
        return mplew.getPacket();
    }

    private static byte[] Tournament__MatchTable(byte nState, byte nSubState) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TOURNAMENT_MATCH_TABLE.getValue()); //Prompts CMatchTableDlg Modal
        return mplew.getPacket();
    }

    private static byte[] Tournament__SetPrize(byte bSetPrize, byte bHasPrize, int nItemID1, int nItemID2) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TOURNAMENT_SET_PRIZE.getValue());

        //0 = "You have failed the set the prize. Please check the item number again."
        //1 = "You have successfully set the prize."
        mplew.write(bSetPrize);

        mplew.write(bHasPrize);

        if (bHasPrize != 0) {
            mplew.writeInt(nItemID1);
            mplew.writeInt(nItemID2);
        }

        return mplew.getPacket();
    }

    private static byte[] Tournament__UEW(byte nState) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.TOURNAMENT_UEW.getValue());

        //Is this a bitflag o.o ?
        //2 = "You have reached the finals by default."
        //4 = "You have reached the semifinals by default."
        //8 or 16 = "You have reached the round of %n by default." | Encodes nState as %n ?!
        mplew.write(nState);

        return mplew.getPacket();
    }
}
