package kr.dogfoot.hwpdrawer.output.control;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.output.Output;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public abstract class ControlOutput extends Output implements Comparable<ControlOutput> {
    public static final ControlOutput[] Zero_Array = new ControlOutput[0];

    protected Area areaWithoutOuterMargin;
    private CharInfoControl controlCharInfo;

    public abstract int zOrder();

    public abstract TextFlowMethod textFlowMethod();

    public abstract VertRelTo vertRelTo();

    public abstract void move(long offsetX, long offsetY);

    public abstract void adjustTextBoxAreaAndVerticalAlignment();

    public Area areaWithoutOuterMargin() {
        return areaWithoutOuterMargin;
    }

    public void areaWithoutOuterMargin(Area controlArea) {
        this.areaWithoutOuterMargin = controlArea;
    }

    public abstract Area areaWithOuterMargin();

    @Override
    public int compareTo(ControlOutput o) {
        if (zOrder() > o.zOrder())
            return 1;
        else if (zOrder() == o.zOrder())
            return 0;
        else
            return -1;
    }

    public CharInfoControl controlCharInfo() {
        return controlCharInfo;
    }

    public void controlCharInfo(CharInfoControl controlCharInfo) {
        this.controlCharInfo = controlCharInfo;
    }

    public boolean isDividedTable() {
        return type().isTable() && ((TableOutput) this).divided();
    }
}
