package connection.packets;

import connection.constants.SendOpcode;
import net.server.channel.handlers.SummonDamageHandler;
import server.maps.MapleSummon;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.List;

public class CSummonedPool {
    /**
     * Gets a packet to spawn a special map object.
     *
     * @param summon
     * @param skillLevel The level of the skill used.
     * @param animated   Animated spawn?
     * @return The spawn packet for the map object.
     */
    public static byte[] spawnSummon(MapleSummon summon, boolean animated) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(25);
        mplew.writeShort(SendOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner()
                .getId());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(summon.getSkill());
        mplew.write(0x0A); //v83
        mplew.write(summon.getSkillLevel());
        mplew.writePos(summon.getPosition());
        mplew.write(summon.getStance());    //bMoveAction & foothold, found thanks to Rien dev team
        mplew.writeShort(0);
        mplew.write(summon.getMovementType()
                .getValue()); // 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele follow, 3 = bird follow
        mplew.write(summon.isPuppet() ? 0 : 1); // 0 and the summon can't attack - but puppets don't attack with 1 either ^.-
        mplew.write(animated ? 0 : 1);
        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a special map object.
     *
     * @param summon
     * @param animated Animated removal?
     * @return The packet removing the object.
     */
    public static byte[] removeSummon(MapleSummon summon, boolean animated) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(11);
        mplew.writeShort(SendOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
        mplew.writeInt(summon.getOwner()
                .getId());
        mplew.writeInt(summon.getObjectId());
        mplew.write(animated ? 4 : 1); // ?
        return mplew.getPacket();
    }

    public static byte[] moveSummon(int cid, int oid, Point startPos, SeekableLittleEndianAccessor movementSlea, long movementDataLength) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writePos(startPos);
        CCommon.rebroadcastMovementList(mplew, movementSlea, movementDataLength);
        return mplew.getPacket();
    }

    public static byte[] summonAttack(int cid, int summonOid, byte direction, List<SummonDamageHandler.SummonAttackEntry> allDamage) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //b2 00 29 f7 00 00 9a a3 04 00 c8 04 01 94 a3 04 00 06 ff 2b 00
        mplew.writeShort(SendOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonOid);
        mplew.write(0);     // char level
        mplew.write(direction);
        mplew.write(allDamage.size());
        for (SummonDamageHandler.SummonAttackEntry attackEntry : allDamage) {
            mplew.writeInt(attackEntry.getMonsterOid()); // oid
            mplew.write(6); // who knows
            mplew.writeInt(attackEntry.getDamage()); // damage
        }

        return mplew.getPacket();
    }

    public static byte[] damageSummon(int cid, int oid, int damage, int monsterIdFrom) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DAMAGE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.write(12);
        mplew.writeInt(damage);         // damage display doesn't seem to work...
        mplew.writeInt(monsterIdFrom);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] summonSkill(int cid, int summonSkillId, int newStance) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SUMMON_SKILL.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        return mplew.getPacket();
    }
}
