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
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import constants.game.GameConstants;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.MonitoredReentrantLock;
import net.server.audit.locks.factory.MonitoredReentrantLockFactory;
import scripting.portal.PortalScriptManager;
import tools.MaplePacketCreator;

import java.awt.*;

public class MapleGenericPortal implements MaplePortal {

    private String name;
    private String target;
    private Point position;
    private int targetmap;
    private int type;
    private boolean status = true;
    private int id;
    private String scriptName;
    private boolean portalState;
    private MonitoredReentrantLock scriptLock = null;

    public MapleGenericPortal(int type) {
        this.type = type;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    @Override
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public boolean getPortalStatus() {
        return status;
    }

    @Override
    public void setPortalStatus(boolean newStatus) {
        this.status = newStatus;
    }

    @Override
    public int getTargetMapId() {
        return targetmap;
    }

    public void setTargetMapId(int targetmapid) {
        this.targetmap = targetmapid;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    @Override
    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;

        if (scriptName != null) {
            if (scriptLock == null) {
                scriptLock = MonitoredReentrantLockFactory.createLock(MonitoredLockType.PORTAL, true);
            }
        } else {
            scriptLock = null;
        }
    }

    @Override
    public void enterPortal(MapleClient c) {
        boolean changed = false;
        if (getScriptName() != null) {
            try {
                scriptLock.lock();
                try {
                    changed = PortalScriptManager.getInstance().executePortalScript(this, c);
                } finally {
                    scriptLock.unlock();
                }
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        } else if (getTargetMapId() != 999999999) {
            MapleCharacter chr = c.getPlayer();
            if (chr.getChalkboard().isPresent() && GameConstants.isFreeMarketRoom(getTargetMapId())) {
                chr.dropMessage(5, "You cannot enter this map with the chalkboard opened.");
            } else {
                MapleMap to = chr.getEventInstance().map(ei -> ei.getMapInstance(getTargetMapId())).orElse(c.getChannelServer().getMapFactory().getMap(getTargetMapId()));
                MaplePortal pto = to.getPortal(getTarget());
                if (pto == null) {// fallback for missing portals - no real life case anymore - interesting for not implemented areas
                    pto = to.getPortal(0);
                }
                chr.changeMap(to, pto); //late resolving makes this harder but prevents us from loading the whole world at once
                changed = true;
            }
        }
        if (!changed) {
            c.announce(MaplePacketCreator.enableActions());
        }
    }

    @Override
    public boolean getPortalState() {
        return portalState;
    }

    @Override
    public void setPortalState(boolean state) {
        this.portalState = state;
    }
}
