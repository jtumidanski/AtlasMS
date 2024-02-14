package constants.net;

public enum NPCTalkMessageType {
    ON_SAY(0),
    ON_SAY_IMAGE(1),
    ON_ASK_YES_NO(2),
    ON_ASK_TEXT(3),
    ON_ASK_NUMBER(4),
    ON_ASK_MENU(5),
    ON_ASK_QUIZ(6),
    ON_ASK_SPEED_QUIZ(7),
    ON_ASK_AVATAR(8),
    ON_ASK_MEMBERSHOP_AVATAR(9),
    ON_ASK_PET(10),
    ON_ASK_PET_ALL(11),
    ON_ASK_YES_NO_ALT(13),
    ON_ASK_BOX_TEXT(14),
    ON_ASK_SLIDE_MENU(15);

    final byte messageType;

    NPCTalkMessageType(int messageType) {
        this.messageType = (byte) messageType;
    }

    public byte getMessageType() {
        return messageType;
    }
}
