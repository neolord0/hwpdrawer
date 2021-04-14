package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlClassifier;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;

import java.util.TreeSet;

public class ControlDrawer {
    private GsoDrawer gsoDrawer;
    private TableDrawer tableDrawer;

    public ControlDrawer(Painter painter, DrawingInfo info) {
        gsoDrawer = new GsoDrawer(painter, info);
        tableDrawer = new TableDrawer(painter, info);
    }

    public void drawControls(TreeSet<ControlClassifier.ControlInfo> controls) throws Exception {
        for (ControlClassifier.ControlInfo controlInfo : controls) {
            draw(controlInfo);
        }
    }

    private void draw(ControlClassifier.ControlInfo controlInfo) throws Exception {
        switch (controlInfo.control().getType()) {
            case Gso:
                GsoControl gso = (GsoControl) controlInfo.control();
                switch(gso.getGsoType()) {
                    case Line:
                        gsoDrawer.line((ControlLine) gso, controlInfo.absoluteArea());
                        break;
                    case Rectangle:
                        gsoDrawer.rectangle((ControlRectangle) gso, controlInfo.absoluteArea());
                        break;
                    case Ellipse:
                        gsoDrawer.ellipse((ControlEllipse) gso, controlInfo.absoluteArea());
                        break;
                    case Arc:
                        gsoDrawer.arc((ControlArc) gso, controlInfo.absoluteArea());
                        break;
                    case Polygon:
                        gsoDrawer.polygon((ControlPolygon) gso, controlInfo.absoluteArea());
                        break;
                    case Curve:
                        gsoDrawer.curve((ControlCurve) gso, controlInfo.absoluteArea());
                        break;
                    case Picture:
                        gsoDrawer.picture((ControlPicture) gso, controlInfo.absoluteArea());
                        break;
                    case OLE:
                        gsoDrawer.ole((ControlOLE) gso, controlInfo.absoluteArea());
                        break;
                    case Container:
                        gsoDrawer.container((ControlContainer) gso, controlInfo.absoluteArea());
                        break;
                    case ObjectLinkLine:
                        gsoDrawer.objectLinkLine((ControlObjectLinkLine) gso, controlInfo.absoluteArea());
                        break;
                }
                break;
            case Table:
                tableDrawer.draw((ControlTable) controlInfo.control(), controlInfo.absoluteArea());
                break;
        }
    }

}
