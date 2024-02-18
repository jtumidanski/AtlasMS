package net.server.handlers.login;

import client.MapleClient;
import connection.packets.CLogin;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import net.server.coordinator.session.MapleSessionCoordinator;
import net.server.coordinator.session.MapleSessionCoordinator.AntiMulticlientResult;
import net.server.world.World;
import org.apache.mina.core.session.IoSession;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ViewAllCharRegisterPicHandler extends AbstractMaplePacketHandler {

    private static int parseAntiMulticlientError(AntiMulticlientResult res) {
        switch (res) {
            case REMOTE_PROCESSING:
                return 10;

            case REMOTE_LOGGEDIN:
                return 7;

            case REMOTE_NO_MATCH:
                return 17;

            case COORDINATOR_ERROR:
                return 8;

            default:
                return 9;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        int charId = slea.readInt();
        slea.readInt(); // please don't let the client choose which world they should login

        String mac = slea.readMapleAsciiString();
        String hwid = slea.readMapleAsciiString();

        if (!hwid.matches("[0-9A-F]{12}_[0-9A-F]{8}")) {
            c.announce(CLogin.getAfterLoginError(17));
            return;
        }

        c.updateMacs(mac);
        c.updateHWID(hwid);

        if (c.hasBannedMac() || c.hasBannedHWID()) {
            MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
            return;
        }

        IoSession session = c.getSession();
        AntiMulticlientResult res = MapleSessionCoordinator.getInstance().attemptGameSession(session, c.getAccID(), hwid);
        if (res != AntiMulticlientResult.SUCCESS) {
            c.announce(CLogin.getAfterLoginError(parseAntiMulticlientError(res)));
            return;
        }

        Server server = Server.getInstance();
        if (!server.haveCharacterEntry(c.getAccID(), charId)) {
            MapleSessionCoordinator.getInstance().closeSession(c.getSession(), true);
            return;
        }

        c.setWorld(server.getCharacterWorld(charId).orElseThrow());
        World wserv = c.getWorldServer();
        if (wserv == null || wserv.isWorldCapacityFull()) {
            c.announce(CLogin.getAfterLoginError(10));
            return;
        }

        int channel = Randomizer.rand(1, server.getWorld(c.getWorld()).orElseThrow().getChannelsSize());
        c.setChannel(channel);

        String pic = slea.readMapleAsciiString();
        c.setPic(pic);

        String[] socket = server.getInetSocket(c.getWorld(), channel);
        if (socket == null) {
            c.announce(CLogin.getAfterLoginError(10));
            return;
        }

        server.unregisterLoginState(c);
        c.setCharacterOnSessionTransitionState(charId);

        try {
            c.announce(CLogin.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
