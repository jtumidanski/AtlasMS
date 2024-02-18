package connection.packets;

import client.Skill;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import connection.constants.SendOpcode;
import server.life.MapleMonster;
import server.movement.MovePath;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;

public class CMob {
    public static byte[] moveMonster(int mobID, boolean mobMoveStartResult, byte actionAndDir, int skillData, MovePath moves) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(mobID);
        mplew.write(0); // bNotForceLandingWhenDiscard
        mplew.writeBool(mobMoveStartResult); // bNotChangeAction
        mplew.write(0); // bNextAttackPossible

        mplew.writeInt(skillData); // sEffect_m_Data; skillId?

        mplew.writeInt(0); // m_aMultiTargetForBall?
        if (0 > 0) {
            // repeats m_aMultiTargetForBall times
            mplew.writeInt(0); // x
            mplew.writeInt(0); // y
        }

        mplew.writeInt(0); // m_aRandTimeforAreaAttack
        if (0 > 0) {
            // repeats m_aRandTimeforAreaAttack times
            mplew.writeInt(0); // m_aRandTimeforAreaAttack
        }

        moves.encode(mplew);
        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid   The ObjectID of the monster being moved.
     * @param moveid     The movement ID.
     * @param currentMp  The current MP of the monster.
     * @param useSkills  Can the monster use skills?
     * @param skillId    The skill ID for the monster to use.
     * @param skillLevel The level of the skill to use.
     * @return The move response packet.
     */

    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(13);
        mplew.writeShort(SendOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.writeBool(useSkills);
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(final int oid, final MonsterStatusEffect mse, final List<Integer> reflection) {
        Map<MonsterStatus, Integer> stati = mse.getStati();
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        writeIntMask(mplew, stati);
        for (Map.Entry<MonsterStatus, Integer> stat : stati.entrySet()) {
            mplew.writeShort(stat.getValue());
            if (mse.isMonsterSkill()) {
                mplew.writeShort(mse.getMobSkill()
                        .getSkillId());
                mplew.writeShort(mse.getMobSkill()
                        .getSkillLevel());
            } else {
                mplew.writeInt(mse.getSkill()
                        .map(Skill::id)
                        .orElse(0));
            }
            mplew.writeShort(-1); // might actually be the buffTime but it's not displayed anywhere
        }
        int size = stati.size(); // size
        if (reflection != null) {
            for (Integer ref : reflection) {
                mplew.writeInt(ref);
            }
            if (reflection.size() > 0) {
                size /= 2; // This gives 2 buffs per reflection but it's really one buff
            }
        }
        mplew.write(size); // size
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        mplew.writeLong(0);
        writeIntMask(mplew, stats);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    static byte[] damageMonster(int oid, int damage, int curhp, int maxhp) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(damage);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    public static byte[] MobDamageMobFriendly(MapleMonster mob, int damage, int remainingHp) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(1); // direction ?
        mplew.writeInt(damage);
        mplew.writeInt(remainingHp);
        mplew.writeInt(mob.getMaxHp());
        return mplew.getPacket();
    }

    /**
     * @param oid
     * @param remhppercentage
     * @return
     */
    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);
        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobOid, byte success) {   // updated packet structure found thanks to Rien dev team
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(mobOid);
        mplew.write(success);
        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobOid, int itemid, byte success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CATCH_MONSTER_WITH_ITEM.getValue());
        mplew.writeInt(mobOid);
        mplew.writeInt(itemid);
        mplew.write(success);
        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid  The ObjectID of the monster being moved.
     * @param moveid    The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @return The move response packet.
     */
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }

    private static void writeIntMask(final MaplePacketLittleEndianWriter mplew, Map<MonsterStatus, Integer> stats) {
        int firstmask = 0;
        int secondmask = 0;
        for (MonsterStatus stat : stats.keySet()) {
            if (stat.isFirst()) {
                firstmask |= stat.getValue();
            } else {
                secondmask |= stat.getValue();
            }
        }
        mplew.writeInt(firstmask);
        mplew.writeInt(secondmask);
    }

    public static byte[] damageMonster(int oid, int damage) {
        return damageMonster(oid, damage, 0, 0);
    }

    public static byte[] healMonster(int oid, int heal, int curhp, int maxhp) {
        return damageMonster(oid, -heal, curhp, maxhp);
    }
}
