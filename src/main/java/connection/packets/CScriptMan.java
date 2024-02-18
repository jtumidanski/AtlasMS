package connection.packets;

import connection.constants.SendOpcode;
import constants.net.NPCTalkMessageType;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CScriptMan {
    /**
     * Possible values for <code>speaker</code>:<br> 0: Npc talking (left)<br>
     * 1: Npc talking (right)<br> 2: Player talking (left)<br> 3: Player talking
     * (left)<br>
     *
     * @param npc      Npcid
     * @param msgType
     * @param talk
     * @param endBytes
     * @param speaker
     * @return
     */
    public static byte[] getNPCTalk(int npc, NPCTalkMessageType msgType, String talk, String endBytes, byte speaker) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(msgType.getMessageType());
        mplew.write(speaker);
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        return mplew.getPacket();
    }

    public static byte[] getDimensionalMirror(String talk) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(9010022);
        mplew.write(NPCTalkMessageType.ON_ASK_SLIDE_MENU.getMessageType());
        mplew.write(0); //speaker
        mplew.writeInt(0);
        mplew.writeInt(4);
        mplew.writeMapleAsciiString(talk);
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkStyle(int npc, String talk, int[] styles) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(NPCTalkMessageType.ON_ASK_AVATAR.getMessageType());
        mplew.write(0); //speaker
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int i = 0; i < styles.length; i++) {
            mplew.writeInt(styles[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // ?
        mplew.writeInt(npc);
        mplew.write(NPCTalkMessageType.ON_ASK_NUMBER.getMessageType());
        mplew.write(0); //speaker
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkText(int npc, String talk, String def) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(4); // Doesn't matter
        mplew.writeInt(npc);
        mplew.write(NPCTalkMessageType.ON_ASK_TEXT.getMessageType());
        mplew.write(0); //speaker
        mplew.writeMapleAsciiString(talk);
        mplew.writeMapleAsciiString(def);//:D
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    // NPC Quiz packets thanks to Eric
    public static byte[] OnAskQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, String sTitle, String sProblemText, String sHintText, int nMinInput, int nMaxInput, int tRemainInitialQuiz) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.write(NPCTalkMessageType.ON_ASK_QUIZ.getMessageType());
        mplew.write(0);
        mplew.write(nResCode);
        if (nResCode == 0x0) {//fail has no bytes <3
            mplew.writeMapleAsciiString(sTitle);
            mplew.writeMapleAsciiString(sProblemText);
            mplew.writeMapleAsciiString(sHintText);
            mplew.writeShort(nMinInput);
            mplew.writeShort(nMaxInput);
            mplew.writeInt(tRemainInitialQuiz);
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskSpeedQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, int nType, int dwAnswer, int nCorrect, int nRemain, int tRemainInitialQuiz) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.write(NPCTalkMessageType.ON_ASK_SPEED_QUIZ.getMessageType());
        mplew.write(0);
        mplew.write(nResCode);
        if (nResCode == 0x0) {//fail has no bytes <3
            mplew.writeInt(nType);
            mplew.writeInt(dwAnswer);
            mplew.writeInt(nCorrect);
            mplew.writeInt(nRemain);
            mplew.writeInt(tRemainInitialQuiz);
        }
        return mplew.getPacket();
    }
}
