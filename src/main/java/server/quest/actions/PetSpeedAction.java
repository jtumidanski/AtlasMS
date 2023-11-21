package server.quest.actions;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import provider.MapleData;
import server.quest.MapleQuest;
import server.quest.MapleQuestActionType;

public class PetSpeedAction extends MapleQuestAction {

    public PetSpeedAction(MapleQuest quest, MapleData data) {
        super(MapleQuestActionType.PETTAMENESS, quest);
        questID = quest.getId();
    }


    @Override
    public void processData(MapleData data) {
    }

    @Override
    public void run(MapleCharacter chr, Integer extSelection) {
        MapleClient c = chr.getClient();

        c.lockClient();
        try {
            // assuming here only the pet leader will gain owner speed
            chr.getPet(0).ifPresent(p -> p.addPetFlag(c.getPlayer(), MaplePet.PetFlag.OWNER_SPEED));
        } finally {
            c.unlockClient();
        }

    }
} 
