package kr.dogfoot.hwpdrawer.painter.html.control;

import kr.dogfoot.hwpdrawer.painter.html.PainterForHTML;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.control.GsoOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;

import java.util.Set;

public class ControlPainter {
    private final GsoPainter gsoPainter;
    private final TablePainter tablePainter;

    public ControlPainter(DrawingInput input, PainterForHTML painter) {
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
