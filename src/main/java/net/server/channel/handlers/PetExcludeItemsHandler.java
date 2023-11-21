package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class PetExcludeItemsHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        final int petId = slea.readInt();
        slea.skip(4);

        MapleCharacter chr = c.getPlayer();
        byte petIndex = chr.getPetIndex(petId);
        if (petIndex < 0) {
            return;
        }

        final Optional<MaplePet> pet = chr.getPet(petIndex);
        if (pet.isEmpty()) {
            return;
        }

        chr.resetExcluded(petId);
        byte amount = slea.readByte();
        for (int i = 0; i < amount; i++) {
            chr.addExcluded(petId, slea.readInt());
        }
        chr.commitExcludedItems();
    }
}
