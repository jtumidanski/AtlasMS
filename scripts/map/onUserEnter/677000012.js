function start(ms) {
    const MapleLifeFactory = Java.type('server.life.MapleLifeFactory');
    const Point = Java.type('java.awt.Point');
    var pos = new Point(842, 0);
    var mobId = 9400633;
    var mobName = "Astaroth";

    var player = ms.getPlayer();
    var map = player.getMap();

    if (map.getMonsterById(mobId) != null) {
        return;
    }

    map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), pos);
    player.message(mobName + " has appeared!");
}