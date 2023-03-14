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
package server.quest;

/**
 * @author Matze
 */
public enum MapleQuestActionType {
    UNDEFINED(-1), EXP(0), ITEM(1), NEXTQUEST(2), MESO(3), QUEST(4), SKILL(5), FAME(6), BUFF(7), PETSKILL(8), YES(9), NO(10), NPC(11), MIN_LEVEL(12), NORMAL_AUTO_START(13), PETTAMENESS(14), PETSPEED(15), INFO(16), ZERO(16);
    final byte type;

    MapleQuestActionType(int type) {
        this.type = (byte) type;
    }

    public static MapleQuestActionType getByWZName(String name) {
        return switch (name) {
            case "exp" -> EXP;
            case "money" -> MESO;
            case "item" -> ITEM;
            case "skill" -> SKILL;
            case "nextQuest" -> NEXTQUEST;
            case "pop" -> FAME;
            case "buffItemID" -> BUFF;
            case "petskill" -> PETSKILL;
            case "no" -> NO;
            case "yes" -> YES;
            case "npc" -> NPC;
            case "lvmin" -> MIN_LEVEL;
            case "normalAutoStart" -> NORMAL_AUTO_START;
            case "pettameness" -> PETTAMENESS;
            case "petspeed" -> PETSPEED;
            case "info" -> INFO;
            case "0" -> ZERO;
            default -> UNDEFINED;
        };
    }
}
