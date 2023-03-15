package server;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.Skill;
import client.SkillFactory;
import client.autoban.AutobanFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import config.YamlConfig;
import constants.inventory.EquipSlot;
import constants.inventory.ItemConstants;
import constants.skills.Assassin;
import constants.skills.Gunslinger;
import constants.skills.NightWalker;
import net.server.Server;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MakerItemFactory.MakerItemCreateEntry;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.StringUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class ItemInformationProvider {
    private final static ItemInformationProvider instance = new ItemInformationProvider();
    protected MapleDataProvider itemData;
    protected MapleDataProvider equipData;
    protected MapleDataProvider stringData;
    protected MapleDataProvider etcData;
    protected MapleData cashStringData;
    protected MapleData consumeStringData;
    protected MapleData eqpStringData;
    protected MapleData etcStringData;
    protected MapleData insStringData;
    protected MapleData petStringData;
    protected Map<Integer, Short> slotMaxCache = new HashMap<>();
    protected Map<Integer, MapleStatEffect> itemEffects = new HashMap<>();
    protected Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<>();
    protected Map<Integer, Equip> equipCache = new HashMap<>();
    protected Map<Integer, MapleData> equipLevelInfoCache = new HashMap<>();
    protected Map<Integer, Integer> equipLevelReqCache = new HashMap<>();
    protected Map<Integer, Integer> equipMaxLevelCache = new HashMap<>();
    protected Map<Integer, List<Integer>> scrollReqsCache = new HashMap<>();
    protected Map<Integer, Integer> wholePriceCache = new HashMap<>();
    protected Map<Integer, Double> unitPriceCache = new HashMap<>();
    protected Map<Integer, Integer> projectileWatkCache = new HashMap<>();
    protected Map<Integer, String> nameCache = new HashMap<>();
    protected Map<Integer, String> descCache = new HashMap<>();
    protected Map<Integer, String> msgCache = new HashMap<>();
    protected Map<Integer, Boolean> accountItemRestrictionCache = new HashMap<>();
    protected Map<Integer, Boolean> dropRestrictionCache = new HashMap<>();
    protected Map<Integer, Boolean> pickupRestrictionCache = new HashMap<>();
    protected Map<Integer, Integer> getMesoCache = new HashMap<>();
    protected Map<Integer, Integer> monsterBookID = new HashMap<>();
    protected Map<Integer, Boolean> untradeableCache = new HashMap<>();
    protected Map<Integer, Boolean> onEquipUntradeableCache = new HashMap<>();
    protected Map<Integer, ScriptedItem> scriptedItemCache = new HashMap<>();
    protected Map<Integer, Boolean> karmaCache = new HashMap<>();
    protected Map<Integer, Integer> triggerItemCache = new HashMap<>();
    protected Map<Integer, Integer> expCache = new HashMap<>();
    protected Map<Integer, Integer> createItem = new HashMap<>();
    protected Map<Integer, Integer> mobItem = new HashMap<>();
    protected Map<Integer, Integer> useDelay = new HashMap<>();
    protected Map<Integer, Integer> mobHP = new HashMap<>();
    protected Map<Integer, Integer> levelCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, List<RewardItem>>> rewardCache = new HashMap<>();
    protected List<Pair<Integer, String>> itemNameCache = new ArrayList<>();
    protected Map<Integer, Boolean> consumeOnPickupCache = new HashMap<>();
    protected Map<Integer, Boolean> isQuestItemCache = new HashMap<>();
    protected Map<Integer, Boolean> isPartyQuestItemCache = new HashMap<>();
    protected Map<Integer, ExpirationReplacement> replaceOnExpireCache = new HashMap<>();
    protected Map<Integer, String> equipmentSlotCache = new HashMap<>();
    protected Map<Integer, Boolean> noCancelMouseCache = new HashMap<>();
    protected Map<Integer, Integer> mobCrystalMakerCache = new HashMap<>();
    protected Map<Integer, Pair<String, Integer>> statUpgradeMakerCache = new HashMap<>();
    protected Map<Integer, MakerItemFactory.MakerItemCreateEntry> makerItemCache = new HashMap<>();
    protected Map<Integer, Integer> makerCatalystCache = new HashMap<>();
    protected Map<Integer, Map<String, Integer>> skillUpgradeCache = new HashMap<>();
    protected Map<Integer, MapleData> skillUpgradeInfoCache = new HashMap<>();
    protected Map<Integer, Pair<Integer, Set<Integer>>> cashPetFoodCache = new HashMap<>();
    protected Map<Integer, QuestConsumableItem> questItemConsCache = new HashMap<>();

    private ItemInformationProvider() {
        loadCardIdData();
        itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Item.wz"));
        equipData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Character.wz"));
        stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
        etcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
        cashStringData = stringData.getData("Cash.img");
        consumeStringData = stringData.getData("Consume.img");
        eqpStringData = stringData.getData("Eqp.img");
        etcStringData = stringData.getData("Etc.img");
        insStringData = stringData.getData("Ins.img");
        petStringData = stringData.getData("Pet.img");

        isQuestItemCache.put(0, false);
        isPartyQuestItemCache.put(0, false);
    }

    public static ItemInformationProvider getInstance() {
        return instance;
    }

    private static short getExtraSlotMaxFromPlayer(MapleClient c, int itemId) {
        short ret = 0;

        // thanks GMChuck for detecting player sensitive data being cached into getSlotMax
        if (ItemConstants.isThrowingStar(itemId)) {
            if (c.getPlayer().getJob().isA(MapleJob.NIGHTWALKER1)) {
                ret += c.getPlayer().getSkillLevel(NightWalker.CLAW_MASTERY) * 10;
            } else {
                ret += c.getPlayer().getSkillLevel(Assassin.CLAW_MASTERY) * 10;
            }
        } else if (ItemConstants.isBullet(itemId)) {
            ret += c.getPlayer().getSkillLevel(Gunslinger.GUN_MASTERY) * 10;
        }

        return ret;
    }

    private static double getRoundedUnitPrice(double unitPrice, int max) {
        double intPart = Math.floor(unitPrice);
        double fractPart = unitPrice - intPart;
        if (fractPart == 0.0) {
            return intPart;
        }

        double fractMask = 0.0;
        double lastFract, curFract = 1.0;
        int i = 1;

        do {
            lastFract = curFract;
            curFract /= 2;

            if (fractPart == curFract) {
                break;
            } else if (fractPart > curFract) {
                fractMask += curFract;
                fractPart -= curFract;
            }

            i++;
        } while (i <= max);

        if (i > max) {
            lastFract = curFract;
            curFract = 0.0;
        }

        if (Math.abs(fractPart - curFract) < Math.abs(fractPart - lastFract)) {
            return intPart + fractMask + curFract;
        } else {
            return intPart + fractMask + lastFract;
        }
    }

    private static double testYourLuck(double prop, int dices) {   // revamped testYourLuck author: David A.
        return Math.pow(1.0 - prop, dices);
    }

    public static boolean rollSuccessChance(double propPercent) {
        return Math.random() >= testYourLuck(propPercent / 100.0, YamlConfig.config.server.SCROLL_CHANCE_ROLLS);
    }

    private static short getMaximumShortMaxIfOverflow(int value1, int value2) {
        return (short) Math.min(Short.MAX_VALUE, Math.max(value1, value2));
    }

    private static short getShortMaxIfOverflow(int value) {
        return (short) Math.min(Short.MAX_VALUE, value);
    }

    private static short chscrollRandomizedStat(int range) {
        return (short) Randomizer.rand(-range, range);
    }

    public static void improveEquipStats(Equip nEquip, Map<String, Integer> stats) {
        for (Entry<String, Integer> stat : stats.entrySet()) {
            switch (stat.getKey()) {
                case "STR":
                    nEquip.setStr(getShortMaxIfOverflow(nEquip.getStr() + stat.getValue()));
                    break;
                case "DEX":
                    nEquip.setDex(getShortMaxIfOverflow(nEquip.getDex() + stat.getValue()));
                    break;
                case "INT":
                    nEquip.setInt(getShortMaxIfOverflow(nEquip.getInt() + stat.getValue()));
                    break;
                case "LUK":
                    nEquip.setLuk(getShortMaxIfOverflow(nEquip.getLuk() + stat.getValue()));
                    break;
                case "PAD":
                    nEquip.setWatk(getShortMaxIfOverflow(nEquip.getWatk() + stat.getValue()));
                    break;
                case "PDD":
                    nEquip.setWdef(getShortMaxIfOverflow(nEquip.getWdef() + stat.getValue()));
                    break;
                case "MAD":
                    nEquip.setMatk(getShortMaxIfOverflow(nEquip.getMatk() + stat.getValue()));
                    break;
                case "MDD":
                    nEquip.setMdef(getShortMaxIfOverflow(nEquip.getMdef() + stat.getValue()));
                    break;
                case "ACC":
                    nEquip.setAcc(getShortMaxIfOverflow(nEquip.getAcc() + stat.getValue()));
                    break;
                case "EVA":
                    nEquip.setAvoid(getShortMaxIfOverflow(nEquip.getAvoid() + stat.getValue()));
                    break;
                case "Speed":
                    nEquip.setSpeed(getShortMaxIfOverflow(nEquip.getSpeed() + stat.getValue()));
                    break;
                case "Jump":
                    nEquip.setJump(getShortMaxIfOverflow(nEquip.getJump() + stat.getValue()));
                    break;
                case "MHP":
                    nEquip.setHp(getShortMaxIfOverflow(nEquip.getHp() + stat.getValue()));
                    break;
                case "MMP":
                    nEquip.setMp(getShortMaxIfOverflow(nEquip.getMp() + stat.getValue()));
                    break;
                case "afterImage":
                    break;
            }
        }
    }

    private static short getRandStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
        return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer.nextDouble() * (lMaxRange * 2 + 1)));
    }

    private static short getRandUpgradedStat(short defaultValue, int maxRange) {
        if (defaultValue == 0) {
            return 0;
        }
        return (short) (defaultValue + Math.floor(Randomizer.nextDouble() * (maxRange + 1)));
    }

    private static int getCrystalForLevel(int level) {
        int range = (level - 1) / 10;

        if (range < 5) {
            return 4260000;
        } else if (range > 11) {
            return 4260008;
        } else {
            switch (range) {
                case 5:
                    return 4260001;

                case 6:
                    return 4260002;

                case 7:
                    return 4260003;

                case 8:
                    return 4260004;

                case 9:
                    return 4260005;

                case 10:
                    return 4260006;

                default:
                    return 4260007;
            }
        }
    }

    public List<Pair<Integer, String>> getAllItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }
        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;
        itemsData = stringData.getData("Cash.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Consume.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        for (MapleData eqpType : itemsData.getChildren()) {
            for (MapleData itemFolder : eqpType.getChildren()) {
                itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
            }
        }
        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Ins.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        itemsData = stringData.getData("Pet.img");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        return itemPairs;
    }

    public List<Pair<Integer, String>> getAllEtcItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }

        List<Pair<Integer, String>> itemPairs = new ArrayList<>();
        MapleData itemsData;

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        return itemPairs;
    }

    private MapleData getStringData(int itemId) {
        String cat = "null";
        MapleData theData;
        if (itemId >= 5010000) {
            theData = cashStringData;
        } else if (itemId >= 2000000 && itemId < 3000000) {
            theData = consumeStringData;
        } else if ((itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1123000) || (itemId >= 1132000 && itemId < 1133000) || (itemId >= 1142000 && itemId < 1143000)) {
            theData = eqpStringData;
            cat = "Eqp/Accessory";
        } else if (itemId >= 1000000 && itemId < 1010000) {
            theData = eqpStringData;
            cat = "Eqp/Cap";
        } else if (itemId >= 1102000 && itemId < 1103000) {
            theData = eqpStringData;
            cat = "Eqp/Cape";
        } else if (itemId >= 1040000 && itemId < 1050000) {
            theData = eqpStringData;
            cat = "Eqp/Coat";
        } else if (itemId >= 20000 && itemId < 22000) {
            theData = eqpStringData;
            cat = "Eqp/Face";
        } else if (itemId >= 1080000 && itemId < 1090000) {
            theData = eqpStringData;
            cat = "Eqp/Glove";
        } else if (itemId >= 30000 && itemId < 35000) {
            theData = eqpStringData;
            cat = "Eqp/Hair";
        } else if (itemId >= 1050000 && itemId < 1060000) {
            theData = eqpStringData;
            cat = "Eqp/Longcoat";
        } else if (itemId >= 1060000 && itemId < 1070000) {
            theData = eqpStringData;
            cat = "Eqp/Pants";
        } else if (itemId >= 1802000 && itemId < 1842000) {
            theData = eqpStringData;
            cat = "Eqp/PetEquip";
        } else if (itemId >= 1112000 && itemId < 1120000) {
            theData = eqpStringData;
            cat = "Eqp/Ring";
        } else if (itemId >= 1092000 && itemId < 1100000) {
            theData = eqpStringData;
            cat = "Eqp/Shield";
        } else if (itemId >= 1070000 && itemId < 1080000) {
            theData = eqpStringData;
            cat = "Eqp/Shoes";
        } else if (itemId >= 1900000 && itemId < 2000000) {
            theData = eqpStringData;
            cat = "Eqp/Taming";
        } else if (itemId >= 1300000 && itemId < 1800000) {
            theData = eqpStringData;
            cat = "Eqp/Weapon";
        } else if (itemId >= 4000000 && itemId < 5000000) {
            theData = etcStringData;
            cat = "Etc";
        } else if (itemId >= 3000000 && itemId < 4000000) {
            theData = insStringData;
        } else if (ItemConstants.isPet(itemId)) {
            theData = petStringData;
        } else {
            return null;
        }
        if (cat.equalsIgnoreCase("null")) {
            return theData.getChildByPath(String.valueOf(itemId));
        } else {
            return theData.getChildByPath(cat + "/" + itemId);
        }
    }

    public boolean noCancelMouse(int itemId) {
        if (noCancelMouseCache.containsKey(itemId)) {
            return noCancelMouseCache.get(itemId);
        }

        Optional<MapleData> item = getItemData(itemId);
        if (item.isEmpty()) {
            noCancelMouseCache.put(itemId, false);
            return false;
        }

        boolean blockMouse = MapleDataTool.getIntConvert("info/noCancelMouse", item.get(), 0) == 1;
        noCancelMouseCache.put(itemId, blockMouse);
        return blockMouse;
    }

    private Optional<MapleData> getItemData(int itemId) {
        Optional<MapleData> ret = Optional.empty();
        String idStr = "0" + itemId;
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = Optional.ofNullable(itemData.getData(topDir.getName() + "/" + iFile.getName()));
                    if (ret.isEmpty()) {
                        return Optional.empty();
                    }
                    ret = ret.map(d -> d.getChildByPath(idStr));
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return Optional.ofNullable(itemData.getData(topDir.getName() + "/" + iFile.getName()));
                }
            }
        }
        root = equipData.getRoot();
        for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return Optional.ofNullable(equipData.getData(topDir.getName() + "/" + iFile.getName()));
                }
            }
        }
        return ret;
    }

    public List<Integer> getItemIdsInRange(int minId, int maxId, boolean ignoreCashItem) {
        List<Integer> list = new ArrayList<>();

        if (ignoreCashItem) {
            for (int i = minId; i <= maxId; i++) {
                if (getItemData(i).isPresent() && !isCash(i)) {
                    list.add(i);
                }
            }
        } else {
            for (int i = minId; i <= maxId; i++) {
                if (getItemData(i).isPresent()) {
                    list.add(i);
                }
            }
        }


        return list;
    }

    public short getSlotMax(MapleClient c, int itemId) {
        Short slotMax = slotMaxCache.get(itemId);
        if (slotMax != null) {
            return (short) (slotMax + getExtraSlotMaxFromPlayer(c, itemId));
        }
        short ret = 0;
        Optional<MapleData> item = getItemData(itemId);
        if (item.isPresent()) {
            MapleData smEntry = item.get().getChildByPath("info/slotMax");
            if (smEntry == null) {
                if (ItemConstants.getInventoryType(itemId).getType() == MapleInventoryType.EQUIP.getType()) {
                    ret = 1;
                } else {
                    ret = 100;
                }
            } else {
                ret = (short) MapleDataTool.getInt(smEntry);
            }
        }

        slotMaxCache.put(itemId, ret);
        return (short) (ret + getExtraSlotMaxFromPlayer(c, itemId));
    }

    public int getMeso(int itemId) {
        if (getMesoCache.containsKey(itemId)) {
            return getMesoCache.get(itemId);
        }
        Optional<MapleData> item = getItemData(itemId);
        if (item.isEmpty()) {
            return -1;
        }
        int pEntry;
        MapleData pData = item.get().getChildByPath("info/meso");
        if (pData == null) {
            return -1;
        }
        pEntry = MapleDataTool.getInt(pData);
        getMesoCache.put(itemId, pEntry);
        return pEntry;
    }

    private Pair<Integer, Double> getItemPriceData(int itemId) {
        Optional<MapleData> item = getItemData(itemId);
        if (item.isEmpty()) {
            wholePriceCache.put(itemId, -1);
            unitPriceCache.put(itemId, 0.0);
            return new Pair<>(-1, 0.0);
        }

        int pEntry = -1;
        MapleData pData = item.get().getChildByPath("info/price");
        if (pData != null) {
            pEntry = MapleDataTool.getInt(pData);
        }

        double fEntry = 0.0f;
        pData = item.get().getChildByPath("info/unitPrice");
        if (pData != null) {
            try {
                fEntry = getRoundedUnitPrice(MapleDataTool.getDouble(pData), 5);
            } catch (Exception e) {
                fEntry = MapleDataTool.getInt(pData);
            }
        }

        wholePriceCache.put(itemId, pEntry);
        unitPriceCache.put(itemId, fEntry);
        return new Pair<>(pEntry, fEntry);
    }

    public int getWholePrice(int itemId) {
        if (wholePriceCache.containsKey(itemId)) {
            return wholePriceCache.get(itemId);
        }

        return getItemPriceData(itemId).getLeft();
    }

    public double getUnitPrice(int itemId) {
        if (unitPriceCache.containsKey(itemId)) {
            return unitPriceCache.get(itemId);
        }

        return getItemPriceData(itemId).getRight();
    }

    public int getPrice(int itemId, int quantity) {
        int retPrice = getWholePrice(itemId);
        if (retPrice == -1) {
            return -1;
        }

        if (!ItemConstants.isRechargeable(itemId)) {
            retPrice *= quantity;
        } else {
            retPrice += Math.ceil(quantity * getUnitPrice(itemId));
        }

        return retPrice;
    }

    public Optional<ExpirationReplacement> getReplaceOnExpire(int itemId) {
        if (replaceOnExpireCache.containsKey(itemId)) {
            return Optional.ofNullable(replaceOnExpireCache.get(itemId));
        }

        Optional<MapleData> data = getItemData(itemId);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        int itemReplacement = MapleDataTool.getInt("info/replace/itemid", data.get(), 0);
        String msg = MapleDataTool.getString("info/replace/msg", data.get(), "");

        ExpirationReplacement ret = new ExpirationReplacement(itemReplacement, msg);
        replaceOnExpireCache.put(itemId, ret);
        return Optional.of(ret);
    }

    protected String getEquipmentSlot(int itemId) {
        if (equipmentSlotCache.containsKey(itemId)) {
            return equipmentSlotCache.get(itemId);
        }

        String ret;

        Optional<MapleData> item = getItemData(itemId);

        if (item.isEmpty()) {
            return null;
        }

        MapleData info = item.get().getChildByPath("info");

        if (info == null) {
            return null;
        }

        ret = MapleDataTool.getString("islot", info, "");

        equipmentSlotCache.put(itemId, ret);

        return ret;
    }

    public Map<String, Integer> getEquipStats(int itemId) {
        if (equipStatsCache.containsKey(itemId)) {
            return equipStatsCache.get(itemId);
        }

        Optional<MapleData> item = getItemData(itemId);
        if (item.isEmpty()) {
            return null;
        }

        MapleData info = item.get().getChildByPath("info");
        if (info == null) {
            return null;
        }

        Map<String, Integer> ret = new LinkedHashMap<>();
        for (MapleData data : info.getChildren()) {
            if (data.getName().startsWith("inc")) {
                ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
            }
            /*else if (data.getName().startsWith("req"))
             ret.put(data.getName(), MapleDataTool.getInt(data.getName(), info, 0));*/
        }

        ret.put("reqJob", MapleDataTool.getInt("reqJob", info, 0));
        ret.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
        ret.put("reqDEX", MapleDataTool.getInt("reqDEX", info, 0));
        ret.put("reqSTR", MapleDataTool.getInt("reqSTR", info, 0));
        ret.put("reqINT", MapleDataTool.getInt("reqINT", info, 0));
        ret.put("reqLUK", MapleDataTool.getInt("reqLUK", info, 0));
        ret.put("reqPOP", MapleDataTool.getInt("reqPOP", info, 0));
        ret.put("cash", MapleDataTool.getInt("cash", info, 0));
        ret.put("tuc", MapleDataTool.getInt("tuc", info, 0));
        ret.put("cursed", MapleDataTool.getInt("cursed", info, 0));
        ret.put("success", MapleDataTool.getInt("success", info, 0));
        ret.put("fs", MapleDataTool.getInt("fs", info, 0));
        equipStatsCache.put(itemId, ret);
        return ret;
    }

    public Integer getEquipLevelReq(int itemId) {
        if (equipLevelReqCache.containsKey(itemId)) {
            return equipLevelReqCache.get(itemId);
        }

        int ret = 0;
        Optional<MapleData> item = getItemData(itemId);
        if (item.isPresent()) {
            MapleData info = item.get().getChildByPath("info");
            if (info != null) {
                ret = MapleDataTool.getInt("reqLevel", info, 0);
            }
        }

        equipLevelReqCache.put(itemId, ret);
        return ret;
    }

    public List<Integer> getScrollReqs(int itemId) {
        if (scrollReqsCache.containsKey(itemId)) {
            return scrollReqsCache.get(itemId);
        }

        List<Integer> ret = new ArrayList<>();
        Optional<MapleData> data = getItemData(itemId);


        data = data.map(d -> d.getChildByPath("req"));
        if (data.isPresent()) {
            for (MapleData req : data.get().getChildren()) {
                ret.add(MapleDataTool.getInt(req));
            }
        }

        scrollReqsCache.put(itemId, ret);
        return ret;
    }

    public MapleWeaponType getWeaponType(int itemId) {
        int cat = (itemId / 10000) % 100;
        MapleWeaponType[] type = {MapleWeaponType.SWORD1H, MapleWeaponType.GENERAL1H_SWING, MapleWeaponType.GENERAL1H_SWING, MapleWeaponType.DAGGER_OTHER, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.WAND, MapleWeaponType.STAFF, MapleWeaponType.NOT_A_WEAPON, MapleWeaponType.SWORD2H, MapleWeaponType.GENERAL2H_SWING, MapleWeaponType.GENERAL2H_SWING, MapleWeaponType.SPEAR_STAB, MapleWeaponType.POLE_ARM_SWING, MapleWeaponType.BOW, MapleWeaponType.CROSSBOW, MapleWeaponType.CLAW, MapleWeaponType.KNUCKLE, MapleWeaponType.GUN};
        if (cat < 30 || cat > 49) {
            return MapleWeaponType.NOT_A_WEAPON;
        }
        return type[cat - 30];
    }

    public void scrollOptionEquipWithChaos(Equip nEquip, int range, boolean option) {
        // option: watk, matk, wdef, mdef, spd, jump, hp, mp
        //   stat: dex, luk, str, int, avoid, acc

        if (!option) {
            if (nEquip.getStr() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                }
            }
        } else {
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range))));
                }
            }

            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range))));
                }
            }
        }
    }

    private void scrollEquipWithChaos(Equip nEquip, int range) {
        if (YamlConfig.config.server.CHSCROLL_STAT_RATE > 0) {
            int temp;
            short curStr, curDex, curInt, curLuk, curWatk, curWdef, curMatk, curMdef, curAcc, curAvoid, curSpeed, curJump, curHp, curMp;

            if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                curStr = nEquip.getStr();
                curDex = nEquip.getDex();
                curInt = nEquip.getInt();
                curLuk = nEquip.getLuk();
                curWatk = nEquip.getWatk();
                curWdef = nEquip.getWdef();
                curMatk = nEquip.getMatk();
                curMdef = nEquip.getMdef();
                curAcc = nEquip.getAcc();
                curAvoid = nEquip.getAvoid();
                curSpeed = nEquip.getSpeed();
                curJump = nEquip.getJump();
                curHp = nEquip.getHp();
                curMp = nEquip.getMp();
            } else {
                curStr = Short.MIN_VALUE;
                curDex = Short.MIN_VALUE;
                curInt = Short.MIN_VALUE;
                curLuk = Short.MIN_VALUE;
                curWatk = Short.MIN_VALUE;
                curWdef = Short.MIN_VALUE;
                curMatk = Short.MIN_VALUE;
                curMdef = Short.MIN_VALUE;
                curAcc = Short.MIN_VALUE;
                curAvoid = Short.MIN_VALUE;
                curSpeed = Short.MIN_VALUE;
                curJump = Short.MIN_VALUE;
                curHp = Short.MIN_VALUE;
                curMp = Short.MIN_VALUE;
            }

            for (int i = 0; i < YamlConfig.config.server.CHSCROLL_STAT_RATE; i++) {
                if (nEquip.getStr() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curStr + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getStr() + chscrollRandomizedStat(range);
                    }

                    curStr = getMaximumShortMaxIfOverflow(temp, curStr);
                }

                if (nEquip.getDex() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curDex + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getDex() + chscrollRandomizedStat(range);
                    }

                    curDex = getMaximumShortMaxIfOverflow(temp, curDex);
                }

                if (nEquip.getInt() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curInt + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getInt() + chscrollRandomizedStat(range);
                    }

                    curInt = getMaximumShortMaxIfOverflow(temp, curInt);
                }

                if (nEquip.getLuk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curLuk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getLuk() + chscrollRandomizedStat(range);
                    }

                    curLuk = getMaximumShortMaxIfOverflow(temp, curLuk);
                }

                if (nEquip.getWatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWatk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getWatk() + chscrollRandomizedStat(range);
                    }

                    curWatk = getMaximumShortMaxIfOverflow(temp, curWatk);
                }

                if (nEquip.getWdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curWdef + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getWdef() + chscrollRandomizedStat(range);
                    }

                    curWdef = getMaximumShortMaxIfOverflow(temp, curWdef);
                }

                if (nEquip.getMatk() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMatk + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMatk() + chscrollRandomizedStat(range);
                    }

                    curMatk = getMaximumShortMaxIfOverflow(temp, curMatk);
                }

                if (nEquip.getMdef() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMdef + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMdef() + chscrollRandomizedStat(range);
                    }

                    curMdef = getMaximumShortMaxIfOverflow(temp, curMdef);
                }

                if (nEquip.getAcc() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAcc + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getAcc() + chscrollRandomizedStat(range);
                    }

                    curAcc = getMaximumShortMaxIfOverflow(temp, curAcc);
                }

                if (nEquip.getAvoid() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curAvoid + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getAvoid() + chscrollRandomizedStat(range);
                    }

                    curAvoid = getMaximumShortMaxIfOverflow(temp, curAvoid);
                }

                if (nEquip.getSpeed() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curSpeed + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getSpeed() + chscrollRandomizedStat(range);
                    }

                    curSpeed = getMaximumShortMaxIfOverflow(temp, curSpeed);
                }

                if (nEquip.getJump() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curJump + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getJump() + chscrollRandomizedStat(range);
                    }

                    curJump = getMaximumShortMaxIfOverflow(temp, curJump);
                }

                if (nEquip.getHp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curHp + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getHp() + chscrollRandomizedStat(range);
                    }

                    curHp = getMaximumShortMaxIfOverflow(temp, curHp);
                }

                if (nEquip.getMp() > 0) {
                    if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                        temp = curMp + chscrollRandomizedStat(range);
                    } else {
                        temp = nEquip.getMp() + chscrollRandomizedStat(range);
                    }

                    curMp = getMaximumShortMaxIfOverflow(temp, curMp);
                }
            }

            nEquip.setStr((short) Math.max(0, curStr));
            nEquip.setDex((short) Math.max(0, curDex));
            nEquip.setInt((short) Math.max(0, curInt));
            nEquip.setLuk((short) Math.max(0, curLuk));
            nEquip.setWatk((short) Math.max(0, curWatk));
            nEquip.setWdef((short) Math.max(0, curWdef));
            nEquip.setMatk((short) Math.max(0, curMatk));
            nEquip.setMdef((short) Math.max(0, curMdef));
            nEquip.setAcc((short) Math.max(0, curAcc));
            nEquip.setAvoid((short) Math.max(0, curAvoid));
            nEquip.setSpeed((short) Math.max(0, curSpeed));
            nEquip.setJump((short) Math.max(0, curJump));
            nEquip.setHp((short) Math.max(0, curHp));
            nEquip.setMp((short) Math.max(0, curMp));
        } else {
            if (nEquip.getStr() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(nEquip.getStr(), (nEquip.getStr() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setStr(getMaximumShortMaxIfOverflow(0, (nEquip.getStr() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getDex() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(nEquip.getDex(), (nEquip.getDex() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setDex(getMaximumShortMaxIfOverflow(0, (nEquip.getDex() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getInt() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(nEquip.getInt(), (nEquip.getInt() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setInt(getMaximumShortMaxIfOverflow(0, (nEquip.getInt() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getLuk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(nEquip.getLuk(), (nEquip.getLuk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setLuk(getMaximumShortMaxIfOverflow(0, (nEquip.getLuk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(nEquip.getWatk(), (nEquip.getWatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWatk(getMaximumShortMaxIfOverflow(0, (nEquip.getWatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getWdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(nEquip.getWdef(), (nEquip.getWdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setWdef(getMaximumShortMaxIfOverflow(0, (nEquip.getWdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMatk() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(nEquip.getMatk(), (nEquip.getMatk() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMatk(getMaximumShortMaxIfOverflow(0, (nEquip.getMatk() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMdef() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(nEquip.getMdef(), (nEquip.getMdef() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMdef(getMaximumShortMaxIfOverflow(0, (nEquip.getMdef() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAcc() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(nEquip.getAcc(), (nEquip.getAcc() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAcc(getMaximumShortMaxIfOverflow(0, (nEquip.getAcc() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getAvoid() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(nEquip.getAvoid(), (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setAvoid(getMaximumShortMaxIfOverflow(0, (nEquip.getAvoid() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getSpeed() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(nEquip.getSpeed(), (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setSpeed(getMaximumShortMaxIfOverflow(0, (nEquip.getSpeed() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getJump() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(nEquip.getJump(), (nEquip.getJump() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setJump(getMaximumShortMaxIfOverflow(0, (nEquip.getJump() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getHp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(nEquip.getHp(), (nEquip.getHp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setHp(getMaximumShortMaxIfOverflow(0, (nEquip.getHp() + chscrollRandomizedStat(range))));
                }
            }
            if (nEquip.getMp() > 0) {
                if (YamlConfig.config.server.USE_ENHANCED_CHSCROLL) {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(nEquip.getMp(), (nEquip.getMp() + chscrollRandomizedStat(range))));
                } else {
                    nEquip.setMp(getMaximumShortMaxIfOverflow(0, (nEquip.getMp() + chscrollRandomizedStat(range))));
                }
            }
        }
    }

    /*
        Issue with clean slate found thanks to Masterrulax
        Vicious added in the clean slate check thanks to Crypter (CrypterDEV)
    */
    public boolean canUseCleanSlate(Equip nEquip) {
        Map<String, Integer> eqstats = this.getEquipStats(nEquip.getItemId());
        return YamlConfig.config.server.USE_ENHANCED_CLNSLATE || nEquip.getUpgradeSlots() < (byte) (eqstats.get("tuc") + nEquip.getVicious());
    }

    public Item scrollEquipWithId(Item equip, int scrollId, boolean usingWhiteScroll, int vegaItemId, boolean isGM) {
        boolean assertGM = (isGM && YamlConfig.config.server.USE_PERFECT_GM_SCROLL);

        if (equip instanceof Equip) {
            Equip nEquip = (Equip) equip;
            Map<String, Integer> stats = this.getEquipStats(scrollId);

            if (((nEquip.getUpgradeSlots() > 0 || ItemConstants.isCleanSlate(scrollId))) || assertGM) {
                double prop = (double) stats.get("success");

                switch (vegaItemId) {
                    case 5610000:
                        if (prop == 10.0f) {
                            prop = 30.0f;
                        }
                        break;
                    case 5610001:
                        if (prop == 60.0f) {
                            prop = 90.0f;
                        }
                        break;
                    case 2049100:
                        prop = 100.0f;
                        break;
                }

                if (assertGM || rollSuccessChance(prop)) {
                    short flag = nEquip.getFlag();
                    switch (scrollId) {
                        case 2040727:
                            flag |= ItemConstants.SPIKES;
                            nEquip.setFlag((byte) flag);
                            break;
                        case 2041058:
                            flag |= ItemConstants.COLD;
                            nEquip.setFlag((byte) flag);
                            break;
                        case 2049000:
                        case 2049001:
                        case 2049002:
                        case 2049003:
                            if (canUseCleanSlate(nEquip)) {
                                nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                            }
                            break;
                        case 2049100:
                        case 2049101:
                        case 2049102:
                            scrollEquipWithChaos(nEquip, YamlConfig.config.server.CHSCROLL_STAT_RANGE);
                            break;

                        default:
                            improveEquipStats(nEquip, stats);
                            break;
                    }
                    if (!ItemConstants.isCleanSlate(scrollId)) {
                        if (!assertGM && !ItemConstants.isModifierScroll(scrollId)) {   // issue with modifier scrolls taking slots found thanks to Masterrulax, justin, BakaKnyx
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                        }
                        nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                    }
                } else {
                    if (!YamlConfig.config.server.USE_PERFECT_SCROLLING && !usingWhiteScroll && !ItemConstants.isCleanSlate(scrollId) && !assertGM && !ItemConstants.isModifierScroll(scrollId)) {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
                    }
                    if (Randomizer.nextInt(100) < stats.get("cursed")) {
                        return null;
                    }
                }
            }
        }
        return equip;
    }

    public Item getEquipById(int equipId) {
        return getEquipById(equipId, -1);
    }

    private Item getEquipById(int equipId, int ringId) {
        Equip nEquip;
        nEquip = new Equip(equipId, (byte) 0, ringId);
        nEquip.setQuantity((short) 1);
        Map<String, Integer> stats = this.getEquipStats(equipId);
        if (stats != null) {
            for (Entry<String, Integer> stat : stats.entrySet()) {
                if (stat.getKey().equals("STR")) {
                    nEquip.setStr((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("DEX")) {
                    nEquip.setDex((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("INT")) {
                    nEquip.setInt((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("LUK")) {
                    nEquip.setLuk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("PAD")) {
                    nEquip.setWatk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("PDD")) {
                    nEquip.setWdef((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MAD")) {
                    nEquip.setMatk((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MDD")) {
                    nEquip.setMdef((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("ACC")) {
                    nEquip.setAcc((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("EVA")) {
                    nEquip.setAvoid((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("Speed")) {
                    nEquip.setSpeed((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("Jump")) {
                    nEquip.setJump((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MHP")) {
                    nEquip.setHp((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("MMP")) {
                    nEquip.setMp((short) stat.getValue().intValue());
                } else if (stat.getKey().equals("tuc")) {
                    nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
                } else if (isUntradeableRestricted(equipId)) {  // thanks Hyun & Thora for showing an issue with more than only "Untradeable" items being flagged as such here
                    short flag = nEquip.getFlag();
                    flag |= ItemConstants.UNTRADEABLE;
                    nEquip.setFlag(flag);
                } else if (stats.get("fs") > 0) {
                    short flag = nEquip.getFlag();
                    flag |= ItemConstants.SPIKES;
                    nEquip.setFlag(flag);
                    equipCache.put(equipId, nEquip);
                }
            }
        }
        return nEquip.copy();
    }

    public Equip randomizeStats(Equip equip) {
        equip.setStr(getRandStat(equip.getStr(), 5));
        equip.setDex(getRandStat(equip.getDex(), 5));
        equip.setInt(getRandStat(equip.getInt(), 5));
        equip.setLuk(getRandStat(equip.getLuk(), 5));
        equip.setMatk(getRandStat(equip.getMatk(), 5));
        equip.setWatk(getRandStat(equip.getWatk(), 5));
        equip.setAcc(getRandStat(equip.getAcc(), 5));
        equip.setAvoid(getRandStat(equip.getAvoid(), 5));
        equip.setJump(getRandStat(equip.getJump(), 5));
        equip.setSpeed(getRandStat(equip.getSpeed(), 5));
        equip.setWdef(getRandStat(equip.getWdef(), 10));
        equip.setMdef(getRandStat(equip.getMdef(), 10));
        equip.setHp(getRandStat(equip.getHp(), 10));
        equip.setMp(getRandStat(equip.getMp(), 10));
        return equip;
    }

    public Equip randomizeUpgradeStats(Equip equip) {
        equip.setStr(getRandUpgradedStat(equip.getStr(), 2));
        equip.setDex(getRandUpgradedStat(equip.getDex(), 2));
        equip.setInt(getRandUpgradedStat(equip.getInt(), 2));
        equip.setLuk(getRandUpgradedStat(equip.getLuk(), 2));
        equip.setMatk(getRandUpgradedStat(equip.getMatk(), 2));
        equip.setWatk(getRandUpgradedStat(equip.getWatk(), 2));
        equip.setAcc(getRandUpgradedStat(equip.getAcc(), 2));
        equip.setAvoid(getRandUpgradedStat(equip.getAvoid(), 2));
        equip.setJump(getRandUpgradedStat(equip.getJump(), 2));
        equip.setWdef(getRandUpgradedStat(equip.getWdef(), 5));
        equip.setMdef(getRandUpgradedStat(equip.getMdef(), 5));
        equip.setHp(getRandUpgradedStat(equip.getHp(), 5));
        equip.setMp(getRandUpgradedStat(equip.getMp(), 5));
        return equip;
    }

    public MapleStatEffect getItemEffect(int itemId) {
        MapleStatEffect ret = itemEffects.get(itemId);
        if (ret == null) {
            Optional<MapleData> item = getItemData(itemId);
            if (item.isEmpty()) {
                return null;
            }
            MapleData spec = item.get().getChildByPath("specEx");
            if (spec == null) {
                spec = item.get().getChildByPath("spec");
            }
            ret = MapleStatEffect.loadItemEffectFromData(spec, itemId);
            itemEffects.put(itemId, ret);
        }
        return ret;
    }

    public int[][] getSummonMobs(int itemId) {
        Optional<MapleData> data = getItemData(itemId);
        int theInt = data.get().getChildByPath("mob").getChildren().size();
        int[][] mobs2spawn = new int[theInt][2];
        for (int x = 0; x < theInt; x++) {
            mobs2spawn[x][0] = MapleDataTool.getIntConvert("mob/" + x + "/id", data.get());
            mobs2spawn[x][1] = MapleDataTool.getIntConvert("mob/" + x + "/prob", data.get());
        }
        return mobs2spawn;
    }

    public int getWatkForProjectile(int itemId) {
        Integer atk = projectileWatkCache.get(itemId);
        if (atk != null) {
            return atk;
        }
        Optional<MapleData> data = getItemData(itemId);
        if (data.isEmpty()) {
            return 0;
        }

        atk = MapleDataTool.getInt("info/incPAD", data.get(), 0);
        projectileWatkCache.put(itemId, atk);
        return atk;
    }

    public String getName(int itemId) {
        if (nameCache.containsKey(itemId)) {
            return nameCache.get(itemId);
        }
        MapleData strings = getStringData(itemId);
        if (strings == null) {
            return null;
        }
        String ret = MapleDataTool.getString("name", strings, null);
        nameCache.put(itemId, ret);
        return ret;
    }

    public String getMsg(int itemId) {
        if (msgCache.containsKey(itemId)) {
            return msgCache.get(itemId);
        }
        MapleData strings = getStringData(itemId);
        if (strings == null) {
            return null;
        }
        String ret = MapleDataTool.getString("msg", strings, null);
        msgCache.put(itemId, ret);
        return ret;
    }

    public boolean isUntradeableRestricted(int itemId) {
        if (untradeableCache.containsKey(itemId)) {
            return untradeableCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Optional<MapleData> data = getItemData(itemId);
            if (data.isPresent()) {
                bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data.get(), 0) == 1;
            }
        }

        untradeableCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isAccountRestricted(int itemId) {
        if (accountItemRestrictionCache.containsKey(itemId)) {
            return accountItemRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Optional<MapleData> data = getItemData(itemId);
            if (data.isPresent()) {
                bRestricted = MapleDataTool.getIntConvert("info/accountSharable", data.get(), 0) == 1;
            }
        }

        accountItemRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isLootRestricted(int itemId) {
        if (dropRestrictionCache.containsKey(itemId)) {
            return dropRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Optional<MapleData> data = getItemData(itemId);
            if (data.isPresent()) {
                bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data.get(), 0) == 1;
                if (!bRestricted) {
                    bRestricted = isAccountRestricted(itemId);
                }
            }
        }

        dropRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public boolean isDropRestricted(int itemId) {
        return isLootRestricted(itemId) || isQuestItem(itemId);
    }

    public boolean isPickupRestricted(int itemId) {
        if (pickupRestrictionCache.containsKey(itemId)) {
            return pickupRestrictionCache.get(itemId);
        }

        boolean bRestricted = false;
        if (itemId != 0) {
            Optional<MapleData> data = getItemData(itemId);
            if (data.isPresent()) {
                bRestricted = MapleDataTool.getIntConvert("info/only", data.get(), 0) == 1;
            }
        }

        pickupRestrictionCache.put(itemId, bRestricted);
        return bRestricted;
    }

    private Pair<Map<String, Integer>, MapleData> getSkillStatsInternal(int itemId) {
        Map<String, Integer> ret = skillUpgradeCache.get(itemId);
        MapleData retSkill = skillUpgradeInfoCache.get(itemId);

        if (ret != null) {
            return new Pair<>(ret, retSkill);
        }

        retSkill = null;
        ret = new LinkedHashMap<>();
        Optional<MapleData> item = getItemData(itemId);
        if (item.isPresent()) {
            MapleData info = item.get().getChildByPath("info");
            if (info != null) {
                for (MapleData data : info.getChildren()) {
                    if (data.getName().startsWith("inc")) {
                        ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
                    }
                }
                ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
                ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
                ret.put("success", MapleDataTool.getInt("success", info, 0));

                retSkill = info.getChildByPath("skill");
            }
        }

        skillUpgradeCache.put(itemId, ret);
        skillUpgradeInfoCache.put(itemId, retSkill);
        return new Pair<>(ret, retSkill);
    }

    public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
        Pair<Map<String, Integer>, MapleData> retData = getSkillStatsInternal(itemId);
        if (retData.getLeft().isEmpty()) {
            return null;
        }

        Map<String, Integer> ret = new LinkedHashMap<>(retData.getLeft());
        MapleData skill = retData.getRight();
        int curskill;
        for (int i = 0; i < skill.getChildren().size(); i++) {
            curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
            if (curskill == 0) {
                break;
            }
            if (curskill / 10000 == playerJob) {
                ret.put("skillid", curskill);
                break;
            }
        }
        ret.putIfAbsent("skillid", 0);
        return ret;
    }

    public Pair<Integer, Boolean> canPetConsume(Integer petId, Integer itemId) {
        Pair<Integer, Set<Integer>> foodData = cashPetFoodCache.get(itemId);

        if (foodData == null) {
            Set<Integer> pets = new HashSet<>(4);
            int inc = 1;

            Optional<MapleData> data = getItemData(itemId);
            if (data.isPresent()) {
                MapleData specData = data.get().getChildByPath("spec");
                for (MapleData specItem : specData.getChildren()) {
                    String itemName = specItem.getName();

                    try {
                        Integer.parseInt(itemName); // check if it's a petid node

                        Integer petid = MapleDataTool.getInt(specItem, 0);
                        pets.add(petid);
                    } catch (NumberFormatException npe) {
                        if (itemName.contentEquals("inc")) {
                            inc = MapleDataTool.getInt(specItem, 1);
                        }
                    }
                }
            }

            foodData = new Pair<>(inc, pets);
            cashPetFoodCache.put(itemId, foodData);
        }

        return new Pair<>(foodData.getLeft(), foodData.getRight().contains(petId));
    }

    public boolean isQuestItem(int itemId) {
        if (isQuestItemCache.containsKey(itemId)) {
            return isQuestItemCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        boolean questItem = data.map(d -> MapleDataTool.getIntConvert("info/quest", d, 0) == 1).orElse(false);
        isQuestItemCache.put(itemId, questItem);
        return questItem;
    }

    public boolean isPartyQuestItem(int itemId) {
        if (isPartyQuestItemCache.containsKey(itemId)) {
            return isPartyQuestItemCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        boolean partyquestItem = data.map(d -> MapleDataTool.getIntConvert("info/pquest", d, 0) == 1).orElse(false);
        isPartyQuestItemCache.put(itemId, partyquestItem);
        return partyquestItem;
    }

    private void loadCardIdData() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT cardid, mobid FROM monstercarddata");
            rs = ps.executeQuery();
            while (rs.next()) {
                monsterBookID.put(rs.getInt(1), rs.getInt(2));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (con != null && !con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public int getCardMobId(int id) {
        return monsterBookID.get(id);
    }

    public boolean isUntradeableOnEquip(int itemId) {
        if (onEquipUntradeableCache.containsKey(itemId)) {
            return onEquipUntradeableCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        boolean untradeableOnEquip = data.map(d -> MapleDataTool.getIntConvert("info/equipTradeBlock", d, 0) > 0).orElse(false);
        onEquipUntradeableCache.put(itemId, untradeableOnEquip);
        return untradeableOnEquip;
    }

    public Optional<ScriptedItem> getScriptedItemInfo(int itemId) {
        if (scriptedItemCache.containsKey(itemId)) {
            return Optional.of(scriptedItemCache.get(itemId));
        }
        if ((itemId / 10000) != 243) {
            return Optional.empty();
        }
        Optional<MapleData> itemInfo = getItemData(itemId);
        ScriptedItem script = new ScriptedItem(MapleDataTool.getInt("spec/npc", itemInfo.get(), 0),
                MapleDataTool.getString("spec/script", itemInfo.get(), ""),
                MapleDataTool.getInt("spec/runOnPickup", itemInfo.get(), 0) == 1);
        scriptedItemCache.put(itemId, script);
        return Optional.of(scriptedItemCache.get(itemId));
    }

    public boolean isKarmaAble(int itemId) {
        if (karmaCache.containsKey(itemId)) {
            return karmaCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        boolean bRestricted = data.map(d -> MapleDataTool.getIntConvert("info/tradeAvailable", d, 0) > 0).orElse(false);
        karmaCache.put(itemId, bRestricted);
        return bRestricted;
    }

    public int getStateChangeItem(int itemId) {
        if (triggerItemCache.containsKey(itemId)) {
            return triggerItemCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int triggerItem = data.map(d -> MapleDataTool.getIntConvert("info/stateChangeItem", d, 0)).orElse(0);
        triggerItemCache.put(itemId, triggerItem);
        return triggerItem;
    }

    public int getCreateItem(int itemId) {
        if (createItem.containsKey(itemId)) {
            return createItem.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int itemFrom = data.map(d -> MapleDataTool.getIntConvert("info/create", d, 0)).orElse(0);
        createItem.put(itemId, itemFrom);
        return itemFrom;
    }

    public int getMobItem(int itemId) {
        if (mobItem.containsKey(itemId)) {
            return mobItem.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int mobItemCatch = data.map(d -> MapleDataTool.getIntConvert("info/mob", d, 0)).orElse(0);
        mobItem.put(itemId, mobItemCatch);
        return mobItemCatch;
    }

    public int getUseDelay(int itemId) {
        if (useDelay.containsKey(itemId)) {
            return useDelay.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int mobUseDelay = data.map(d -> MapleDataTool.getIntConvert("info/useDelay", d, 0)).orElse(0);
        useDelay.put(itemId, mobUseDelay);
        return mobUseDelay;
    }

    public int getMobHP(int itemId) {
        if (mobHP.containsKey(itemId)) {
            return mobHP.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int mobHPItem = data.map(d -> MapleDataTool.getIntConvert("info/mobHP", d, 0)).orElse(0);
        mobHP.put(itemId, mobHPItem);
        return mobHPItem;
    }

    public int getExpById(int itemId) {
        if (expCache.containsKey(itemId)) {
            return expCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int exp = data.map(d -> MapleDataTool.getIntConvert("spec/exp", d, 0)).orElse(0);
        expCache.put(itemId, exp);
        return exp;
    }

    public int getMaxLevelById(int itemId) {
        if (levelCache.containsKey(itemId)) {
            return levelCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        int level = data.map(d -> MapleDataTool.getIntConvert("info/maxLevel", d, 256)).orElse(0);
        levelCache.put(itemId, level);
        return level;
    }

    public Pair<Integer, List<RewardItem>> getItemReward(int itemId) {
        if (rewardCache.containsKey(itemId)) {
            return rewardCache.get(itemId);
        }
        int totalprob = 0;
        List<RewardItem> rewards = new ArrayList<>();
        for (MapleData child : getItemData(itemId).map(d -> d.getChildByPath("reward")).map(MapleData::getChildren).orElse(Collections.emptyList())) {
            int itemId1 = MapleDataTool.getInt("item", child, 0);
            short probability = (byte) MapleDataTool.getInt("prob", child, 0);
            short quantity = (short) MapleDataTool.getInt("count", child, 0);
            String effect = MapleDataTool.getString("Effect", child, "");
            String worldMessage = MapleDataTool.getString("worldMsg", child, null);
            int period = MapleDataTool.getInt("period", child, -1);
            RewardItem reward = new RewardItem(itemId1, period, probability, quantity, effect, worldMessage);
            totalprob += reward.probability();
            rewards.add(reward);
        }
        Pair<Integer, List<RewardItem>> hmm = new Pair<>(totalprob, rewards);
        rewardCache.put(itemId, hmm);
        return hmm;
    }

    public boolean isConsumeOnPickup(int itemId) {
        if (consumeOnPickupCache.containsKey(itemId)) {
            return consumeOnPickupCache.get(itemId);
        }
        Optional<MapleData> data = getItemData(itemId);
        boolean consume = MapleDataTool.getIntConvert("spec/consumeOnPickup", data.get(), 0) == 1 || MapleDataTool.getIntConvert("specEx/consumeOnPickup", data.get(), 0) == 1;
        consumeOnPickupCache.put(itemId, consume);
        return consume;
    }

    public final boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case GENERAL2H_SWING:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM_SWING:
            case SPEAR_STAB:
            case SWORD2H:
            case GUN:
            case KNUCKLE:
                return true;
            default:
                return false;
        }
    }

    public boolean isCash(int itemId) {
        int itemType = itemId / 1000000;
        if (itemType == 5) {
            return true;
        }
        if (itemType != 1) {
            return false;
        }

        Map<String, Integer> eqpStats = getEquipStats(itemId);
        return eqpStats != null && eqpStats.get("cash") == 1;
    }

    public boolean isUpgradeable(int itemId) {
        Item it = this.getEquipById(itemId);
        Equip eq = (Equip) it;

        return (eq.getUpgradeSlots() > 0 || eq.getStr() > 0 || eq.getDex() > 0 || eq.getInt() > 0 || eq.getLuk() > 0 ||
                eq.getWatk() > 0 || eq.getMatk() > 0 || eq.getWdef() > 0 || eq.getMdef() > 0 || eq.getAcc() > 0 ||
                eq.getAvoid() > 0 || eq.getSpeed() > 0 || eq.getJump() > 0 || eq.getHp() > 0 || eq.getMp() > 0);
    }

    public boolean isUnmerchable(int itemId) {
        if (YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_CASH && isCash(itemId)) {
            return true;
        }

        if (YamlConfig.config.server.USE_ENFORCE_UNMERCHABLE_PET && ItemConstants.isPet(itemId)) {
            return true;
        }

        return false;
    }

    public Collection<Item> canWearEquipment(MapleCharacter chr, Collection<Item> items) {
        MapleInventory inv = chr.getInventory(MapleInventoryType.EQUIPPED);
        if (inv.checked()) {
            return items;
        }
        Collection<Item> itemz = new LinkedList<>();
        if (chr.getJob() == MapleJob.SUPERGM || chr.getJob() == MapleJob.GM) {
            for (Item item : items) {
                Equip equip = (Equip) item;
                equip.wear(true);
                itemz.add(item);
            }
            return itemz;
        }
        boolean highfivestamp = false;
        /* Removed because players shouldn't even get this, and gm's should just be gm job.
         try {
         for (Pair<Item, MapleInventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) {
         if (ii.getRight() == MapleInventoryType.CASH) {
         if (ii.getLeft().getItemId() == 5590000) {
         highfivestamp = true;
         }
         }
         }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }*/
        int tdex = chr.getDex(), tstr = chr.getStr(), tint = chr.getInt(), tluk = chr.getLuk(), fame = chr.getFame();
        if (chr.getJob() != MapleJob.SUPERGM || chr.getJob() != MapleJob.GM) {
            for (Item item : inv.list()) {
                Equip equip = (Equip) item;
                tdex += equip.getDex();
                tstr += equip.getStr();
                tluk += equip.getLuk();
                tint += equip.getInt();
            }
        }
        for (Item item : items) {
            Equip equip = (Equip) item;
            int reqLevel = getEquipLevelReq(equip.getItemId());
            if (highfivestamp) {
                reqLevel -= 5;
                if (reqLevel < 0) {
                    reqLevel = 0;
                }
            }
            /*
             int reqJob = getEquipStats(equip.getItemId()).get("reqJob");
             if (reqJob != 0) {
             Really hard check, and not really needed in this one
             Gm's should just be GM job, and players cannot change jobs.
             }*/
            if (reqLevel > chr.getLevel()) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqDEX") > tdex) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqSTR") > tstr) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqLUK") > tluk) {
                continue;
            } else if (getEquipStats(equip.getItemId()).get("reqINT") > tint) {
                continue;
            }
            int reqPOP = getEquipStats(equip.getItemId()).get("reqPOP");
            if (reqPOP > 0) {
                if (getEquipStats(equip.getItemId()).get("reqPOP") > fame) {
                    continue;
                }
            }
            equip.wear(true);
            itemz.add(equip);
        }
        inv.checked(true);
        return itemz;
    }

    public boolean canWearEquipment(MapleCharacter chr, Equip equip, int dst) {
        int id = equip.getItemId();

        if (ItemConstants.isWeddingRing(id) && chr.hasJustMarried()) {
            chr.dropMessage(5, "The Wedding Ring cannot be equipped on this map.");  // will dc everyone due to doubled couple effect
            return false;
        }

        String islot = getEquipmentSlot(id);
        if (!EquipSlot.getFromTextSlot(islot).isAllowed(dst, isCash(id))) {
            equip.wear(false);
            String itemName = ItemInformationProvider.getInstance().getName(equip.getItemId());
            Server.getInstance().broadcastGMMessage(chr.getWorld(), MaplePacketCreator.sendYellowTip("[Warning]: " + chr.getName() + " tried to equip " + itemName + " into slot " + dst + "."));
            AutobanFactory.PACKET_EDIT.alert(chr, chr.getName() + " tried to forcibly equip an item.");
            FilePrinter.printError(FilePrinter.EXPLOITS + chr.getName() + ".txt", chr.getName() + " tried to equip " + itemName + " into " + dst + " slot.");
            return false;
        }

        if (chr.getJob() == MapleJob.SUPERGM || chr.getJob() == MapleJob.GM) {
            equip.wear(true);
            return true;
        }


        boolean highfivestamp = false;
        /* Removed check above for message ><
         try {
         for (Pair<Item, MapleInventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) {
         if (ii.getRight() == MapleInventoryType.CASH) {
         if (ii.getLeft().getItemId() == 5590000) {
         highfivestamp = true;
         }
         }
         }
         } catch (SQLException ex) {
            ex.printStackTrace();
         }*/

        int reqLevel = getEquipLevelReq(equip.getItemId());
        if (highfivestamp) {
            reqLevel -= 5;
        }
        int i = 0; //lol xD
        //Removed job check. Shouldn't really be needed.
        if (reqLevel > chr.getLevel()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqDEX") > chr.getTotalDex()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqSTR") > chr.getTotalStr()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqLUK") > chr.getTotalLuk()) {
            i++;
        } else if (getEquipStats(equip.getItemId()).get("reqINT") > chr.getTotalInt()) {
            i++;
        }
        int reqPOP = getEquipStats(equip.getItemId()).get("reqPOP");
        if (reqPOP > 0) {
            if (getEquipStats(equip.getItemId()).get("reqPOP") > chr.getFame()) {
                i++;
            }
        }

        if (i > 0) {
            equip.wear(false);
            return false;
        }
        equip.wear(true);
        return true;
    }

    public ArrayList<Pair<Integer, String>> getItemDataByName(String name) {
        ArrayList<Pair<Integer, String>> ret = new ArrayList<>();
        for (Pair<Integer, String> itemPair : ItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair.getRight().toLowerCase().contains(name.toLowerCase())) {
                ret.add(itemPair);
            }
        }
        return ret;
    }

    private MapleData getEquipLevelInfo(int itemId) {
        MapleData equipLevelData = equipLevelInfoCache.get(itemId);
        if (equipLevelData == null) {
            if (equipLevelInfoCache.containsKey(itemId)) {
                return null;
            }

            Optional<MapleData> iData = getItemData(itemId);
            if (iData.isPresent()) {
                MapleData data = iData.get().getChildByPath("info/level");
                if (data != null) {
                    equipLevelData = data.getChildByPath("info");
                }
            }

            equipLevelInfoCache.put(itemId, equipLevelData);
        }

        return equipLevelData;
    }

    public int getEquipLevel(int itemId, boolean getMaxLevel) {
        Integer eqLevel = equipMaxLevelCache.get(itemId);
        if (eqLevel == null) {
            eqLevel = 1;    // greater than 1 means that it was supposed to levelup on GMS

            MapleData data = getEquipLevelInfo(itemId);
            if (data != null) {
                if (getMaxLevel) {
                    int curLevel = 1;

                    while (true) {
                        MapleData data2 = data.getChildByPath(Integer.toString(curLevel));
                        if (data2 == null || data2.getChildren().size() <= 1) {
                            eqLevel = curLevel;
                            equipMaxLevelCache.put(itemId, eqLevel);
                            break;
                        }

                        curLevel++;
                    }
                } else {
                    MapleData data2 = data.getChildByPath("1");
                    if (data2 != null && data2.getChildren().size() > 1) {
                        eqLevel = 2;
                    }
                }
            }
        }

        return eqLevel;
    }

    public List<Pair<String, Integer>> getItemLevelupStats(int itemId, int level) {
        List<Pair<String, Integer>> list = new LinkedList<>();
        MapleData data = getEquipLevelInfo(itemId);
        if (data != null) {
            MapleData data2 = data.getChildByPath(Integer.toString(level));
            if (data2 != null) {
                for (MapleData da : data2.getChildren()) {
                    if (Math.random() < 0.9) {
                        if (da.getName().startsWith("incDEXMin")) {
                            list.add(new Pair<>("incDEX", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incDEXMax")))));
                        } else if (da.getName().startsWith("incSTRMin")) {
                            list.add(new Pair<>("incSTR", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incSTRMax")))));
                        } else if (da.getName().startsWith("incINTMin")) {
                            list.add(new Pair<>("incINT", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incINTMax")))));
                        } else if (da.getName().startsWith("incLUKMin")) {
                            list.add(new Pair<>("incLUK", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incLUKMax")))));
                        } else if (da.getName().startsWith("incMHPMin")) {
                            list.add(new Pair<>("incMHP", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMHPMax")))));
                        } else if (da.getName().startsWith("incMMPMin")) {
                            list.add(new Pair<>("incMMP", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMMPMax")))));
                        } else if (da.getName().startsWith("incPADMin")) {
                            list.add(new Pair<>("incPAD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incPADMax")))));
                        } else if (da.getName().startsWith("incMADMin")) {
                            list.add(new Pair<>("incMAD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMADMax")))));
                        } else if (da.getName().startsWith("incPDDMin")) {
                            list.add(new Pair<>("incPDD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incPDDMax")))));
                        } else if (da.getName().startsWith("incMDDMin")) {
                            list.add(new Pair<>("incMDD", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incMDDMax")))));
                        } else if (da.getName().startsWith("incACCMin")) {
                            list.add(new Pair<>("incACC", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incACCMax")))));
                        } else if (da.getName().startsWith("incEVAMin")) {
                            list.add(new Pair<>("incEVA", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incEVAMax")))));
                        } else if (da.getName().startsWith("incSpeedMin")) {
                            list.add(new Pair<>("incSpeed", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incSpeedMax")))));
                        } else if (da.getName().startsWith("incJumpMin")) {
                            list.add(new Pair<>("incJump", Randomizer.rand(MapleDataTool.getInt(da), MapleDataTool.getInt(data2.getChildByPath("incJumpMax")))));
                        }
                    }
                }
            }
        }

        return list;
    }

    public Pair<String, Integer> getMakerReagentStatUpgrade(int itemId) {
        try {
            Pair<String, Integer> statUpgd = statUpgradeMakerCache.get(itemId);
            if (statUpgd != null) {
                return statUpgd;
            } else if (statUpgradeMakerCache.containsKey(itemId)) {
                return null;
            }

            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT stat, value FROM makerreagentdata WHERE itemid = ?");
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String statType = rs.getString("stat");
                int statGain = rs.getInt("value");

                statUpgd = new Pair<>(statType, statGain);
            }
            rs.close();
            ps.close();
            con.close();

            statUpgradeMakerCache.put(itemId, statUpgd);
            return statUpgd;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getMakerCrystalFromLeftover(Integer leftoverId) {
        try {
            Integer itemid = mobCrystalMakerCache.get(leftoverId);
            if (itemid != null) {
                return itemid;
            }

            itemid = -1;

            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data WHERE itemid = ? ORDER BY dropperid;");
            ps.setInt(1, leftoverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dropperid = rs.getInt("dropperid");
                itemid = getCrystalForLevel(MapleLifeFactory.getMonsterLevel(dropperid) - 1);
            }
            rs.close();
            ps.close();
            con.close();

            mobCrystalMakerCache.put(leftoverId, itemid);
            return itemid;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public MakerItemCreateEntry getMakerItemEntry(int toCreate) {
        MakerItemCreateEntry makerEntry;

        if ((makerEntry = makerItemCache.get(toCreate)) != null) {
            return new MakerItemCreateEntry(makerEntry);
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT req_level, req_maker_level, req_meso, quantity FROM makercreatedata WHERE itemid = ?");
                ps.setInt(1, toCreate);
                ResultSet rs = ps.executeQuery();
                int reqLevel = -1;
                int reqMakerLevel = -1;
                int cost = -1;
                int toGive = -1;
                if (rs.next()) {
                    reqLevel = rs.getInt("req_level");
                    reqMakerLevel = rs.getInt("req_maker_level");
                    cost = rs.getInt("req_meso");
                    toGive = rs.getInt("quantity");
                }
                ps.close();
                rs.close();
                makerEntry = new MakerItemCreateEntry(cost, reqLevel, reqMakerLevel);
                makerEntry.addGainItem(toCreate, toGive);
                ps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ?");
                ps.setInt(1, toCreate);
                rs = ps.executeQuery();
                while (rs.next()) {
                    makerEntry.addReqItem(rs.getInt("req_item"), rs.getInt("count"));
                }
                rs.close();
                ps.close();
                con.close();
                makerItemCache.put(toCreate, new MakerItemCreateEntry(makerEntry));
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                makerEntry = null;
            }
        }

        return makerEntry;
    }

    public int getMakerCrystalFromEquip(Integer equipId) {
        try {
            return getCrystalForLevel(getEquipLevelReq(equipId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public int getMakerStimulantFromEquip(Integer equipId) {
        try {
            return getCrystalForLevel(getEquipLevelReq(equipId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public List<Pair<Integer, Integer>> getMakerDisassembledItems(Integer itemId) {
        List<Pair<Integer, Integer>> items = new LinkedList<>();

        Connection con;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ? AND req_item >= 4260000 AND req_item < 4270000");
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                items.add(new Pair<>(rs.getInt("req_item"), rs.getInt("count") / 2));   // return to the player half of the crystals needed
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public int getMakerDisassembledFee(Integer itemId) {
        int fee = -1;
        Connection con;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT req_meso FROM makercreatedata WHERE itemid = ?");
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {   // cost is 13.6363~ % of the original value, trim by 1000.
                float val = (float) (rs.getInt("req_meso") * 0.13636363636364);
                fee = (int) (val / 1000);
                fee *= 1000;
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fee;
    }

    public int getMakerStimulant(int itemId) {  // thanks to Arnah
        Integer itemid = makerCatalystCache.get(itemId);
        if (itemid != null) {
            return itemid;
        }

        itemid = -1;
        for (MapleData md : etcData.getData("ItemMake.img").getChildren()) {
            MapleData me = md.getChildByPath(StringUtil.getLeftPaddedStr(Integer.toString(itemId), '0', 8));

            if (me != null) {
                itemid = MapleDataTool.getInt(me.getChildByPath("catalyst"), -1);
                break;
            }
        }

        makerCatalystCache.put(itemId, itemid);
        return itemid;
    }

    public Set<String> getWhoDrops(Integer itemId) {
        Set<String> list = new HashSet<>();
        Connection con;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data WHERE itemid = ? LIMIT 50");
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String resultName = MapleMonsterInformationProvider.getInstance().getMobNameFromId(rs.getInt("dropperid"));
                if (!resultName.isEmpty()) {
                    list.add(resultName);
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private boolean canUseSkillBook(MapleCharacter player, Integer skillBookId) {
        Map<String, Integer> skillData = getSkillStats(skillBookId, player.getJob().getId());
        if (skillData == null || skillData.get("skillid") == 0) {
            return false;
        }
        Optional<Skill> skill = SkillFactory.getSkill(skillData.get("skillid"));
        return skill.filter(value -> (skillData.get("skillid") != 0 && ((player.getSkillLevel(value) >= skillData.get("reqSkillLevel") || skillData.get("reqSkillLevel") == 0) && player.getMasterLevel(value) < skillData.get("masterLevel")))).isPresent();

    }

    public List<Integer> usableMasteryBooks(MapleCharacter player) {
        List<Integer> masterybook = new LinkedList<>();
        for (Integer i = 2290000; i <= 2290139; i++) {
            if (canUseSkillBook(player, i)) {
                masterybook.add(i);
            }
        }

        return masterybook;
    }

    public List<Integer> usableSkillBooks(MapleCharacter player) {
        List<Integer> skillbook = new LinkedList<>();
        for (Integer i = 2280000; i <= 2280019; i++) {
            if (canUseSkillBook(player, i)) {
                skillbook.add(i);
            }
        }

        return skillbook;
    }

    public final Optional<QuestConsumableItem> getQuestConsumablesInfo(final int itemId) {
        if (questItemConsCache.containsKey(itemId)) {
            return Optional.ofNullable(questItemConsCache.get(itemId));
        }
        Optional<MapleData> data = getItemData(itemId);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        QuestConsumableItem qcItem = null;

        MapleData infoData = data.get().getChildByPath("info");
        if (infoData.getChildByPath("uiData") != null) {
            int experience = MapleDataTool.getInt("exp", infoData);
            int grade = MapleDataTool.getInt("grade", infoData);
            int questId = MapleDataTool.getInt("questId", infoData);

            Map<Integer, Integer> items = new HashMap<>(2);
            MapleData ciData = infoData.getChildByPath("consumeItem");
            if (ciData != null) {
                for (MapleData ciItem : ciData.getChildren()) {
                    int consumeItemId = MapleDataTool.getInt("0", ciItem);
                    int quantity = MapleDataTool.getInt("1", ciItem);

                    items.put(consumeItemId, quantity);
                }
            }
            qcItem = new QuestConsumableItem(questId, grade, experience, items);
        }

        questItemConsCache.put(itemId, qcItem);
        return Optional.ofNullable(qcItem);
    }
}
