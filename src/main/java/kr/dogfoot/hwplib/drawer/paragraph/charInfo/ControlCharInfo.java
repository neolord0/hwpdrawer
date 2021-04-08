package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public class ControlCharInfo implements CharInfo {
    public HWPCharControlExtend character;
    private CharShape charShape;
    private int index;
    private int position;
    private long x;
    private Control control;
    private long width;
    private long height;

    public ControlCharInfo(HWPCharControlExtend character, CharShape charShape, int index, int position) {
        this.character = character;
        this.charShape = charShape;
        this.index = index;
        this.position = position;
        x = 0;
        control = null;
        width = 0;
        height = 0;
    }

    public ControlCharInfo control(Control control) {
        this.control = control;
        CtrlHeaderGso headerGso = null;
        switch (control.getType()) {
            case Table:
                headerGso = ((ControlTable) control).getHeader();
                break;
            case Gso:
                headerGso = ((GsoControl) control).getHeader();
                break;
        }
        width = headerGso.getWidth();
        height = headerGso.getHeight();
        return this;
    }

    @Override
    public Type type() {
        return Type.Control;
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
        return height;
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

    public HWPCharControlExtend controlCharacter() {
        return character;
    }

    public Control control() {
        return control;
    }
}
