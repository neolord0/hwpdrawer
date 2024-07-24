package kr.dogfoot.hwpdrawer.drawer.control;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.drawer.control.table.CellDrawer;
import kr.dogfoot.hwpdrawer.drawer.control.table.TableDrawerForDivide;
import kr.dogfoot.hwpdrawer.drawer.control.table.TableDrawerForDivideByCell;
import kr.dogfoot.hwpdrawer.drawer.control.table.TableDrawerForNoDivide;
import kr.dogfoot.hwpdrawer.drawer.paralist.ParaListDrawerForControl;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.control.GsoOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;

import java.util.Queue;

public class ControlDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private final CellDrawer cellDrawer;
    private final TableDrawerForNoDivide tableDrawerForNoDivide;
    private final TableDrawerForDivide tableDrawerForDivide;
    private final TableDrawerForDivideByCell tableDrawerForDivideByCell;

    public ControlDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        cellDrawer = new CellDrawer(input, output);
        tableDrawerForNoDivide = new TableDrawerForNoDivide(input, output, cellDrawer);
        tableDrawerForDivide = new TableDrawerForDivide(input, output, cellDrawer);
        tableDrawerForDivideByCell = new TableDrawerForDivideByCell(input, output, cellDrawer);
    }

    public ControlOutput draw(CharInfoControl controlCharInfo) throws Exception {
        ControlOutput controlOutput = null;
        switch (controlCharInfo.control().getType()) {
            case Gso: {
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

                if (controlOutput != null) {
                    controlOutput.controlCharInfo(controlCharInfo);
                    controlCharInfo.output(controlOutput);
                }
            }
            break;
            case Table:
                controlOutput = table(controlCharInfo);
                break;
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
        return new ParaListDrawerForControl(input, output)
                .draw(paragraphList, textBoxArea);
    }


    private ControlOutput table(CharInfoControl controlCharInfo) throws Exception {
        Queue<TableOutput> tableOutputs = null;
        ControlTable table = (ControlTable) controlCharInfo.control();
        switch (table.getTable().getProperty().getDivideAtPageBoundary()) {
            case NoDivide:
                tableOutputs = tableDrawerForNoDivide.draw(controlCharInfo);
                break;
            case Divide:
                tableOutputs = tableDrawerForDivide.draw(controlCharInfo);
                break;
            case DivideByCell:
                tableOutputs = tableDrawerForDivideByCell.draw(controlCharInfo);
                break;
        }
        if (tableOutputs != null) {
            ControlOutput controlOutput = tableOutputs.poll();
            if (!tableOutputs.isEmpty()) {
                output.addDividedTables(tableOutputs);
            }
            return controlOutput;
        } else {
            return null;
        }
    }

}
