package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteResult;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.coordinator.world.MapleInviteCoordinator.MapleInviteResult;
import net.server.world.MapleMessenger;
import net.server.world.MapleMessengerCharacter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Optional;

public final class MessengerHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.tryacquireClient()) {
            try {
                String input;
                byte mode = slea.readByte();
                switch (mode) {
                    case 0x00 -> {
                        int messengerId = slea.readInt();
                        answerInvite(c.getPlayer(), messengerId);
                    }
                    case 0x02 -> closeMessenger(c.getPlayer());
                    case 0x03 -> {
                        String name = slea.readMapleAsciiString();
                        inviteToMessenger(c.getPlayer(), name);
                    }
                    case 0x05 -> {
                        String targeted = slea.readMapleAsciiString();
                        declineChat(c.getPlayer(), targeted);
                    }
                    case 0x06 -> {
                        input = slea.readMapleAsciiString();
                        chat(c.getPlayer(), input);
                    }
                }
            } finally {
                c.releaseClient();
            }
        }
    }

    private static void closeMessenger(MapleCharacter c) {
        c.closePlayerMessenger();
    }

    private static void answerInvite(MapleCharacter c, int messengerId) {
        Optional<MapleMessenger> messenger = c.getMessenger();
        if (messenger.isPresent()) {
            MapleInviteCoordinator.answerInvite(InviteType.MESSENGER, c.getId(), messengerId, false);
            return;
        }

        if (messengerId == 0) {
            MapleInviteCoordinator.removeInvite(InviteType.MESSENGER, c.getId());
            messenger = Optional.of(c.getWorldServer().createMessenger(c.getId(), c.getName(), c.getClient().getChannel()));
            c.setMessenger(messenger.get());
            c.setMessengerPosition(messenger.get().getPositionByName(c.getName()));
            return;
        }

        messenger = c.getWorldServer().getMessenger(messengerId);
        if (messenger.isEmpty()) {
            return;
        }

        MapleInviteResult inviteRes = MapleInviteCoordinator.answerInvite(InviteType.MESSENGER, c.getId(), messengerId, true);
        InviteResult res = inviteRes.result;
        if (res != InviteResult.ACCEPTED) {
            c.message("Could not verify your Maple Messenger accept since the invitation rescinded.");
            return;
        }

        if (messenger.get().getMembers().size() < 3) {
            c.getWorldServer().joinMessenger(messenger.get().getId(), c.getId(), c.getName(), c.getName(), c.getClient().getChannel());
            c.setMessenger(messenger.get());
            c.setMessengerPosition(messenger.get().getPositionByName(c.getName()));
        }
    }

    private static void inviteToMessenger(MapleCharacter c, String name) {
        Optional<MapleMessenger> messenger = c.getMessenger();

        if (messenger.isEmpty()) {
            c.announce(MaplePacketCreator.messengerChat(c.getName() + " : This Maple Messenger is currently unavailable. Please quit this chat."));
            return;
        }

        if (messenger.get().getMembers().size() >= 3) {
            c.announce(MaplePacketCreator.messengerChat(c.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
            return;
        }

        Optional<MapleCharacter> target = c.getClient().getChannelServer().getPlayerStorage().getCharacterByName(name);
        if (target.isEmpty()) {
            if (c.getWorldServer().find(name) > -1) {
                c.getWorldServer().messengerInvite(c.getName(), messenger.get().getId(), name, c.getClient().getChannel());
                return;
            }
            c.announce(MaplePacketCreator.messengerNote(name, 4, 0));
            return;
        }

        if (target.get().getMessenger().isPresent()) {
            c.announce(MaplePacketCreator.messengerChat(c.getName() + " : " + name + " is already using Maple Messenger"));
            return;
        }

        if (!MapleInviteCoordinator.createInvite(InviteType.MESSENGER, c, messenger.get().getId(), target.get().getId())) {
            c.announce(MaplePacketCreator.messengerChat(c.getName() + " : " + name + " is already managing a Maple Messenger invitation"));
            return;
        }

        target.get().announce(MaplePacketCreator.messengerInvite(c.getName(), messenger.get().getId()));
        c.announce(MaplePacketCreator.messengerNote(name, 4, 1));
    }

    private static void chat(MapleCharacter character, String input) {
        character.getMessenger().ifPresent(m -> character.getWorldServer().messengerChat(m, input, character.getName()));
    }

    private static void declineChat(MapleCharacter character, String targeted) {
        character.getWorldServer().declineChat(targeted, character);
    }
}
