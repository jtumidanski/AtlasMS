package connection.packets;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import connection.constants.SendOpcode;
import constants.game.GameConstants;
import net.server.Server;
import net.server.world.World;
import server.CashShop;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

public class CCashShop {
    public static byte[] showCash(MapleCharacter mc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.QUERY_CASH_RESULT.getValue());

        mplew.writeInt(mc.getCashShop()
                .getCash(1));
        mplew.writeInt(mc.getCashShop()
                .getCash(2));
        mplew.writeInt(mc.getCashShop()
                .getCash(4));

        return mplew.getPacket();
    }

    public static byte[] showWorldTransferSuccess(Item item, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0xA9);
        addCashItemInformation(mplew, item, accountId);
        return mplew.getPacket();
    }

    public static byte[] showNameChangeSuccess(Item item, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0xA7);
        addCashItemInformation(mplew, item, accountId);
        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItems(int accountId, int maplePoints, int mesos, List<Item> cashItems, List<Pair<Integer, Integer>> items) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0x5E);
        mplew.write((byte) cashItems.size());
        for (Item item : cashItems) {
            addCashItemInformation(mplew, item, accountId);
        }
        mplew.writeInt(maplePoints);
        mplew.writeInt(items.size());
        for (Pair<Integer, Integer> itemPair : items) {
            int quantity = itemPair.getLeft();
            mplew.writeShort((short) quantity); //quantity (0 = 1 for cash items)
            mplew.writeShort(0x1F); //0 = ?, >=0x20 = ?, <0x20 = ? (does nothing?)
            mplew.writeInt(itemPair.getRight());
        }
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] showBoughtCashPackage(List<Item> cashPackage, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x8E);
        mplew.write(cashPackage.size());

        for (Item item : cashPackage) {
            addCashItemInformation(mplew, item, accountId);
        }

        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] showBoughtQuestItem(int itemId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x92);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.write(0x0B);
        mplew.write(0);
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] showWishList(MapleCharacter mc, boolean update) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        if (update) {
            mplew.write(0x5A);
        } else {
            mplew.write(0x54);
        }

        for (int sn : mc.getCashShop()
                .getWishList()) {
            mplew.writeInt(sn);
        }

        for (int i = mc.getCashShop()
                .getWishList()
                .size(); i < 10; i++) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] showBoughtCashItem(Item item, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x5C);
        addCashItemInformation(mplew, item, accountId);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCashRing(Item ring, String recipient, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0x8C);
        addCashItemInformation(mplew, ring, accountId);
        mplew.writeMapleAsciiString(recipient);
        mplew.writeInt(ring.getItemId());
        mplew.writeShort(1); //quantity
        return mplew.getPacket();
    }

    /*
     * 00 = Due to an unknown error, failed
     * A3 = Request timed out. Please try again.
     * A4 = Due to an unknown error, failed + warpout
     * A5 = You don't have enough cash.
     * A6 = long as shet msg
     * A7 = You have exceeded the allotted limit of price for gifts.
     * A8 = You cannot send a gift to your own account. Log in on the char and purchase
     * A9 = Please confirm whether the character's name is correct.
     * AA = Gender restriction!
     * AB = gift cannot be sent because recipient inv is full
     * AC = exceeded the number of cash items you can have
     * AD = check and see if the character name is wrong or there is gender restrictions
     * //Skipped a few
     * B0 = Wrong Coupon Code
     * B1 = Disconnect from CS because of 3 wrong coupon codes < lol
     * B2 = Expired Coupon
     * B3 = Coupon has been used already
     * B4 = Nexon internet cafes? lolfk
     * B8 = Due to gender restrictions, the coupon cannot be used.
     * BB = inv full
     * BC = long as shet "(not?) available to purchase by a use at the premium" msg
     * BD = invalid gift recipient
     * BE = invalid receiver name
     * BF = item unavailable to purchase at this hour
     * C0 = not enough items in stock, therefore not available
     * C1 = you have exceeded spending limit of NX
     * C2 = not enough mesos? Lol not even 1 mesos xD
     * C3 = cash shop unavailable during beta phase
     * C4 = check birthday code
     * C7 = only available to users buying cash item, whatever msg too long
     * C8 = already applied for this
     * CD = You have reached the daily purchase limit for the cash shop.
     * D0 = coupon account limit reached
     * D2 = coupon system currently unavailable
     * D3 = item can only be used 15 days after registration
     * D4 = not enough gift tokens
     * D6 = fresh people cannot gift items lul
     * D7 = bad people cannot gift items >:(
     * D8 = cannot gift due to limitations
     * D9 = cannot gift due to amount of gifted times
     * DA = cannot be gifted due to technical difficulties
     * DB = cannot transfer to char below level 20
     * DC = cannot transfer char to same world
     * DD = cannot transfer char to new server world
     * DE = cannot transfer char out of this world
     * DF = cannot transfer char due to no empty char slots
     * E0 = event or free test time ended
     * E6 = item cannot be purchased with MaplePoints
     * E7 = lol sorry for the inconvenience, eh?
     * E8 = cannot purchase by anyone under 7
     */
    public static byte[] showCashShopMessage(byte message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x61);
        mplew.write(message);

        return mplew.getPacket();
    }

    public static byte[] showCashInventory(MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x50);
        mplew.writeShort(c.getPlayer()
                .getCashShop()
                .getInventory()
                .size());

        for (Item item : c.getPlayer()
                .getCashShop()
                .getInventory()) {
            addCashItemInformation(mplew, item, c.getAccID());
        }

        mplew.writeShort(c.getPlayer()
                .getStorage()
                .getSlots());
        mplew.writeShort(c.getCharacterSlots());

        return mplew.getPacket();
    }

    public static byte[] showGifts(List<Pair<Item, String>> gifts) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x52);
        mplew.writeShort(gifts.size());

        for (Pair<Item, String> gift : gifts) {
            CShopDlg.addCashItemInformation(mplew, gift.getLeft(), 0, gift.getRight());
        }

        return mplew.getPacket();
    }

    public static byte[] showGiftSucceed(String to, CashShop.CashItem item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x63);
        mplew.writeMapleAsciiString(to);
        mplew.writeInt(item.getItemId());
        mplew.writeShort(item.getCount());
        mplew.writeInt(item.getPrice());

        return mplew.getPacket();
    }

    public static byte[] showBoughtInventorySlots(int type, short slots) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x65);
        mplew.write(type);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] showBoughtStorageSlots(short slots) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x67);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCharacterSlot(short slots) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(5);
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x69);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] takeFromCashInventory(Item item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x6D);
        mplew.writeShort(item.getPosition());
        CCommon.addItemInfo(mplew, item, true);

        return mplew.getPacket();
    }

    public static byte[] deleteCashItem(Item item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0x71);
        mplew.writeLong(item.getCashId());
        return mplew.getPacket();
    }

    public static byte[] refundCashItem(Item item, int maplePoints) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());
        mplew.write(0x8A);
        mplew.writeLong(item.getCashId());
        mplew.writeInt(maplePoints);
        return mplew.getPacket();
    }

    public static byte[] putIntoCashInventory(Item item, int accountId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_OPERATION.getValue());

        mplew.write(0x6F);
        addCashItemInformation(mplew, item, accountId);

        return mplew.getPacket();
    }

    public static byte[] sendNameTransferCheck(String availableName, boolean canUseName) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_CHECK_NAME_CHANGE.getValue());
        //Send provided name back to client to add to temporary cache of checked & accepted names
        mplew.writeMapleAsciiString(availableName);
        mplew.writeBool(!canUseName);
        return mplew.getPacket();
    }

    /*  0: no error, send rules
                1: name change already submitted
                2: name change within a month
                3: recently banned
                4: unknown error
            */
    public static byte[] sendNameTransferRules(int error) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_CHECK_NAME_CHANGE_POSSIBLE_RESULT.getValue());
        mplew.writeInt(0);
        mplew.write(error);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*  1: cannot find char info,
                2: cannot transfer under 20,
                3: cannot send banned,
                4: cannot send married,
                5: cannot send guild leader,
                6: cannot send if account already requested transfer,
                7: cannot transfer within 30days,
                8: must quit family,
                9: unknown error
            */
    public static byte[] sendWorldTransferRules(int error, MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_CHECK_TRANSFER_WORLD_POSSIBLE_RESULT.getValue());
        mplew.writeInt(0); //ignored
        mplew.write(error);
        mplew.writeInt(0);
        mplew.writeBool(error == 0); //0 = ?, otherwise list servers
        if (error == 0) {
            List<World> worlds = Server.getInstance()
                    .getWorlds();
            mplew.writeInt(worlds.size());
            for (World world : worlds) {
                mplew.writeMapleAsciiString(GameConstants.WORLD_NAMES[world.getId()]);
            }
        }
        return mplew.getPacket();
    }

    // Cash Shop Surprise packets found thanks to Arnah (Vertisy)
    public static byte[] onCashItemGachaponOpenFailed() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_CASH_ITEM_GACHAPON_RESULT.getValue());
        mplew.write(0xE4);
        return mplew.getPacket();
    }

    public static byte[] onCashGachaponOpenSuccess(int accountid, long sn, int remainingBoxes, Item item, int itemid, int nSelectedItemCount, boolean bJackpot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CASHSHOP_CASH_ITEM_GACHAPON_RESULT.getValue());
        mplew.write(0xE5);   // subopcode thanks to Ubaware
        mplew.writeLong(sn);// sn of the box used
        mplew.writeInt(remainingBoxes);
        addCashItemInformation(mplew, item, accountid);
        mplew.writeInt(itemid);// the itemid of the liSN?
        mplew.write(nSelectedItemCount);// the total count now? o.O
        mplew.writeBool(bJackpot);// "CashGachaponJackpot"
        return mplew.getPacket();
    }

    public static byte[] enableCSUse(MapleCharacter mc) {
        return showCash(mc);
    }

    public static void addCashItemInformation(final MaplePacketLittleEndianWriter mplew, Item item, int accountId) {
        CShopDlg.addCashItemInformation(mplew, item, accountId, null);
    }
}
