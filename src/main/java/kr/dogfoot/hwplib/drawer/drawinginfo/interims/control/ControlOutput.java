package kr.dogfoot.hwplib.drawer.drawinginfo.interims.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.interims.Output;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public abstract class ControlOutput extends Output implements Comparable<ControlOutput>  {
    protected Area controlArea;

    public abstract int zOrder();

    public abstract int textFlowMethod();

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
