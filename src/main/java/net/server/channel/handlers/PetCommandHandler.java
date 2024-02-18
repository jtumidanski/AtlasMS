package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import client.inventory.PetCommand;
import client.inventory.PetDataFactory;
import connection.packets.CPet;
import net.AbstractMaplePacketHandler;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class PetCommandHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        int petId = slea.readInt();
        byte petIndex = chr.getPetIndex(petId);
        if (petIndex == -1) {
            return;
        }

        Optional<MaplePet> pet = chr.getPet(petIndex);
        if (pet.isEmpty()) {
            return;
        }

        slea.readInt();
        slea.readByte();
        byte command = slea.readByte();
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.get().getItemId(), command);
        if (Randomizer.nextInt(100) < petCommand.probability()) {
            pet.get().gainClosenessFullness(chr, petCommand.increase(), 0, command);
            chr.getMap().broadcastMessage(CPet.commandResponse(chr.getId(), petIndex, false, command, false));
        } else {
            chr.getMap().broadcastMessage(CPet.commandResponse(chr.getId(), petIndex, true, command, false));
        }
    }
}
