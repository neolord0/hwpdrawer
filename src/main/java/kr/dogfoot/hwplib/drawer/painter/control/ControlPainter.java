package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;

import java.util.TreeSet;

public class ControlPainter {
    private GsoPainter gsoPainter;
    private TablePainter tablePainter;

    public ControlPainter(Painter painter, DrawingInfo info) {
        gsoPainter = new GsoPainter(painter, info);
        tablePainter = new TablePainter(painter, info);
    }

    public void paintControls(TreeSet<ControlCharInfo> controlCharInfos) throws Exception {
        for (ControlCharInfo controlCharInfo : controlCharInfos) {
            paint(controlCharInfo);
        }
    }

    private void paint(ControlCharInfo controlCharInfo) throws Exception {
        if (controlCharInfo.control() == null) {
            return;
        }
        switch (controlCharInfo.control().getType()) {
            case Gso:
                GsoControl gso = (GsoControl) controlCharInfo.control();
                switch(gso.getGsoType()) {
                    case Line:
                        gsoPainter.line((ControlLine) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Rectangle:
                        gsoPainter.rectangle((ControlRectangle) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Ellipse:
                        gsoPainter.ellipse((ControlEllipse) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Arc:
                        gsoPainter.arc((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Polygon:
                        gsoPainter.polygon((ControlPolygon) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Curve:
                        gsoPainter.curve((ControlCurve) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Picture:
                        gsoPainter.picture((ControlPicture) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case OLE:
                        gsoPainter.ole((ControlOLE) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Container:
                        gsoPainter.container((ControlContainer) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case ObjectLinkLine:
                        gsoPainter.objectLinkLine((ControlObjectLinkLine) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                }
                break;
            case Table:
                tablePainter.paint((ControlTable) controlCharInfo.control(), controlCharInfo.areaWithoutOuterMargin());
                break;
        }
    }
}
