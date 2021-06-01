package kr.dogfoot.hwplib.drawer.paragraph.control;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;

public class ControlDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    public ControlDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public ControlOutput draw(ControlCharInfo controlCharInfo) throws Exception {
        switch (controlCharInfo.control().getType()) {
            case Gso:
                GsoControl gso = (GsoControl) controlCharInfo.control();
                switch (gso.getGsoType()) {
                    case Line:
                        return line((ControlLine) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Rectangle:
                        return rectangle((ControlRectangle) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Ellipse:
                        return ellipse((ControlEllipse) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Arc:
                        return arc((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Polygon:
                        return polygon((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Curve:
                        return curve((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Picture:
                        return picture((ControlPicture) gso, controlCharInfo.areaWithoutOuterMargin());
                    case OLE:
                        return ole((ControlOLE) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Container:
                        return container((ControlContainer) gso, controlCharInfo.areaWithoutOuterMargin());
                    case ObjectLinkLine:
                        return objectLinkLine((ControlObjectLinkLine) gso, controlCharInfo.areaWithoutOuterMargin());
                }
                break;
            case Table:
                ControlTable table = (ControlTable) controlCharInfo.control();

                return table(table, controlCharInfo.areaWithoutOuterMargin());
        }
        return null;
    }

    private GsoOutput line(ControlLine line, Area controlArea) {
        return null;
    }

    private GsoOutput rectangle(ControlRectangle rectangle, Area controlArea) throws Exception {
        GsoOutput output2 = output.startGso(rectangle, controlArea);
        TextBox textBox = rectangle.getTextBox();
        if (rectangle.getTextBox() != null) {
            output2
                    .textMargin(
                            textBox.getListHeader().getLeftMargin(),
                            textBox.getListHeader().getTopMargin(),
                            textBox.getListHeader().getRightMargin(),
                            textBox.getListHeader().getBottomMargin())
                    .verticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());

            long calculatedContentHeight = drawTextBox(textBox.getParagraphList(), output2.textArea().widthHeight());
            output2.calculatedContentHeight(calculatedContentHeight);
        }

        output.endGso();
        return output2;
    }


    private GsoOutput ellipse(ControlEllipse ellipse, Area controlArea) {
        return null;
    }

    private GsoOutput arc(ControlArc arc, Area controlArea) {
        return null;
    }

    private GsoOutput polygon(ControlArc polygon, Area controlArea) {
        return null;
    }

    private GsoOutput curve(ControlArc curve, Area controlArea) {
        return null;
    }

    private GsoOutput picture(ControlPicture picture, Area controlArea) {
        GsoOutput output2 = output.startGso(picture, controlArea);

        output.endGso();
        return output2;
    }

    private GsoOutput ole(ControlOLE ole, Area controlArea) {
        return null;
    }

    private GsoOutput container(ControlContainer container, Area controlArea) {
        GsoOutput output2 = output.startGso(container, controlArea);

        output.endGso();
        return output2;
    }

    private GsoOutput objectLinkLine(ControlObjectLinkLine objectLinkLine, Area controlArea) {
        return null;
    }

    private TableOutput table(ControlTable table, Area controlArea) throws Exception {
        TableOutput output2 = output.startTable(table, controlArea);

        for(Row row : table.getRowList()) {
            for (Cell cell : row.getCellList()) {
                drawCell(cell, output2);
            }
        }

        output2.calculateCellPosition();

        output.endTable();
        return output2;
    }

    private void drawCell(Cell cell, TableOutput tableOutput) throws Exception {
        CellOutput output2 = output.startCell(cell, tableOutput);
        if (cell.getParagraphList() != null) {
            output2
                    .textMargin(
                            cell.getListHeader().getLeftMargin(),
                            cell.getListHeader().getTopMargin(),
                            cell.getListHeader().getRightMargin(),
                            cell.getListHeader().getBottomMargin())
                    .verticalAlignment(cell.getListHeader().getProperty().getTextVerticalAlignment());

            long calculatedContentHeight = drawTextBox(cell.getParagraphList(), output2.textArea());
            output2.calculatedContentHeight(calculatedContentHeight);

        }
        tableOutput.addCell(output2);
        output.endCell();
    }

    private long drawTextBox(ParagraphList paragraphList, Area textArea) throws Exception {
        ParaListDrawer paragraphListDrawer = new ParaListDrawer(input, output);
        return paragraphListDrawer.drawForControl(paragraphList, textArea);
    }
}
