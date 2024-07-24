package kr.dogfoot.hwpdrawer.drawer.charInfo;

import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.PositionCalculator;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public class CharInfoControl extends CharInfo {
    public static final CharInfoControl[] Zero_Array = new CharInfoControl[0];

    public static CharInfoControl create(HWPCharControlExtend character, Control control, DrawingInput input, Area currentTextArea) {
        CharInfoControl charInfo = new CharInfoControl(character, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition());
        if (character.getCode() == 11) {
            CtrlHeaderGso gsoHeader = null;
            switch (control.getType()) {
                case Table:
                    gsoHeader = ((ControlTable) control).getHeader();
                    break;
                case Gso:
                    gsoHeader = ((GsoControl) control).getHeader();
                    break;
            }
            if (gsoHeader != null) {
                charInfo
                        .control(control, gsoHeader)
                        .area(input, currentTextArea);
            }
        }
        return charInfo;
    }

    private Control control;
    private ControlOutput output;
    private CtrlHeaderGso gsoHeader;
    private Area areaWithOuterMargin;
    private Area areaWithoutOuterMargin;

    public CharInfoControl(HWPChar character, CharShape charShape, int paraIndex, int index, int position) {
        super(character, charShape, paraIndex, index, position);
        control = null;
    }

    public CharInfoControl(CharInfoControl other) {
        super(other);

        this.control = other.control;
        this.output = other.output;
        this.gsoHeader = other.gsoHeader;
        this.areaWithOuterMargin = new Area(other.areaWithOuterMargin);
        this.areaWithoutOuterMargin = new Area(other.areaWithoutOuterMargin);
    }

    public CharInfoControl control(Control control, CtrlHeaderGso gsoHeader) {
        this.control = control;
        this.gsoHeader = gsoHeader;
        return this;
    }


    public void output(ControlOutput output) {
        this.output = output;
    }

    public ControlOutput output() {
        return output;
    }

    public CharInfoControl area(DrawingInput input, Area currentTextPartArea) {
        areaWithoutOuterMargin = PositionCalculator.singleObject().area(gsoHeader, input, currentTextPartArea);
        areaWithOuterMargin = new Area(areaWithoutOuterMargin)
                .expand(gsoHeader.getOutterMarginLeft(),
                        gsoHeader.getOutterMarginTop(),
                        gsoHeader.getOutterMarginRight(),
                        gsoHeader.getOutterMarginBottom());
        return this;
    }

    @Override
    public Type type() {
        return Type.Control;
    }

    @Override
    public double width() {
        if (isLikeLetter()) {
            return areaWithOuterMargin.width();
        }
        return 0;
    }

    @Override
    public long height() {
        if (isLikeLetter()) {
            return areaWithOuterMargin.height();
        }
        return 0;
    }

    public Control control() {
        return control;
    }

    public CtrlHeaderGso header() {
        return gsoHeader;
    }

    public boolean isLikeLetter() {
        if (gsoHeader == null) {
            return false;
        }
        return gsoHeader.getProperty().isLikeWord();
    }

    public TextFlowMethod textFlowMethod() {
        if (gsoHeader == null) {
            return null;
        }
        return gsoHeader.getProperty().getTextFlowMethod();
    }

    public Area areaWithOuterMargin() {
        return areaWithOuterMargin;
    }

    public Area areaWithoutOuterMargin() {
        return areaWithoutOuterMargin;
    }
}
