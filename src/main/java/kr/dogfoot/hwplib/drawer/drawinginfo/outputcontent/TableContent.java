package kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class TableContent extends ControlContent {
    private ControlTable table;

    public TableContent(ControlTable table) {

    }

    @Override
    public int zOrder() {
        return 0;
    }

    @Override
    public int textFlowMethod() {
        return 0;
    }

    @Override
    public Area calculatedControlArea() {
        return null;
    }

    @Override
    public void adjustTextAreaAndVerticalAlignment() {

    }

    @Override
    public VertRelTo vertRelTo() {
        return null;
    }

    @Override
    public Type type() {
        return null;
    }
}
