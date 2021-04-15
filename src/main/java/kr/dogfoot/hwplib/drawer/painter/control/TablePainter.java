package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;

public class TablePainter {
    private Painter painter;
    private DrawingInfo info;

    public TablePainter(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;
    }

    public void paint(ControlTable control, Area absoluteArea) {
    }
}
