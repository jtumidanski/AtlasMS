package connection.packets;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleMount;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.newyear.NewYearCardRecord;
import connection.constants.SendOpcode;
import constants.inventory.ItemConstants;
import net.server.guild.MapleGuildSummary;
import server.maps.MapleMiniGame;
import server.maps.MaplePlayerShop;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.Set;

public class CUserPool {
    /**
     * Gets a packet spawning a player as a mapobject to other clients.
     *
     * @param target        The client receiving this packet.
     * @param chr           The character to spawn to other clients.
     * @param enteringField Whether the character to spawn is not yet present in the map or already is.
     * @return The spawn player packet.
     */
    public static byte[] spawnPlayerMapObject(MapleClient target, MapleCharacter chr, boolean enteringField) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel()); //v83
        mplew.writeMapleAsciiString(chr.getName());
        if (chr.getGuildId() < 1) {
            mplew.writeMapleAsciiString("");
            mplew.write(new byte[6]);
        } else {
            MapleGuildSummary gs = chr.getClient()
                    .getWorldServer()
                    .getGuildSummary(chr.getGuildId(), chr.getWorld());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeMapleAsciiString("");
                mplew.write(new byte[6]);
            }
        }

        writeForeignBuffs(mplew, chr);

        mplew.writeShort(chr.getJob()
                .getId());

                /* replace "mplew.writeShort(chr.getJob().getId())" with this snippet for 3rd person FJ animation on all classes
                if (chr.getJob().isA(MapleJob.HERMIT) || chr.getJob().isA(MapleJob.DAWNWARRIOR2) || chr.getJob().isA(MapleJob.NIGHTWALKER2)) {
			mplew.writeShort(chr.getJob().getId());
                } else {
			mplew.writeShort(412);
                }*/

        CCommon.addCharLook(mplew, chr, false);
        mplew.writeInt(chr.getInventory(MapleInventoryType.CASH)
                .countById(5110000));
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(ItemConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0);

        if (enteringField) {
            Point spawnPos = new Point(chr.getPosition());
            spawnPos.y -= 42;
            mplew.writePos(spawnPos);
            mplew.write(6);
        } else {
            mplew.writePos(chr.getPosition());
            mplew.write(chr.getStance());
        }

        mplew.writeShort(0);//chr.getFh()
        mplew.write(0);
        MaplePet[] pet = chr.getPets();
        for (int i = 0; i < 3; i++) {
            if (pet[i] != null) {
                CCommon.addPetInfo(mplew, pet[i], false);
            }
        }
        mplew.write(0); //end of pets
        mplew.writeInt(chr.getMount()
                .map(MapleMount::getLevel)
                .orElse(1));
        mplew.writeInt(chr.getMount()
                .map(MapleMount::getExp)
                .orElse(0));
        mplew.writeInt(chr.getMount()
                .map(MapleMount::getTiredness)
                .orElse(0));

        MaplePlayerShop mps = chr.getPlayerShop();
        if (mps != null && mps.isOwner(chr)) {
            if (mps.hasFreeSlot()) {
                CCommon.addAnnounceBox(mplew, mps, mps.getVisitors().length);
            } else {
                CCommon.addAnnounceBox(mplew, mps, 1);
            }
        } else {
            MapleMiniGame miniGame = chr.getMiniGame();
            if (miniGame != null && miniGame.isOwner(chr)) {
                if (miniGame.hasFreeSlot()) {
                    CCommon.addAnnounceBox(mplew, miniGame, 1, 0);
                } else {
                    CCommon.addAnnounceBox(mplew, miniGame, 2, miniGame.isMatchInProgress() ? 1 : 0);
                }
            } else {
                mplew.write(0);
            }
        }

        if (chr.getChalkboard()
                .isPresent()) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getChalkboard()
                    .get());
        } else {
            mplew.write(0);
        }
        CCommon.addRingLook(mplew, chr, true);  // crush
        CCommon.addRingLook(mplew, chr, false); // friendship
        CCommon.addMarriageRingLook(target, mplew, chr);
        encodeNewYearCardInfo(mplew, chr);  // new year seems to crash sometimes...
        mplew.write(0);
        mplew.write(0);
        mplew.write(chr.getTeam());//only needed in specific fields
        return mplew.getPacket();
    }

    public static byte[] removePlayerFromMap(int cid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    private static void writeForeignBuffs(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(0);
        mplew.writeShort(0); //v83
        mplew.write(0xFC);
        mplew.write(1);
        if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
            mplew.writeInt(2);
        } else {
            mplew.writeInt(0);
        }
        long buffmask = 0;
        Integer buffvalue = null;
        if ((chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null || chr.getBuffedValue(MapleBuffStat.WIND_WALK) != null) && !chr.isHidden()) {
            buffmask |= MapleBuffStat.DARKSIGHT.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
            buffmask |= MapleBuffStat.COMBO.getValue();
            buffvalue = chr.getBuffedValue(MapleBuffStat.COMBO);
        }
        if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
            buffmask |= MapleBuffStat.SHADOWPARTNER.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SOULARROW) != null) {
            buffmask |= MapleBuffStat.SOULARROW.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
            buffvalue = chr.getBuffedValue(MapleBuffStat.MORPH);
        }
        mplew.writeInt((int) ((buffmask >> 32) & 0xffffffffL));
        if (buffvalue != null) {
            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) { //TEST
                mplew.writeShort(buffvalue);
            } else {
                mplew.write(buffvalue.byteValue());
            }
        }
        mplew.writeInt((int) (buffmask & 0xffffffffL));

        // Energy Charge
        mplew.writeInt(chr.getEnergyBar() == 15000 ? 1 : 0);
        mplew.writeShort(0);
        mplew.skip(4);

        boolean dashBuff = chr.getBuffedValue(MapleBuffStat.DASH) != null;
        // Dash Speed
        mplew.writeInt(dashBuff ? 1 << 24 : 0);
        mplew.skip(11);
        mplew.writeShort(0);
        // Dash Jump
        mplew.skip(9);
        mplew.writeInt(dashBuff ? 1 << 24 : 0);
        mplew.writeShort(0);
        mplew.write(0);

        // Monster Riding
        Integer bv = chr.getBuffedValue(MapleBuffStat.MONSTER_RIDING);
        if (bv != null) {
            mplew.writeInt(chr.getMount()
                    .map(MapleMount::getItemId)
                    .orElse(0));
            mplew.writeInt(chr.getMount()
                    .map(MapleMount::getSkillId)
                    .orElse(0));
        } else {
            mplew.writeLong(0);
        }

        int CHAR_MAGIC_SPAWN = Randomizer.nextInt();    // skill references found thanks to Rien dev team
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        // Speed Infusion
        mplew.skip(8);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.write(0);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.writeShort(0);
        // Homing Beacon
        mplew.skip(9);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.writeInt(0);
        // Zombify
        mplew.skip(9);
        mplew.writeInt(CHAR_MAGIC_SPAWN);
        mplew.writeShort(0);
        mplew.writeShort(0);
    }

    private static void encodeNewYearCardInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Set<NewYearCardRecord> newyears = chr.getReceivedNewYearRecords();
        if (!newyears.isEmpty()) {
            mplew.write(1);

            mplew.writeInt(newyears.size());
            for (NewYearCardRecord nyc : newyears) {
                mplew.writeInt(nyc.getId());
            }
        } else {
            mplew.write(0);
        }
    }
}
