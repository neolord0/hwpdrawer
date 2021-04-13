package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;

public class TableDrawer {
    private DrawingInfo info;

    public TableDrawer() {
    }

    public void info(DrawingInfo info) {
        this.info = info;
    }

    public void draw(ControlTable control, Area absoluteArea) {
    }
}
