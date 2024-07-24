package kr.dogfoot.hwpdrawer.painter.html.control;

import kr.dogfoot.hwpdrawer.painter.html.PainterForHTML;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.control.table.CellOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;

public class TablePainter {
    private DrawingInput input;
    private PainterForHTML painter;

    public TablePainter(DrawingInput input, PainterForHTML painter) {
        this.input = input;
        this.painter = painter;
    }

    public void paint(TableOutput tableOutput) throws Exception {
        for (CellOutput[] cellOutputs : tableOutput.cellOutputs()) {
            for (CellOutput cellOutput : cellOutputs) {
                if (cellOutput == null) {
                    continue;
                }
                ListHeaderForCell lh = cellOutput.cell().getListHeader();
                Area cellArea = new Area(0, 0,
                        lh.getWidth(),
                        tableOutput.cellPosition().height(lh.getRowIndex(), lh.getRowSpan()))
                        .move(tableOutput.cellPosition().x(lh.getColIndex()) + tableOutput.areaWithoutOuterMargin().left(),
                                tableOutput.cellPosition().y(lh.getRowIndex()) + tableOutput.areaWithoutOuterMargin().top());

                BorderFill borderFill = input.borderFill(lh.getBorderFillId());
                painter.startCellDiv(cellArea, borderFill, true);

                Area textArea = new Area(cellArea).applyMargin(
                        lh.getLeftMargin(),
                        lh.getTopMargin(),
                        lh.getRightMargin(),
                        lh.getBottomMargin());
                cellOutput
                        .adjustTextBoxAreaAndVerticalAlignment(cellArea, textArea);

                painter.paintContent(cellOutput.content());

                painter.endCellDiv(cellArea, borderFill);
            }
        }
    }

    public static String paintCell(CellOutput cellOutput, DrawingInput input) throws Exception {
        PainterForHTML painter = new PainterForHTML(input);

        ListHeaderForCell lh = cellOutput.cell().getListHeader();
        Area cellArea = new Area(0, 0,
                lh.getWidth(),
                Math.max(cellOutput.calculatedContentHeight(), lh.getHeight()));

        BorderFill borderFill = input.borderFill(lh.getBorderFillId());
        painter.startCellDiv(cellArea, borderFill, false);

        Area textArea = new Area(cellArea).applyMargin(
                lh.getLeftMargin(),
                lh.getTopMargin(),
                lh.getRightMargin(),
                lh.getBottomMargin());
        cellOutput
                .adjustTextBoxAreaAndVerticalAlignment(cellArea, textArea);

        painter.paintContent(cellOutput.content());

        painter.endCellDiv(cellArea, borderFill);

        return painter.toString();
    }
}
