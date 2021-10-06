package kr.dogfoot.hwplib.drawer.util;

public class CharPosition {
    public final static CharPosition ParaList_Start_Position = new CharPosition();

    private int paraIndex;
    private int charIndex;
    private int charPosition;

    public CharPosition() {
        this.paraIndex = 0;
        this.charIndex = 0;
        this.charPosition = 0;
    }

    public CharPosition(int paraIndex, int charIndex, int charPosition) {
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

    public boolean equals(CharPosition other) {
        return paraIndex == other.paraIndex && charIndex == other.charIndex;
    }

    public String toString() {
        return  paraIndex + ":" + charIndex;
    }
}
