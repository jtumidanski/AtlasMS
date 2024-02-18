package connection.packets;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import connection.constants.SendOpcode;
import net.server.channel.handlers.PlayerInteractionHandler;
import server.MapleTrade;
import server.maps.MapleHiredMerchant;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CMiniRoomBaseDlg {
    public static byte[] getPlayerShopChat(MapleCharacter chr, String chat, boolean owner) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CHAT.getCode());
        mplew.write(PlayerInteractionHandler.Action.CHAT_THING.getCode());
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(chr.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopNewVisitor(MapleCharacter chr, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.VISIT.getCode());
        mplew.write(slot);
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopRemoveVisitor(int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        if (slot != 0) {
            mplew.writeShort(slot);
        }
        return mplew.getPacket();
    }

    public static byte[] getTradePartnerAdd(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.VISIT.getCode());
        mplew.write(1);
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        return mplew.getPacket();
    }

    public static byte[] tradeInvite(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.INVITE.getCode());
        mplew.write(3);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.write(new byte[]{(byte) 0xB7, (byte) 0x50, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] getTradeMesoSet(byte number, int meso) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SET_MESO.getCode());
        mplew.write(number);
        mplew.writeInt(meso);
        return mplew.getPacket();
    }

    public static byte[] getTradeItemAdd(byte number, Item item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SET_ITEMS.getCode());
        mplew.write(number);
        mplew.write(item.getPosition());
        CCommon.addItemInfo(mplew, item, true);
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopItemUpdate(MaplePlayerShop shop) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
        mplew.write(shop.getItems()
                .size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem()
                    .getQuantity());
            mplew.writeInt(item.getPrice());
            CCommon.addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopOwnerUpdate(MaplePlayerShop.SoldItem item, int position) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.UPDATE_PLAYERSHOP.getCode());
        mplew.write(position);
        mplew.writeShort(item.getQuantity());
        mplew.writeMapleAsciiString(item.getBuyer());

        return mplew.getPacket();
    }

    /**
     * @param c
     * @param shop
     * @param owner
     * @return
     */
    public static byte[] getPlayerShop(MaplePlayerShop shop, boolean owner) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(4);
        mplew.write(4);
        mplew.write(owner ? 0 : 1);

        if (owner) {
            List<MaplePlayerShop.SoldItem> sold = shop.getSold();
            mplew.write(sold.size());
            for (MaplePlayerShop.SoldItem s : sold) {
                mplew.writeInt(s.getItemId());
                mplew.writeShort(s.getQuantity());
                mplew.writeInt(s.getMesos());
                mplew.writeMapleAsciiString(s.getBuyer());
            }
        } else {
            mplew.write(0);
        }

        CCommon.addCharLook(mplew, shop.getOwner(), false);
        mplew.writeMapleAsciiString(shop.getOwner()
                .getName());

        MapleCharacter[] visitors = shop.getVisitors();
        for (int i = 0; i < 3; i++) {
            if (visitors[i] != null) {
                mplew.write(i + 1);
                CCommon.addCharLook(mplew, visitors[i], false);
                mplew.writeMapleAsciiString(visitors[i].getName());
            }
        }

        mplew.write(0xFF);
        mplew.writeMapleAsciiString(shop.getDescription());
        List<MaplePlayerShopItem> items = shop.getItems();
        mplew.write(0x10);  //TODO SLOTS, which is 16 for most stores...slotMax
        mplew.write(items.size());
        for (MaplePlayerShopItem item : items) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem()
                    .getQuantity());
            mplew.writeInt(item.getPrice());
            CCommon.addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(3);
        mplew.write(2);
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            CCommon.addCharLook(mplew, trade.getPartner()
                    .getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner()
                    .getChr()
                    .getName());
        }
        mplew.write(number);
        CCommon.addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer()
                .getName());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    public static byte[] getTradeConfirmation() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CONFIRM.getCode());
        return mplew.getPacket();
    }

    /**
     * Possible values for <code>operation</code>:<br> 2: Trade cancelled, by the
     * other character<br> 7: Trade successful<br> 8: Trade unsuccessful<br>
     * 9: Cannot carry more one-of-a-kind items<br> 12: Cannot trade on different maps<br>
     * 13: Cannot trade, game files damaged<br>
     *
     * @param number
     * @param operation
     * @return
     */
    public static byte[] getTradeResult(byte number, byte operation) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(number);
        mplew.write(operation);
        return mplew.getPacket();
    }

    public static byte[] getMiniGame(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(1);
        mplew.write(0);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        CCommon.addCharLook(mplew, minigame.getOwner(), false);
        mplew.writeMapleAsciiString(minigame.getOwner()
                .getName());
        minigame.getVisitor()
                .ifPresent(v -> writeMiniGameVisitorLook(mplew, v));
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(1);
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
        mplew.writeInt(minigame.getOwnerScore());
        minigame.getVisitor()
                .ifPresent(v -> writeMiniGameVisitorScore(minigame, mplew, v, 1));
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.write(piece);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameReady(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.READY.getCode());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameUnReady(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.UN_READY.getCode());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameStart(MapleMiniGame game, int loser) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.START.getCode());
        mplew.write(loser);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkipOwner(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SKIP.getCode());
        mplew.write(0x01);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRequestTie(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.REQUEST_TIE.getCode());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameDenyTie(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ANSWER_TIE.getCode());
        return mplew.getPacket();
    }

    /**
     * 1 = Room already closed  2 = Can't enter due full cappacity 3 = Other requests at this minute
     * 4 = Can't do while dead 5 = Can't do while middle event 6 = This character unable to do it
     * 7, 20 = Not allowed to trade anymore 9 = Can only trade on same map 10 = May not open store near portal
     * 11, 14 = Can't start game here 12 = Can't open store at this channel 13 = Can't estabilish miniroom
     * 15 = Stores only an the free market 16 = Lists the rooms at FM (?) 17 = You may not enter this store
     * 18 = Owner undergoing store maintenance 19 = Unable to enter tournament room 21 = Not enough mesos to enter
     * 22 = Incorrect password
     *
     * @param status
     * @return
     */
    public static byte[] getMiniRoomError(int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(0);
        mplew.write(status);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkipVisitor(MapleMiniGame game) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(PlayerInteractionHandler.Action.SKIP.getCode());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameMoveOmok(MapleMiniGame game, int move1, int move2, int move3) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(12);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.MOVE_OMOK.getCode());
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameNewVisitor(MapleMiniGame minigame, MapleCharacter chr, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.VISIT.getCode());
        mplew.write(slot);
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeInt(1);
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
        mplew.writeInt(minigame.getVisitorScore());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRemoveVisitor() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(1);
        return mplew.getPacket();
    }

    static byte[] getMiniGameResult(MapleMiniGame game, int tie, int result, int forfeit) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.GET_RESULT.getCode());

        int matchResultType;
        if (tie == 0 && forfeit != 1) {
            matchResultType = 0;
        } else if (tie != 0) {
            matchResultType = 1;
        } else {
            matchResultType = 2;
        }

        mplew.write(matchResultType);
        mplew.writeBool(result == 2); // host/visitor wins

        boolean omok = game.isOmok();
        if (matchResultType == 1) {
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok)); // wins
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok)); // ties
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok)); // losses
            mplew.writeInt(game.getOwnerScore()); // points

            mplew.writeInt(0); // unknown
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok))
                    .orElse(0)); // wins
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok))
                    .orElse(0)); // ties
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok))
                    .orElse(0)); // losses
            mplew.writeInt(game.getVisitorScore()); // points
            mplew.write(0);
        } else {
            mplew.writeInt(0);
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok)); // wins
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok)); // ties
            mplew.writeInt(game.getOwner()
                    .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok)); // losses
            mplew.writeInt(game.getOwnerScore()); // points
            mplew.writeInt(0);
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, omok))
                    .orElse(0)); // wins
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, omok))
                    .orElse(0)); // ties
            mplew.writeInt(game.getVisitor()
                    .map(v -> v.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, omok))
                    .orElse(0)); // losses
            mplew.writeInt(game.getVisitorScore()); // points
        }

        return mplew.getPacket();
    }

    public static byte[] getMiniGameOwnerWin(MapleMiniGame game, boolean forfeit) {
        return getMiniGameResult(game, 0, 1, forfeit ? 1 : 0);
    }

    public static byte[] getMiniGameVisitorWin(MapleMiniGame game, boolean forfeit) {
        return getMiniGameResult(game, 0, 2, forfeit ? 1 : 0);
    }

    public static byte[] getMiniGameTie(MapleMiniGame game) {
        return getMiniGameResult(game, 1, 3, 0);
    }

    public static byte[] getMiniGameClose(boolean visitor, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.writeBool(visitor);
        mplew.write(type); /* 2 : CRASH 3 : The room has been closed 4 : You have left the room 5 : You have been expelled  */
        return mplew.getPacket();
    }

    public static byte[] getMatchCard(MapleClient c, MapleMiniGame minigame, boolean owner, int piece) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(2);
        mplew.write(2);
        mplew.write(owner ? 0 : 1);
        mplew.write(0);
        CCommon.addCharLook(mplew, minigame.getOwner(), false);
        mplew.writeMapleAsciiString(minigame.getOwner()
                .getName());
        minigame.getVisitor()
                .ifPresent(v -> writeMiniGameVisitorLook(mplew, v));
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(2);
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, false));
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, false));
        mplew.writeInt(minigame.getOwner()
                .getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, false));

        //set vs
        mplew.writeInt(minigame.getOwnerScore());
        minigame.getVisitor()
                .ifPresent(v -> writeMiniGameVisitorScore(minigame, mplew, v, 2));
        mplew.write(0xFF);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.write(piece);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getMatchCardStart(MapleMiniGame game, int loser) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.START.getCode());
        mplew.write(loser);

        int last;
        if (game.getMatchesToWin() > 10) {
            last = 30;
        } else if (game.getMatchesToWin() > 6) {
            last = 20;
        } else {
            last = 12;
        }

        mplew.write(last);
        for (int i = 0; i < last; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCardNewVisitor(MapleMiniGame minigame, MapleCharacter chr, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.VISIT.getCode());
        mplew.write(slot);
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeInt(1);
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, false));
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, false));
        mplew.writeInt(chr.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, false));
        mplew.writeInt(minigame.getVisitorScore());
        return mplew.getPacket();
    }

    public static byte[] getMatchCardSelect(MapleMiniGame game, int turn, int slot, int firstslot, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.SELECT_CARD.getCode());
        mplew.write(turn);
        if (turn == 1) {
            mplew.write(slot);
        } else if (turn == 0) {
            mplew.write(slot);
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static byte[] getPlayerShopChat(MapleCharacter chr, String chat, byte slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CHAT.getCode());
        mplew.write(PlayerInteractionHandler.Action.CHAT_THING.getCode());
        mplew.write(slot);
        mplew.writeMapleAsciiString(chr.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getTradeChat(MapleCharacter chr, String chat, boolean owner) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CHAT.getCode());
        mplew.write(PlayerInteractionHandler.Action.CHAT_THING.getCode());
        mplew.write(owner ? 0 : 1);
        mplew.writeMapleAsciiString(chr.getName() + " : " + chat);
        return mplew.getPacket();
    }

    public static byte[] getHiredMerchant(MapleCharacter chr, MapleHiredMerchant hm, boolean firstTime) {//Thanks Dustin
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(0x05);
        mplew.write(0x04);
        mplew.writeShort(hm.getVisitorSlotThreadsafe(chr) + 1);
        mplew.writeInt(hm.getItemId());
        mplew.writeMapleAsciiString("Hired Merchant");

        MapleCharacter[] visitors = hm.getVisitors();
        for (int i = 0; i < 3; i++) {
            if (visitors[i] != null) {
                mplew.write(i + 1);
                CCommon.addCharLook(mplew, visitors[i], false);
                mplew.writeMapleAsciiString(visitors[i].getName());
            }
        }
        mplew.write(-1);
        if (hm.isOwner(chr)) {
            List<Pair<String, Byte>> msgList = hm.getMessages();

            mplew.writeShort(msgList.size());
            for (int i = 0; i < msgList.size(); i++) {
                mplew.writeMapleAsciiString(msgList.get(i)
                        .getLeft());
                mplew.write(msgList.get(i)
                        .getRight());
            }
        } else {
            mplew.writeShort(0);
        }
        mplew.writeMapleAsciiString(hm.getOwner());
        if (hm.isOwner(chr)) {
            mplew.writeShort(0);
            mplew.writeShort(hm.getTimeOpen());
            mplew.write(firstTime ? 1 : 0);
            List<MapleHiredMerchant.SoldItem> sold = hm.getSold();
            mplew.write(sold.size());
            for (MapleHiredMerchant.SoldItem s : sold) {
                mplew.writeInt(s.getItemId());
                mplew.writeShort(s.getQuantity());
                mplew.writeInt(s.getMesos());
                mplew.writeMapleAsciiString(s.getBuyer());
            }
            mplew.writeInt(chr.getMerchantMeso());//:D?
        }
        mplew.writeMapleAsciiString(hm.getDescription());
        mplew.write(0x10); //TODO SLOTS, which is 16 for most stores...slotMax
        mplew.writeInt(hm.isOwner(chr) ? chr.getMerchantMeso() : chr.getMeso());
        mplew.write(hm.getItems()
                .size());
        if (hm.getItems()
                .isEmpty()) {
            mplew.write(0);//Hmm??
        } else {
            for (MaplePlayerShopItem item : hm.getItems()) {
                mplew.writeShort(item.getBundles());
                mplew.writeShort(item.getItem()
                        .getQuantity());
                mplew.writeInt(item.getPrice());
                CCommon.addItemInfo(mplew, item.getItem(), true);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateHiredMerchant(MapleHiredMerchant hm, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
        mplew.writeInt(hm.isOwner(chr) ? chr.getMerchantMeso() : chr.getMeso());
        mplew.write(hm.getItems()
                .size());
        for (MaplePlayerShopItem item : hm.getItems()) {
            mplew.writeShort(item.getBundles());
            mplew.writeShort(item.getItem()
                    .getQuantity());
            mplew.writeInt(item.getPrice());
            CCommon.addItemInfo(mplew, item.getItem(), true);
        }
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantChat(String message, byte slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.CHAT.getCode());
        mplew.write(PlayerInteractionHandler.Action.CHAT_THING.getCode());
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantVisitorLeave(int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        if (slot != 0) {
            mplew.write(slot);
        }
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantOwnerLeave() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.getCode());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantOwnerMaintenanceLeave() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.getCode());
        mplew.write(5);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantMaintenanceMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.ROOM.getCode());
        mplew.write(0x00);
        mplew.write(0x12);
        return mplew.getPacket();
    }

    public static byte[] leaveHiredMerchant(int slot, int status2) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.EXIT.getCode());
        mplew.write(slot);
        mplew.write(status2);
        return mplew.getPacket();
    }

    public static byte[] hiredMerchantVisitorAdd(MapleCharacter chr, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Action.VISIT.getCode());
        mplew.write(slot);
        CCommon.addCharLook(mplew, chr, false);
        mplew.writeMapleAsciiString(chr.getName());
        return mplew.getPacket();
    }

    public static byte[] shopErrorMessage(int error, int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x0A);
        mplew.write(type);
        mplew.write(error);
        return mplew.getPacket();
    }

    private static void writeMiniGameVisitorLook(MaplePacketLittleEndianWriter mplew, MapleCharacter visitor) {
        mplew.write(1);
        CCommon.addCharLook(mplew, visitor, false);
        mplew.writeMapleAsciiString(visitor.getName());
    }

    private static void writeMiniGameVisitorScore(MapleMiniGame minigame, MaplePacketLittleEndianWriter mplew, MapleCharacter visitor, int mode) {
        mplew.write(1);
        mplew.writeInt(mode);
        mplew.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.WIN, true));
        mplew.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.TIE, true));
        mplew.writeInt(visitor.getMiniGamePoints(MapleMiniGame.MiniGameResult.LOSS, true));
        mplew.writeInt(minigame.getVisitorScore());
    }
}
