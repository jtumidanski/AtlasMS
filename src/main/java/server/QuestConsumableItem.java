package server;

import java.util.Map;

public record QuestConsumableItem(int questId, int experience, int grade, Map<Integer, Integer> items) {
    public Integer itemRequirement(int itemId) {
        return items.get(itemId);
    }
}
