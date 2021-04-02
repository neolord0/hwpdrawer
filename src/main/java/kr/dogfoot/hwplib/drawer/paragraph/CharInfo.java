package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;

public class CharInfo {
    public final static CharInfo[] Zero_Array = new CharInfo[0];

    private HWPCharNormal ch;
    private double width;
    private CharShape charShape;
    private int index;
    private int position;
    private long x;

    public CharInfo(HWPCharNormal ch, CharShape charShape, int index, int position) {
        this.ch = ch;
        this.charShape = charShape;
        this.index = index;
        this.position = position;
    }

    public CharInfo calculateWidth(Painter painter) throws UnsupportedEncodingException {
        if (ch.isSpace()) {
            width = charShape.getBaseSize() / 2;
        } else {
            if (ch.getType() == HWPCharType.Normal) {
                width = painter.getCharWidth(ch.getCh(), charShape);
            } else {
                width = 0;
            }
        }
        width = width * charShape.getRelativeSizes().getHangul() / 100;
        width = width * charShape.getRatios().getHangul() / 100;
        return this;
    }

    public HWPCharNormal character() {
        return ch;
    }

    public double width() {
        return width;
    }

    public double widthAddingCharSpace() {
        return width + (width * charShape.getCharSpaces().getHangul() / 100);
    }

    public CharShape charShape() {
        return charShape;
    }

    public long x() {
        return x;
    }

    public void x(long x) {
        this.x = x;
    }

    public int index() {
        return index;
    }

    public int position() {
        return position;
    }
}
