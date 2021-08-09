package kr.dogfoot.hwplib.drawer.drawer.control;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.drawer.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.drawer.control.table.TableResult;
import kr.dogfoot.hwplib.drawer.drawer.control.table.TableDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;

public class ControlDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final TableDrawer tableDrawer;

    public ControlDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        tableDrawer = new TableDrawer(input, output);
    }

    public ControlOutput draw(ControlCharInfo controlCharInfo) throws Exception {
        ControlOutput controlOutput = null;
        switch (controlCharInfo.control().getType()) {
            case Gso:
                GsoControl gso = (GsoControl) controlCharInfo.control();
                switch (gso.getGsoType()) {
                    case Line:
                        controlOutput = line((ControlLine) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Rectangle:
                        controlOutput = rectangle((ControlRectangle) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Ellipse:
                        controlOutput = ellipse((ControlEllipse) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Arc:
                        controlOutput = arc((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Polygon:
                        controlOutput = polygon((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Curve:
                        controlOutput = curve((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Picture:
                        controlOutput = picture((ControlPicture) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case OLE:
                        controlOutput = ole((ControlOLE) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case Container:
                        controlOutput = container((ControlContainer) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                    case ObjectLinkLine:
                        controlOutput = objectLinkLine((ControlObjectLinkLine) gso, controlCharInfo.areaWithoutOuterMargin());
                        break;
                }
                break;
            case Table:
                TableResult tableDrawResult = tableDrawer.draw(controlCharInfo);
                if (tableDrawResult.split()) {
                    input.addSplitTableDrawResult(tableDrawResult);
                }
                controlOutput = tableDrawResult.tableOutputForCurrentPage();
                break;
        }

        if (controlOutput != null) {
            controlOutput.controlCharInfo(controlCharInfo);
            controlCharInfo.output(controlOutput);
        }
        return controlOutput;
    }

    private GsoOutput line(ControlLine line, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput rectangle(ControlRectangle rectangle, Area areaWithoutOuterMargin) throws Exception {
        GsoOutput output2 = output.startGso(rectangle, areaWithoutOuterMargin);
        TextBox textBox = rectangle.getTextBox();
        if (rectangle.getTextBox() != null) {
            output2
                    .textMargin(
                            textBox.getListHeader().getLeftMargin(),
                            textBox.getListHeader().getTopMargin(),
                            textBox.getListHeader().getRightMargin(),
                            textBox.getListHeader().getBottomMargin())
                    .verticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());

            long calculatedContentHeight = drawTextBox(textBox.getParagraphList(), output2.textBoxArea().widthHeight());
            output2.calculatedContentHeight(calculatedContentHeight);
        }

        output.endGso();
        return output2;
    }


    private GsoOutput ellipse(ControlEllipse ellipse, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput arc(ControlArc arc, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput polygon(ControlArc polygon, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput curve(ControlArc curve, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput picture(ControlPicture picture, Area areaWithoutOuterMargin) {
        GsoOutput output2 = output.startGso(picture, areaWithoutOuterMargin);

        output.endGso();
        return output2;
    }

    private GsoOutput ole(ControlOLE ole, Area areaWithoutOuterMargin) {
        return null;
    }

    private GsoOutput container(ControlContainer container, Area areaWithoutOuterMargin) {
        GsoOutput output2 = output.startGso(container, areaWithoutOuterMargin);

        output.endGso();
        return output2;
    }

    private GsoOutput objectLinkLine(ControlObjectLinkLine objectLinkLine, Area controlArea) {
        return null;
    }

    private long drawTextBox(ParagraphList paragraphList, Area textBoxArea) throws Exception {
        ParaListDrawer paragraphListDrawer = new ParaListDrawer(input, output);
        return paragraphListDrawer.drawForControl(paragraphList, textBoxArea);
    }
}
