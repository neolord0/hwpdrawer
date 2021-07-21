package kr.dogfoot.hwplib.drawer.util;

public class TextPosition {
    public final static TextPosition ParaList_Start_Position = new TextPosition();

    private int paraIndex;
    private int charIndex;
    private int charPosition;

    public TextPosition() {
        this.paraIndex = 0;
        this.charIndex = 0;
        this.charPosition = 0;
    }

    public TextPosition(int paraIndex, int charIndex, int charPosition) {
        set(paraIndex, charIndex, charPosition);
    }

    public void set(int paraIndex, int charIndex, int charPosition) {
        this.paraIndex = paraIndex;
        this.charIndex = charIndex;
        this.charPosition = charPosition;
    }

    public int paraIndex() {
        return paraIndex;
    }

    public void paraIndex(int paraIndex) {
        this.paraIndex = paraIndex;
    }

    public int charIndex() {
        return charIndex;
    }

    public void charIndex(int charIndex) {
        this.charIndex = charIndex;
    }

    public int charPosition() {
        return charPosition;
    }

    public void charPosition(int charPosition) {
        this.charPosition = charPosition;
    }
}
