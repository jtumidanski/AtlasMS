package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MaplePet;
import client.inventory.PetCommand;
import client.inventory.PetDataFactory;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PetCommandHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        int petId = slea.readInt();
        byte petIndex = chr.getPetIndex(petId);
        MaplePet pet;
        if (petIndex == -1) {
            return;
        } else {
            pet = chr.getPet(petIndex);
        }
        slea.readInt();
        slea.readByte();
        byte command = slea.readByte();
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getItemId(), command);
        if (Randomizer.nextInt(100) < petCommand.probability()) {
            pet.gainClosenessFullness(chr, petCommand.increase(), 0, command);
            chr.getMap().broadcastMessage(MaplePacketCreator.commandResponse(chr.getId(), petIndex, false, command, false));
        } else {
            chr.getMap().broadcastMessage(MaplePacketCreator.commandResponse(chr.getId(), petIndex, true, command, false));
        }
    }
}
