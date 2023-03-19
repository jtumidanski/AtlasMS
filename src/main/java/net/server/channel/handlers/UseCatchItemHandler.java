package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanManager;
import client.inventory.MapleInventoryType;
import client.inventory.manipulator.MapleInventoryManipulator;
import constants.MonsterId;
import constants.UseItemId;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.ItemInformationProvider;
import server.life.MapleMonster;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseCatchItemHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor accessor, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        AutobanManager abm = chr.getAutobanManager();
        accessor.readInt();
        abm.setTimestamp(5, Server.getInstance().getCurrentTimestamp(), 4);
        accessor.readShort();
        int itemId = accessor.readInt();
        int objectId = accessor.readInt();

        if (chr.getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId) <= 0) {
            return;
        }

        chr.getMap().getMonsterByOid(objectId).ifPresent(m -> handlePacket(c, itemId, m));
    }

    private static void handlePacket(MapleClient c, int itemId, MapleMonster mob) {
        switch (itemId) {
            case UseItemId.PHEROMONE_PERFUME -> usePheromonePerfume(c, itemId, mob);
            case UseItemId.POUCH -> usePouch(c, itemId, mob);
            case UseItemId.ELEMENT_ROCK -> useElementRock(c, itemId, mob);
            case UseItemId.CLIFFS_MAGIC_CANE -> useCliffsMagicCane(c, itemId, mob);
            case UseItemId.FIRST_TRANSPARENT_MARBLE -> useFirstTransparentMarble(c, itemId, mob);
            case UseItemId.SECOND_TRANSPARENT_MARBLE -> useSecondTransparentMarble(c, itemId, mob);
            case UseItemId.THIRD_TRANSPARENT_MARBLE -> thirdTransparentMarble(c, itemId, mob);
            case UseItemId.PURIFICATION_MARBLE -> usePurificationMarble(c, itemId, mob);
            case UseItemId.FISH_NET -> useFishNet(c, itemId, mob);
            default -> defaultCatchItem(c, itemId, mob);
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void defaultCatchItem(MapleClient c, int itemId, MapleMonster mob) {
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int itemGanho = ii.getCreateItem(itemId);
        int mobItem = ii.getMobItem(itemId);

        if (itemGanho == 0 || mobItem != mob.getId()) {
            return;
        }

        int timeCatch = ii.getUseDelay(itemId);
        int mobHp = ii.getMobHP(itemId);

        if (timeCatch == 0 || (c.getPlayer().getAutobanManager().getLastSpam(10) + timeCatch) >= currentServerTime()) {
            return;
        }

        if (mobHp != 0 && mob.getHp() < ((mob.getMaxHp() / 100) * mobHp)) {
            performCatch(c, mob, itemId, itemGanho);
        } else if (mob.getId() != MonsterId.P_JUNIOR) {
            if (mobHp != 0) {
                c.getPlayer().getAutobanManager().spam(10);
                c.announce(MaplePacketCreator.catchMessage(0));
            }
        } else {
            c.getPlayer().message("You cannot use the Fishing Net yet.");
        }
    }

    private static void useFishNet(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.P_JUNIOR) {
            return;
        }

        if ((c.getPlayer().getAutobanManager().getLastSpam(10) + 3000) >= currentServerTime()) {
            c.getPlayer().message("You cannot use the Fishing Net yet.");
            return;
        }

        c.getPlayer().getAutobanManager().spam(10);
        performCatch(c, mob, itemId, 2022323);
    }

    private static void usePurificationMarble(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.POISON_FLOWER) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.4)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 4001169);
    }

    private static void thirdTransparentMarble(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.MUSHMOM) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.3)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 2109003);
    }

    private static void useSecondTransparentMarble(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.FAUST) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.3)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 2109002);
    }

    private static void useFirstTransparentMarble(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.KING_SLIME) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.3)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 2109001);
    }

    private static void performCatch(MapleClient c, MapleMonster mob, int catchItemId, int rewardItemId) {
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), catchItemId, (byte) 1));
        mob.getMap().killMonster(mob, null, false);
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, catchItemId, 1, true, true);
        MapleInventoryManipulator.addById(c, rewardItemId, (short) 1, "", -1);
    }

    /**
     * Evaluate if the Mob is above %age of health. If they are, they are too strong to catch.
     * @param mob the mob to consider.
     * @param percentage % as a double to compare.
     * @return true if the mob is too strong
     */
    private static boolean isMobTooStrongToCatch(MapleMonster mob, double percentage) {
        return mob.getHp() >= Math.round(((double) mob.getMaxHp() * percentage));
    }

    private static void useCliffsMagicCane(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.LOST_RUDOLPH) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.4)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 4031887);
    }

    private static void useElementRock(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.SCORPION) {
            return;
        }

        if ((c.getPlayer().getAutobanManager().getLastSpam(10) + 800) >= currentServerTime()) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.4)) {
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        if (c.getPlayer().canHold(4031868, 1)) {
            if (Math.random() < 0.5) { // 50% chance
                performCatch(c, mob, itemId, 4031868);
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.catchMonster(mob.getObjectId(), itemId, (byte) 0));
            }
        } else {
            c.getPlayer().dropMessage(5, "Make a ETC slot available before using this item.");
        }

        c.getPlayer().getAutobanManager().spam(10);
    }

    private static void usePouch(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.GHOST) {
            return;
        }

        if ((c.getPlayer().getAutobanManager().getLastSpam(10) + 1000) >= currentServerTime()) {
            return;
        }

        if (isMobTooStrongToCatch(mob, 0.4)) {
            c.getPlayer().getAutobanManager().spam(10);
            c.announce(MaplePacketCreator.catchMessage(0));
            return;
        }

        performCatch(c, mob, itemId, 4031830);
    }

    private static void usePheromonePerfume(MapleClient c, int itemId, MapleMonster mob) {
        if (mob.getId() != MonsterId.TAMABLE_HOG) {
            return;
        }

        performCatch(c, mob, itemId, 1902000);
    }
}
