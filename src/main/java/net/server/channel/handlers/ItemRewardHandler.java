package net.server.channel.handlers;

import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.ItemInformationProvider;
import server.RewardItem;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

public final class ItemRewardHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();

        Item it = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (it == null || it.getItemId() != itemId || c.getPlayer().getInventory(MapleInventoryType.USE).countById(itemId) < 1) {
            return;
        }

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        Pair<Integer, List<RewardItem>> rewards = ii.getItemReward(itemId);
        for (RewardItem reward : rewards.getRight()) {
            if (!MapleInventoryManipulator.checkSpace(c, reward.itemId(), reward.quantity(), "")) {
                c.announce(MaplePacketCreator.getShowInventoryFull());
                break;
            }
            if (Randomizer.nextInt(rewards.getLeft()) < reward.probability()) {
                if (ItemConstants.getInventoryType(reward.itemId()) == MapleInventoryType.EQUIP) {
                    final Item item = ii.getEquipById(reward.itemId());
                    if (reward.period() != -1) {
                        item.setExpiration(currentServerTime() + ((long) reward.period() * 60 * 60 * 10));
                    }
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                } else {
                    MapleInventoryManipulator.addById(c, reward.itemId(), reward.quantity(), "", -1);
                }
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, false, false);
                if (reward.worldMessage() != null) {
                    String msg = reward.worldMessage();
                    msg = msg.replaceAll("/name", c.getPlayer().getName());
                    msg = msg.replaceAll("/item", ii.getName(reward.itemId()));
                    Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.serverNotice(6, msg));
                }
                break;
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }
}
