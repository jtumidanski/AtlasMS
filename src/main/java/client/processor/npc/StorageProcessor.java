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
package client.processor.npc;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.inventory.manipulator.MapleKarmaManipulator;
import config.YamlConfig;
import connection.packets.CTrunkDlg;
import connection.packets.CWvsContext;
import constants.inventory.ItemConstants;
import server.ItemInformationProvider;
import server.MapleStorage;
import tools.FilePrinter;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

/**
 * @author Matze
 * @author Ronan - inventory concurrency protection on storing items
 */
public class StorageProcessor {

    public static void storageAction(SeekableLittleEndianAccessor slea, MapleClient c) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        MapleCharacter chr = c.getPlayer();
        MapleStorage storage = chr.getStorage();
        byte mode = slea.readByte();

        if (chr.getLevel() < 15) {
            chr.dropMessage(1, "You may only use the storage once you have reached level 15.");
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (c.tryacquireClient()) {
            try {
                if (mode == 4) { // take out
                    byte typeIndex = slea.readByte();
                    Optional<MapleInventoryType> type = MapleInventoryType.getByType(typeIndex);
                    if (type.isEmpty()) {
                        c.disconnect(true, false);
                        return;
                    }

                    byte slot = slea.readByte();
                    if (slot < 0 || slot > storage.getSlots()) { // removal starts at zero
                        AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit with storage.");
                        FilePrinter.print(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to work with storage slot " + slot);
                        c.disconnect(true, false);
                        return;
                    }
                    slot = storage.getSlot(type.get(), slot);
                    Item item = storage.getItem(slot);
                    if (item != null) {
                        if (ii.isPickupRestricted(item.getItemId()) && chr.haveItemWithId(item.getItemId(), true)) {
                            c.announce(CTrunkDlg.getStorageError((byte) 0x0C));
                            return;
                        }

                        int takeoutFee = storage.getTakeOutFee();
                        if (chr.getMeso() < takeoutFee) {
                            c.announce(CTrunkDlg.getStorageError((byte) 0x0B));
                            return;
                        } else {
                            chr.gainMeso(-takeoutFee, false);
                        }

                        if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                            if (storage.takeOut(item)) {
                                chr.setUsedStorage();

                                MapleKarmaManipulator.toggleKarmaFlagToUntradeable(item);
                                MapleInventoryManipulator.addFromDrop(c, item, false);

                                String itemName = ii.getName(item.getItemId());
                                FilePrinter.print(FilePrinter.STORAGE + c.getAccountName() + ".txt", c.getPlayer().getName() + " took out " + item.getQuantity() + " " + itemName + " (" + item.getItemId() + ")");

                                storage.sendTakenOut(c, item.getInventoryType());
                            } else {
                                c.announce(CWvsContext.enableActions());
                                return;
                            }
                        } else {
                            c.announce(CTrunkDlg.getStorageError((byte) 0x0A));
                        }
                    }
                } else if (mode == 5) { // store
                    short slot = slea.readShort();
                    int itemId = slea.readInt();
                    short quantity = slea.readShort();
                    MapleInventoryType invType = ItemConstants.getInventoryType(itemId);
                    MapleInventory inv = chr.getInventory(invType);
                    if (slot < 1 || slot > inv.getSlotLimit()) { //player inv starts at one
                        AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit with storage.");
                        FilePrinter.print(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to store item at slot " + slot);
                        c.disconnect(true, false);
                        return;
                    }
                    if (quantity < 1) {
                        c.announce(CWvsContext.enableActions());
                        return;
                    }
                    if (storage.isFull()) {
                        c.announce(CTrunkDlg.getStorageError((byte) 0x11));
                        return;
                    }

                    int storeFee = storage.getStoreFee();
                    if (chr.getMeso() < storeFee) {
                        c.announce(CTrunkDlg.getStorageError((byte) 0x0B));
                    } else {
                        Item item;

                        inv.lockInventory();    // thanks imbee for pointing a dupe within storage
                        try {
                            item = inv.getItem(slot);
                            if (item != null && item.getItemId() == itemId && (item.getQuantity() >= quantity || ItemConstants.isRechargeable(itemId))) {
                                if (ItemConstants.isWeddingRing(itemId) || ItemConstants.isWeddingToken(itemId)) {
                                    c.announce(CWvsContext.enableActions());
                                    return;
                                }

                                if (ItemConstants.isRechargeable(itemId)) {
                                    quantity = item.getQuantity();
                                }

                                MapleInventoryManipulator.removeFromSlot(c, invType, slot, quantity, false);
                            } else {
                                c.announce(CWvsContext.enableActions());
                                return;
                            }

                            item = item.copy();     // thanks Robin Schulz & BHB88 for noticing a inventory glitch when storing items
                        } finally {
                            inv.unlockInventory();
                        }

                        chr.gainMeso(-storeFee, false, true, false);

                        MapleKarmaManipulator.toggleKarmaFlagToUntradeable(item);
                        item.setQuantity(quantity);

                        storage.store(item);    // inside a critical section, "!(storage.isFull())" is still in effect...
                        chr.setUsedStorage();

                        String itemName = ii.getName(item.getItemId());
                        FilePrinter.print(FilePrinter.STORAGE + c.getAccountName() + ".txt", c.getPlayer().getName() + " stored " + item.getQuantity() + " " + itemName + " (" + item.getItemId() + ")");

                        storage.sendStored(c, ItemConstants.getInventoryType(itemId));
                    }
                } else if (mode == 6) { // arrange items
                    if (YamlConfig.config.server.USE_STORAGE_ITEM_SORT) {
                        storage.arrangeItems(c);
                    }
                    c.announce(CWvsContext.enableActions());
                } else if (mode == 7) { // meso
                    int meso = slea.readInt();
                    int storageMesos = storage.getMeso();
                    int playerMesos = chr.getMeso();
                    if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                        if (meso < 0 && (storageMesos - meso) < 0) {
                            meso = Integer.MIN_VALUE + storageMesos;
                            if (meso < playerMesos) {
                                c.announce(CWvsContext.enableActions());
                                return;
                            }
                        } else if (meso > 0 && (playerMesos + meso) < 0) {
                            meso = Integer.MAX_VALUE - playerMesos;
                            if (meso > storageMesos) {
                                c.announce(CWvsContext.enableActions());
                                return;
                            }
                        }
                        storage.setMeso(storageMesos - meso);
                        chr.gainMeso(meso, false, true, false);
                        chr.setUsedStorage();
                        FilePrinter.print(FilePrinter.STORAGE + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + (meso > 0 ? " took out " : " stored ") + Math.abs(meso) + " mesos");
                        storage.sendMeso(c);
                    } else {
                        c.announce(CWvsContext.enableActions());
                        return;
                    }
                } else if (mode == 8) {// close... unless the player decides to enter cash shop!
                    storage.close();
                }
            } finally {
                c.releaseClient();
            }
        }
    }
}
