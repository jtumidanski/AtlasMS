package net.server.channel.handlers;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import scripting.event.EventInstanceManager;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MobDamageMobFriendlyHandler extends AbstractMaplePacketHandler {
    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int attacker = slea.readInt();
        slea.readInt();
        int damaged = slea.readInt();

        MapleMap map = c.getPlayer().getMap();

        if (map.getMonsterByOid(attacker).isEmpty()) {
            return;
        }
        map.getMonsterByOid(damaged).ifPresent(m -> handlePacket(c, map, m));
    }

    private static void handlePacket(MapleClient c, MapleMap map, MapleMonster monster) {
        int damage = Randomizer.nextInt(((monster.getMaxHp() / 13 + monster.getPADamage() * 10)) * 2 + 500) / 10; // Formula planned by Beng.

        if (monster.getHp() - damage < 1) {     // friendly dies
            if (monster.getId() == 9300102) {
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Watch Hog has been injured by the aliens. Better luck next time..."));
            } else if (monster.getId() == 9300061) {  //moon bunny
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Moon Bunny went home because he was sick."));
            } else if (monster.getId() == 9300093) {   //tylus
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Tylus has fallen by the overwhelming forces of the ambush."));
            } else if (monster.getId() == 9300137) {   //juliet
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Juliet has fainted in the middle of the combat."));
            } else if (monster.getId() == 9300138) {   //romeo
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Romeo has fainted in the middle of the combat."));
            } else if (monster.getId() == 9400322 || monster.getId() == 9400327 || monster.getId() == 9400332) { //snowman
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Snowman has melted on the heat of the battle."));
            } else if (monster.getId() == 9300162) {   //delli
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Delli vanished after the ambush, sheets still laying on the ground..."));
            }

            map.killFriendlies(monster);
        } else {
            EventInstanceManager eim = map.getEventInstance();
            if (eim != null) {
                eim.friendlyDamaged(monster);
            }
        }

        monster.applyAndGetHpDamage(damage, false);
        int remainingHp = monster.getHp();
        if (remainingHp <= 0) {
            remainingHp = 0;
            map.removeMapObject(monster);
        }

        map.broadcastMessage(MaplePacketCreator.MobDamageMobFriendly(monster, damage, remainingHp), monster.getPosition());
        c.announce(MaplePacketCreator.enableActions());
    }
}