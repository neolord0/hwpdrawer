package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.util.TextPosition;

public class RedrawException extends Exception {
    private TextPosition position;
    private long startY;

    public RedrawException(int paraIndex, int charIndex, int charPosition, long startY) {
        this.position = new TextPosition(paraIndex, charIndex, charPosition);
        this.startY = startY;
    }

    public TextPosition position() {
        return position;
    }

    public long startY() {
        return startY;
    }
}
