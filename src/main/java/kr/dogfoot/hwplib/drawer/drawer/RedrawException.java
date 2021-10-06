package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.util.CharPosition;

public class RedrawException extends Exception {
    private CharPosition position;
    private long startY;

    public RedrawException(int paraIndex, int charIndex, int charPosition, long startY) {
        this.position = new CharPosition(paraIndex, charIndex, charPosition);
        this.startY = startY;
    }

    public CharPosition position() {
        return position;
    }

    public long startY() {
        return startY;
    }
}
