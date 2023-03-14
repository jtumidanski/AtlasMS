package net.server.channel.handlers;

import client.MapleClient;
import config.YamlConfig;
import net.AbstractMaplePacketHandler;
import tools.LogHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class AdminChatHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isGM()) {
            return;
        }
        byte mode = slea.readByte();
        String message = slea.readMapleAsciiString();
        byte[] packet = MaplePacketCreator.serverNotice(slea.readByte(), message);

        switch (mode) {
            case 0:// /alertall, /noticeall, /slideall
                c.getWorldServer().broadcastPacket(packet);
                if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
                    LogHelper.logChat(c, "Alert All", message);
                }
                break;
            case 1:// /alertch, /noticech, /slidech
                c.getChannelServer().broadcastPacket(packet);
                if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
                    LogHelper.logChat(c, "Alert Ch", message);
                }
                break;
            case 2:// /alertm /alertmap, /noticem /noticemap, /slidem /slidemap
                c.getPlayer().getMap().broadcastMessage(packet);
                if (YamlConfig.config.server.USE_ENABLE_CHAT_LOG) {
                    LogHelper.logChat(c, "Alert Map", message);
                }
                break;

        }
    }
}
