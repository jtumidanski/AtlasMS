/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package connection.constants;

public enum RecvOpcode {
    LOGIN_PASSWORD(0x01),
    GUEST_LOGIN(0x02),
    SERVERLIST_REREQUEST(0x04),
    CHARLIST_REQUEST(0x05),
    SERVERSTATUS_REQUEST(0x06),
    ACCEPT_TOS(0x07),
    SET_GENDER(0x08),
    AFTER_LOGIN(0x09),
    REGISTER_PIN(0x0A),
    SERVERLIST_REQUEST(0x0B),
    PLAYER_DC(0x0C),
    VIEW_ALL_CHAR(0x0D),
    PICK_ALL_CHAR(0x0E),
    NAME_TRANSFER(0x10),
    WORLD_TRANSFER(0x12),
    CHAR_SELECT(0x13),
    PLAYER_LOGGEDIN(0x14),
    CHECK_CHAR_NAME(0x15),
    CREATE_CHAR(0x16),
    DELETE_CHAR(0x17),
    PONG(0x18),
    CLIENT_START_ERROR(0x19),
    CLIENT_ERROR(0x1A),
    STRANGE_DATA(0x1B),
    RELOG(0x1C),
    REGISTER_PIC(0x1D),
    CHAR_SELECT_WITH_PIC(0x1E),
    VIEW_ALL_PIC_REGISTER(0x1F),
    VIEW_ALL_WITH_PIC(0x20),

    // 0x20
    // 0x23
    // 0x24 CNMCOClientObject::AttachAuth
    PACKET_ERROR(0x25),

    CHANGE_MAP(0x28),
    CHANGE_CHANNEL(0x29),
    ENTER_CASHSHOP(0x2A),
    MOVE_PLAYER(0x2B),
    CANCEL_CHAIR(0x2C),
    USE_CHAIR(0x2D),
    CLOSE_RANGE_ATTACK(0x2E),
    RANGED_ATTACK(0x2F),
    MAGIC_ATTACK(0x30),
    TOUCH_MONSTER_ATTACK(0x31),
    TAKE_DAMAGE(0x32),
    GENERAL_CHAT(0x34),
    CLOSE_CHALKBOARD(0x35),
    FACE_EXPRESSION(0x36),
    USE_ITEMEFFECT(0x37),
    USE_DEATHITEM(0x38),
    MOB_BANISH_PLAYER(0x3B),
    MONSTER_BOOK_COVER(0x3C),
    NPC_TALK(0x3D),
    REMOTE_STORE(0x3E),
    NPC_TALK_MORE(0x3F),
    NPC_SHOP(0x40),
    STORAGE(0x41),
    HIRED_MERCHANT_REQUEST(0x42),
    FREDRICK_ACTION(0x43),
    DUEY_ACTION(0x44),
    OWL_ACTION(0x45),
    OWL_WARP(0x46),
    ADMIN_SHOP(0x47),
    ITEM_SORT(0x48),
    ITEM_SORT2(0x49),
    ITEM_MOVE(0x4A), // needed?
    USE_ITEM(0x4B),
    CANCEL_ITEM_EFFECT(0x4C),

    //0x4D
    USE_SUMMON_BAG(0x4E),
    PET_FOOD(0x4F),
    USE_MOUNT_FOOD(0x50),
    SCRIPTED_ITEM(0x51),
    USE_CASH_ITEM(0x52),

    //0x53

    USE_CATCH_ITEM(0x54),
    USE_SKILL_BOOK(0x55),

    //0x56
    USE_TELEPORT_ROCK(0x57),
    USE_RETURN_SCROLL(0x58),
    USE_UPGRADE_SCROLL(0x59),
    DISTRIBUTE_AP(0x5A),
    AUTO_DISTRIBUTE_AP(0x5B),
    HEAL_OVER_TIME(0x5C),
    DISTRIBUTE_SP(0x5D),
    SPECIAL_MOVE(0x5E),
    CANCEL_BUFF(0x5F),
    SKILL_EFFECT(0x60),
    MESO_DROP(0x61),
    GIVE_FAME(0x62),
    CHAR_INFO_REQUEST(0x64),
    SPAWN_PET(0x65),
    CANCEL_DEBUFF(0x66),
    CHANGE_MAP_SPECIAL(0x67),
    USE_INNER_PORTAL(0x68),
    TROCK_ADD_MAP(0x69),

    //0x6A 6B 6C
    REPORT(0x6D),
    QUEST_ACTION(0x6E),

    //0x6F
    GRENADE_EFFECT(0x70),
    SKILL_MACRO(0x71),
    USE_ITEM_REWARD(0x73),
    MAKER_SKILL(0x74),

    //0x75 76
    USE_REMOTE(0x77),
    WATER_OF_LIFE(0x78),

    //0x79 CRepairDurabilityDlg::SendRepairDurabilityAll
    //0x7A CRepairDurabilityDlg::SendRepairDurability
    ADMIN_CHAT(0x7C),
    MULTI_CHAT(0x7D),
    WHISPER(0x7E),
    SPOUSE_CHAT(0x7F),
    MESSENGER(0x80),
    PLAYER_INTERACTION(0x81),
    PARTY_OPERATION(0x82),

    //0x83 84 85 all party result
    DENY_PARTY_REQUEST(0x84),
    GUILD_OPERATION(0x86),
    DENY_GUILD_REQUEST(0x87),
    ADMIN_COMMAND(0x88),
    ADMIN_LOG(0x89),
    BUDDYLIST_MODIFY(0x8A),
    NOTE_ACTION(0x8B),
    USE_DOOR(0x8D),
    CHANGE_KEYMAP(0x8F),
    RPS_ACTION(0x90),
    RING_ACTION(0x91),
    WEDDING_ACTION(0x92),
    //WEDDING_TALK(0x8B),
    //WEDDING_TALK_MORE(0x8B),
    // 0x94 95 96
    ALLIANCE_OPERATION(0x97),
    DENY_ALLIANCE_REQUEST(0x98),
    OPEN_FAMILY_PEDIGREE(0x99),
    OPEN_FAMILY(0x9A),
    ADD_FAMILY(0x9B),
    SEPARATE_FAMILY_BY_SENIOR(0x9C),
    SEPARATE_FAMILY_BY_JUNIOR(0x9D),
    ACCEPT_FAMILY(0x9E),
    USE_FAMILY(0x9F),
    CHANGE_FAMILY_MESSAGE(0xA0),
    FAMILY_SUMMON_RESPONSE(0xA1),
    BBS_OPERATION(0xA3),
    ENTER_MTS(0xA4),
    USE_SOLOMON_ITEM(0xA5),
    USE_GACHA_EXP(0xA6),
    NEW_YEAR_CARD_REQUEST(0xA7),

    // 0xA8
    CASHSHOP_SURPRISE(0xA9),

    // 0xAA CUICashGachapon::OnButtonClicked
    CLICK_GUIDE(0xAC),
    ARAN_COMBO_COUNTER(0xAD),

    // 0xAE
    // 0xAF CWvsContext::CheckOpBoardHasNew()
    // 0xB0 CUIAccountMoreInfo::SendLoadAccountMoreInfoRequest
    // 0xB1 CUIFindFriendDetail::SetDetailInfo
    MOVE_PET(0xB3),
    PET_CHAT(0xB4),
    PET_COMMAND(0xB5),
    PET_LOOT(0xB6),
    PET_AUTO_POT(0xB7),
    PET_EXCLUDE_ITEMS(0xB8),
    MOVE_SUMMON(0xBB),
    SUMMON_ATTACK(0xBC),
    DAMAGE_SUMMON(0xBD),
    BEHOLDER(0xBE),
    MOVE_DRAGON(0xC1),
    CHANGE_QUICKSLOT(0xC3),
    MOVE_LIFE(0xC8),
    AUTO_AGGRO(0xC9),
    FIELD_DAMAGE_MOB(0xCB),
    MOB_DAMAGE_MOB_FRIENDLY(0xCC),
    MONSTER_BOMB(0xCD),
    MOB_DAMAGE_MOB(0xCE),

    //0xCF CMob::Update
    NPC_ACTION(0xD2),

    //0xD3

    ITEM_PICKUP(0xD7),
    DAMAGE_REACTOR(0xDA),
    TOUCHING_REACTOR(0xDB),
    PLAYER_MAP_TRANSFER(0xDC),
    SNOWBALL(0xE0),
    LEFT_KNOCKBACK(0xE1),
    COCONUT(0xE2),
    MATCH_TABLE(0xE3),

    // 0xE4

    MONSTER_CARNIVAL(0xE7),
    PARTY_SEARCH_REGISTER(0xE9),
    PARTY_SEARCH_START(0xEB),
    PARTY_SEARCH_UPDATE(0xEC),

    // 0xE7
    CHECK_CASH(0xF1),
    CASHSHOP_OPERATION(0xF2),
    COUPON_CODE(0xF3),
    OPEN_ITEMUI(0xFA),
    CLOSE_ITEMUI(0xFB),
    USE_ITEMUI(0xFC),

    // 0xFD CUIRaiseWnd::SendPutItem
    // 0x100
    // 0x103
    // 0x105
    // 0x106
    // 0x109
    // 0x10A
    MTS_OPERATION(0x10B),
    USE_MAPLELIFE(0x10E),
    USE_HAMMER(0x112);

    // 0x114
    // 0x11E

    private int code;

    RecvOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }
}
