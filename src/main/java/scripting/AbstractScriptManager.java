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
package scripting;

import client.MapleClient;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {
    private ScriptEngineFactory sef;

    protected AbstractScriptManager() {
        sef = new ScriptEngineManager().getEngineByName("graal.js").getFactory();
    }

    protected Optional<ScriptEngine> getScriptEngine(String path) {
        Path scriptFile = Path.of("scripts", path);
        if (!Files.exists(scriptFile)) {
            return Optional.empty();
        }

        ScriptEngine engine = sef.getScriptEngine();
        if (!(engine instanceof GraalJSScriptEngine graalScriptEngine)) {
            throw new IllegalStateException("ScriptEngineFactory did not provide a GraalJSScriptEngine");
        }

        enableScriptHostAccess(graalScriptEngine);

        try (BufferedReader br = Files.newBufferedReader(scriptFile, StandardCharsets.UTF_8)) {
            engine.eval(br);
        } catch (final ScriptException | IOException t) {
            //log.warn("Exception during script eval for file: {}", path, t);
            return Optional.empty();
        }

        return Optional.of(graalScriptEngine);
    }

    protected Optional<ScriptEngine> getScriptEngine(String path, MapleClient c) {
        Optional<ScriptEngine> engine = c.getScriptEngine("scripts/" + path).or(() -> getScriptEngine(path));
        engine.ifPresent(scriptEngine -> c.setScriptEngine(path, scriptEngine));
        return engine;
    }

    /**
     * Allow usage of "Java.type()" in script to look up host class
     */
    private void enableScriptHostAccess(GraalJSScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("polyglot.js.allowHostAccess", true);
        bindings.put("polyglot.js.allowHostClassLookup", true);
    }

    protected void resetContext(String path, MapleClient c) {
        c.removeScriptEngine("scripts/" + path);
    }
}
