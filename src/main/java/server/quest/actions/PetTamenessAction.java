package server.quest.actions;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import provider.MapleData;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;

public class PetTamenessAction extends MapleQuestAction {
    int tameness;

    public PetTamenessAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.PETTAMENESS, quest);
        questID = quest.getId();
        processData(data);
    }


    @Override
    public void processData(MapleData data) {
        tameness = MapleDataTool.getInt(data);
    }

    @Override
    public void run(MapleCharacter chr, Integer extSelection) {
        MapleClient c = chr.getClient();

        c.lockClient();
        try {
            // assuming here only the pet leader will gain tameness
            chr.getPet(0).ifPresent(p -> p.gainClosenessFullness(chr, tameness, 0, 0));
        } finally {
            c.unlockClient();
        }
    }
} 
