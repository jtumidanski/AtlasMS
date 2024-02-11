package server.movement;

import tools.data.input.LittleEndianAccessor;
import tools.data.output.LittleEndianWriter;

import java.awt.*;

public class Elem {

    private byte bMoveAction;
    private byte bStat;
    private short x;
    private short y;
    private short vx;
    private short vy;
    private short fh;
    private short fhFallStart;
    private short xOffset;
    private short yOffset;
    private short tElapse;
    private byte type;

    public Elem() {
    }

    public Elem(byte bMoveAction, byte bStat, short x, short y, short vx, short vy, short fh, short fhFallStart, short xOffset, short yOffset, short tElapse, byte type) {
        this.bMoveAction = bMoveAction;
        this.bStat = bStat;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.fh = fh;
        this.fhFallStart = fhFallStart;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.tElapse = tElapse;
        this.type = type;
    }

    public Point getPosition(short yOffset) {
        return new Point(x, y + yOffset);
    }

    public byte getType() {
        return type;
    }

    public byte getBMoveAction() {
        return bMoveAction;
    }

    public void decode(LittleEndianAccessor lea) {
        type = lea.readByte();

        switch (type) {
            case 0: // normal move
            case 5:
            case 15:
            case 17:
                x = lea.readShort();
                y = lea.readShort();
                vx = lea.readShort();
                vy = lea.readShort();
                fh = lea.readShort();
                if (type == 15) {
                    fhFallStart = lea.readShort();
                }
                xOffset = lea.readShort();
                yOffset = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 3:
            case 4: // tele... -.-
            case 7: // assaulter
            case 8: // assassinate
            case 9: // rush
            case 11: //chair
                x = lea.readShort();
                y = lea.readShort();
                fh = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 14:
                vx = lea.readShort();
                vy = lea.readShort();
                fhFallStart = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 23:
                x = lea.readShort();
                y = lea.readShort();
                vx = lea.readShort();
                vy = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 1:
            case 2:
            case 6: // fj
            case 12:
            case 13: // Shot-jump-back thing
            case 16: // Float
            case 18:
            case 19: // Springs on maps
            case 20: // Aran Combat Step
            case 22:
            case 24:
                vx = lea.readShort();
                vy = lea.readShort();
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
            case 10: // Change Equip
                bStat = lea.readByte();
                break;
            default:
                bMoveAction = lea.readByte();
                tElapse = lea.readShort();
                break;
        }
    }

    public void encode(LittleEndianWriter lew) {
        lew.write(type);
        switch (type) {
            case 0: // normal move
            case 5:
            case 15:
            case 17:
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.writeShort(fh);
                if (type == 15) {
                    lew.writeShort(fhFallStart);
                }
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 3:
            case 4: // tele... -.-
            case 7: // assaulter
            case 8: // assassinate
            case 9: // rush
            case 11: //chair
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(fh);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 14:
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.writeShort(fhFallStart);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 23:
                lew.writeShort(x);
                lew.writeShort(y);
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 1:
            case 2:
            case 6: // fj
            case 12:
            case 13: // Shot-jump-back thing
            case 16: // Float
            case 18:
            case 19: // Springs on maps
            case 20: // Aran Combat Step
            case 22:
            case 24:
                lew.writeShort(vx);
                lew.writeShort(vy);
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
            case 10: // Change Equip
                lew.write(bStat);
                break;
            default:
                lew.write(bMoveAction);
                lew.writeShort(tElapse);
                break;
        }
    }
}
