package server.partyquest;

import client.MapleCharacter;
import config.YamlConfig;
import connection.packets.CField;
import constants.string.LanguageConstants;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import server.TimerManager;
import server.maps.MapleMap;
import server.maps.MapleReactor;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public class MonsterCarnival {
    private MapleParty p1;
    private MapleParty p2;
    private MapleMap map;
    private ScheduledFuture<?> timer;
    private ScheduledFuture<?> effectTimer;
    private ScheduledFuture<?> respawnTask;
    private long startTime = 0;
    private int summonsR = 0;
    private int summonsB = 0;
    private int room = 0;
    private MapleCharacter leader1;
    private MapleCharacter leader2;
    private MapleCharacter team1;
    private MapleCharacter team2;
    private int redCP;
    private int blueCP;
    private int redTotalCP;
    private int blueTotalCP;
    private int redTimeupCP;
    private int blueTimeupCP;
    private boolean cpq1;

    public MonsterCarnival(MapleParty p1, MapleParty p2, int mapId, boolean cpq1, int room) {
        try {
            this.cpq1 = cpq1;
            this.room = room;
            this.p1 = p1;
            this.p2 = p2;
            Channel cs = Server.getInstance().getChannel(p2.getLeader().getWorld(), p2.getLeader().getChannel()).orElseThrow();
            p1.setEnemy(p2);
            p2.setEnemy(p1);
            map = cs.getMapFactory().getDisposableMap(mapId);
            startTime = System.currentTimeMillis() + 10 * 60 * 1000;
            int redPortal = 0;
            int bluePortal = 0;
            if (map.isPurpleCPQMap()) {
                redPortal = 2;
                bluePortal = 1;
            }
            for (MaplePartyCharacter mpc : p1.getMembers()) {
                if (mpc.getPlayer().isEmpty()) {
                    continue;
                }

                MapleCharacter mc = mpc.getPlayer().get();
                mc.setMonsterCarnival(this);
                mc.setTeam(0);
                mc.setFestivalPoints(0);
                mc.forceChangeMap(map, map.getPortal(redPortal));
                mc.dropMessage(6, LanguageConstants.getMessage(mc, LanguageConstants.CPQEntry));
                if (p1.getLeader().getId() == mc.getId()) {
                    leader1 = mc;
                }
                team1 = mc;
            }
            for (MaplePartyCharacter mpc : p2.getMembers()) {
                if (mpc.getPlayer().isEmpty()) {
                    continue;
                }

                MapleCharacter mc = mpc.getPlayer().get();
                mc.setMonsterCarnival(this);
                mc.setTeam(1);
                mc.setFestivalPoints(0);
                mc.forceChangeMap(map, map.getPortal(bluePortal));
                mc.dropMessage(6, LanguageConstants.getMessage(mc, LanguageConstants.CPQEntry));
                if (p2.getLeader().getId() == mc.getId()) {
                    leader2 = mc;
                }
                team2 = mc;
            }
            if (team1 == null || team2 == null) {
                p1.getMembers().stream()
                        .map(MaplePartyCharacter::getPlayer)
                        .flatMap(Optional::stream)
                        .forEach(c -> c.dropMessage(5, LanguageConstants.getMessage(c, LanguageConstants.CPQError)));
                p2.getMembers().stream()
                        .map(MaplePartyCharacter::getPlayer)
                        .flatMap(Optional::stream)
                        .forEach(c -> c.dropMessage(5, LanguageConstants.getMessage(c, LanguageConstants.CPQError)));
                return;
            }

            timer = TimerManager.getInstance().schedule(this::timeUp, map.getTimeDefault() * 1000L);
            effectTimer = TimerManager.getInstance().schedule(this::complete, map.getTimeDefault() * 1000L - 10 * 1000);
            respawnTask = TimerManager.getInstance().register(this::respawn, YamlConfig.config.server.RESPAWN_INTERVAL);

            cs.initMonsterCarnival(cpq1, room);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void respawn() {
        map.respawn();
    }

    public void playerDisconnected(int characterId) {
        int team = -1;
        team = p1.getMembers().stream()
                .map(MaplePartyCharacter::getId)
                .anyMatch(id -> id == characterId) ? 0 : team;
        team = p2.getMembers().stream()
                .map(MaplePartyCharacter::getId)
                .anyMatch(id -> id == characterId) ? 1 : team;
        if (team == -1) {
            // could not locate team for player in carnival. this is a problem.
            return;
        }

        int finalTeam = team;
        map.getAllPlayers().forEach(c -> playerDisconnectedMessage(c, finalTeam));
        earlyFinish();
    }

    private void playerDisconnectedMessage(MapleCharacter character, int team) {
        character.dropMessage(5, LanguageConstants.getMessage(character, team == 0 ? LanguageConstants.CPQRed : LanguageConstants.CPQBlue) + LanguageConstants.getMessage(character, LanguageConstants.CPQPlayerExit));
    }

    private void earlyFinish() {
        dispose(true);
    }

    public void leftParty(int charid) {
        playerDisconnected(charid);
    }

    protected void dispose() {
        dispose(false);
    }

    public boolean canSummonR() {
        return summonsR < map.getMaxMobs();
    }

    public void summonR() {
        summonsR++;
    }

    public boolean canSummonB() {
        return summonsB < map.getMaxMobs();
    }

    public void summonB() {
        summonsB++;
    }

    public boolean canGuardianR() {
        int teamReactors = (int) map.getAllReactors().stream()
                .map(MapleReactor::getName)
                .map(name -> name.substring(0, 1))
                .filter(id -> id.contentEquals("0"))
                .count();
        return teamReactors < map.getMaxReactors();
    }

    public boolean canGuardianB() {
        int teamReactors = (int) map.getAllReactors().stream()
                .map(MapleReactor::getName)
                .map(name -> name.substring(0, 1))
                .filter(id -> id.contentEquals("1"))
                .count();
        return teamReactors < map.getMaxReactors();
    }

    protected void dispose(boolean warpout) {
        Channel cs = map.getChannelServer();
        MapleMap out;
        if (!cpq1) { // cpq2
            out = cs.getMapFactory().getMap(980030010);
        } else {
            out = cs.getMapFactory().getMap(980000010);
        }
        for (MaplePartyCharacter mpc : leader1.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
            if (mpc.getPlayer().isEmpty()) {
                continue;
            }

            MapleCharacter mc = mpc.getPlayer().get();
            mc.resetCP();
            mc.setTeam(-1);
            mc.setMonsterCarnival(null);
            if (warpout) {
                mc.changeMap(out, out.getPortal(0));
            }
        }
        for (MaplePartyCharacter mpc : leader2.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
            if (mpc.getPlayer().isEmpty()) {
                continue;
            }

            MapleCharacter mc = mpc.getPlayer().get();
            mc.resetCP();
            mc.setTeam(-1);
            mc.setMonsterCarnival(null);
            if (warpout) {
                mc.changeMap(out, out.getPortal(0));
            }
        }
        if (this.timer != null) {
            this.timer.cancel(true);
            this.timer = null;
        }
        if (this.effectTimer != null) {
            this.effectTimer.cancel(true);
            this.effectTimer = null;
        }
        if (this.respawnTask != null) {
            this.respawnTask.cancel(true);
            this.respawnTask = null;
        }
        redTotalCP = 0;
        blueTotalCP = 0;
        leader1.getParty().ifPresent(p -> p.setEnemy(null));
        leader2.getParty().ifPresent(p -> p.setEnemy(null));
        map.dispose();
        map = null;

        cs.finishMonsterCarnival(cpq1, room);
    }

    public ScheduledFuture<?> getTimer() {
        return this.timer;
    }

    private void finish(int winningTeam) {
        try {
            Channel cs = map.getChannelServer();
            if (winningTeam == 0) {
                for (MaplePartyCharacter mpc : leader1.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
                    if (mpc.getPlayer().isEmpty()) {
                        continue;
                    }

                    MapleCharacter mc = mpc.getPlayer().get();
                    mc.gainFestivalPoints(this.redTotalCP);
                    mc.setMonsterCarnival(null);
                    if (cpq1) {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 2), cs.getMapFactory().getMap(map.getId() + 2).getPortal(0));
                    } else {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 200), cs.getMapFactory().getMap(map.getId() + 200).getPortal(0));
                    }
                    mc.setTeam(-1);
                    mc.dispelDebuffs();
                }
                for (MaplePartyCharacter mpc : leader2.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
                    if (mpc.getPlayer().isEmpty()) {
                        continue;
                    }

                    MapleCharacter mc = mpc.getPlayer().get();

                    mc.gainFestivalPoints(this.blueTotalCP);
                    mc.setMonsterCarnival(null);
                    if (cpq1) {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 3), cs.getMapFactory().getMap(map.getId() + 3).getPortal(0));
                    } else {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 300), cs.getMapFactory().getMap(map.getId() + 300).getPortal(0));
                    }
                    mc.setTeam(-1);
                    mc.dispelDebuffs();
                }
            } else if (winningTeam == 1) {
                for (MaplePartyCharacter mpc : leader2.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
                    if (mpc.getPlayer().isEmpty()) {
                        continue;
                    }

                    MapleCharacter mc = mpc.getPlayer().get();
                    mc.gainFestivalPoints(this.blueTotalCP);
                    mc.setMonsterCarnival(null);
                    if (cpq1) {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 2), cs.getMapFactory().getMap(map.getId() + 2).getPortal(0));
                    } else {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 200), cs.getMapFactory().getMap(map.getId() + 200).getPortal(0));
                    }
                    mc.setTeam(-1);
                    mc.dispelDebuffs();
                }
                for (MaplePartyCharacter mpc : leader1.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
                    if (mpc.getPlayer().isEmpty()) {
                        continue;
                    }

                    MapleCharacter mc = mpc.getPlayer().get();
                    mc.gainFestivalPoints(this.redTotalCP);
                    mc.setMonsterCarnival(null);
                    if (cpq1) {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 3), cs.getMapFactory().getMap(map.getId() + 3).getPortal(0));
                    } else {
                        mc.changeMap(cs.getMapFactory().getMap(map.getId() + 300), cs.getMapFactory().getMap(map.getId() + 300).getPortal(0));
                    }
                    mc.setTeam(-1);
                    mc.dispelDebuffs();
                }
            }
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void timeUp() {
        int cp1 = this.redTimeupCP;
        int cp2 = this.blueTimeupCP;
        if (cp1 == cp2) {
            extendTime();
            return;
        }
        if (cp1 > cp2) {
            finish(0);
        } else {
            finish(1);
        }
    }

    public long getTimeLeft() {
        return (startTime - System.currentTimeMillis());
    }

    public int getTimeLeftSeconds() {
        return (int) (getTimeLeft() / 1000);
    }

    private void extendTime() {
        for (MapleCharacter chrMap : map.getAllPlayers()) {
            chrMap.dropMessage(5, LanguageConstants.getMessage(chrMap, LanguageConstants.CPQExtendTime));
        }
        startTime = System.currentTimeMillis() + 3 * 60 * 1000;

        map.broadcastMessage(CField.getClock(3 * 60));

        timer = TimerManager.getInstance().schedule(this::timeUp, map.getTimeExpand() * 1000L);
        effectTimer = TimerManager.getInstance().schedule(this::complete, map.getTimeExpand() * 1000L - 10 * 1000); // thanks Vcoc for noticing a time set issue here
    }

    public void complete() {
        int cp1 = this.redTotalCP;
        int cp2 = this.blueTotalCP;

        this.redTimeupCP = cp1;
        this.blueTimeupCP = cp2;

        if (cp1 == cp2) {
            return;
        }
        boolean redWin = cp1 > cp2;
        int chnl = leader1.getClient().getChannel();
        int chnl1 = leader2.getClient().getChannel();
        if (chnl != chnl1) {
            throw new RuntimeException("Os lideres estao em canais diferentes.");
        }

        map.killAllMonsters();
        for (MaplePartyCharacter mpc : leader1.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
            if (mpc.getPlayer().isEmpty()) {
                continue;
            }

            MapleCharacter mc = mpc.getPlayer().get();
            if (redWin) {
                mc.announce(CField.showEffect("quest/carnival/win"));
                mc.announce(CField.playSound("MobCarnival/Win"));
                mc.dispelDebuffs();
            } else {
                mc.announce(CField.showEffect("quest/carnival/lose"));
                mc.announce(CField.playSound("MobCarnival/Lose"));
                mc.dispelDebuffs();
            }
        }
        for (MaplePartyCharacter mpc : leader2.getParty().map(MapleParty::getMembers).orElse(Collections.emptyList())) {
            if (mpc.getPlayer().isEmpty()) {
                continue;
            }

            MapleCharacter mc = mpc.getPlayer().get();
            if (!redWin) {
                mc.announce(CField.showEffect("quest/carnival/win"));
                mc.announce(CField.playSound("MobCarnival/Win"));
                mc.dispelDebuffs();
            } else {
                mc.announce(CField.showEffect("quest/carnival/lose"));
                mc.announce(CField.playSound("MobCarnival/Lose"));
                mc.dispelDebuffs();
            }
        }
    }

    public MapleParty getRed() {
        return p1;
    }

    public void setRed(MapleParty p1) {
        this.p1 = p1;
    }

    public MapleParty getBlue() {
        return p2;
    }

    public void setBlue(MapleParty p2) {
        this.p2 = p2;
    }

    public int getTotalCP(int team) {
        if (team == 0) {
            return redTotalCP;
        } else if (team == 1) {
            return blueTotalCP;
        } else {
            throw new RuntimeException("Equipe desconhecida");
        }
    }

    public void setTotalCP(int totalCP, int team) {
        if (team == 0) {
            this.redTotalCP = totalCP;
        } else if (team == 1) {
            this.blueTotalCP = totalCP;
        }
    }

    public int getCP(int team) {
        if (team == 0) {
            return redCP;
        } else if (team == 1) {
            return blueCP;
        } else {
            throw new RuntimeException("Equipe desconhecida" + team);
        }
    }

    public void setCP(int CP, int team) {
        if (team == 0) {
            this.redCP = CP;
        } else if (team == 1) {
            this.blueCP = CP;
        }
    }

    public int getRoom() {
        return this.room;
    }

    public MapleMap getEventMap() {
        return this.map;
    }
}
