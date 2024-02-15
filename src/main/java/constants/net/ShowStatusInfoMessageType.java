package constants.net;

public enum ShowStatusInfoMessageType {
    ON_DROP_PICK_UP(0),
    ON_QUEST_RECORD(1),
    ON_CASH_ITEM_EXPIRE(2),
    ON_INCREASE_EXP(3),
    ON_INCREASE_SP(4),
    ON_INCREASE_FAME(5),
    ON_INCREASE_MONEY(6),
    ON_INCREASE_GUILD_POINT(7),
    ON_GIVE_BUFF(8),
    ON_GENERAL_ITEM_EXPIRE(9),
    ON_SYSTEM(10),
    ON_QUEST_RECORD_EX(11),
    ON_ITEM_PROTECTION_EXPIRE(12),
    ON_ITEM_EXPIRE_REPLACE(13),
    ON_SKILL_EXPIRE(14);

    final byte messageType;

    ShowStatusInfoMessageType(int messageType) {
        this.messageType = (byte) messageType;
    }

    public byte getMessageType() {
        return messageType;
    }
}
