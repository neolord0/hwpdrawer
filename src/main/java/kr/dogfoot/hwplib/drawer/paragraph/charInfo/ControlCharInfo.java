package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.PositionCalculator;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public class ControlCharInfo extends CharInfo {
    public static ControlCharInfo create(HWPCharControlExtend character, Control control, DrawingInfo info) {
        ControlCharInfo charInfo = new ControlCharInfo(character, info.charShape(), info.charIndex(), info.charPosition());
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
                        .area(info);
            }
        }
        return charInfo;
    }

    private Control control;
    private ControlOutput output;
    private CtrlHeaderGso gsoHeader;
    private Area areaWithOuterMargin;
    private Area areaWithoutOuterMargin;

    public ControlCharInfo(HWPChar character, CharShape charShape, int index, int position) {
        super(character, charShape, index, position);
        control = null;
    }

    public ControlCharInfo control(Control control, CtrlHeaderGso gsoHeader) {
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

    public ControlCharInfo area(DrawingInfo info) {
        areaWithoutOuterMargin = PositionCalculator.singleObject().area(gsoHeader, info);
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

    public short textFlowMethod() {
        if (gsoHeader == null) {
            return -1;
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
