package kr.dogfoot.hwpdrawer.util;

/**
 * 문서 내에서의 문자 위치를 나타내는 클래스.
 */
public class CharPosition {
    private int paraIndex;
    private int charIndex;

    public CharPosition() {
        this(0, 0);
    }

    public CharPosition(int paraIndex, int charIndex) {
        this.paraIndex = paraIndex;
        this.charIndex = charIndex;
    }

    public int paraIndex() { return paraIndex; }
    public CharPosition paraIndex(int paraIndex) { this.paraIndex = paraIndex; return this; }

    public int charIndex() { return charIndex; }
    public CharPosition charIndex(int charIndex) { this.charIndex = charIndex; return this; }
}
