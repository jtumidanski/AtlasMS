package server;

public record RewardItem(int itemId, int period, short probability, short quantity, String effect, String worldMessage) {
}
