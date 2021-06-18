package kr.dogfoot.hwplib.drawer.interimoutput.control;

import kr.dogfoot.hwplib.drawer.interimoutput.Output;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public abstract class ControlOutput extends Output implements Comparable<ControlOutput> {
    public static final ControlOutput[] Zero_Array = new ControlOutput[0];

    protected Area controlArea;

    public abstract int zOrder();

    public abstract TextFlowMethod textFlowMethod();

    public abstract VertRelTo vertRelTo();

    public abstract void move(long offsetX, long offsetY);

    public abstract void adjustTextAreaAndVerticalAlignment();

    public Area controlArea() {
        return controlArea;
    }

    public void controlArea(Area controlArea) {
        this.controlArea = controlArea;
    }

    @Override
    public int compareTo(ControlOutput o) {
        if (zOrder() > o.zOrder())
            return 1;
        else if (zOrder() == o.zOrder())
            return 0;
        else
            return -1;
    }
}
