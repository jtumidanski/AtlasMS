/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.reactor;

import client.MapleClient;
import scripting.AbstractScriptManager;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;
import tools.DatabaseConnection;
import tools.FilePrinter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Lerk
 */
public class ReactorScriptManager extends AbstractScriptManager {

    private static ReactorScriptManager instance = new ReactorScriptManager();
    private Map<Integer, List<ReactorDropEntry>> drops = new HashMap<>();

    public static ReactorScriptManager getInstance() {
        return instance;
    }

    public void onHit(MapleClient c, MapleReactor reactor) {
        try {
            Optional<Invocable> iv = initializeInvocable(c, reactor);
            if (iv.isEmpty()) {
                return;
            }

            ReactorActionManager rm = new ReactorActionManager(c, reactor, iv.get());
            iv.get().invokeFunction("hit");
        } catch (final NoSuchMethodException e) {
        } //do nothing, hit is OPTIONAL

        catch (final ScriptException | NullPointerException e) {
            FilePrinter.printError(FilePrinter.REACTOR + reactor.getId() + ".txt", e);
        }
    }

    public void act(MapleClient c, MapleReactor reactor) {
        try {
            Optional<Invocable> iv = initializeInvocable(c, reactor);
            if (iv.isEmpty()) {
                return;
            }

            ReactorActionManager rm = new ReactorActionManager(c, reactor, iv.get());
            iv.get().invokeFunction("act");
        } catch (final ScriptException | NoSuchMethodException | NullPointerException e) {
            FilePrinter.printError(FilePrinter.REACTOR + reactor.getId() + ".txt", e);
        }
    }

    public List<ReactorDropEntry> getDrops(int rid) {
        List<ReactorDropEntry> ret = drops.get(rid);
        if (ret == null) {
            ret = new LinkedList<>();
            try {
                Connection con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = con.prepareStatement("SELECT itemid, chance, questid FROM reactordrops WHERE reactorid = ? AND chance >= 0")) {
                    ps.setInt(1, rid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ret.add(new ReactorDropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("questid")));
                        }
                    }
                }

                con.close();
            } catch (Throwable e) {
                FilePrinter.printError(FilePrinter.REACTOR + rid + ".txt", e);
            }
            drops.put(rid, ret);
        }
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }

    public void touch(MapleClient c, MapleReactor reactor) {
        touching(c, reactor, true);
    }

    public void untouch(MapleClient c, MapleReactor reactor) {
        touching(c, reactor, false);
    }

    private void touching(MapleClient c, MapleReactor reactor, boolean touching) {
        try {
            Optional<Invocable> iv = initializeInvocable(c, reactor);
            if (iv.isEmpty()) {
                return;
            }

            ReactorActionManager rm = new ReactorActionManager(c, reactor, iv.get());
            if (touching) {
                iv.get().invokeFunction("touch");
            } else {
                iv.get().invokeFunction("untouch");
            }
        } catch (final ScriptException | NoSuchMethodException | NullPointerException ute) {
            FilePrinter.printError(FilePrinter.REACTOR + reactor.getId() + ".txt", ute);
        }
    }

    private Optional<Invocable> initializeInvocable(MapleClient c, MapleReactor reactor) {
        Optional<ScriptEngine> engine = getScriptEngine("reactor/" + reactor.getId() + ".js", c);
        if (engine.isEmpty()) {
            return Optional.empty();
        }

        Invocable iv = (Invocable) engine.get();
        ReactorActionManager rm = new ReactorActionManager(c, reactor, iv);
        engine.get().put("rm", rm);

        return Optional.of(iv);
    }
}