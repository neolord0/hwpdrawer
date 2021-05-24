package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;

public class TablePainter {
    private final Painter painter;
    private final DrawingInput input;

    public TablePainter(DrawingInput input, Painter painter) {
        this.painter = painter;
        this.input = input;
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
                        .move(tableOutput.cellPosition().x(lh.getColIndex()) + tableOutput.controlArea().left(),
                                tableOutput.cellPosition().y(lh.getRowIndex()) + tableOutput.controlArea().top());

                BorderFill borderFill = input.borderFill(lh.getBorderFillId());
                painter.backgroundPainter().paint(borderFill.getFillInfo(), cellArea);
                painter.cellBorder(cellArea, borderFill);

                Area textArea = new Area(cellArea).applyMargin(
                        lh.getLeftMargin(),
                        lh.getTopMargin(),
                        lh.getRightMargin(),
                        lh.getBottomMargin());
                cellOutput
                        .adjustTextAreaAndVerticalAlignment(cellArea, textArea);

                painter.paintContent(cellOutput.content());
            }
        }
    }
}
