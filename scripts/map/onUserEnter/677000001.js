function start(ms) {
    const MapleLifeFactory = Java.type('server.life.MapleLifeFactory');
    const Point = Java.type('java.awt.Point');
    var pos = new Point(461, 61);
    var mobId = 9400612;
    var mobName = "Marbas";

    var player = ms.getPlayer();
    var map = player.getMap();

    if (map.getMonsterById(mobId) != null) {
        return;
    }

    map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), pos);
    player.message(mobName + " has appeared!");
}