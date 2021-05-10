package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.GsoOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.table.TableOutput;
import kr.dogfoot.hwplib.drawer.painter.Painter;

import java.util.Set;

public class ControlPainter {
    private GsoPainter gsoPainter;
    private TablePainter tablePainter;

    public ControlPainter(Painter painter, DrawingInfo info) {
        gsoPainter = new GsoPainter(painter, info);
        tablePainter = new TablePainter(painter, info);
    }

    public void paintControls(Set<ControlOutput> controlOuputs) throws Exception {
        for (ControlOutput controlOutput : controlOuputs) {
            paintControl(controlOutput);
        }
    }

    public void paintControl(ControlOutput controlOutput) throws Exception {
        switch (controlOutput.type()) {
            case Gso:
                GsoOutput gsoOutput = (GsoOutput) controlOutput;

                switch (gsoOutput.gso().getGsoType()) {
                    case Line:
                        gsoPainter.line(gsoOutput);
                        break;
                    case Rectangle:
                        gsoPainter.rectangle(gsoOutput);
                        break;
                    case Ellipse:
                        gsoPainter.ellipse(gsoOutput);
                        break;
                    case Arc:
                        gsoPainter.arc(gsoOutput);
                        break;
                    case Polygon:
                        gsoPainter.polygon(gsoOutput);
                        break;
                    case Curve:
                        gsoPainter.curve(gsoOutput);
                        break;
                    case Picture:
                        gsoPainter.picture(gsoOutput);
                        break;
                    case OLE:
                        gsoPainter.ole(gsoOutput);
                        break;
                    case Container:
                        gsoPainter.container(gsoOutput);
                        break;
                    case ObjectLinkLine:
                        gsoPainter.objectLinkLine(gsoOutput);
                        break;
                }
                break;
            case Table:
                tablePainter.paint((TableOutput) controlOutput);
                break;
        }
    }
}
