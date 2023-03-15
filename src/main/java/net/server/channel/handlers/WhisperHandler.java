package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.autoban.AutobanFactory;
import config.YamlConfig;
import net.AbstractMaplePacketHandler;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.LogHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class WhisperHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 6) {
            String recipient = slea.readMapleAsciiString();
            String text = slea.readMapleAsciiString();
            handleWhisper(c, recipient, text);
        } else if (mode == 5) {
            String recipient = slea.readMapleAsciiString();
            handleFind(c, recipient);
        } else if (mode == 0x44) {
            String recipient = slea.readMapleAsciiString();
            handleBuddyFind(c, recipient);
        }
    }

    private static void handleBuddyFind(MapleClient c, String recipient) {
        MapleCharacter player = c.getWorldServer().getPlayerStorage().getCharacterByName(recipient).orElse(null);
        if (player == null || c.getPlayer().gmLevel() < player.gmLevel()) {
            return;
        }

        if (player.getCashShop().isOpened()) {  // in CashShop
            c.announce(MaplePacketCreator.getBuddyFindReply(player.getName(), -1, 2));
            return;
        }

        if (player.isAwayFromWorld()) {  // in MTS
            c.announce(MaplePacketCreator.getBuddyFindReply(player.getName(), -1, 0));
            return;
        }

        if (player.getClient().getChannel() != c.getChannel()) { // in another channel
            c.announce(MaplePacketCreator.getBuddyFindReply(player.getName(), player.getClient().getChannel() - 1, 3));
            return;
        }

        c.announce(MaplePacketCreator.getBuddyFindReply(player.getName(), player.getMap().getId(), 1));
    }

    private static void handleFind(MapleClient c, String recipient) {
        Optional<MapleCharacter> target = c.getWorldServer().getPlayerStorage().getCharacterByName(recipient);
        if (target.isPresent() && c.getPlayer().gmLevel() >= target.map(MapleCharacter::gmLevel).orElse(Integer.MAX_VALUE)) {
            handleFind(c, target.get());
            return;
        }

        if (!c.getPlayer().isGM()) {
            c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
            return;
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT gm FROM characters WHERE name = ?");
            ps.setString(1, recipient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gm") >= c.getPlayer().gmLevel()) {
                    c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    return;
                }
            }
            rs.close();
            ps.close();
            con.close();
            byte channel = (byte) (c.getWorldServer().find(recipient) - 1);
            if (channel > -1) {
                c.announce(MaplePacketCreator.getFindReply(recipient, channel, 3));
            } else {
                c.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void handleFind(MapleClient c, MapleCharacter target) {
        if (target.getCashShop().isOpened()) {
            // in CashShop
            c.announce(MaplePacketCreator.getFindReply(target.getName(), -1, 2));
            return;
        }

        if (target.isAwayFromWorld()) {
            // in MTS
            c.announce(MaplePacketCreator.getFindReply(target.getName(), -1, 0));
            return;
        }
        if (target.getClient().getChannel() != c.getChannel()) {
            // in another channel
            c.announce(MaplePacketCreator.getFindReply(target.getName(), target.getClient().getChannel() - 1, 3));
            return;
        }

        c.announce(MaplePacketCreator.getFindReply(target.getName(), target.getMap().getId(), 1));
    }

    private static void handleWhisper(MapleClient c, String recipientName, String text) {
        Optional<MapleCharacter> recipient = c.getChannelServer().getPlayerStorage().getCharacterByName(recipientName);
        if (c.getPlayer().getAutobanManager().getLastSpam(7) + 200 > currentServerTime()) {
            return;
        }

        if (text.length() > Byte.MAX_VALUE && !recipient.map(MapleCharacter::isGM).orElse(false)) {
            AutobanFactory.PACKET_EDIT.alert(c.getPlayer(), c.getPlayer().getName() + " tried to packet edit with whispers.");
            FilePrinter.printError(FilePrinter.EXPLOITS + c.getPlayer().getName() + ".txt", c.getPlayer().getName() + " tried to send text with length of " + text.length());
            c.disconnect(true, false);
            return;
        }

        if (recipient.isEmpty()) {
            World world = c.getWorldServer();
            if (!world.isConnected(recipientName)) {
                c.announce(MaplePacketCreator.getWhisperReply(recipientName, (byte) 0));
                return;
            }

            recipient = world.getPlayerStorage().getCharacterByName(recipientName);
            if (recipient.isEmpty()) {
                c.announce(MaplePacketCreator.getWhisperReply(recipientName, (byte) 0));
                return;
            }
        }
        handleWhisper(c, recipient.get(), text);
    }

    private static void handleWhisper(MapleClient c, MapleCharacter recipient, String text) {
        recipient.announce(MaplePacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
        if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
            LogHelper.logChat(c, "Whisper To " + recipient.getName(), text);
        }
        if (recipient.isHidden() && recipient.gmLevel() >= c.getPlayer().gmLevel()) {
            c.announce(MaplePacketCreator.getWhisperReply(recipient.getName(), (byte) 0));
        } else {
            c.announce(MaplePacketCreator.getWhisperReply(recipient.getName(), (byte) 1));
        }
    }
}
