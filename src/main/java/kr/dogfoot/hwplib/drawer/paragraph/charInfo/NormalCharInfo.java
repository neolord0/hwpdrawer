package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;

public class NormalCharInfo implements CharInfo {
    private HWPCharNormal character;
    private double width;
    private CharShape charShape;
    private int index;
    private int position;
    private long x;

    public NormalCharInfo(HWPCharNormal ch, CharShape charShape, int index, int position) {
        this.character = ch;
        this.charShape = charShape;
        this.index = index;
        this.position = position;
    }

    public NormalCharInfo calculateWidth(Painter painter) throws UnsupportedEncodingException {
        if (character.isSpace()) {
            width = charShape.getBaseSize() / 2;
        } else {
            if (character.getType() == HWPCharType.Normal) {
                width = painter.getCharWidth(character.getCh(), charShape);
            } else {
                width = 0;
            }
        }
        width = width * charShape.getRelativeSizes().getHangul() / 100;
        width = width * charShape.getRatios().getHangul() / 100;
        return this;
    }

    @Override
    public Type type() {
        return Type.Normal;
    }

    @Override
    public HWPChar character() {
        return character;
    }

    @Override
    public double width() {
        return width;
    }

    @Override
    public double widthAddingCharSpace() {
        return width + (width * charShape.getCharSpaces().getHangul() / 100);
    }

    @Override
    public long height() {
        return charShape.getBaseSize();
    }

    @Override
    public long x() {
        return x;
    }

    @Override
    public void x(long x) {
        this.x = x;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public CharShape charShape() {
        return charShape;
    }

    public HWPCharNormal normalCharacter() {
        return character;
    }
}
