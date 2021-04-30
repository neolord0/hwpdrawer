package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.ControlContent;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.GsoContent;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
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

    public void paintControls(TreeSet<ControlContent> controlContents) throws Exception {
        for (ControlContent controlContent : controlContents) {
            paintControl(controlContent);
        }
    }

    public void paintControl(ControlContent controlContent) throws Exception {
        switch (controlContent.type()) {
            case Gso:
                GsoContent gsoContent = (GsoContent) controlContent;

                switch (gsoContent.control().getGsoType()) {
                    case Line:
                        gsoPainter.line(gsoContent);
                        break;
                    case Rectangle:
                        gsoPainter.rectangle(gsoContent);
                        break;
                    case Ellipse:
                        gsoPainter.ellipse(gsoContent);
                        break;
                    case Arc:
                        gsoPainter.arc(gsoContent);
                        break;
                    case Polygon:
                        gsoPainter.polygon(gsoContent);
                        break;
                    case Curve:
                        gsoPainter.curve(gsoContent);
                        break;
                    case Picture:
                        gsoPainter.picture(gsoContent);
                        break;
                    case OLE:
                        gsoPainter.ole(gsoContent);
                        break;
                    case Container:
                        gsoPainter.container(gsoContent);
                        break;
                    case ObjectLinkLine:
                        gsoPainter.objectLinkLine(gsoContent);
                        break;
                }
                break;
            case Table:
                // tablePainter.paint((ControlTable) controlCharInfo.control(), area);
                break;
        }
    }
}
