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

public enum SendOpcode {

    LOGIN_STATUS(0x00), // CLogin::OnCheckPasswordResult
    GUEST_ID_LOGIN(0x01), // CLogin::OnGuestIDLoginResult
    ACCOUNT_INFO(0x02), // CLogin::OnAccountInfoResult
    SERVERSTATUS(0x03), //CLogin::OnCheckUserLimitResult
    GENDER_DONE(0x04), // CLogin::OnSetAccountResult
    CONFIRM_EULA_RESULT(0x05), // CLogin::OnConfirmEULAResult
    CHECK_PINCODE(0x06), // CLogin::OnCheckPinCodeResult
    UPDATE_PINCODE(0x07), // CLogin::OnUpdatePinCodeResult

    VIEW_ALL_CHAR(0x08), // CLogin::OnViewAllCharResult
    SELECT_CHARACTER_BY_VAC(0x09), // CLogin::OnSelectCharacterByVACResult

    SERVERLIST(0x0A), // CLogin::OnWorldInformation
    CHARLIST(0x0B), // CLogin::OnSelectWorldResult
    SERVER_IP(0x0C), // CLogin::OnSelectCharacterResult
    CHAR_NAME_RESPONSE(0x0D), // CLogin::OnCheckDuplicatedIDResult
    ADD_NEW_CHAR_ENTRY(0x0E), // CLogin::OnCreateNewCharacterResult
    DELETE_CHAR_RESPONSE(0x0F), // CLogin::OnDeleteCharacterResult
    CHANGE_CHANNEL(0x10), // CClientSocket::OnMigrateCommand
    PING(0x11), // CClientSocket::OnAliveReq
    KOREAN_INTERNET_CAFE_SHIT(0x12),
    CHANNEL_SELECTED(0x14),
    HACKSHIELD_REQUEST(0x15),
    RELOG_RESPONSE(0x16), // sub_633496

    // 0x17 CLogin::OnEnableSPWResult
    CHECK_CRC_RESULT(0x19),
    LAST_CONNECTED_WORLD(0x1A), // CLogin::OnLatestConnectedWorld
    RECOMMENDED_WORLD_MESSAGE(0x1B), // CLogin::OnRecommendWorldMessage
    CHECK_SPW_RESULT(0x1C), // CLogin::OnCheckSPWResult
    INVENTORY_OPERATION(0x1D), // CWvsContext::OnInventoryOperation
    INVENTORY_GROW(0x1E), // CWvsContext::OnInventoryGrow
    STAT_CHANGED(0x1F), // CWvsContext::OnStatChanged
    GIVE_BUFF(0x20), // CWvsContext::OnTemporaryStatSet
    CANCEL_BUFF(0x21), // CWvsContext::OnTemporaryStatReset
    FORCED_STAT_SET(0x22), // CWvsContext::OnForcedStatSet
    FORCED_STAT_RESET(0x23), // CWvsContext::OnForcedStatReset
    UPDATE_SKILLS(0x24), // CWvsContext::OnChangeSkillRecordResult
    SKILL_USE_RESULT(0x25), // CWvsContext::OnSkillUseResult
    FAME_RESPONSE(0x26), // CWvsContext::OnGivePopularityResult
    SHOW_STATUS_INFO(0x27), // CWvsContext::OnMessage
    OPEN_FULL_CLIENT_DOWNLOAD_LINK(0x28), // CWvsContext::OnOpenFullClientDownloadLink
    MEMO_RESULT(0x29), // CWvsContext::OnMemoResult
    MAP_TRANSFER_RESULT(0x2A), // CWvsContext::OnMapTransferResult
    WEDDING_PHOTO(0x2B), // CWvsContext::OnAntiMacroResult
    CLAIM_RESULT(0x2D), // CWvsContext::OnClaimResult
    CLAIM_AVAILABLE_TIME(0x2E), // CWvsContext::OnSetClaimSvrAvailableTime
    CLAIM_STATUS_CHANGED(0x2F), // CWvsContext::OnClaimSvrStatusChanged
    SET_TAMING_MOB_INFO(0x30), // CWvsContext::OnSetTamingMobInfo
    QUEST_CLEAR(0x31), // CWvsContext::OnQuestClear
    ENTRUSTED_SHOP_CHECK_RESULT(0x32), // CWvsContext::OnEntrustedShopCheckResult
    SKILL_LEARN_ITEM_RESULT(0x33), // CWvsContext::OnSkillLearnItemResult
    GATHER_ITEM_RESULT(0x34), // CWvsContext::OnGatherItemResult
    SORT_ITEM_RESULT(0x35), // CWvsContext::OnSortItemResult
    SUE_CHARACTER_RESULT(0x37), // CWvsContext::OnSueCharacterResult

    //0x38 ??
    TRADE_MONEY_LIMIT(0x39), // CWvsContext::OnTradeMoneyLimit
    SET_GENDER(0x3A), // CWvsContext::OnSetGender
    GUILD_BBS_PACKET(0x3B), // CWvsContext::OnGuildBBSPacket
    CHAR_INFO(0x3D), // CWvsContext::OnCharacterInfo
    PARTY_OPERATION(0x3E), // CWvsContext::OnPartyResult

    //0x40 CWvsContext::OnExpedtionResult
    BUDDYLIST(0x41), // CWvsContext::OnFriendResult

    //0x42 ??
    GUILD_OPERATION(0x43), // CWvsContext::OnGuildResult
    ALLIANCE_OPERATION(0x44), // CWvsContext::OnAllianceResult
    SPAWN_PORTAL(0x45), // CWvsContext::OnTownPortal
    SERVERMESSAGE(0x46), // CWvsContext::OnBroadcastMsg
    INCUBATOR_RESULT(0x47), // CWvsContext::OnIncubatorResult
    SHOP_SCANNER_RESULT(0x48), // CWvsContext::OnShopScannerResult
    SHOP_LINK_RESULT(0x49), // CWvsContext::OnShopLinkResult

    MARRIAGE_REQUEST(0x4A), // CWvsContext::OnMarriageRequest
    MARRIAGE_RESULT(0x4B), // CWvsContext::OnMarriageResult
    WEDDING_GIFT_RESULT(0x4C), // CWvsContext::OnWeddingGiftResult
    NOTIFY_MARRIED_PARTNER_MAP_TRANSFER(0x4D), // CWvsContext::OnNotifyMarriedPartnerMapTransfer

    CASH_PET_FOOD_RESULT(0x4E), // CWvsContext::OnCashPetFoodResult
    SET_WEEK_EVENT_MESSAGE(0x4F), // CWvsContext::OnSetWeekEventMessage
    SET_POTION_DISCOUNT_RATE(0x50), // CWvsContext::OnSetPotionDiscountRate

    BRIDLE_MOB_CATCH_FAIL(0x51), // CWvsContext::OnBridleMobCatchFail
    IMITATED_NPC_RESULT(0x52), // CWvsContext::OnImitatedNPCResult
    IMITATED_NPC_DATA(0x53), // CNpcPool::OnNpcImitateData CWvsContext::OnImitatedNPCData
    LIMITED_NPC_DISABLE_INFO(0x54), // CNpcPool::OnUpdateLimitedDisableInfo CWvsContext::OnLimitedNPCDisableInfo
    MONSTER_BOOK_SET_CARD(0x55), // CWvsContext::OnMonsterBookSetCard
    MONSTER_BOOK_SET_COVER(0x56), // CWvsContext::OnMonsterBookSetCover
    HOUR_CHANGED(0x57), // CWvsContext::OnHourChanged
    MINIMAP_ON_OFF(0x58), // CWvsContext::OnMiniMapOnOff
    CONSULT_AUTHKEY_UPDATE(0x59), // CWvsContext::OnConsultAuthkeyUpdate
    CLASS_COMPETITION_AUTHKEY_UPDATE(0x5A), // CWvsContext::OnClassCompetitionAuthkeyUpdate
    WEB_BOARD_AUTHKEY_UPDATE(0x5B), // CWvsContext::OnWebBoardAuthkeyUpdate
    SESSION_VALUE(0x5C), // CWvsContext::OnSessionValue
    PARTY_VALUE(0x5D), // CWvsContext::OnPartyValue
    FIELD_SET_VARIABLE(0x5E), // CWvsContext::OnFieldSetVariable
    BONUS_EXP_CHANGED(0x5F), // CWvsContext::OnBonusExpRateChanged

    // 0x60 CWvsContext::OnPotionDiscountRateChanged
    FAMILY_CHART_RESULT(0x61), // CWvsContext::OnFamilyChartResult
    FAMILY_INFO_RESULT(0x62), // CWvsContext::OnFamilyInfoResult
    FAMILY_RESULT(0x63), // CWvsContext::OnFamilyResult
    FAMILY_JOIN_REQUEST(0x64), // CWvsContext::OnFamilyJoinRequest
    FAMILY_JOIN_REQUEST_RESULT(0x65), // CWvsContext::OnFamilyJoinRequestResult
    FAMILY_JOIN_ACCEPTED(0x66), // CWvsContext::OnFamilyJoinAccepted
    FAMILY_PRIVILEGE_LIST(0x67), // CWvsContext::OnFamilyPrivilegeList
    FAMILY_REP_GAIN(0x68), // CWvsContext::OnFamilyFamousPointIncResult
    FAMILY_NOTIFY_LOGIN_OR_LOGOUT(0x69), // CWvsContext::OnFamilyNotifyLoginOrLogout
    FAMILY_SET_PRIVILEGE(0x6A), // CWvsContext::OnFamilySetPrivilege
    FAMILY_SUMMON_REQUEST(0x6B), // CWvsContext::OnFamilySummonRequest

    NOTIFY_LEVELUP(0x6C), // CWvsContext::OnNotifyLevelUp
    NOTIFY_MARRIAGE(0x6D), // CWvsContext::OnNotifyWedding
    NOTIFY_JOB_CHANGE(0x6E), // CWvsContext::OnNotifyJobChange
    MAPLE_TV_USE_RES(0x70), // CWvsContext::OnMapleTVUseRes
    AVATAR_MEGAPHONE_RESULT(0x71), // CWvsContext::OnAvatarMegaphoneRes
    SET_AVATAR_MEGAPHONE(0x72), // CWvsContext::OnSetAvatarMegaphone
    CLEAR_AVATAR_MEGAPHONE(0x73), // CWvsContext::OnClearAvatarMegaphone
    CANCEL_NAME_CHANGE_RESULT(0x74), // CWvsContext::OnCancelNameChangeResult
    CANCEL_TRANSFER_WORLD_RESULT(0x75), // CWvsContext::OnCancelTransferWorldResult
    DESTROY_SHOP_RESULT(0x76), // CWvsContext::OnDestroyShopResult
    FAKE_GM_NOTICE(0x77), // sub_AC26E5
    SUCCESS_IN_USE_GACHAPON_BOX(0x78), // CWvsContext::OnSuccessInUsegachaponBox
    NEW_YEAR_CARD_RES(0x79), // CWvsContext::OnNewYearCardRes
    RANDOM_MORPH_RES(0x7A), // CWvsContext::OnRandomMorphRes
    CANCEL_NAME_CHANGE_BY_OTHER(0x7B), // CWvsContext::OnCancelNameChangebyOther
    SET_EXTRA_PENDANT_SLOT(0x7C), // CWvsContext::OnSetBuyEquipExt
    SCRIPT_PROGRESS_MESSAGE(0x7D), // CWvsContext::OnScriptProgressMessage
    DATA_CRC_CHECK_FAILED(0x7E), // CWvsContext::OnDataCRCCheckFailed

    //0x7F CWvsContext::OnCakePieEventResult
    //0x80 CWvsContext::OnUpdateGMBoard
    //0x81 CWvsContext::OnShowSlotMessage
    //0x82 CWvsContext::OnAccountMoreInfo
    //0x83 CWvsContext::OnFindFirend
    MACRO_SYS_DATA_INIT(0x84), // CWvsContext::OnMacroSysDataInit
    SET_FIELD(0x85), // CStage::OnSetField
    SET_ITC(0x86), // CStage::OnSetITC
    SET_CASH_SHOP(0x87), // CStage::OnSetCashShop
    SET_BACK_EFFECT(0x88), // CMapLoadable::OnSetBackEffect
    SET_MAP_OBJECT_VISIBLE(0x89), // CMapLoadable::OnSetMapObjectVisible
    CLEAR_BACK_EFFECT(0x8A), // CMapLoadable::OnClearBackEffect
    BLOCKED_MAP(0x8B), // CField::OnTransferFieldReqIgnored
    BLOCKED_SERVER(0x8C), // CField::OnTransferChannelReqIgnored
    FORCED_MAP_EQUIP(0x8D), // CField::OnFieldSpecificData
    MULTICHAT(0x8E), // CField::OnGroupMessage
    WHISPER(0x8F), // CField::OnWhisper
    SPOUSE_CHAT(0x90), // CField::OnCoupleMessage
    SUMMON_ITEM_INAVAILABLE(0x91), // CField::OnSummonItemInavailable
    FIELD_EFFECT(0x92), // CField::OnFieldEffect
    FIELD_OBSTACLE_ONOFF(0x93), // CField::OnFieldObstacleOnOff
    FIELD_OBSTACLE_ONOFF_LIST(0x94), // CField::OnFieldObstacleOnOffStatus
    FIELD_OBSTACLE_ALL_RESET(0x95), // CField::OnFieldObstacleAllReset
    BLOW_WEATHER(0x96), // CField::OnBlowWeather
    PLAY_JUKEBOX(0x97), // CField::OnPlayJukeBox
    ADMIN_RESULT(0x98), // CField::OnAdminResult
    OX_QUIZ(0x99), // CField::OnQuiz
    GMEVENT_INSTRUCTIONS(0x9A), // CField::OnDesc
    CLOCK(0x9B),
    CONTI_MOVE(0x9C), // CField_ContiMove::OnContiMove
    CONTI_STATE(0x9D), // CField_ContiMove::OnContiState
    SET_QUEST_CLEAR(0x9E), // CField::OnSetQuestClear
    SET_QUEST_TIME(0x9F), // CField::OnSetQuestTime
    ARIANT_RESULT(0xA0), // CField::OnWarnMessage
    SET_OBJECT_STATE(0xA1), // CField::OnSetObjectState
    STOP_CLOCK(0xA2), // CField::OnDestroyClock
    ARIANT_ARENA_SHOW_RESULT(0xA3), // CField_AriantArena::OnShowResult

    // 0xA4 CField::OnStalkResult
    PYRAMID_GAUGE(0xA5), // CField_Massacre::OnMassacreIncGauge
    PYRAMID_SCORE(0xA6), // CField_MassacreResult::OnMassacreResult
    QUICKSLOT_INIT(0xA7), // CQuickslotKeyMappedMan::OnInit

    // 0xA8 sub_5604F9

    // 0xA9 sub_5605FF

    // 0xAA CField::OnFootHoldInfo
    SPAWN_PLAYER(0xAB), // CUserPool::OnUserEnterField
    REMOVE_PLAYER_FROM_MAP(0xAC), // CUserPool::OnUserLeaveField
    CHATTEXT(0xAD), // CUser::OnChat
    CHATTEXT1(0xAE), // CUser::OnChat
    CHALKBOARD(0xAF), // CUser::OnADBoard
    UPDATE_CHAR_BOX(0xB0), // CUser::OnMiniRoomBalloon
    SHOW_CONSUME_EFFECT(0xB1), // CUser::SetConsumeItemEffect
    SHOW_SCROLL_EFFECT(0xB2), // CUser::ShowItemUpgradeEffect
    SPAWN_PET(0xB4), // CUser::OnPetPacket

    // 0xB5 CUser::OnPetPacket
    // 0xB6 CUser::OnPetPacket
    MOVE_PET(0xB7), // CPet::OnMove
    PET_CHAT(0xB8), // CPet::OnAction
    PET_NAMECHANGE(0xB9), // CPet::OnNameChanged
    PET_EXCEPTION_LIST(0xBA), // CPet::OnLoadExceptionList
    PET_COMMAND(0xBB), // CPet::OnActionCommand
    SPAWN_SPECIAL_MAPOBJECT(0xBC), // CSummonedPool::OnPacket
    REMOVE_SPECIAL_MAPOBJECT(0xBD), // CSummonedPool::OnPacket
    MOVE_SUMMON(0xBE), // CSummonedPool::OnMove
    SUMMON_ATTACK(0xBF), // CSummonedPool::OnAttack
    DAMAGE_SUMMON(0xC0), // CSummonedPool::OnHit
    SUMMON_SKILL(0xC1), // CSummonedPool::OnSkill
    SPAWN_DRAGON(0xC2), // CUser::OnDragonPacket
    MOVE_DRAGON(0xC3), // CUser::OnDragonPacket
    REMOVE_DRAGON(0xC4), // CUser::OnDragonPacket
    MOVE_PLAYER(0xC6), // CUserRemote::OnMove
    CLOSE_RANGE_ATTACK(0xC7), // CUserRemote::OnAttack
    RANGED_ATTACK(0xC8), // CUserRemote::OnAttack
    MAGIC_ATTACK(0xC9), // CUserRemote::OnAttack
    ENERGY_ATTACK(0xCA), // CUserRemote::OnAttack
    SKILL_EFFECT(0xCB), // CUserRemote::OnSkillPrepare
    CANCEL_SKILL_EFFECT(0xCC), // CUserRemote::OnSkillCancel
    DAMAGE_PLAYER(0xCD), // CUserRemote::OnHit
    FACIAL_EXPRESSION(0xCE), // CUserPool::OnUserRemotePacket
    SHOW_ITEM_EFFECT(0xCF), // CUserPool::OnUserRemotePacket

    // 0xD0 CUserRemote::OnShowUpgradeTombEffect
    SHOW_CHAIR(0xD1), // CUserPool::OnUserRemotePacket
    UPDATE_CHAR_LOOK(0xD2), // CUserRemote::OnAvatarModified
    SHOW_FOREIGN_EFFECT(0xD3), // CUser::OnEffect
    GIVE_FOREIGN_BUFF(0xD4), // CUserRemote::OnSetTemporaryStat
    CANCEL_FOREIGN_BUFF(0xD5), // CUserRemote::OnResetTemporaryStat
    UPDATE_PARTYMEMBER_HP(0xD6), // CUserRemote::OnReceiveHP
    GUILD_NAME_CHANGED(0xD7), // CUserRemote::OnGuildNameChanged
    GUILD_MARK_CHANGED(0xD8), // CUserRemote::OnGuildMarkChanged
    THROW_GRENADE(0xD9), // CUserRemote::OnThrowGrenade
    CANCEL_CHAIR(0xDA), // CUserLocal::OnSitResult
    SHOW_ITEM_GAIN_INCHAT(0xDB), // CUser::OnEffec
    DOJO_WARP_UP(0xDC), // CUserLocal::OnTeleport
    LUCKSACK_PASS(0xDE), // CUserLocal::OnMesoGive_Succeeded // TODO handling of this might be wrong
    LUCKSACK_FAIL(0xDF), // CUserLocal::OnMesoGive_Failed // TODO handling of this might be wrong
    MESO_BAG_MESSAGE(0xDD),  // TODO handling of this might be wrong
    UPDATE_QUEST_INFO(0xE0), // CUserLocal::OnQuestResult

    // 0xE1 CUserLocal::OnNotifyHPDecByField
    // 0xE2 nullsub_18

    PLAYER_HINT(0xE3), // CUserLocal::OnBalloonMsg

    // 0xE4 CUserLocal::OnPlayEventSound
    // 0xE5 CUserLocal::OnPlayMinigameSound
    MAKER_RESULT(0xE6), // CUserLocal::OnMakerResult

    // 0xE7 ??
    KOREAN_EVENT(0xE8), // CUserLocal::OnOpenClassCompetitionPage
    OPEN_UI(0xE9), // CUserLocal::OnOpenUI

    // 0xEA  // CUserLocal::OnOpenUIWithOption
    LOCK_UI(0xEB), // CUserLocal::SetDirectionMode
    DISABLE_UI(0xEC), // CUserLocal::OnSetStandAloneMode
    SPAWN_GUIDE(0xED), // CUserLocal::OnHireTutor
    TALK_GUIDE(0xEE), // CUserLocal::OnTutorMsg
    SHOW_COMBO(0xEF), // CUserLocal::OnIncComboResponse

    // 0xF0 CUser::OnRandomEmotion
    // 0xF1 CUserLocal::OnResignQuestReturn
    // 0xF2 CUserLocal::OnPassMateName
    // 0xF3 CUserLocal::OnRadioSchedule
    // 0xF4 CUserLocal::OnOpenSkillGuide
    // 0xF5 CUserLocal::OnNoticeMsg
    // 0xF6 CUserLocal::OnChatMsg
    // 0xF7 CUserLocal::OnBuffzoneEffect
    // 0xF8 CUserLocal::OnGoToCommoditySN
    // 0xF9 CUserLocal::OnDamageMeter
    COOLDOWN(0xFA), // CUserLocal::OnSkillCooltimeSet
    SPAWN_MONSTER(0xFC), // CMobPool::OnMobEnterField
    KILL_MONSTER(0xFD), // CMobPool::OnMobLeaveField
    SPAWN_MONSTER_CONTROL(0xFE), // CMobPool::OnMobChangeController
    MOVE_MONSTER(0xFF), // CMob::OnMove
    MOVE_MONSTER_RESPONSE(0x100), // CMob::OnCtrlAck

    // 0x101 ??
    APPLY_MONSTER_STATUS(0x102), // CMob::OnStatSet
    CANCEL_MONSTER_STATUS(0x103), // CMob::OnStatReset
    RESET_MONSTER_ANIMATION(0x104), // CMob::OnSuspendReset

    // 0x105 CMob::OnAffected
    DAMAGE_MONSTER(0x106), // CMob::OnDamaged

    // 0x107 CMob::OnSpecialEffectBySkill
    // 0x108 ??
    ARIANT_THING(0x109), // CMobPool::OnMobCrcKeyChanged
    SHOW_MONSTER_HP(0x10A), // CMob::OnHPIndicator
    CATCH_MONSTER(0x10B), // CMob::OnCatchEffect
    CATCH_MONSTER_WITH_ITEM(0x10C), // CMob::OnEffectByItem
    SHOW_MAGNET(0x10D), // CMob::OnMobSpeaking

    // 0x10E CMob::OnIncMobChargeCount
    // 0x10F CMob::OnMobSkillDelay
    // 0x110 CMob::OnMobAttackedByMob
    SPAWN_NPC(0x112), // CNpcPool::OnNpcEnterField
    REMOVE_NPC(0x113), // CNpcPool::OnNpcLeaveField
    SPAWN_NPC_REQUEST_CONTROLLER(0x114), // CNpcPool::OnNpcChangeController
    NPC_ACTION(0x115), // CNpc::OnMove

    // 0x116 CNpc::OnUpdateLimitedInfo
    // 0x117 CNpc::OnSetSpecialAction
    SET_NPC_SCRIPTABLE(0x118), // CNpcTemplate::OnSetNpcScript
    SPAWN_HIRED_MERCHANT(0x11A), // CEmployeePool::OnEmployeeEnterField
    DESTROY_HIRED_MERCHANT(0x11B), // CEmployeePool::OnEmployeeLeaveField
    UPDATE_HIRED_MERCHANT(0x11C), // CEmployeePool::OnEmployeeMiniRoomBalloon
    DROP_ITEM_FROM_MAPOBJECT(0x11D), // CDropPool::OnDropEnterField
    REMOVE_ITEM_FROM_MAP(0x11E), // CDropPool::OnDropLeaveField
    CANNOT_SPAWN_KITE(0x11F), // CMessageBoxPool::OnCreateFailed
    SPAWN_KITE(0x120), // CMessageBoxPool::OnMessageBoxEnterField
    REMOVE_KITE(0x121), // CMessageBoxPool::OnMessageBoxLeaveField
    SPAWN_MIST(0x122), // CAffectedAreaPool::OnAffectedAreaCreated
    REMOVE_MIST(0x123), // CAffectedAreaPool::OnAffectedAreaRemoved
    SPAWN_DOOR(0x124), // CTownPortalPool::OnTownPortalCreated
    REMOVE_DOOR(0x125), // CTownPortalPool::OnTownPortalRemoved
    REACTOR_HIT(0x126), // CReactorPool::OnReactorChangeState
    // 0x127 ??
    REACTOR_SPAWN(0x128), // CReactorPool::OnReactorEnterField
    REACTOR_DESTROY(0x129), // CReactorPool::OnReactorLeaveField
    SNOWBALL_STATE(0x12A), // CField_SnowBall::OnSnowBallState
    HIT_SNOWBALL(0x12B), // CField_SnowBall::OnSnowBallHit
    SNOWBALL_MESSAGE(0x12C), // CField_SnowBall::OnSnowBallMsg
    LEFT_KNOCK_BACK(0x12D), // CField_SnowBall::OnSnowBallTouch
    COCONUT_HIT(0x12E), // CField_Coconut::OnCoconutHit
    COCONUT_SCORE(0x12F), // CField_Coconut::OnCoconutScore
    GUILD_BOSS_HEALER_MOVE(0x130), // CField_GuildBoss::OnHealerMove
    GUILD_BOSS_PULLEY_STATE_CHANGE(0x131), // CField_GuildBoss::OnPulleyStateChange
    MONSTER_CARNIVAL_START(0x132), // CField_MonsterCarnival::OnEnter
    MONSTER_CARNIVAL_OBTAINED_CP(0x133), // CField_MonsterCarnival::OnPersonalCP
    MONSTER_CARNIVAL_PARTY_CP(0x134), // CField_MonsterCarnival::OnTeamCP
    MONSTER_CARNIVAL_SUMMON(0x135), // CField_MonsterCarnival::OnRequestResult
    MONSTER_CARNIVAL_MESSAGE(0x136), // CField_MonsterCarnival::OnRequestResult
    MONSTER_CARNIVAL_DIED(0x137), // CField_MonsterCarnival::OnProcessForDeath
    MONSTER_CARNIVAL_LEAVE(0x138), // CField_MonsterCarnival::OnShowMemberOutMsg

    // 0x139 CField_MonsterCarnival::OnShowGameResult

    ARIANT_ARENA_USER_SCORE(0x13A), // CField_AriantArena::OnUserScore
    SHEEP_RANCH_INFO(0x13C), // CField_Battlefield::OnScoreUpdate
    SHEEP_RANCH_CLOTHES(0x13D), // CField_Battlefield::OnTeamChanged
    WITCH_TOWER_SCORE_UPDATE(0x13E), // CField_Witchtower::OnScoreUpdate
    HORNTAIL_CAVE(0x13F), // CField::OnHontailTimer
    ZAKUM_SHRINE(0x140), // CField::OnZakumTimer
    NPC_TALK(0x141), // CScriptMan::OnPacket
    OPEN_NPC_SHOP(0x142), // CShopDlg::OnPacket
    CONFIRM_SHOP_TRANSACTION(0x143), // CShopDlg::OnPacket
    ADMIN_SHOP_MESSAGE(0x144), // CAdminShopDlg::OnPacket
    ADMIN_SHOP(0x145), // CAdminShopDlg::OnPacket
    STORAGE(0x146), // CTrunkDlg::OnPacket
    FREDRICK_MESSAGE(0x147), // CStoreBankDlg::OnPacket
    FREDRICK(0x148), // CStoreBankDlg::OnPacket
    RPS_GAME(0x149), // CRPSGameDlg::OnPacket
    MESSENGER(0x14A), // CUIMessenger::OnPacket
    PLAYER_INTERACTION(0x14B), // CMiniRoomBaseDlg::OnPacketBase

    TOURNAMENT(0x14C), // CField_Tournament::OnTournament
    TOURNAMENT_MATCH_TABLE(0x14D), // CField_Tournament::OnTournamentMatchTable
    TOURNAMENT_SET_PRIZE(0x14E), // CField_Tournament::OnTournamentSetPrize
    TOURNAMENT_UEW(0x14F), // CField_Tournament::OnTournamentUEW
    TOURNAMENT_CHARACTERS(0x150), // nullsub_12

    WEDDING_PROGRESS(0x151), // CField_Wedding::OnWeddingProgress
    WEDDING_CEREMONY_END(0x152), // CField_Wedding::OnWeddingCeremonyEnd

    PARCEL(0x153), // CParcelDlg::OnPacket

    CHARGE_PARAM_RESULT(0x154), // CCashShop::OnChargeParamResult
    QUERY_CASH_RESULT(0x155), // CCashShop::OnQueryCashResult
    CASHSHOP_OPERATION(0x156), // CCashShop::OnCashItemResult
    CASHSHOP_PURCHASE_EXP_CHANGED(0x157), // CCashShop::OnPurchaseExpChanged
    CASHSHOP_GIFT_INFO_RESULT(0x158), // CCashShop::OnGiftMateInfoResult
    CASHSHOP_CHECK_NAME_CHANGE(0x159), // CCashShop::OnCheckDuplicatedIDResult
    CASHSHOP_CHECK_NAME_CHANGE_POSSIBLE_RESULT(0x15A), // CCashShop::OnCheckNameChangePossibleResult
    CASHSHOP_REGISTER_NEW_CHARACTER_RESULT(0x14A),
    CASHSHOP_CHECK_TRANSFER_WORLD_POSSIBLE_RESULT(0x15C), // CCashShop::OnCheckTransferWorldPossibleResult
    CASHSHOP_GACHAPON_STAMP_RESULT(0x15D), // CCashShop::OnCashShopGachaponStampResult
    CASHSHOP_CASH_ITEM_GACHAPON_RESULT(0x15E), // CCashShop::OnCashItemGachaponResult
    // 0x15F CCashShop::OnCashItemGachaponResult
    CASHSHOP_CASH_GACHAPON_OPEN_RESULT(0x14E),

    // 0x161 CCashShop::OnOneADay
    KEYMAP(0x163), // CFuncKeyMappedMan::OnInit
    AUTO_HP_POT(0x164), // CFuncKeyMappedMan::OnPetConsumeItemInit
    AUTO_MP_POT(0x165), // CFuncKeyMappedMan::OnPetConsumeMPItemInit
    SEND_TV(0x16A), // CMapleTVMan::OnSetMessage
    REMOVE_TV(0x16B), // CMapleTVMan::OnClearMessage
    ENABLE_TV(0x16C), // CMapleTVMan::OnSendMessageResult
    MTS_OPERATION2(0x15B), // CField::OnCharacterSale TODO
    MTS_OPERATION(0x15C), // CField::OnCharacterSale TODO
    MAPLELIFE_RESULT(0x15D), // TODO
    MAPLELIFE_ERROR(0x173), // CField::OnItemUpgrade // TODO
    // 0x174 CField::OnItemUpgrade // TODO
    VICIOUS_HAMMER(0x162),

    // 0x17A CField::OnVega
    VEGA_SCROLL(0x17B); // CField::OnVega
    // 0x17C CField::OnVega
    // 0x17D CField::OnVega
    private int code;

    SendOpcode(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }
}
