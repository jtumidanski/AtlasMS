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
public enum MapleQuestRequirementType {
    UNDEFINED(-1), JOB(0), ITEM(1), QUEST(2), MIN_LEVEL(3), MAX_LEVEL(4), END_DATE(5), MOB(6), NPC(7), FIELD_ENTER(8), INTERVAL(9), SCRIPT(10), PET(11), MIN_PET_TAMENESS(12), MONSTER_BOOK(13), NORMAL_AUTO_START(14), INFO_NUMBER(15), INFO_EX(16), COMPLETED_QUEST(17), START(18), END(19), DAY_BY_DAY(20), MESO(21), BUFF(22), EXCEPT_BUFF(23);
    final byte type;

    MapleQuestRequirementType(int type) {
        this.type = (byte) type;
    }

    public static MapleQuestRequirementType getByWZName(String name) {
        return switch (name) {
            case "job" -> JOB;
            case "quest" -> QUEST;
            case "item" -> ITEM;
            case "lvmin" -> MIN_LEVEL;
            case "lvmax" -> MAX_LEVEL;
            case "end" -> END_DATE;
            case "mob" -> MOB;
            case "npc" -> NPC;
            case "fieldEnter" -> FIELD_ENTER;
            case "interval" -> INTERVAL;
            case "startscript" -> SCRIPT;
            case "endscript" -> SCRIPT;
            case "pet" -> PET;
            case "pettamenessmin" -> MIN_PET_TAMENESS;
            case "mbmin" -> MONSTER_BOOK;
            case "normalAutoStart" -> NORMAL_AUTO_START;
            case "infoNumber" -> INFO_NUMBER;
            case "infoex" -> INFO_EX;
            case "questComplete" -> COMPLETED_QUEST;
            case "start" -> START;
	/*} else if(name.equals("end")) {   already coded
            return END;*/
            case "daybyday" -> DAY_BY_DAY;
            case "money" -> MESO;
            case "buff" -> BUFF;
            case "exceptbuff" -> EXCEPT_BUFF;
            default -> UNDEFINED;
        };
    }

    public byte getType() {
        return type;
    }
}
