package kr.dogfoot.hwpdrawer.painter.image.control;

import kr.dogfoot.hwpdrawer.painter.image.PainterForImage;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.control.table.CellOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;

public class TablePainter {
    private final DrawingInput input;
    private final PainterForImage painter;

    public TablePainter(DrawingInput input, PainterForImage painter) {
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
                painter.backgroundPainter().paint(borderFill.getFillInfo(), cellArea);
                painter.cellBorder(cellArea, borderFill);

                Area textArea = new Area(cellArea).applyMargin(
                        lh.getLeftMargin(),
                        lh.getTopMargin(),
                        lh.getRightMargin(),
                        lh.getBottomMargin());
                cellOutput
                        .adjustTextBoxAreaAndVerticalAlignment(cellArea, textArea);

                painter.paintContent(cellOutput.content());
            }
        }
    }
}
