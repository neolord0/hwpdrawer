package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public abstract class CharInfo {
    protected HWPChar character;
    protected CharShape charShape;
    protected int index;
    protected int position;
    protected long x;

    public CharInfo(HWPChar character, CharShape charShape, int index, int position) {
        this.character = character;
        this.charShape = charShape;
        this.index = index;
        this.position = position;
        x = 0;
    }

    public abstract Type type();

    public HWPChar character() {
        return character;
    }

    public CharShape charShape() {
        return charShape;
    }

    public int index() {
        return index;
    }

    public int position() {
        return position;
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

    public enum Type {
        Normal,
        Control
    }
}
