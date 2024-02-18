package connection.packets;

import client.Skill;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import connection.constants.SendOpcode;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Collection;
import java.util.Map;

public class CMobPool {
    /**
     * Internal function to handler monster spawning and controlling.
     *
     * @param life              The mob to perform operations with.
     * @param requestController Requesting control of mob?
     * @param newSpawn          New spawn (fade in?)
     * @param aggro             Aggressive mob?
     * @param effect            The spawn effect to use.
     * @return The spawn/control packet.
     */
    static byte[] spawnMonsterInternal(MapleMonster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (makeInvis) {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(0);
            mplew.writeInt(life.getObjectId());
            return mplew.getPacket();
        }
        if (requestController) {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
            mplew.write(aggro ? 2 : 1);
        } else {
            mplew.writeShort(SendOpcode.SPAWN_MONSTER.getValue());
        }
        mplew.writeInt(life.getObjectId());
        mplew.write(life.getController()
                .isEmpty() ? 5 : 1);
        mplew.writeInt(life.getId());

        if (requestController) {
            encodeTemporary(mplew, life.getStati());    // thanks shot for noticing encode temporary buffs missing
        } else {
            mplew.skip(16);
        }

        mplew.writePos(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(0); //Origin FH //life.getStartFh()
        mplew.writeShort(life.getFh());


        /**
         * -4: Fake -3: Appear after linked mob is dead -2: Fade in 1: Smoke 3:
         * King Slime spawn 4: Summoning rock thing, used for 3rd job? 6:
         * Magical shit 7: Smoke shit 8: 'The Boss' 9/10: Grim phantom shit?
         * 11/12: Nothing? 13: Frankenstein 14: Angry ^ 15: Orb animation thing,
         * ?? 16: ?? 19: Mushroom castle boss thing
         */

        if (life.getParentMobOid() != 0) {
            MapleMonster parentMob = life.getMap()
                    .getMonsterByOid(life.getParentMobOid())
                    .orElse(null);
            if (parentMob != null && parentMob.isAlive()) {
                mplew.write(effect != 0 ? effect : -3);
                mplew.writeInt(life.getParentMobOid());
            } else {
                encodeParentlessMobSpawnEffect(mplew, newSpawn, effect);
            }
        } else {
            encodeParentlessMobSpawnEffect(mplew, newSpawn, effect);
        }

        mplew.write(life.getTeam());
        mplew.writeInt(0); // getItemEffect
        return mplew.getPacket();
    }

    /**
     * Makes a monster previously spawned as non-targettable, targettable.
     *
     * @param life The mob to make targettable.
     * @return The packet to make the mob targettable.
     */
    public static byte[] makeMonsterReal(MapleMonster life) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.write(5);
        mplew.writeInt(life.getId());
        encodeTemporary(mplew, life.getStati());
        mplew.writePos(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);//life.getStartFh()
        mplew.writeShort(life.getFh());
        mplew.writeShort(-1);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client that a monster was killed.
     *
     * @param oid       The objectID of the killed monster.
     * @param animation 0 = dissapear, 1 = fade out, 2+ = special
     * @return The kill monster packet.
     */
    public static byte[] killMonster(int oid, int animation) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation);
        mplew.write(animation);
        return mplew.getPacket();
    }

    /**
     * Removes a monster invisibility.
     *
     * @param life
     * @return
     */
    public static byte[] removeMonsterInvisibility(MapleMonster life) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        return mplew.getPacket();
    }

    /**
     * Handles monsters not being targettable, such as Zakum's first body.
     *
     * @param life   The mob to spawn as non-targettable.
     * @param effect The effect to show when spawning.
     * @return The packet to spawn the mob as non-targettable.
     */
    public static byte[] spawnFakeMonster(MapleMonster life, int effect) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.write(5);
        mplew.writeInt(life.getId());
        encodeTemporary(mplew, life.getStati());
        mplew.writePos(life.getPosition());
        mplew.write(life.getStance());
        mplew.writeShort(0);//life.getStartFh()
        mplew.writeShort(life.getFh());
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0);
            mplew.writeShort(0);
        }
        mplew.writeShort(-2);
        mplew.write(life.getTeam());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * Gets a stop control monster packet.
     *
     * @param oid The ObjectID of the monster to stop controlling.
     * @return The stop control monster packet.
     */
    public static byte[] stopControllingMonster(int oid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(7);
        mplew.writeShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life     The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @return The spawn monster packet.
     */
    public static byte[] spawnMonster(MapleMonster life, boolean newSpawn) {
        return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
    }

    /**
     * Gets a spawn monster packet.
     *
     * @param life     The monster to spawn.
     * @param newSpawn Is it a new spawn?
     * @param effect   The spawn effect.
     * @return The spawn monster packet.
     */
    public static byte[] spawnMonster(MapleMonster life, boolean newSpawn, int effect) {
        return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
    }

    /**
     * Gets a control monster packet.
     *
     * @param life     The monster to give control to.
     * @param newSpawn Is it a new spawn?
     * @param aggro    Aggressive monster?
     * @return The monster control packet.
     */
    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
    }

    /**
     * Makes a monster invisible for Ariant PQ.
     *
     * @param life
     * @return
     */
    public static byte[] makeMonsterInvisible(MapleMonster life) {
        return spawnMonsterInternal(life, true, false, false, 0, true);
    }

    private static void encodeParentlessMobSpawnEffect(MaplePacketLittleEndianWriter mplew, boolean newSpawn, int effect) {
        if (effect > 0) {
            mplew.write(effect);
            mplew.write(0);
            mplew.writeShort(0);
            if (effect == 15) {
                mplew.write(0);
            }
        }
        mplew.write(newSpawn ? -2 : -1);
    }

    public static byte[] killMonster(int oid, boolean animation) {
        return killMonster(oid, animation ? 1 : 0);
    }

    private static void encodeTemporary(MaplePacketLittleEndianWriter mplew, Map<MonsterStatus, MonsterStatusEffect> stati) {
        int pCounter = -1, mCounter = -1;

        writeLongEncodeTemporaryMask(mplew, stati.keySet());    // packet structure mapped thanks to Eric

        for (Map.Entry<MonsterStatus, MonsterStatusEffect> s : stati.entrySet()) {
            MonsterStatusEffect mse = s.getValue();
            mplew.writeShort(mse.getStati()
                    .get(s.getKey()));

            MobSkill mobSkill = mse.getMobSkill();
            if (mobSkill != null) {
                mplew.writeShort(mobSkill.getSkillId());
                mplew.writeShort(mobSkill.getSkillLevel());

                switch (s.getKey()) {
                    case WEAPON_REFLECT:
                        pCounter = mobSkill.getX();
                        break;

                    case MAGIC_REFLECT:
                        mCounter = mobSkill.getY();
                        break;
                }
            } else {
                mplew.writeInt(mse.getSkill()
                        .map(Skill::id)
                        .orElse(0));
            }

            mplew.writeShort(-1);    // duration
        }

        // reflect packet structure found thanks to Arnah (Vertisy)
        if (pCounter != -1) {
            mplew.writeInt(pCounter);// wPCounter_
        }
        if (mCounter != -1) {
            mplew.writeInt(mCounter);// wMCounter_
        }
        if (pCounter != -1 || mCounter != -1) {
            mplew.writeInt(100);// nCounterProb_
        }
    }

    private static void writeLongEncodeTemporaryMask(final MaplePacketLittleEndianWriter mplew, Collection<MonsterStatus> stati) {
        int[] masks = new int[4];

        for (MonsterStatus statup : stati) {
            int pos = statup.isFirst() ? 0 : 2;
            for (int i = 0; i < 2; i++) {
                masks[pos + i] |= statup.getValue() >> 32 * i;
            }
        }

        for (int i = 0; i < masks.length; i++) {
            mplew.writeInt(masks[i]);
        }
    }
}
