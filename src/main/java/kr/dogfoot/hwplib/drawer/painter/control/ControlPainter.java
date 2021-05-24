package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.painter.Painter;

import java.util.Set;

public class ControlPainter {
    private final GsoPainter gsoPainter;
    private final TablePainter tablePainter;

    public ControlPainter(DrawingInput input, Painter painter) {
        gsoPainter = new GsoPainter(input, painter);
        tablePainter = new TablePainter(input, painter);
    }

    public void paintControls(Set<ControlOutput> controlOutputs) throws Exception {
        for (ControlOutput controlOutput : controlOutputs) {
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
