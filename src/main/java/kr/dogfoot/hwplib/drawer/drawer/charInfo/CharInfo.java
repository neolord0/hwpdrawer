package kr.dogfoot.hwplib.drawer.drawer.charInfo;

import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public abstract class CharInfo {
    protected HWPChar character;
    protected CharShape charShape;
    protected int paraIndex;
    protected int index;
    protected int position;
    protected long x;

    public CharInfo(HWPChar character, CharShape charShape, int paraIndex, int index, int position) {
        this.character = character;
        this.charShape = charShape;
        this.paraIndex = paraIndex;
        this.index = index;
        this.position = position;
        x = 0;
    }

    public CharInfo(CharInfo other) {
        this.character = other.character;
        this.charShape = other.charShape;
        this.paraIndex = other.paraIndex;
        this.index = other.index;
        this.position = other.position;
        this.x = other.x;
    }

    public abstract Type type();

    public HWPChar character() {
        return character;
    }

    public CharShape charShape() {
        return charShape;
    }

    public int paraIndex() {
        return paraIndex;
    }

    public int index() {
        return index;
    }

    public long x() {
        return x;
    }

    public void x(long x) {
        this.x = x;
    }

    public abstract double width();

    public double widthAddingCharSpace() {
        return width() + (width() * charShape.getCharSpaces().getHangul() / 100);
    }

    public abstract long height();

    public boolean equals(CharInfo that) {
        return this.paraIndex == that.paraIndex && this.index == that.index;
    }

    public int prePosition() {
        return position - character.getCharSize();
    }

    public TextPosition position() {
        return new TextPosition(paraIndex, index, prePosition());
    }

    public enum Type {
        Normal,
        Control
    }
}
