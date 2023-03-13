function start(ms) {
    const MapleLifeFactory = Java.type('server.life.MapleLifeFactory');
    const Point = Java.type('java.awt.Point');
    var mobId = 3300000 + (Math.floor(Math.random() * 3) + 5);
    var player = ms.getPlayer();
    var map = player.getMap();

    map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(-28, -67));
}