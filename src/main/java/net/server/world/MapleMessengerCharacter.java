package net.server.world;

public record MapleMessengerCharacter(int id, String name, int channelId, int position) {
    public MapleMessengerCharacter(int id, String name, int channelId) {
        this(id, name, channelId, 0);
    }
    public MapleMessengerCharacter setPosition(int position) {
        return new MapleMessengerCharacter(id, name, channelId, position);
    }
}
