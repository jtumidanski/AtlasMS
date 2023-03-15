package client;

import server.MapleStatEffect;
import server.life.Element;

import java.util.List;

public record Skill(int id, int animationTime, Element element, List<MapleStatEffect> effects, boolean action) {
    public MapleStatEffect getEffect(int level) {
        return effects.get(level - 1);
    }

    public boolean isBeginnerSkill() {
        return id % 10000000 < 10000;
    }

    public int getMaxLevel() {
        return effects.size();
    }

    public boolean isFourthJob() {
        int job = id / 10000;
        if (job == 2212) {
            return false;
        }
        if (id == 22170001 || id == 22171003 || id == 22171004 || id == 22181002 || id == 22181003) {
            return true;
        }
        return job % 10 == 2;
    }
}