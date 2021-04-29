package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer.ControlContent;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;
import kr.dogfoot.hwplib.tool.blankfilemaker.BorderFillAdder;

public class TablePainter {
    private Painter painter;
    private DrawingInfo info;
    private CellPositionCalculator cellPositionCalculator;


    public TablePainter(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;

        cellPositionCalculator = new CellPositionCalculator();
    }

    public void paint(ControlTable table, Area areaWithoutOuterMargin) throws Exception {
        cellPositionCalculator.reset(table.getTable().getColumnCount(), table.getTable().getRowCount());
        ControlContent[][] cellContent = new ControlContent[table.getTable().getColumnCount()][table.getTable().getRowCount()];
        for (Row row : table.getRowList()) {
            for (Cell cell : row.getCellList()) {
                ListHeaderForCell lh = cell.getListHeader();
                cellContent[lh.getColIndex()][lh.getRowIndex()] = drawCell(lh.getWidth(), cell);
                long calculatedHeight = cellContent[lh.getColIndex()][lh.getRowIndex()].height() + lh.getTopMargin() + lh.getBottomMargin();

                cellPositionCalculator
                        .addColumnInfo(lh.getColIndex(), lh.getColSpan(), lh.getWidth())
                        .addRowInfo(
                                lh.getRowIndex(),
                                lh.getRowSpan(),
                                Math.max(calculatedHeight, lh.getHeight()));
            }
        }

        cellPositionCalculator.calculate();

        for (Row row : table.getRowList()) {
            for (Cell cell : row.getCellList()) {
                ListHeaderForCell lh = cell.getListHeader();
                Area cellArea = new Area(0, 0,
                        lh.getWidth(),
                        cellPositionCalculator.height(lh.getRowIndex(), lh.getRowSpan()))
                        .moveX(cellPositionCalculator.x(lh.getColIndex()) + areaWithoutOuterMargin.left())
                        .moveY(cellPositionCalculator.y(lh.getRowIndex()) + areaWithoutOuterMargin.top());

                BorderFill borderFill = info.getBorderFill(lh.getBorderFillId());
                painter.backgroundPainter().paint(borderFill.getFillInfo(), cellArea);
                painter.setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0, 0, 0));
                painter.rectangle(cellArea, false);
                paintContent(cellContent[lh.getColIndex()][lh.getRowIndex()], cellArea, cell);

            }
        }
    }

    public ControlContent drawCell(long width, Cell cell) throws Exception {
        Area fakeCellArea = new Area(0, 0, width,  0);
        Area fakeTextArea = new Area(fakeCellArea)
                .applyMargin(cell.getListHeader().getLeftMargin(), 0, cell.getListHeader().getRightMargin(), 0)
                .moveX(-cell.getListHeader().getLeftMargin());

        info.startContentContentAndParagraphList(fakeCellArea, fakeTextArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);
        for (Paragraph paragraph : cell.getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        return info.endControlContentAndParagraphList();
    }

    private void paintContent(ControlContent controlContent, Area cellArea, Cell cell) throws Exception {
        Area textArea = new Area(cellArea).applyMargin(
                cell.getListHeader().getLeftMargin(),
                cell.getListHeader().getTopMargin(),
                cell.getListHeader().getRightMargin(),
                cell.getListHeader().getBottomMargin());

        controlContent
                .adjustArea(cellArea, textArea)
                .adjustVerticalAlignment(cell.getListHeader().getProperty().getTextVerticalAlignment());

        painter.controlPainter().paintControls(controlContent.behindControls());
        painter.textPainter().paintTextParts(controlContent.textParts());
        painter.controlPainter().paintControls(controlContent.nonBehindControls());
    }
}
