package connection.packets;

import connection.constants.SendOpcode;
import server.events.gm.MapleSnowball;
import tools.data.output.MaplePacketLittleEndianWriter;

public class CFieldSnowBall {
    public static byte[] rollSnowBall(boolean entermap, int state, MapleSnowball ball0, MapleSnowball ball1) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SNOWBALL_STATE.getValue());
        if (entermap) {
            mplew.skip(21);
        } else {
            mplew.write(state);// 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
            mplew.writeInt(ball0.getSnowmanHP() / 75);
            mplew.writeInt(ball1.getSnowmanHP() / 75);
            mplew.writeShort(ball0.getPosition());//distance snowball down, 84 03 = max
            mplew.write(-1);
            mplew.writeShort(ball1.getPosition());//distance snowball up, 84 03 = max
            mplew.write(-1);
        }
        return mplew.getPacket();
    }

    public static byte[] hitSnowBall(int what, int damage) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.HIT_SNOWBALL.getValue());
        mplew.write(what);
        mplew.writeInt(damage);
        return mplew.getPacket();
    }

    /**
     * Sends a Snowball Message<br>
     * <p>
     * Possible values for <code>message</code>:<br> 1: ... Team's snowball has
     * passed the stage 1.<br> 2: ... Team's snowball has passed the stage
     * 2.<br> 3: ... Team's snowball has passed the stage 3.<br> 4: ... Team is
     * attacking the snowman, stopping the progress<br> 5: ... Team is moving
     * again<br>
     *
     * @param message
     */
    public static byte[] snowballMessage(int team, int message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.SNOWBALL_MESSAGE.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeInt(message);
        return mplew.getPacket();
    }

    public static byte[] leftKnockBack() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);
        mplew.writeShort(SendOpcode.LEFT_KNOCK_BACK.getValue());
        return mplew.getPacket();
    }
}
