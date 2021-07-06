package kr.dogfoot.hwplib.drawer.paragraph;

public class RedrawException extends Exception {
    private int paraIndex;
    private int charIndex;
    private int charPosition;
    private long startY;

    public RedrawException(int paraIndex, int charIndex, int charPosition, long startY) {
        this.paraIndex = paraIndex;
        this.charIndex = charIndex;
        this.charPosition = charPosition;
        this.startY = startY;
    }

    public int paraIndex() {
        return paraIndex;
    }

    public int charIndex() {
        return charIndex;
    }

    public int charPosition() {
        return charPosition;
    }

    public long startY() {
        return startY;
    }
}
