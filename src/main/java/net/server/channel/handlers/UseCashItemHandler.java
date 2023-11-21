package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.SkillMacro;
import client.creator.veteran.BowmanCreator;
import client.creator.veteran.MagicianCreator;
import client.creator.veteran.PirateCreator;
import client.creator.veteran.ThiefCreator;
import client.creator.veteran.WarriorCreator;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.ModifyInventory;
import client.inventory.manipulator.MapleInventoryManipulator;
import client.inventory.manipulator.MapleKarmaManipulator;
import client.processor.npc.DueyProcessor;
import client.processor.stat.AssignAPProcessor;
import client.processor.stat.AssignSPProcessor;
import config.YamlConfig;
import constants.game.GameConstants;
import constants.inventory.ItemConstants;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.ItemInformationProvider;
import server.MapleShop;
import server.MapleShopFactory;
import server.TimerManager;
import server.maps.AbstractMapleMapObject;
import server.maps.FieldLimit;
import server.maps.MapleKite;
import server.maps.MapleMap;
import server.maps.MaplePlayerShopItem;
import server.maps.MapleTVEffect;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class UseCashItemHandler extends AbstractMaplePacketHandler {
    private static void remove(MapleClient c, short position, int itemId) {
        MapleInventory cashInv = c.getPlayer().getInventory(MapleInventoryType.CASH);
        cashInv.lockInventory();
        try {
            Item it = cashInv.getItem(position);
            short adjustedPosition = position;
            if (it == null || it.getItemId() != itemId) {
                adjustedPosition = cashInv.findById(itemId)
                        .map(Item::getPosition)
                        .orElse(position);
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, adjustedPosition, (short) 1, true, false);
        } finally {
            cashInv.unlockInventory();
        }
    }

    private static boolean getIncubatedItem(MapleClient c, int id) {
        Optional<MapleInventoryType> type = MapleInventoryType.getByType((byte) (id / 1000000));
        if (type.isEmpty()) {
            return false;
        }

        if (c.getPlayer().getInventory(type.get()).isFull()) {
            return false;
        }

        final int[] ids = {1012070, 1302049, 1302063, 1322027, 2000004, 2000005, 2020013, 2020015, 2040307, 2040509, 2040519, 2040521, 2040533, 2040715, 2040717, 2040810, 2040811, 2070005, 2070006, 4020009,};
        final int[] quantitys = {1, 1, 1, 1, 240, 200, 200, 200, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3};
        int amount = 0;
        for (int i = 0; i < ids.length; i++) {
            if (i == id) {
                amount = quantitys[i];
            }
        }

        MapleInventoryManipulator.addById(c, id, (short) amount);
        return true;
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final MapleCharacter player = c.getPlayer();

        long timeNow = currentServerTime();
        if (timeNow - player.getLastUsedCashItem() < 3000) {
            player.dropMessage(1, "You have used a cash item recently. Wait a moment, then try again.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        player.setLastUsedCashItem(timeNow);

        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        short position = slea.readShort();
        int itemId = slea.readInt();
        int itemType = itemId / 10000;

        MapleInventory cashInv = player.getInventory(MapleInventoryType.CASH);
        Optional<Item> toUse = Optional.ofNullable(cashInv.getItem(position));
        if (toUse.isEmpty() || toUse.get().getItemId() != itemId) {
            toUse = cashInv.findById(itemId);

            if (toUse.isEmpty()) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }

            position = toUse.get().getPosition();
        }

        if (toUse.get().getQuantity() < 1) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }

        String medal = "";
        Item medalItem = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -49);
        if (medalItem != null) {
            medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
        }

        if (itemType == 504) { // vip teleport rock
            String error1 = "Either the player could not be found or you were trying to teleport to an illegal location.";
            boolean vip = slea.readByte() == 1 && itemId / 1000 >= 5041;
            remove(c, position, itemId);
            boolean success = false;
            if (!vip) {
                int mapId = slea.readInt();
                if (itemId / 1000 >= 5041 || mapId / 100000000 == player.getMapId() / 100000000) { //check vip or same continent
                    MapleMap targetMap = c.getChannelServer().getMapFactory().getMap(mapId);
                    if (!FieldLimit.CANNOTVIPROCK.check(targetMap.getFieldLimit()) && (targetMap.getForcedReturnId() == 999999999 || mapId < 100000000)) {
                        player.forceChangeMap(targetMap, targetMap.getRandomPlayerSpawnpoint());
                        success = true;
                    } else {
                        player.dropMessage(1, error1);
                    }
                } else {
                    player.dropMessage(1, "You cannot teleport between continents with this teleport rock.");
                }
            } else {
                String name = slea.readMapleAsciiString();
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name).orElse(null);
                if (victim != null) {
                    MapleMap targetMap = victim.getMap();
                    if (!FieldLimit.CANNOTVIPROCK.check(targetMap.getFieldLimit()) && (targetMap.getForcedReturnId() == 999999999 || targetMap.getId() < 100000000)) {
                        if (!victim.isGM() || victim.gmLevel() <= player.gmLevel()) {   // thanks Yoboes for noticing non-GM's being unreachable through rocks
                            player.forceChangeMap(targetMap, targetMap.findClosestPlayerSpawnpoint(victim.getPosition()));
                            success = true;
                        } else {
                            player.dropMessage(1, error1);
                        }
                    } else {
                        player.dropMessage(1, "You cannot teleport to this map.");
                    }
                } else {
                    player.dropMessage(1, "Player could not be found in this channel.");
                }
            }

            if (!success) {
                MapleInventoryManipulator.addById(c, itemId, (short) 1);
                c.announce(MaplePacketCreator.enableActions());
            }
        } else if (itemType == 505) { // AP/SP reset
            if (!player.isAlive()) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }

            if (itemId > 5050000) {
                int SPTo = slea.readInt();
                if (!AssignSPProcessor.canSPAssign(c, SPTo)) {  // exploit found thanks to Arnah
                    return;
                }

                int SPFrom = slea.readInt();
                Skill skillSPTo = SkillFactory.getSkill(SPTo).orElseThrow();
                Skill skillSPFrom = SkillFactory.getSkill(SPFrom).orElseThrow();
                byte curLevel = player.getSkillLevel(skillSPTo);
                byte curLevelSPFrom = player.getSkillLevel(skillSPFrom);
                if ((curLevel < skillSPTo.getMaxLevel()) && curLevelSPFrom > 0) {
                    player.changeSkillLevel(skillSPFrom, (byte) (curLevelSPFrom - 1), player.getMasterLevel(skillSPFrom), -1);
                    player.changeSkillLevel(skillSPTo, (byte) (curLevel + 1), player.getMasterLevel(skillSPTo), -1);

                    // update macros, thanks to Arnah
                    if ((curLevelSPFrom - 1) == 0) {
                        boolean updated = false;
                        for (SkillMacro macro : player.getMacros()) {
                            if (macro == null) {
                                continue;
                            }

                            boolean update = false;// cleaner?
                            if (macro.getSkill1() == SPFrom) {
                                update = true;
                                macro.setSkill1(0);
                            }
                            if (macro.getSkill2() == SPFrom) {
                                update = true;
                                macro.setSkill2(0);
                            }
                            if (macro.getSkill3() == SPFrom) {
                                update = true;
                                macro.setSkill3(0);
                            }
                            if (update) {
                                updated = true;
                                player.updateMacros(macro.getPosition(), macro);
                            }
                        }
                        if (updated) {
                            player.sendMacros();
                        }
                    }
                }
            } else {
                int APTo = slea.readInt();
                int APFrom = slea.readInt();

                if (!AssignAPProcessor.APResetAction(c, APFrom, APTo)) {
                    return;
                }
            }
            remove(c, position, itemId);
        } else if (itemType == 506) {
            Item eq = null;
            if (itemId == 5060000) { // Item tag.
                int equipSlot = slea.readShort();
                if (equipSlot == 0) {
                    return;
                }
                eq = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) equipSlot);
                eq.setOwner(player.getName());
            } else if (itemId == 5060001 || itemId == 5061000 || itemId == 5061001 || itemId == 5061002 || itemId == 5061003) { // Sealing lock
                Optional<MapleInventoryType> type = MapleInventoryType.getByType((byte) slea.readInt());
                if (type.isEmpty()) {
                    return;
                }

                eq = player.getInventory(type.get()).getItem((short) slea.readInt());
                if (eq == null) { //Check if the type is EQUIPMENT?
                    return;
                }
                short flag = eq.getFlag();
                flag |= ItemConstants.LOCK;
                if (eq.getExpiration() > -1) {
                    return; //No perma items pls
                }
                eq.setFlag(flag);

                long period = 0;
                if (itemId == 5061000) {
                    period = 7;
                } else if (itemId == 5061001) {
                    period = 30;
                } else if (itemId == 5061002) {
                    period = 90;
                } else if (itemId == 5061003) {
                    period = 365;
                }

                if (period > 0) {
                    eq.setExpiration(currentServerTime() + (period * 60 * 60 * 24 * 1000));
                }

                // double-remove found thanks to BHB
            } else if (itemId == 5060002) { // Incubator
                byte inventory2 = (byte) slea.readInt();
                short slot2 = (short) slea.readInt();
                Optional<MapleInventoryType> type = MapleInventoryType.getByType(inventory2);
                if (type.isEmpty()) {
                    return;
                }

                Item item2 = player.getInventory(type.get()).getItem(slot2);
                if (item2 == null) {
                    return;
                }
                if (getIncubatedItem(c, itemId)) {
                    MapleInventoryManipulator.removeFromSlot(c, type.get(), slot2, (short) 1, false);
                    remove(c, position, itemId);
                }
                return;
            }
            slea.readInt(); // time stamp
            if (eq != null) {
                player.forceUpdateItem(eq);
                remove(c, position, itemId);
            }
        } else if (itemType == 507) {
            boolean whisper;
            switch ((itemId / 1000) % 10) {
                case 1: // Megaphone
                    if (player.getLevel() > 9) {
                        player.getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(2, medal + player.getName() + " : " + slea.readMapleAsciiString()));
                    } else {
                        player.dropMessage(1, "You may not use this until you're level 10.");
                        return;
                    }
                    break;
                case 2: // Super megaphone
                    Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)));
                    break;
                case 5: // Maple TV
                    int tvType = itemId % 10;
                    boolean megassenger = false;
                    boolean ear = false;
                    MapleCharacter victim = null;
                    if (tvType != 1) {
                        if (tvType >= 3) {
                            megassenger = true;
                            if (tvType == 3) {
                                slea.readByte();
                            }
                            ear = 1 == slea.readByte();
                        } else if (tvType != 2) {
                            slea.readByte();
                        }
                        if (tvType != 4) {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString()).orElse(null);
                        }
                    }
                    List<String> messages = new LinkedList<>();
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < 5; i++) {
                        String message = slea.readMapleAsciiString();
                        if (megassenger) {
                            builder.append(" ").append(message);
                        }
                        messages.add(message);
                    }
                    slea.readInt();

                    if (!MapleTVEffect.broadcastMapleTVIfNotActive(player, victim, messages, tvType)) {
                        player.dropMessage(1, "MapleTV is already in use.");
                        return;
                    }

                    if (megassenger) {
                        Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + builder, ear));
                    }

                    break;
                case 6: //item megaphone
                    String msg = medal + player.getName() + " : " + slea.readMapleAsciiString();
                    whisper = slea.readByte() == 1;
                    Item item = null;
                    if (slea.readByte() == 1) { //item
                        Optional<MapleInventoryType> type = MapleInventoryType.getByType((byte) slea.readInt());
                        if (type.isEmpty()) {
                            return;
                        }

                        item = player.getInventory(type.get()).getItem((short) slea.readInt());
                        if (item == null) {
                            return;
                        }
                        // thanks Conrad for noticing that untradeable items should be allowed in megas
                    }
                    Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.itemMegaphone(msg, whisper, c.getChannel(), item));
                    break;
                case 7: //triple megaphone
                    int lines = slea.readByte();
                    if (lines < 1 || lines > 3) //hack
                    {
                        return;
                    }
                    String[] msg2 = new String[lines];
                    for (int i = 0; i < lines; i++) {
                        msg2[i] = medal + player.getName() + " : " + slea.readMapleAsciiString();
                    }
                    whisper = slea.readByte() == 1;
                    Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.getMultiMegaphone(msg2, c.getChannel(), whisper));
                    break;
            }
            remove(c, position, itemId);
        } else if (itemType == 508) {
            String text = slea.readMapleAsciiString();
            handleKite(c, itemId, text, position);
        } else if (itemType == 509) {
            String sendTo = slea.readMapleAsciiString();
            String msg = slea.readMapleAsciiString();
            handleNote(c, position, itemId, sendTo, msg);
        } else if (itemType == 510) {
            player.getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
            remove(c, position, itemId);
        } else if (itemType == 512) {
            String text = slea.readMapleAsciiString();
            handleMapBuff(c, position, itemId, text);
        } else if (itemType == 517) {
            String newName = slea.readMapleAsciiString();
            handlePetRename(c, position, itemId, newName);
        } else if (itemType == 520) {
            handleMesoSack(c, position, itemId);
        } else if (itemType == 523) {
            int searchItemId = slea.readInt();
            handleOwlOfMinerva(c, position, itemId, searchItemId);
        } else if (itemType == 524) {
            handlePetFood(c, position, itemId);
        } else if (itemType == 530) {
            handleCharacterTransformationItem(c, position, itemId);
        } else if (itemType == 533) {
            DueyProcessor.dueySendTalk(c, true);
        } else if (itemType == 537) {
            String message = slea.readMapleAsciiString();
            handleChalkboardUsage(player, message);
        } else if (itemType == 539) {
            List<String> strLines = new LinkedList<>();
            for (int i = 0; i < 4; i++) {
                strLines.add(slea.readMapleAsciiString());
            }

            handleAvatarMegaphone(slea, c, player, position, itemId, medal, strLines);
        } else if (itemType == 540) {
            slea.readByte();
            slea.readInt();
            if (itemId == 5400000) { //name change
                c.announce(MaplePacketCreator.showNameChangeCancel(player.cancelPendingNameChange()));
            } else if (itemId == 5401000) { //world transfer
                c.announce(MaplePacketCreator.showWorldTransferCancel(player.cancelPendingWorldTranfer()));
            }
            remove(c, position, itemId);
            c.announce(MaplePacketCreator.enableActions());
        } else if (itemType == 543) {
            if (itemId == 5432000 && !c.gainCharacterSlot()) {
                player.dropMessage(1, "You have already used up all 12 extra character slots.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }

            String name = slea.readMapleAsciiString();
            int face = slea.readInt();
            int hair = slea.readInt();
            int haircolor = slea.readInt();
            int skin = slea.readInt();
            int gender = slea.readInt();
            int jobid = slea.readInt();
            int improveSp = slea.readInt();

            int createStatus;
            switch (jobid) {
                case 0:
                    createStatus = WarriorCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                    break;

                case 1:
                    createStatus = MagicianCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                    break;

                case 2:
                    createStatus = BowmanCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                    break;

                case 3:
                    createStatus = ThiefCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
                    break;

                default:
                    createStatus = PirateCreator.createCharacter(c, name, face, hair + haircolor, skin, gender, improveSp);
            }

            if (createStatus == 0) {
                c.announce(MaplePacketCreator.sendMapleLifeError(0));   // success!

                player.showHint("#bSuccess#k on creation of the new character through the Maple Life card.");
                remove(c, position, itemId);
            } else {
                if (createStatus == -1) {    // check name
                    c.announce(MaplePacketCreator.sendMapleLifeNameError());
                } else {
                    c.announce(MaplePacketCreator.sendMapleLifeError(-1 * createStatus));
                }
            }
        } else if (itemType == 545) { // MiuMiu's travel store
            if (player.getShop() == null) {
                MapleShop shop = MapleShopFactory.getInstance().getShop(1338);
                if (shop != null) {
                    shop.sendShop(c);
                    remove(c, position, itemId);
                }
            } else {
                c.announce(MaplePacketCreator.enableActions());
            }
        } else if (itemType == 550) { //Extend item expiration
            c.announce(MaplePacketCreator.enableActions());
        } else if (itemType == 552) {
            Optional<MapleInventoryType> type = MapleInventoryType.getByType((byte) slea.readInt());
            if (type.isEmpty()) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }

            short slot = (short) slea.readInt();
            Item item = player.getInventory(type.get()).getItem(slot);
            if (item == null || item.getQuantity() <= 0 || MapleKarmaManipulator.hasKarmaFlag(item) || !ii.isKarmaAble(item.getItemId())) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }

            MapleKarmaManipulator.setKarmaFlag(item);
            player.forceUpdateItem(item);
            remove(c, position, itemId);
            c.announce(MaplePacketCreator.enableActions());
        } else if (itemType == 552) { //DS EGG THING
            c.announce(MaplePacketCreator.enableActions());
        } else if (itemType == 557) {
            slea.readInt();
            int itemSlot = slea.readInt();
            slea.readInt();
            final Equip equip = (Equip) player.getInventory(MapleInventoryType.EQUIP).getItem((short) itemSlot);
            if (equip.getVicious() >= 2 || player.getInventory(MapleInventoryType.CASH).findById(5570000).isEmpty()) {
                return;
            }
            equip.setVicious(equip.getVicious() + 1);
            equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
            remove(c, position, itemId);
            c.announce(MaplePacketCreator.enableActions());
            c.announce(MaplePacketCreator.sendHammerData(equip.getVicious()));
            player.forceUpdateItem(equip);
        } else if (itemType == 561) { //VEGA'S SPELL
            if (slea.readInt() != 1) {
                return;
            }

            final byte eSlot = (byte) slea.readInt();
            final Item eitem = player.getInventory(MapleInventoryType.EQUIP).getItem(eSlot);

            if (slea.readInt() != 2) {
                return;
            }

            final byte uSlot = (byte) slea.readInt();
            final Item uitem = player.getInventory(MapleInventoryType.USE).getItem(uSlot);
            if (eitem == null || uitem == null) {
                return;
            }

            Equip toScroll = (Equip) eitem;
            if (toScroll.getUpgradeSlots() < 1) {
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            }

            //should have a check here against PE hacks
            if (itemId / 1000000 != 5) {
                itemId = 0;
            }

            player.toggleBlockCashShop();

            final int curlevel = toScroll.getLevel();
            c.announce(MaplePacketCreator.sendVegaScroll(0x40));

            final Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, uitem.getItemId(), false, itemId, player.isGM());
            c.announce(MaplePacketCreator.sendVegaScroll(scrolled.getLevel() > curlevel ? 0x41 : 0x43));
            //opcodes 0x42, 0x44: "this item cannot be used"; 0x39, 0x45: crashes

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, uSlot, (short) 1, false);
            remove(c, position, itemId);

            final MapleClient client = c;
            TimerManager.getInstance().schedule(() -> {
                if (!player.isLoggedin()) {
                    return;
                }

                player.toggleBlockCashShop();

                final List<ModifyInventory> mods = new ArrayList<>();
                mods.add(new ModifyInventory(3, scrolled));
                mods.add(new ModifyInventory(0, scrolled));
                client.announce(MaplePacketCreator.modifyInventory(true, mods));

                ScrollResult scrollResult = scrolled.getLevel() > curlevel ? ScrollResult.SUCCESS : ScrollResult.FAIL;
                player.getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(player.getId(), scrollResult, false, false));
                if (eSlot < 0 && (scrollResult == ScrollResult.SUCCESS)) {
                    player.equipChanged();
                }

                client.announce(MaplePacketCreator.enableActions());
            }, 1000 * 3);
        } else {
            System.out.println("NEW CASH ITEM: " + itemType + "\n" + slea);
            c.announce(MaplePacketCreator.enableActions());
        }
    }

    private static void handleAvatarMegaphone(SeekableLittleEndianAccessor slea, MapleClient c, MapleCharacter player, short position, int itemId, String medal, List<String> strLines) {
        Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.getAvatarMega(player, medal, c.getChannel(), itemId, strLines, (slea.readByte() != 0)));
        TimerManager.getInstance().schedule(() -> Server.getInstance().broadcastMessage(c.getWorld(), MaplePacketCreator.byeAvatarMega()), 1000 * 10);
        remove(c, position, itemId);
    }

    private static void handleChalkboardUsage(MapleCharacter player, String message) {
        if (GameConstants.isFreeMarketRoom(player.getMapId())) {
            player.dropMessage(5, "You cannot use the chalkboard here.");
            player.announce(MaplePacketCreator.enableActions());
            return;
        }

        player.setChalkboard(message);
        player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, false));
        player.announce(MaplePacketCreator.enableActions());
    }

    private static void handleCharacterTransformationItem(MapleClient c, short position, int itemId) {
        ItemInformationProvider.getInstance().getItemEffect(itemId).applyTo(c.getPlayer());
        remove(c, position, itemId);
    }

    private static void handlePetFood(MapleClient c, short position, int itemId) {
        for (byte i = 0; i < 3; i++) {
            Optional<MaplePet> pet = c.getPlayer().getPet(i);
            if (pet.isPresent()) {
                Pair<Integer, Boolean> p = pet.get().canConsume(itemId);

                if (p.getRight()) {
                    pet.get().gainClosenessFullness(c.getPlayer(), p.getLeft(), 100, 1);
                    remove(c, position, itemId);
                    break;
                }
            } else {
                break;
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void handleOwlOfMinerva(MapleClient c, short position, int itemId, int searchItemId) {
        if (!YamlConfig.config.server.USE_ENFORCE_ITEM_SUGGESTION) {
            c.getWorldServer().addOwlItemSearch(searchItemId);
        }
        c.getPlayer().setOwlSearch(searchItemId);
        List<Pair<MaplePlayerShopItem, AbstractMapleMapObject>> hmsAvailable = c.getWorldServer().getAvailableItemBundles(searchItemId);
        if (!hmsAvailable.isEmpty()) {
            remove(c, position, itemId);
        }

        c.announce(MaplePacketCreator.owlOfMinerva(c, searchItemId, hmsAvailable));
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void handleMesoSack(MapleClient c, short position, int itemId) {
        c.getPlayer().gainMeso(ItemInformationProvider.getInstance().getMeso(itemId), true, false, true);
        remove(c, position, itemId);
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void handlePetRename(MapleClient c, short position, int itemId, String newName) {
        c.getPlayer().getPet(0).ifPresent(p -> handlePetRename(c, p, position, itemId, newName));
        c.announce(MaplePacketCreator.enableActions());
    }

    private static void handlePetRename(MapleClient c, MaplePet pet, short position, int itemId, String newName) {
        pet.setName(newName);
        pet.saveToDb();

        Item item = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
        if (item != null) {
            c.getPlayer().forceUpdateItem(item);
        }

        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.changePetName(c.getPlayer(), newName, 1), true);
        remove(c, position, itemId);
    }

    private static void handleMapBuff(MapleClient c, short position, int itemId, String text) {
        ItemInformationProvider.getInstance().getStateChangeItemId(itemId)
                .map(stateChangeItemId -> ItemInformationProvider.getInstance().getItemEffect(stateChangeItemId))
                .ifPresent(statEffect -> c.getPlayer().getMap().getCharacters().forEach(statEffect::applyTo));

        String message = ItemInformationProvider.getInstance().getMsg(itemId)
                .replaceFirst("%s", c.getPlayer().getName())
                .replaceFirst("%s", text);
        c.getPlayer().getMap().startMapEffect(message, itemId);
        remove(c, position, itemId);
    }

    private static void handleNote(MapleClient c, short position, int itemId, String sendTo, String msg) {
        c.getPlayer().sendNote(sendTo, msg, (byte) 0);
        remove(c, position, itemId);
    }

    private static void handleKite(MapleClient c, int itemId, String text, short position) {
        MapleKite kite = new MapleKite(c.getPlayer(), text, itemId);

        if (GameConstants.isFreeMarketRoom(c.getPlayer().getMapId())) {
            c.announce(MaplePacketCreator.sendCannotSpawnKite());
            return;
        }

        c.getPlayer().getMap().spawnKite(kite);
        remove(c, position, itemId);
    }
}
