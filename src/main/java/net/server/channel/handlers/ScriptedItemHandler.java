package net.server.channel.handlers;

import client.MapleClient;
import client.inventory.Item;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import scripting.item.ItemScriptManager;
import server.ItemInformationProvider;
import server.ScriptedItem;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class ScriptedItemHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // trash stamp, thanks RMZero213
        short itemSlot = slea.readShort(); // item slot, thanks RMZero213
        int itemId = slea.readInt();

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Optional<ScriptedItem> info = ii.getScriptedItemInfo(itemId);
        if (info.isEmpty()) {
            return;
        }

        Item item = c.getPlayer().getInventory(ItemConstants.getInventoryType(itemId)).getItem(itemSlot);
        if (item == null || item.getItemId() != itemId || item.getQuantity() < 1) {
            return;
        }

        ItemScriptManager.getInstance().runItemScript(c, info.get());
    }
}
