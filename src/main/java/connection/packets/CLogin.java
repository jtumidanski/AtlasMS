package connection.packets;

import client.MapleCharacter;
import client.MapleClient;
import config.YamlConfig;
import connection.constants.SendOpcode;
import net.server.Server;
import net.server.channel.Channel;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

public class CLogin {
    /**
     * Gets a login failed packet.
     * <p>
     * Possible values for <code>reason</code>:<br> 3: ID deleted or blocked<br>
     * 4: Incorrect password<br> 5: Not a registered id<br> 6: System error<br>
     * 7: Already logged in<br> 8: System error<br> 9: System error<br> 10:
     * Cannot process so many connections<br> 11: Only users older than 20 can
     * use this channel<br> 13: Unable to log on as master at this ip<br> 14:
     * Wrong gateway or personal info and weird korean button<br> 15: Processing
     * request with that korean button!<br> 16: Please verify your account
     * through email...<br> 17: Wrong gateway or personal info<br> 21: Please
     * verify your account through email...<br> 23: License agreement<br> 25:
     * Maple Europe notice =[ FUCK YOU NEXON<br> 27: Some weird full client
     * notice, probably for trial versions<br>
     *
     * @param reason The reason logging in failed.
     * @return The login failed packet.
     */
    public static byte[] getLoginFailed(int reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason);
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getPermBan(byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(2); // Account is banned
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeLong(CCommon.getTime(-1));

        return mplew.getPacket();
    }

    public static byte[] getTempBan(long timestampTill, byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.write(2);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(reason);
        mplew.writeLong(CCommon.getTime(timestampTill)); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return mplew.getPacket();
    }

    /**
     * Gets a successful authentication packet.
     *
     * @param c
     * @return the successful authentication packet
     */
    public static byte[] getAuthSuccess(MapleClient c) {
        Server.getInstance()
                .loadAccountCharacters(c);    // locks the login session until data is recovered from the cache or the DB.
        Server.getInstance()
                .loadAccountStorages(c);

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(c.getAccID());
        mplew.write(c.getGender());

        boolean canFly = Server.getInstance()
                .canFly(c.getAccID());
        mplew.writeBool((YamlConfig.config.server.USE_ENFORCE_ADMIN_ACCOUNT || canFly) && c.getGMLevel() > 1);    // thanks Steve(kaito1410) for pointing the GM account boolean here
        mplew.write(((YamlConfig.config.server.USE_ENFORCE_ADMIN_ACCOUNT || canFly) && c.getGMLevel() > 1) ? 0x80 : 0);  // Admin Byte. 0x80,0x40,0x20.. Rubbish.
        mplew.write(0); // Country Code.

        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.write(0);

        mplew.write(0); // IsQuietBan
        mplew.writeLong(0);//IsQuietBanTimeStamp
        mplew.writeLong(0); //CreationTimeStamp

        mplew.writeInt(1); // 1: Remove the "Select the world you want to play in"

        mplew.write(YamlConfig.config.server.ENABLE_PIN && !c.canBypassPin() ? 0 : 1); // 0 = Pin-System Enabled, 1 = Disabled
        mplew.write(YamlConfig.config.server.ENABLE_PIC && !c.canBypassPic() ? (c.getPic() == null || c.getPic()
                .equals("") ? 0 : 1) : 2); // 0 = Register PIC, 1 = Ask for PIC, 2 = Disabled
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] sendGuestTOS() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUEST_ID_LOGIN.getValue());
        mplew.writeShort(0x100);
        mplew.writeInt(Randomizer.nextInt(999999));
        mplew.writeLong(0);
        mplew.writeLong(CCommon.getTime(-2));
        mplew.writeLong(CCommon.getTime(System.currentTimeMillis()));
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("http://maplesolaxia.com");
        return mplew.getPacket();
    }

    /**
     * Gets a packet detailing a server status message.
     * <p>
     * Possible values for <code>status</code>:<br> 0 - Normal<br> 1 - Highly
     * populated<br> 2 - Full
     *
     * @param status The server status.
     * @return The server status packet.
     */
    public static byte[] getServerStatus(int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeShort(SendOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);
        return mplew.getPacket();
    }

    /**
     * Gets a packet detailing a PIN operation.
     * <p>
     * Possible values for <code>mode</code>:<br> 0 - PIN was accepted<br> 1 -
     * Register a new PIN<br> 2 - Invalid pin / Reenter<br> 3 - Connection
     * failed due to system error<br> 4 - Enter the pin
     *
     * @param mode The mode.
     * @return
     */
    static byte[] pinOperation(byte mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.CHECK_PINCODE.getValue());
        mplew.write(mode);
        return mplew.getPacket();
    }

    public static byte[] pinRegistered() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.UPDATE_PINCODE.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] showAllCharacter(int chars, int unk) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendOpcode.VIEW_ALL_CHAR.getValue());
        mplew.write(chars > 0 ? 1 : 5); // 2: already connected to server, 3 : unk error (view-all-characters), 5 : cannot find any
        mplew.writeInt(chars);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static byte[] showAllCharacterInfo(int worldid, List<MapleCharacter> chars, boolean usePic) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.VIEW_ALL_CHAR.getValue());
        mplew.write(0);
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, true);
        }
        mplew.write(usePic ? 1 : 2);
        return mplew.getPacket();
    }

    /**
     * Gets a login failed packet.
     * <p>
     * Possible values for <code>reason</code>:<br> 2: ID deleted or blocked<br>
     * 3: ID deleted or blocked<br> 4: Incorrect password<br> 5: Not a
     * registered id<br> 6: Trouble logging into the game?<br> 7: Already logged
     * in<br> 8: Trouble logging into the game?<br> 9: Trouble logging into the
     * game?<br> 10: Cannot process so many connections<br> 11: Only users older
     * than 20 can use this channel<br> 12: Trouble logging into the game?<br>
     * 13: Unable to log on as master at this ip<br> 14: Wrong gateway or
     * personal info and weird korean button<br> 15: Processing request with
     * that korean button!<br> 16: Please verify your account through
     * email...<br> 17: Wrong gateway or personal info<br> 21: Please verify
     * your account through email...<br> 23: Crashes<br> 25: Maple Europe notice
     * =[ FUCK YOU NEXON<br> 27: Some weird full client notice, probably for
     * trial versions<br>
     *
     * @param reason The reason logging in failed.
     * @return The login failed packet.
     */
    public static byte[] getAfterLoginError(int reason) {//same as above o.o
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);
        mplew.writeShort(SendOpcode.SELECT_CHARACTER_BY_VAC.getValue());
        mplew.writeShort(reason);//using other types than stated above = CRASH
        return mplew.getPacket();
    }

    /**
     * Gets a packet detailing a server and its channels.
     *
     * @param serverId
     * @param serverName  The name of the server.
     * @param flag
     * @param eventmsg
     * @param channelLoad Load of the channel - 1200 seems to be max.
     * @return The server info packet.
     */
    public static byte[] getServerList(int serverId, String serverName, int flag, String eventmsg, List<Channel> channelLoad) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        mplew.writeMapleAsciiString(serverName);
        mplew.write(flag);
        mplew.writeMapleAsciiString(eventmsg);
        mplew.write(100); // rate modifier, don't ask O.O!
        mplew.write(0); // event xp * 2.6 O.O!
        mplew.write(100); // rate modifier, don't ask O.O!
        mplew.write(0); // drop rate * 2.6
        mplew.write(0);
        mplew.write(channelLoad.size());
        for (Channel ch : channelLoad) {
            mplew.writeMapleAsciiString(serverName + "-" + ch.getId());
            mplew.writeInt(ch.getChannelCapacity());

            // thanks GabrielSin for this channel packet structure part
            mplew.write(1);// nWorldID
            mplew.write(ch.getId() - 1);// nChannelID
            mplew.writeBool(false);// bAdultChannel
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet saying that the server list is over.
     *
     * @return The end of server list packet.
     */
    public static byte[] getEndOfServerList() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    /**
     * Gets a packet with a list of characters.
     *
     * @param c        The MapleClient to load characters of.
     * @param serverId The ID of the server requested.
     * @param status   The charlist request result.
     * @return The character list packet.
     * <p>
     * Possible values for <code>status</code>:
     * <br> 2: ID deleted or blocked<br>
     * <br> 3: ID deleted or blocked<br>
     * <br> 4: Incorrect password<br>
     * <br> 5: Not an registered ID<br>
     * <br> 6: Trouble logging in?<br>
     * <br> 10: Server handling too many connections<br>
     * <br> 11: Only 20 years or older<br>
     * <br> 13: Unable to log as master at IP<br>
     * <br> 14: Wrong gateway or personal info<br>
     * <br> 15: Still processing request<br>
     * <br> 16: Verify account via email<br>
     * <br> 17: Wrong gateway or personal info<br>
     * <br> 21: Verify account via email<br>
     */
    public static byte[] getCharList(MapleClient c, int serverId, int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHARLIST.getValue());
        mplew.write(status);
        List<MapleCharacter> chars = c.loadCharacters(serverId);
        mplew.write((byte) chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, false);
        }

        mplew.write(YamlConfig.config.server.ENABLE_PIC && !c.canBypassPic() ? (c.getPic() == null || c.getPic()
                .equals("") ? 0 : 1) : 2);
        mplew.writeInt(YamlConfig.config.server.COLLECTIVE_CHARSLOT ? chars.size() + c.getAvailableCharacterSlots() : c.getCharacterSlots());
        mplew.writeInt(0);// TODO m_nBuyCharCount
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the channel server.
     *
     * @param inetAddr The InetAddress of the requested channel server.
     * @param port     The port the channel is on.
     * @param clientId The ID of the client.
     * @return The server IP packet.
     */
    public static byte[] getServerIP(InetAddress inetAddr, int port, int clientId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        byte[] addr = inetAddr.getAddress();
        mplew.write(addr);
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write(new byte[]{0, 0, 0, 0, 0});
        return mplew.getPacket();
    }

    public static byte[] charNameResponse(String charname, boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] addNewCharEntry(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(0);
        addCharEntry(mplew, chr, false);
        return mplew.getPacket();
    }

    /**
     * State :
     * 0x00 = success
     * 0x06 = Trouble logging into the game?
     * 0x09 = Unknown error
     * 0x0A = Could not be processed due to too many connection requests to the server.
     * 0x12 = invalid bday
     * 0x14 = incorrect pic
     * 0x16 = Cannot delete a guild master.
     * 0x18 = Cannot delete a character with a pending wedding.
     * 0x1A = Cannot delete a character with a pending world transfer.
     * 0x1D = Cannot delete a character that has a family.
     *
     * @param cid
     * @param state
     * @return
     */
    public static byte[] deleteCharResponse(int cid, int state) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);
        return mplew.getPacket();
    }

    /**
     * Gets the response to a relog request.
     *
     * @return The relog response packet.
     */
    public static byte[] getRelogResponse() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.RELOG_RESPONSE.getValue());
        mplew.write(1);//1 O.O Must be more types ):
        return mplew.getPacket();
    }

    public static byte[] selectWorld(int world) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.LAST_CONNECTED_WORLD.getValue());
        mplew.writeInt(world);//According to GMS, it should be the world that contains the most characters (most active)
        return mplew.getPacket();
    }

    public static byte[] sendRecommended(List<Pair<Integer, String>> worlds) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RECOMMENDED_WORLD_MESSAGE.getValue());
        mplew.write(worlds.size());//size
        for (Iterator<Pair<Integer, String>> it = worlds.iterator(); it.hasNext(); ) {
            Pair<Integer, String> world = it.next();
            mplew.writeInt(world.getLeft());
            mplew.writeMapleAsciiString(world.getRight());
        }
        return mplew.getPacket();
    }

    public static byte[] wrongPic() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeShort(SendOpcode.CHECK_SPW_RESULT.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static void addCharEntry(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean viewall) {
        CCommon.addCharStats(mplew, chr);
        CCommon.addCharLook(mplew, chr, false);
        if (!viewall) {
            mplew.write(0);
        }
        if (chr.isGM() || chr.isGmJob()) {  // thanks Daddy Egg (Ubaware), resinate for noticing GM jobs crashing on non-GM players account
            mplew.write(0);
            return;
        }
        mplew.write(1); // world rank enabled (next 4 ints are not sent if disabled) Short??
        mplew.writeInt(chr.getRank()); // world rank
        mplew.writeInt(chr.getRankMove()); // move (negative is downwards)
        mplew.writeInt(chr.getJobRank()); // job rank
        mplew.writeInt(chr.getJobRankMove()); // move (negative is downwards)
    }

    public static byte[] requestPin() {
        return pinOperation((byte) 4);
    }

    public static byte[] requestPinAfterFailure() {
        return pinOperation((byte) 2);
    }

    public static byte[] registerPin() {
        return pinOperation((byte) 1);
    }

    public static byte[] pinAccepted() {
        return pinOperation((byte) 0);
    }
}
