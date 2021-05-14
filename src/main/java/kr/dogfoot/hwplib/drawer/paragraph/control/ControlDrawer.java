package kr.dogfoot.hwplib.drawer.paragraph.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;

public class ControlDrawer {
    private final DrawingInfo info;

    public ControlDrawer(DrawingInfo info) {
        this.info = info;
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
        GsoOutput output = info.output().startGso(rectangle, controlArea);
        TextBox textBox = rectangle.getTextBox();
        if (rectangle.getTextBox() != null) {
            output
                    .textMargin(
                            textBox.getListHeader().getLeftMargin(),
                            textBox.getListHeader().getTopMargin(),
                            textBox.getListHeader().getRightMargin(),
                            textBox.getListHeader().getBottomMargin())
                    .verticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());

            long calculatedContentHeight = drawTextBox(textBox.getParagraphList(), output.textArea().widthHeight());
            output.calculatedContentHeight(calculatedContentHeight);
        }

        info.output().endGso();
        return output;
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
        GsoOutput output = info.output().startGso(picture, controlArea);

        info.output().endGso();
        return output;
    }

    private GsoOutput ole(ControlOLE ole, Area controlArea) {
        return null;
    }

    private GsoOutput container(ControlContainer container, Area controlArea) {
        GsoOutput output = info.output().startGso(container, controlArea);

        info.output().endGso();
        return output;
    }

    private GsoOutput objectLinkLine(ControlObjectLinkLine objectLinkLine, Area controlArea) {
        return null;
    }

    private TableOutput table(ControlTable table, Area controlArea) throws Exception {
        TableOutput output = info.output().startTable(table, controlArea);

        for(Row row : table.getRowList()) {
            for (Cell cell : row.getCellList()) {
                drawCell(cell, output);
            }
        }

        output.calculateCellPosition();

        info.output().endTable();
        return output;
    }

    private void drawCell(Cell cell, TableOutput tableOutput) throws Exception {
        CellOutput cellOutput = info.output().startCell(cell, tableOutput);
        if (cell.getParagraphList() != null) {
            cellOutput
                    .textMargin(
                            cell.getListHeader().getLeftMargin(),
                            cell.getListHeader().getTopMargin(),
                            cell.getListHeader().getRightMargin(),
                            cell.getListHeader().getBottomMargin())
                    .verticalAlignment(cell.getListHeader().getProperty().getTextVerticalAlignment());

            long calculatedContentHeight = drawTextBox(cell.getParagraphList(), cellOutput.textArea());
            cellOutput.calculatedContentHeight(calculatedContentHeight);

        }
        tableOutput.addCell(cellOutput);
        info.output().endCell();
    }

    private long drawTextBox(ParagraphList paragraphList, Area textArea) throws Exception {
        info.startControlParagraphList(textArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : paragraphList) {
            paragraphDrawer.draw(paragraph);
        }

        return info.endControlParagraphList();
    }
}
