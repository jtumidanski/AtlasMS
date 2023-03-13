function start(ms) {
    const MapleLifeFactory = Java.type('server.life.MapleLifeFactory');
    const Point = Java.type('java.awt.Point');
    var map = ms.getClient().getChannelServer().getMapFactory().getMap(926000000);
    map.resetPQ(1);

    if (map.countMonster(9100013) == 0) {
        map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9100013), new Point(82, 200));
    }

    return (true);
}
