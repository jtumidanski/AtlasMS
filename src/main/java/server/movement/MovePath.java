package server.movement;

import tools.data.input.LittleEndianAccessor;
import tools.data.output.LittleEndianWriter;

import java.awt.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MovePath {
    private final List<Elem> lElem = new LinkedList<>();
    private Point startPosition;

    public void decode(LittleEndianAccessor lea) {
        startPosition = lea.readPos();
        byte size = lea.readByte();
        for (int i = 0; i < size; i++) {
            Elem elem = new Elem();
            elem.decode(lea);
            lElem.add(elem);
        }
    }

    public void encode(LittleEndianWriter lew) {
        lew.writePos(startPosition);
        lew.write(lElem.size());
        for (Elem elem : lElem) {
            elem.encode(lew);
        }
    }

    public static MovePath idle(Point position, byte stance) {
        MovePath movePath = new MovePath();
        movePath.startPosition = position;
        movePath.lElem.add(new Elem(stance, (byte) 0, (short) position.x, (short) position.y, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 0));
        return movePath;
    }

    public List<Elem> Movement() {
        return Collections.unmodifiableList(lElem);
    }
}
