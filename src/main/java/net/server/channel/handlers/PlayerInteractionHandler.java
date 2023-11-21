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
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.inventory.manipulator.MapleKarmaManipulator;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import server.ItemInformationProvider;
import server.MapleTrade;
import server.maps.FieldLimit;
import server.maps.MapleHiredMerchant;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMiniGame;
import server.maps.MapleMiniGame.MiniGameType;
import server.maps.MaplePlayerShop;
import server.maps.MaplePlayerShopItem;
import server.maps.MaplePortal;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Matze
 * @author Ronan - concurrency safety and reviewed minigames
 */
public final class PlayerInteractionHandler extends AbstractMaplePacketHandler {
    private static int establishMiniroomStatus(MapleCharacter chr, boolean isMinigame) {
        if (isMinigame && FieldLimit.CANNOTMINIGAME.check(chr.getMap().getFieldLimit())) {
            return 11;
        }

        if (chr.getChalkboard().isPresent()) {
            return 13;
        }

        if (chr.getEventInstance().isPresent()) {
            return 5;
        }

        return 0;
    }

    private static boolean isTradeOpen(MapleCharacter chr) {
        if (chr.getTrade() != null) {   // thanks to Rien dev team
            //Apparently there is a dupe exploit that causes racing conditions when saving/retrieving from the db with stuff like trade open.
            chr.announce(MaplePacketCreator.enableActions());
            return true;
        }

        return false;
    }

    private static boolean canPlaceStore(MapleCharacter chr) {
        try {
            for (MapleMapObject mmo : chr.getMap().getMapObjectsInRange(chr.getPosition(), 23000, Arrays.asList(MapleMapObjectType.HIRED_MERCHANT, MapleMapObjectType.PLAYER))) {
                if (mmo instanceof MapleCharacter) {
                    MapleCharacter mc = (MapleCharacter) mmo;
                    if (mc.getId() == chr.getId()) {
                        continue;
                    }

                    MaplePlayerShop shop = mc.getPlayerShop();
                    if (shop != null && shop.isOwner(mc)) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(13));
                        return false;
                    }
                } else {
                    chr.announce(MaplePacketCreator.getMiniRoomError(13));
                    return false;
                }
            }

            Point cpos = chr.getPosition();
            MaplePortal portal = chr.getMap().findClosestTeleportPortal(cpos);
            if (portal != null && portal.getPosition().distance(cpos) < 120.0) {
                chr.announce(MaplePacketCreator.getMiniRoomError(10));
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.tryacquireClient()) {    // thanks GabrielSin for pointing dupes within player interactions
            c.announce(MaplePacketCreator.enableActions());
            return;
        }

        try {
            byte mode = slea.readByte();
            final MapleCharacter chr = c.getPlayer();

            if (mode == Action.CREATE.getCode()) {
                if (!chr.isAlive()) {    // thanks GabrielSin for pointing this
                    chr.announce(MaplePacketCreator.getMiniRoomError(4));
                    return;
                }

                byte createType = slea.readByte();
                if (createType == 3) {  // trade
                    MapleTrade.startTrade(chr);
                } else if (createType == 1) { // omok mini game
                    int status = establishMiniroomStatus(chr, true);
                    if (status > 0) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(status));
                        return;
                    }

                    String desc = slea.readMapleAsciiString();
                    String pw;

                    if (slea.readByte() != 0) {
                        pw = slea.readMapleAsciiString();
                    } else {
                        pw = "";
                    }

                    int type = slea.readByte();
                    if (type > 11) {
                        type = 11;
                    } else if (type < 0) {
                        type = 0;
                    }
                    if (!chr.haveItem(4080000 + type)) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(6));
                        return;
                    }

                    MapleMiniGame game = new MapleMiniGame(chr, desc, pw);
                    chr.setMiniGame(game);
                    game.setPieceType(type);
                    game.setGameType(MiniGameType.OMOK);
                    chr.getMap().addMapObject(game);
                    chr.getMap().broadcastMessage(MaplePacketCreator.addOmokBox(chr, 1, 0));
                    game.sendOmok(c, type);
                } else if (createType == 2) { // matchcard
                    int status = establishMiniroomStatus(chr, true);
                    if (status > 0) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(status));
                        return;
                    }

                    String desc = slea.readMapleAsciiString();
                    String pw;

                    if (slea.readByte() != 0) {
                        pw = slea.readMapleAsciiString();
                    } else {
                        pw = "";
                    }

                    int type = slea.readByte();
                    if (type > 2) {
                        type = 2;
                    } else if (type < 0) {
                        type = 0;
                    }
                    if (!chr.haveItem(4080100)) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(6));
                        return;
                    }

                    MapleMiniGame game = new MapleMiniGame(chr, desc, pw);
                    game.setPieceType(type);
                    if (type == 0) {
                        game.setMatchesToWin(6);
                    } else if (type == 1) {
                        game.setMatchesToWin(10);
                    } else if (type == 2) {
                        game.setMatchesToWin(15);
                    }
                    game.setGameType(MiniGameType.MATCH_CARD);
                    chr.setMiniGame(game);
                    chr.getMap().addMapObject(game);
                    chr.getMap().broadcastMessage(MaplePacketCreator.addMatchCardBox(chr, 1, 0));
                    game.sendMatchCard(c, type);
                } else if (createType == 4 || createType == 5) { // shop
                    if (!GameConstants.isFreeMarketRoom(chr.getMapId())) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(15));
                        return;
                    }

                    int status = establishMiniroomStatus(chr, false);
                    if (status > 0) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(status));
                        return;
                    }

                    if (!canPlaceStore(chr)) {
                        return;
                    }

                    String desc = slea.readMapleAsciiString();
                    slea.skip(3);
                    int itemId = slea.readInt();
                    if (chr.getInventory(MapleInventoryType.CASH).countById(itemId) < 1) {
                        chr.announce(MaplePacketCreator.getMiniRoomError(6));
                        return;
                    }

                    if (ItemConstants.isPlayerShop(itemId)) {
                        MaplePlayerShop shop = new MaplePlayerShop(chr, desc, itemId);
                        chr.setPlayerShop(shop);
                        chr.getMap().addMapObject(shop);
                        shop.sendShop(c);
                        c.getWorldServer().registerPlayerShop(shop);
                        //c.announce(MaplePacketCreator.getPlayerShopRemoveVisitor(1));
                    } else if (ItemConstants.isHiredMerchant(itemId)) {
                        MapleHiredMerchant merchant = new MapleHiredMerchant(chr, desc, itemId);
                        chr.setHiredMerchant(merchant);
                        c.getWorldServer().registerHiredMerchant(merchant);
                        chr.getClient().getChannelServer().addHiredMerchant(chr.getId(), merchant);
                        chr.announce(MaplePacketCreator.getHiredMerchant(chr, merchant, true));
                    }
                }
            } else if (mode == Action.INVITE.getCode()) {
                int otherCid = slea.readInt();
                Optional<MapleCharacter> other = chr.getMap().getCharacterById(otherCid);
                if (other.isEmpty() || chr.getId() == other.get().getId()) {
                    return;
                }

                MapleTrade.inviteTrade(chr, other.get());
            } else if (mode == Action.DECLINE.getCode()) {
                MapleTrade.declineTrade(chr);
            } else if (mode == Action.VISIT.getCode()) {
                if (chr.getTrade() != null && chr.getTrade().getPartner() != null) {
                    if (!chr.getTrade().isFullTrade() && !chr.getTrade().getPartner().isFullTrade()) {
                        MapleTrade.visitTrade(chr, chr.getTrade().getPartner().getChr());
                    } else {
                        chr.announce(MaplePacketCreator.getMiniRoomError(2));
                        return;
                    }
                } else {
                    if (isTradeOpen(chr)) {
                        return;
                    }

                    int oid = slea.readInt();
                    MapleMapObject ob = chr.getMap().getMapObject(oid).orElse(null);
                    if (ob instanceof MaplePlayerShop) {
                        MaplePlayerShop shop = (MaplePlayerShop) ob;
                        shop.visitShop(chr);
                    } else if (ob instanceof MapleMiniGame) {
                        slea.skip(1);
                        String pw = slea.available() > 1 ? slea.readMapleAsciiString() : "";

                        MapleMiniGame game = (MapleMiniGame) ob;
                        if (game.checkPassword(pw)) {
                            if (game.hasFreeSlot() && !game.isVisitor(chr)) {
                                game.addVisitor(chr);
                                chr.setMiniGame(game);
                                switch (game.getGameType()) {
                                    case OMOK:
                                        game.sendOmok(c, game.getPieceType());
                                        break;
                                    case MATCH_CARD:
                                        game.sendMatchCard(c, game.getPieceType());
                                        break;
                                }
                            } else {
                                chr.announce(MaplePacketCreator.getMiniRoomError(2));
                            }
                        } else {
                            chr.announce(MaplePacketCreator.getMiniRoomError(22));
                        }
                    } else if (ob instanceof MapleHiredMerchant && chr.getHiredMerchant() == null) {
                        MapleHiredMerchant merchant = (MapleHiredMerchant) ob;
                        merchant.visitShop(chr);
                    }
                }
            } else if (mode == Action.CHAT.getCode()) { // chat lol
                String message = slea.readMapleAsciiString();
                chat(chr, message);
            } else if (mode == Action.EXIT.getCode()) {
                if (chr.getTrade() != null) {
                    MapleTrade.cancelTrade(chr, MapleTrade.TradeResult.PARTNER_CANCEL);
                } else {
                    chr.closePlayerShop();
                    chr.closeMiniGame(false);
                    chr.closeHiredMerchant(true);
                }
            } else if (mode == Action.OPEN_STORE.getCode() || mode == Action.OPEN_CASH.getCode()) {
                openStore(slea, chr, mode);
            } else if (mode == Action.READY.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                game.broadcast(MaplePacketCreator.getMiniGameReady(game));
            } else if (mode == Action.UN_READY.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                game.broadcast(MaplePacketCreator.getMiniGameUnReady(game));
            } else if (mode == Action.START.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                if (game.getGameType().equals(MiniGameType.OMOK)) {
                    game.minigameMatchStarted();
                    game.broadcast(MaplePacketCreator.getMiniGameStart(game, game.getLoser()));
                    chr.getMap().broadcastMessage(MaplePacketCreator.addOmokBox(game.getOwner(), 2, 1));
                } else if (game.getGameType().equals(MiniGameType.MATCH_CARD)) {
                    game.minigameMatchStarted();
                    game.shuffleList();
                    game.broadcast(MaplePacketCreator.getMatchCardStart(game, game.getLoser()));
                    chr.getMap().broadcastMessage(MaplePacketCreator.addMatchCardBox(game.getOwner(), 2, 1));
                }
            } else if (mode == Action.GIVE_UP.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                if (game.getGameType().equals(MiniGameType.OMOK)) {
                    if (game.isOwner(chr)) {
                        game.minigameMatchVisitorWins(true);
                    } else {
                        game.minigameMatchOwnerWins(true);
                    }
                } else if (game.getGameType().equals(MiniGameType.MATCH_CARD)) {
                    if (game.isOwner(chr)) {
                        game.minigameMatchVisitorWins(true);
                    } else {
                        game.minigameMatchOwnerWins(true);
                    }
                }
            } else if (mode == Action.REQUEST_TIE.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                if (!game.isTieDenied(chr)) {
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitor(MaplePacketCreator.getMiniGameRequestTie(game));
                    } else {
                        game.broadcastToOwner(MaplePacketCreator.getMiniGameRequestTie(game));
                    }
                }
            } else if (mode == Action.ANSWER_TIE.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                if (slea.readByte() != 0) {
                    game.minigameMatchDraw();
                } else {
                    game.denyTie(chr);

                    if (game.isOwner(chr)) {
                        game.broadcastToVisitor(MaplePacketCreator.getMiniGameDenyTie(game));
                    } else {
                        game.broadcastToOwner(MaplePacketCreator.getMiniGameDenyTie(game));
                    }
                }
            } else if (mode == Action.SKIP.getCode()) {
                MapleMiniGame game = chr.getMiniGame();
                if (game.isOwner(chr)) {
                    game.broadcast(MaplePacketCreator.getMiniGameSkipOwner(game));
                } else {
                    game.broadcast(MaplePacketCreator.getMiniGameSkipVisitor(game));
                }
            } else if (mode == Action.MOVE_OMOK.getCode()) {
                int x = slea.readInt(); // x point
                int y = slea.readInt(); // y point
                int type = slea.readByte(); // piece ( 1 or 2; Owner has one piece, visitor has another, it switches every game.)
                chr.getMiniGame().setPiece(x, y, type, chr);
            } else if (mode == Action.SELECT_CARD.getCode()) {
                int turn = slea.readByte(); // 1st turn = 1; 2nd turn = 0
                int slot = slea.readByte(); // slot
                MapleMiniGame game = chr.getMiniGame();
                int firstslot = game.getFirstSlot();
                if (turn == 1) {
                    game.setFirstSlot(slot);
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitor(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, turn));
                    } else {
                        game.getOwner().announce(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, turn));
                    }
                } else if ((game.getCardId(firstslot)) == (game.getCardId(slot))) {
                    if (game.isOwner(chr)) {
                        game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, 2));
                        game.setOwnerPoints();
                    } else {
                        game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, 3));
                        game.setVisitorPoints();
                    }
                } else if (game.isOwner(chr)) {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, 0));
                } else {
                    game.broadcast(MaplePacketCreator.getMatchCardSelect(game, turn, slot, firstslot, 1));
                }
            } else if (mode == Action.SET_MESO.getCode()) {
                chr.getTrade().setMeso(slea.readInt());
            } else if (mode == Action.SET_ITEMS.getCode()) {
                ItemInformationProvider ii = ItemInformationProvider.getInstance();
                Optional<MapleInventoryType> ivType = MapleInventoryType.getByType(slea.readByte());
                if (ivType.isEmpty()) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                short pos = slea.readShort();
                Item item = chr.getInventory(ivType.get()).getItem(pos);
                short quantity = slea.readShort();
                byte targetSlot = slea.readByte();

                if (targetSlot < 1 || targetSlot > 9) {
                    System.out.println("[Hack] " + chr.getName() + " Trying to dupe on trade slot.");
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (item == null) {
                    c.announce(MaplePacketCreator.serverNotice(1, "Invalid item description."));
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (ii.isUnmerchable(item.getItemId())) {
                    if (ItemConstants.isPet(item.getItemId())) {
                        c.announce(MaplePacketCreator.serverNotice(1, "Pets are not allowed to be traded."));
                    } else {
                        c.announce(MaplePacketCreator.serverNotice(1, "Cash items are not allowed to be traded."));
                    }

                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (quantity < 1 || quantity > item.getQuantity()) {
                    c.announce(MaplePacketCreator.serverNotice(1, "You don't have enough quantity of the item."));
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }

                MapleTrade trade = chr.getTrade();
                if (trade != null) {
                    if ((quantity <= item.getQuantity() && quantity >= 0) || ItemConstants.isRechargeable(item.getItemId())) {
                        if (ii.isDropRestricted(item.getItemId())) { // ensure that undroppable items do not make it to the trade window
                            if (!MapleKarmaManipulator.hasKarmaFlag(item)) {
                                c.announce(MaplePacketCreator.serverNotice(1, "That item is untradeable."));
                                c.announce(MaplePacketCreator.enableActions());
                                return;
                            }
                        }

                        MapleInventory inv = chr.getInventory(ivType.get());
                        inv.lockInventory();
                        try {
                            Item checkItem = chr.getInventory(ivType.get()).getItem(pos);
                            if (checkItem != item || checkItem.getPosition() != item.getPosition()) {
                                c.announce(MaplePacketCreator.serverNotice(1, "Invalid item description."));
                                c.announce(MaplePacketCreator.enableActions());
                                return;
                            }

                            Item tradeItem = item.copy();
                            if (ItemConstants.isRechargeable(item.getItemId())) {
                                quantity = item.getQuantity();
                            }

                            tradeItem.setQuantity(quantity);
                            tradeItem.setPosition(targetSlot);

                            if (trade.addItem(tradeItem)) {
                                MapleInventoryManipulator.removeFromSlot(c, ivType.get(), item.getPosition(), quantity, true);

                                trade.getChr().announce(MaplePacketCreator.getTradeItemAdd((byte) 0, tradeItem));
                                if (trade.getPartner() != null) {
                                    trade.getPartner().getChr().announce(MaplePacketCreator.getTradeItemAdd((byte) 1, tradeItem));
                                }
                            }
                        } catch (Exception e) {
                            FilePrinter.printError(FilePrinter.TRADE_EXCEPTION, e, "Player '" + chr + "' tried to add " + ii.getName(item.getItemId()) + " qty. " + item.getQuantity() + " in trade (slot " + targetSlot + ") then exception occurred.");
                        } finally {
                            inv.unlockInventory();
                        }
                    }
                }
            } else if (mode == Action.CONFIRM.getCode()) {
                MapleTrade.completeTrade(chr);
            } else if (mode == Action.ADD_ITEM.getCode() || mode == Action.PUT_ITEM.getCode()) {
                byte inventoryType = slea.readByte();
                short slot = slea.readShort();
                short bundles = slea.readShort();
                short perBundle = slea.readShort();
                int price = slea.readInt();
                addItem(chr, inventoryType, slot, bundles, perBundle, price);
            } else if (mode == Action.REMOVE_ITEM.getCode()) {
                if (isTradeOpen(chr)) {
                    return;
                }

                MaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop.isOwner(chr)) {
                    if (shop.isOpen()) {
                        c.announce(MaplePacketCreator.serverNotice(1, "You can't take it with the store open."));
                        return;
                    }

                    int slot = slea.readShort();
                    if (slot >= shop.getItems().size() || slot < 0) {
                        AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to packet edit with a player shop.");
                        FilePrinter.printError(FilePrinter.EXPLOITS + chr.getName() + ".txt", chr.getName() + " tried to remove item at slot " + slot);
                        c.disconnect(true, false);
                        return;
                    }

                    shop.takeItemBack(slot, chr);
                }
            } else if (mode == Action.MERCHANT_MESO.getCode()) {
                withdrawMeso(chr);
            } else if (mode == Action.MERCHANT_ORGANIZE.getCode()) {
                merchantOrganize(chr);
            } else if (mode == Action.BUY.getCode() || mode == Action.MERCHANT_BUY.getCode()) {
                int itemId = slea.readByte();
                short quantity = slea.readShort();
                buy(c, chr, quantity, itemId);
            } else if (mode == Action.TAKE_ITEM_BACK.getCode()) {
                int slot = slea.readShort();
                takeItemBack(chr, slot);
            } else if (mode == Action.CLOSE_MERCHANT.getCode()) {
                closeMerchant(chr);
            } else if (mode == Action.MAINTENANCE_OFF.getCode()) {
                maintenanceOff(chr);
            } else if (mode == Action.BAN_PLAYER.getCode()) {
                slea.skip(1);
                String playerName = slea.readMapleAsciiString();
                banPlayer(chr, playerName);
            } else if (mode == Action.EXPEL.getCode()) {
                expel(chr);
            } else if (mode == Action.EXIT_AFTER_GAME.getCode()) {
                exitAfterGame(chr, true);
            } else if (mode == Action.CANCEL_EXIT_AFTER_GAME.getCode()) {
                exitAfterGame(chr, false);
            }
        } finally {
            c.releaseClient();
        }
    }

    private static void addItem(MapleCharacter chr, byte inventoryType, short slot, short bundles, short perBundle, int price) {
        if (isTradeOpen(chr)) {
            return;
        }

        Optional<MapleInventoryType> ivType = MapleInventoryType.getByType(inventoryType);
        if (ivType.isEmpty()) {
            chr.announce(MaplePacketCreator.enableActions());
            return;
        }

        Item ivItem = chr.getInventory(ivType.get()).getItem(slot);

        if (ivItem == null || ivItem.isUntradeable()) {
            chr.announce(MaplePacketCreator.serverNotice(1, "Could not perform shop operation with that item."));
            chr.announce(MaplePacketCreator.enableActions());
            return;
        }

        if (ItemInformationProvider.getInstance().isUnmerchable(ivItem.getItemId())) {
            if (ItemConstants.isPet(ivItem.getItemId())) {
                chr.announce(MaplePacketCreator.serverNotice(1, "Pets are not allowed to be sold on the Player Store."));
            } else {
                chr.announce(MaplePacketCreator.serverNotice(1, "Cash items are not allowed to be sold on the Player Store."));
            }

            chr.announce(MaplePacketCreator.enableActions());
            return;
        }


        if (ItemConstants.isRechargeable(ivItem.getItemId())) {
            perBundle = 1;
            bundles = 1;
        } else if (ivItem.getQuantity() < (bundles * perBundle)) {     // thanks GabrielSin for finding a dupe here
            chr.announce(MaplePacketCreator.serverNotice(1, "Could not perform shop operation with that item."));
            chr.announce(MaplePacketCreator.enableActions());
            return;
        }

        if (perBundle <= 0 || perBundle * bundles > 2000 || bundles <= 0 || price <= 0 || price > Integer.MAX_VALUE) {
            AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to packet edit with hired merchants.");
            FilePrinter.printError(FilePrinter.EXPLOITS + chr.getName() + ".txt", chr.getName() + " might of possibly packet edited Hired Merchants\nperBundle: " + perBundle + "\nperBundle * bundles (This multiplied cannot be greater than 2000): " + perBundle * bundles + "\nbundles: " + bundles + "\nprice: " + price);
            return;
        }

        Item sellItem = ivItem.copy();
        if (!ItemConstants.isRechargeable(ivItem.getItemId())) {
            sellItem.setQuantity(perBundle);
        }

        MaplePlayerShopItem shopItem = new MaplePlayerShopItem(sellItem, bundles, price);
        MaplePlayerShop shop = chr.getPlayerShop();
        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();

        if ((shop == null && merchant.isEmpty()) || (shop != null && !shop.isOwner(chr)) || (merchant.isPresent() && !merchant.get().isOwner(chr))) {
            chr.announce(MaplePacketCreator.serverNotice(1, "You can't sell without owning a shop."));
            return;
        }


        if (shop != null && shop.isOwner(chr)) {
            if (shop.isOpen() || !shop.addItem(shopItem)) {
                chr.announce(MaplePacketCreator.serverNotice(1, "You can't sell it anymore."));
                return;
            }

            if (ItemConstants.isRechargeable(ivItem.getItemId())) {
                MapleInventoryManipulator.removeFromSlot(chr.getClient(), ivType.get(), slot, ivItem.getQuantity(), true);
            } else {
                MapleInventoryManipulator.removeFromSlot(chr.getClient(), ivType.get(), slot, (short) (bundles * perBundle), true);
            }

            chr.announce(MaplePacketCreator.getPlayerShopItemUpdate(shop));
            return;
        }

        if (merchant.isPresent() && merchant.get().isOwner(chr)) {
            if (ivType.get().equals(MapleInventoryType.CASH) && merchant.get().isPublished()) {
                chr.announce(MaplePacketCreator.serverNotice(1, "Cash items are only allowed to be sold when first opening the store."));
                return;
            }

            if (merchant.get().isOpen() || !merchant.get().addItem(shopItem)) { // thanks Vcoc for pointing an exploit with unlimited shop slots
                chr.announce(MaplePacketCreator.serverNotice(1, "You can't sell it anymore."));
                return;
            }

            if (ItemConstants.isRechargeable(ivItem.getItemId())) {
                MapleInventoryManipulator.removeFromSlot(chr.getClient(), ivType.get(), slot, ivItem.getQuantity(), true);
            } else {
                MapleInventoryManipulator.removeFromSlot(chr.getClient(), ivType.get(), slot, (short) (bundles * perBundle), true);
            }

            chr.announce(MaplePacketCreator.updateHiredMerchant(merchant.get(), chr));

            if (YamlConfig.config.server.USE_ENFORCE_MERCHANT_SAVE) {
                chr.saveCharToDB(false);
            }

            try {
                merchant.get().saveItems(false);   // thanks Masterrulax for realizing yet another dupe with merchants/Fredrick
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void chat(MapleCharacter chr, String message) {
        if (chr.getTrade() != null) {
            chr.getTrade().chat(message);
            return;
        }

        if (chr.getPlayerShop() != null) {
            MaplePlayerShop shop = chr.getPlayerShop();
            if (shop != null) {
                shop.chat(chr.getClient(), message);
                return;
            }
        }

        if (chr.getMiniGame() != null) {
            MapleMiniGame game = chr.getMiniGame();
            if (game != null) {
                game.chat(chr.getClient(), message);
                return;
            }
        }

        chr.getHiredMerchant().ifPresent(m -> m.sendMessage(chr, message));
    }

    private static void openStore(SeekableLittleEndianAccessor slea, MapleCharacter chr, byte mode) {
        if (isTradeOpen(chr)) {
            return;
        }

        if (mode == Action.OPEN_STORE.getCode()) {
            slea.readByte();    //01
        } else {
            slea.readShort();
            int birthday = slea.readInt();
            if (!CashOperationHandler.checkBirthday(chr.getClient(), birthday)) { // birthday check here found thanks to lucasziron
                chr.announce(MaplePacketCreator.serverNotice(1, "Please check again the birthday date."));
                return;
            }

            chr.announce(MaplePacketCreator.hiredMerchantOwnerMaintenanceLeave());
        }

        if (!canPlaceStore(chr)) {    // thanks Ari for noticing player shops overlapping on opening time
            return;
        }

        MaplePlayerShop shop = chr.getPlayerShop();
        if (shop != null && shop.isOwner(chr)) {
            if (YamlConfig.config.server.USE_ERASE_PERMIT_ON_OPENSHOP) {
                try {
                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.CASH, shop.getItemId(), 1, true, false);
                } catch (RuntimeException re) {
                } // fella does not have a player shop permit...
            }

            chr.getMap().broadcastMessage(MaplePacketCreator.updatePlayerShopBox(shop));
            shop.setOpen(true);
            return;
        }

        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();
        if (merchant.isPresent() && merchant.get().isOwner(chr)) {
            chr.setHasMerchant(true);
            merchant.get().setOpen(true);
            chr.getMap().addMapObject(merchant.get());
            chr.setHiredMerchant(null);
            chr.getMap().broadcastMessage(MaplePacketCreator.spawnHiredMerchantBox(merchant.get()));
        }
    }

    private static void withdrawMeso(MapleCharacter chr) {
        chr.getHiredMerchant().ifPresent(m -> m.withdrawMesos(chr));
    }

    private static void merchantOrganize(MapleCharacter chr) {
        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();
        if (merchant.isEmpty() || !merchant.get().isOwner(chr)) {
            return;
        }

        merchant.get().withdrawMesos(chr);
        merchant.get().clearInexistentItems();

        if (merchant.get().getItems().isEmpty()) {
            merchant.get().closeOwnerMerchant(chr);
            return;
        }
        chr.announce(MaplePacketCreator.updateHiredMerchant(merchant.get(), chr));
    }

    private static void buy(MapleClient c, MapleCharacter chr, short quantity, int itemId) {
        if (isTradeOpen(chr)) {
            return;
        }

        if (quantity < 1) {
            AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to packet edit with a hired merchant and or player shop.");
            FilePrinter.printError(FilePrinter.EXPLOITS + chr.getName() + ".txt", chr.getName() + " tried to buy item " + itemId + " with quantity " + quantity);
            c.disconnect(true, false);
            return;
        }

        MaplePlayerShop shop = chr.getPlayerShop();
        if (shop != null && shop.isVisitor(chr)) {
            if (shop.buy(c, itemId, quantity)) {
                shop.broadcast(MaplePacketCreator.getPlayerShopItemUpdate(shop));
                return;
            }
        }

        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();
        if (merchant.isPresent() && !merchant.get().isOwner(chr)) {
            merchant.get().buy(c, itemId, quantity);
            merchant.get().broadcastToVisitorsThreadsafe(MaplePacketCreator.updateHiredMerchant(merchant.get(), chr));
        }
    }

    private static void takeItemBack(MapleCharacter chr, int slot) {
        if (isTradeOpen(chr)) {
            return;
        }

        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();
        if (merchant.isPresent() && merchant.get().isOwner(chr)) {
            if (merchant.get().isOpen()) {
                chr.announce(MaplePacketCreator.serverNotice(1, "You can't take it with the store open."));
                return;
            }

            if (slot >= merchant.get().getItems().size() || slot < 0) {
                AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to packet edit with a hired merchant.");
                FilePrinter.printError(FilePrinter.EXPLOITS + chr.getName() + ".txt", chr.getName() + " tried to remove item at slot " + slot);
                chr.getClient().disconnect(true, false);
                return;
            }

            merchant.get().takeItemBack(slot, chr);
        }
    }

    private static void closeMerchant(MapleCharacter chr) {
        if (isTradeOpen(chr)) {
            return;
        }
        chr.getHiredMerchant().ifPresent(m -> m.closeOwnerMerchant(chr));
    }

    private static void maintenanceOff(MapleCharacter chr) {
        if (isTradeOpen(chr)) {
            return;
        }

        Optional<MapleHiredMerchant> merchant = chr.getHiredMerchant();
        if (merchant.isPresent()) {
            if (merchant.get().isOwner(chr)) {
                if (merchant.get().getItems().isEmpty()) {
                    merchant.get().closeOwnerMerchant(chr);
                } else {
                    merchant.get().clearMessages();
                    merchant.get().setOpen(true);
                    merchant.get().getMap().broadcastMessage(MaplePacketCreator.updateHiredMerchantBox(merchant.get()));
                }
            }
        }

        chr.setHiredMerchant(null);
        chr.announce(MaplePacketCreator.enableActions());
    }

    private static void banPlayer(MapleCharacter chr, String playerName) {
        MaplePlayerShop shop = chr.getPlayerShop();
        if (shop != null && shop.isOwner(chr)) {
            shop.banPlayer(playerName);
        }
    }

    private static void expel(MapleCharacter chr) {
        MapleMiniGame miniGame = chr.getMiniGame();
        if (miniGame != null && miniGame.isOwner(chr)) {
            miniGame.getVisitor().ifPresent(PlayerInteractionHandler::closeForVisitor);
        }
    }

    private static void exitAfterGame(MapleCharacter chr, boolean quit) {
        MapleMiniGame miniGame = chr.getMiniGame();
        if (miniGame != null) {
            miniGame.setQuitAfterGame(chr, quit);
        }
    }

    private static void closeForVisitor(MapleCharacter visitor) {
        visitor.closeMiniGame(false);
        visitor.announce(MaplePacketCreator.getMiniGameClose(true, 5));
    }

    public enum Action {
        CREATE(0),
        INVITE(2),
        DECLINE(3),
        VISIT(4),
        ROOM(5),
        CHAT(6),
        CHAT_THING(8),
        EXIT(0xA),
        OPEN_STORE(0xB),
        OPEN_CASH(0xE),
        SET_ITEMS(0xF),
        SET_MESO(0x10),
        CONFIRM(0x11),
        TRANSACTION(0x14),
        ADD_ITEM(0x16),
        BUY(0x17),
        UPDATE_MERCHANT(0x19),
        UPDATE_PLAYERSHOP(0x1A),
        REMOVE_ITEM(0x1B),
        BAN_PLAYER(0x1C),
        MERCHANT_THING(0x1D),
        OPEN_THING(0x1E),
        PUT_ITEM(0x21),
        MERCHANT_BUY(0x22),
        TAKE_ITEM_BACK(0x26),
        MAINTENANCE_OFF(0x27),
        MERCHANT_ORGANIZE(0x28),
        CLOSE_MERCHANT(0x29),
        REAL_CLOSE_MERCHANT(0x2A),
        MERCHANT_MESO(0x2B),
        SOMETHING(0x2D),
        VIEW_VISITORS(0x2E),
        BLACKLIST(0x2F),
        REQUEST_TIE(0x32),
        ANSWER_TIE(0x33),
        GIVE_UP(0x34),
        EXIT_AFTER_GAME(0x38),
        CANCEL_EXIT_AFTER_GAME(0x39),
        READY(0x3A),
        UN_READY(0x3B),
        EXPEL(0x3C),
        START(0x3D),
        GET_RESULT(0x3E),
        SKIP(0x3F),
        MOVE_OMOK(0x40),
        SELECT_CARD(0x44);
        final byte code;

        Action(int code) {
            this.code = (byte) code;
        }

        public byte getCode() {
            return code;
        }
    }
}
