/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.Elem;
import server.movement.MovePath;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author Danny (Leifde)
 * @author ExtremeDevilz
 * @author Ronan (HeavenMS)
 */
public final class MoveLifeHandler extends AbstractMovementPacketHandler {

    private static boolean inRangeInclusive(Byte pVal, Integer pMin, Integer pMax) {
        return !(pVal < pMin) || (pVal > pMax);
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleMap map = player.getMap();

        if (player.isChangingMaps()) {  // thanks Lame for noticing mob movement shuffle (mob OID on different maps) happening on map transitions
            return;
        }

        int objectid = slea.readInt();
        Optional<MapleMapObject> mmo = map.getMapObject(objectid);
        if (mmo.isEmpty() || mmo.get()
                .getType() != MapleMapObjectType.MONSTER) {
            return;
        }
        MapleMonster monster = (MapleMonster) mmo.get();
        List<MapleCharacter> banishPlayers = null;


        short moveid = slea.readShort(); // move id
        byte dwFlag = slea.readByte(); // bSomeRand | 8 * (bRushMove | 2 * (2 * nMobCtrlState));
        boolean isNextAtkPossible = (dwFlag & 0xF0) != 0; // is mob should use skill? (saw chronos did 'dwFlag > 0')
        boolean mobMoveStartResult = dwFlag > 0;
        byte nActionAndDir = slea.readByte();

        int skillData = slea.readInt(); // !CMob::DoSkill(v7, (unsigned __int8)dwData, BYTE1(dwData), dwData >> 16)
        byte skillID = (byte) (skillData & 0xFF);
        byte slv = (byte) (skillData >> 8 & 0xFF);
        int delay = skillData >> 16;

        int nMultiTargetSize = slea.readInt();
        for (int i = 0; i < nMultiTargetSize; i++) {
            slea.readInt(); // aMultiTargetForBall[i].x
            slea.readInt(); // aMultiTargetForBall[i].y
        }
        int nRandTimeSize = slea.readInt();
        for (int i = 0; i < nRandTimeSize; i++) {
            slea.readInt(); // m_aRandTimeforAreaAttack[i]
        }

        slea.readByte();
        slea.readInt();
        int start_x = slea.readInt(); // possibly x
        int start_y = slea.readInt(); // possibly y
        slea.readInt();

        MobSkill toUse;
        int useSkillId = 0, useSkillLevel = 0;

        MobSkill nextUse = null;
        int nextSkillId = 0, nextSkillLevel = 0;

        int castPos;
        if (skillID > 0) {
            useSkillId = skillID;
            useSkillLevel = slv;

            castPos = monster.getSkillPos(useSkillId, useSkillLevel);
            if (castPos != -1) {
                toUse = MobSkillFactory.getMobSkill(useSkillId, useSkillLevel);

                if (monster.canUseSkill(toUse, true)) {
                    int animationTime = MapleMonsterInformationProvider.getInstance()
                            .getMobSkillAnimationTime(toUse);
                    if (animationTime > 0 && toUse.getSkillId() != 129) {
                        toUse.applyDelayedEffect(player, monster, true, animationTime);
                    } else {
                        banishPlayers = new LinkedList<>();
                        toUse.applyEffect(player, monster, true, banishPlayers);
                    }
                }
            }
        } else {
//            castPos = (rawActivity - 24) / 2;

//            int atkStatus = monster.canUseAttack(castPos, isSkill);
//            if (atkStatus < 1) {
//                rawActivity = -1;
//                pOption = 0;
//            }
        }

        int mobMp = monster.getMp();
        if (mobMoveStartResult) {
            int noSkills = monster.getNoSkills();
            if (noSkills > 0) {
                int rndSkill = Randomizer.nextInt(noSkills);

                Pair<Integer, Integer> skillToUse = monster.getSkills()
                        .get(rndSkill);
                nextSkillId = skillToUse.getLeft();
                nextSkillLevel = skillToUse.getRight();
                nextUse = MobSkillFactory.getMobSkill(nextSkillId, nextSkillLevel);

                if (!(nextUse != null && monster.canUseSkill(nextUse, false) && nextUse.getHP() >= (int) (((float) monster.getHp() / monster.getMaxHp()) * 100) && mobMp >= nextUse.getMpCon())) {
                    // thanks OishiiKawaiiDesu for noticing mobs trying to cast skills they are not supposed to be able

                    nextSkillId = 0;
                    nextSkillLevel = 0;
                    nextUse = null;
                }
            }
        }

        Point startPos = new Point(start_x, start_y - 2);
        Point serverStartPos = new Point(monster.getPosition());

        Boolean aggro = monster.aggroMoveLifeUpdate(player);
        if (aggro == null) {
            return;
        }

        if (nextUse != null) {
            c.announce(MaplePacketCreator.moveMonsterResponse(objectid, moveid, mobMp, aggro, nextSkillId, nextSkillLevel));
        } else {
            c.announce(MaplePacketCreator.moveMonsterResponse(objectid, moveid, mobMp, aggro));
        }

        final MovePath res = new MovePath();
        res.decode(slea);

        res.Movement()
                .stream()
                .filter(m -> m.getType() == 0)
                .map(m -> m.getPosition((short) -2))
                .forEach(monster::setPosition);
        res.Movement()
                .stream()
                .map(Elem::getBMoveAction)
                .forEach(monster::setStance);

        map.broadcastMessage(player, MaplePacketCreator.moveMonster(objectid, mobMoveStartResult, nActionAndDir, skillData, res), serverStartPos);
        //updatePosition(res, monster, -2); //does this need to be done after the packet is broadcast?
        map.moveMonster(monster, monster.getPosition());

        slea.readByte();
        slea.readByte();
        slea.readByte();
        slea.readByte();
        slea.readInt();

        if (banishPlayers != null) {
            for (MapleCharacter chr : banishPlayers) {
                chr.changeMapBanish(monster.getBanish()
                        .getMap(), monster.getBanish()
                        .getPortal(), monster.getBanish()
                        .getMsg());
            }
        }
    }
}