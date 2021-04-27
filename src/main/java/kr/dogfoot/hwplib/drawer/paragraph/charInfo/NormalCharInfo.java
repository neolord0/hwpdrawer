package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.drawer.util.CharacterSizeGetter;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;

public class NormalCharInfo extends CharInfo {
    private double width;

    public NormalCharInfo(HWPChar character, CharShape charShape, int index, int position) {
        super(character, charShape, index, position);
    }

    public NormalCharInfo calculateWidth() throws UnsupportedEncodingException {
        if (character.isSpace()) {
            width = charShape.getBaseSize() / 2;
        } else {
            if (character.getType() == HWPCharType.Normal) {
                width = CharacterSizeGetter.singleObject().getCharWidth(normalCharacter().getCh(), charShape);
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
    public long height() {
        return charShape.getBaseSize();
    }

    @Override
    public double width() {
        return width;
    }

    public HWPCharNormal normalCharacter() {
        return (HWPCharNormal) character;
    }
}
