package connection.packets;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.MapleMount;
import connection.constants.SendOpcode;
import constants.skills.Buccaneer;
import constants.skills.Corsair;
import constants.skills.ThunderBreaker;
import net.server.guild.MapleGuild;
import server.life.MobSkill;
import server.movement.MovePath;
import tools.Pair;
import tools.data.output.LittleEndianWriter;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class CUserRemote {
    public static byte[] movePlayer(int cid, MovePath moves) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        moves.encode(mplew);
        return mplew.getPacket();
    }

    public static byte[] closeRangeAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CLOSE_RANGE_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
        return mplew.getPacket();
    }

    public static byte[] rangedAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, projectile, damage, speed, direction, display);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] magicAttack(MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int charge, int speed, int direction, int display) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
        if (charge != -1) {
            mplew.writeInt(charge);
        }
        return mplew.getPacket();
    }

    public static byte[] skillEffect(MapleCharacter from, int skillId, int level, byte flags, int speed, byte direction) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        mplew.write(speed);
        mplew.write(direction); //Mmmk
        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(damage);
        if (skill != -4) {
            mplew.writeInt(monsteridfrom);
            mplew.write(direction);
            if (pgmr) {
                mplew.write(pgmr_1);
                mplew.write(is_pg ? 1 : 0);
                mplew.writeInt(oid);
                mplew.write(6);
                mplew.writeShort(pos_x);
                mplew.writeShort(pos_y);
                mplew.write(0);
            } else {
                mplew.writeShort(0);
            }
            mplew.writeInt(damage);
            if (fake > 0) {
                mplew.writeInt(fake);
            }
        } else {
            mplew.writeInt(damage);
        }

        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);
        mplew.writeShort(SendOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        return mplew.getPacket();
    }

    public static byte[] itemEffect(int characterid, int itemid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] showChair(int characterid, int itemid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    public static byte[] updateCharLook(MapleClient target, MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        CCommon.addCharLook(mplew, chr, false);
        CCommon.addRingLook(mplew, chr, true);
        CCommon.addRingLook(mplew, chr, false);
        CCommon.addMarriageRingLook(target, mplew, chr);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /**
     * @param cid
     * @param statups
     * @param mount
     * @return
     */
    public static byte[] showMonsterRiding(int cid, MapleMount mount) { //Gtfo with this, this is just giveForeignBuff
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(MapleBuffStat.MONSTER_RIDING.getValue());
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.writeInt(mount.getItemId());
        mplew.writeInt(mount.getSkillId());
        mplew.writeInt(0); //Server Tick value.
        mplew.writeShort(0);
        mplew.write(0); //Times you have been buffed
        return mplew.getPacket();
    }

    public static byte[] giveForeignDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        // Poison damage visibility and missing diseases status visibility, extended through map transitions thanks to Ronan

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        CCommon.writeLongMaskD(mplew, statups);
        for (Pair<MapleDisease, Integer> statup : statups) {
            if (statup.getLeft() == MapleDisease.POISON) {
                mplew.writeShort(statup.getRight()
                        .shortValue());
            }
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
        }
        mplew.writeShort(0); // same as give_buff
        mplew.writeShort(900);//Delay
        return mplew.getPacket();
    }

    public static byte[] giveForeignBuff(int cid, List<Pair<MapleBuffStat, Integer>> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        CCommon.writeLongMask(mplew, statups);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeShort(statup.getRight()
                    .shortValue());
        }
        mplew.writeInt(0);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] giveForeignSlowDebuff(int cid, List<Pair<MapleDisease, Integer>> statups, MobSkill skill) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeLongMaskSlowD(mplew);
        for (Pair<MapleDisease, Integer> statup : statups) {
            if (statup.getLeft() == MapleDisease.POISON) {
                mplew.writeShort(statup.getRight()
                        .shortValue());
            }
            mplew.writeShort(skill.getSkillId());
            mplew.writeShort(skill.getSkillLevel());
        }
        mplew.writeShort(0); // same as give_buff
        mplew.writeShort(900);//Delay
        return mplew.getPacket();
    }

    public static byte[] giveForeignChairSkillEffect(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeLongMaskChair(mplew);

        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeShort(100);
        mplew.writeShort(1);

        mplew.writeShort(0);
        mplew.writeShort(900);

        mplew.skip(7);

        return mplew.getPacket();
    }

    // packet found thanks to Ronan
    public static byte[] giveForeignWKChargeEffect(int cid, int buffid, List<Pair<MapleBuffStat, Integer>> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(19);
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        CCommon.writeLongMask(mplew, statups);
        mplew.writeInt(buffid);
        mplew.writeShort(600);
        mplew.writeShort(1000);//Delay
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] giveForeignPirateBuff(int cid, int buffid, int time, List<Pair<MapleBuffStat, Integer>> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        boolean infusion = buffid == Buccaneer.SPEED_INFUSION || buffid == ThunderBreaker.SPEED_INFUSION || buffid == Corsair.SPEED_INFUSION;
        mplew.writeShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        CCommon.writeLongMask(mplew, statups);
        mplew.writeShort(0);
        for (Pair<MapleBuffStat, Integer> statup : statups) {
            mplew.writeInt(statup.getRight()
                    .shortValue());
            mplew.writeInt(buffid);
            mplew.skip(infusion ? 10 : 5);
            mplew.writeShort(time);
        }
        mplew.writeShort(0);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignFirstDebuff(int cid, long mask) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(mask);
        mplew.writeLong(0);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignDebuff(int cid, long mask) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        mplew.writeLong(0);
        mplew.writeLong(mask);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignBuff(int cid, List<MapleBuffStat> statups) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        CCommon.writeLongMaskFromList(mplew, statups);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignSlowDebuff(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeLongMaskSlowD(mplew);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignChairSkillEffect(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(19);
        mplew.writeShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeLongMaskChair(mplew);

        return mplew.getPacket();
    }

    public static byte[] updatePartyMemberHP(int cid, int curhp, int maxhp) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    /**
     * Guild Name & Mark update packet, thanks to Arnah (Vertisy)
     *
     * @param guildName The Guild name, blank for nothing.
     */
    public static byte[] guildNameChanged(int chrid, String guildName) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_NAME_CHANGED.getValue());
        mplew.writeInt(chrid);
        mplew.writeMapleAsciiString(guildName);
        return mplew.getPacket();
    }

    public static byte[] guildMarkChanged(int chrid, MapleGuild guild) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.GUILD_MARK_CHANGED.getValue());
        mplew.writeInt(chrid);
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        return mplew.getPacket();
    }

    public static byte[] throwGrenade(int cid, Point p, int keyDown, int skillId, int skillLevel) { // packets found thanks to GabrielSin
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.THROW_GRENADE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(p.x);
        mplew.writeInt(p.y);
        mplew.writeInt(keyDown);
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);
        return mplew.getPacket();
    }

    public static byte[] cancelChair(int id) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.CANCEL_CHAIR.getValue());
        if (id < 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }
        return mplew.getPacket();
    }

    private static void addAttackBody(LittleEndianWriter lew, MapleCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
        lew.writeInt(chr.getId());
        lew.write(numAttackedAndDamage);
        lew.write(0x5B);//?
        lew.write(skilllevel);
        if (skilllevel > 0) {
            lew.writeInt(skill);
        }
        lew.write(display);
        lew.write(direction);
        lew.write(stance);
        lew.write(speed);
        lew.write(0x0A);
        lew.writeInt(projectile);
        for (Integer oned : damage.keySet()) {
            List<Integer> onedList = damage.get(oned);
            if (onedList != null) {
                lew.writeInt(oned);
                lew.write(0x0);
                if (skill == 4211006) {
                    lew.write(onedList.size());
                }
                for (Integer eachd : onedList) {
                    lew.writeInt(eachd);
                }
            }
        }
    }

    private static void writeLongMaskSlowD(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeInt(0);
        mplew.writeInt(2048);
        mplew.writeLong(0);
    }

    private static void writeLongMaskChair(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeInt(0);
        mplew.writeInt(262144);
        mplew.writeLong(0);
    }
}
