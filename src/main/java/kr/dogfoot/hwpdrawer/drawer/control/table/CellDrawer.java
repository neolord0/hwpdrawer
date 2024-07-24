package kr.dogfoot.hwpdrawer.drawer.control.table;

import kr.dogfoot.hwpdrawer.util.CharPosition;
import kr.dogfoot.hwpdrawer.drawer.control.table.info.CellDrawInfo;
import kr.dogfoot.hwpdrawer.drawer.paralist.ParaListDrawerForCell;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.control.table.CellOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

public class CellDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private TableOutput currentTableOutput;

    public CellDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public void currentTableOutput(TableOutput currentTableOutput) {
        this.currentTableOutput = currentTableOutput;
    }

    public CellDrawInfo draw(Cell cell, CharPosition fromPosition, int startTextColumnIndex, ControlOutput[] childControlsCrossingPage, boolean canDivide) throws Exception {
        return draw(cell, fromPosition, startTextColumnIndex, childControlsCrossingPage, canDivide, 0);
    }

    public CellDrawInfo draw(Cell cell, CharPosition fromPosition, int startTextColumnIndex, ControlOutput[] childControlsCrossingPage, boolean canDivide, long addingY) throws Exception {
        if (cell.getParagraphList() != null) {
            CellOutput cellOutput = output.startCell(cell);

            ListHeaderForCell lh = cell.getListHeader();
            setTextMarginAndVerticalAlignment(cellOutput, lh);

            long topInPage = currentTableOutput.cellPosition().currentCellTop(cell.getListHeader().getColIndex())
                    + currentTableOutput.areaWithoutOuterMargin().top()
                    + addingY;

            CellDrawInfo cellDrawInfo = new ParaListDrawerForCell(input, output).draw(
                    cell.getParagraphList(),
                    cellOutput.textBoxArea(),
                    canDivide,
                    topInPage,
                    lh.getBottomMargin() + currentTableOutput.table().getHeader().getOutterMarginBottom(),
                    fromPosition,
                    startTextColumnIndex,
                    childControlsCrossingPage);

            checkCrossPageAndOverPage(cellDrawInfo, topInPage, lh);
            cellOutput.calculatedContentHeight(cellDrawInfo.height());

            output.endCell();
            return cellDrawInfo;
        } else {
            return new CellDrawInfo()
                    .cell(cell)
                    .height(cell.getListHeader().getHeight());
        }
    }

    private void setTextMarginAndVerticalAlignment(CellOutput cellOutput, ListHeaderForCell lh) {
        if (lh != null) {
            cellOutput
                    .textMargin(lh.getLeftMargin(), lh.getTopMargin(), lh.getRightMargin(), lh.getBottomMargin())
                    .verticalAlignment(lh.getProperty().getTextVerticalAlignment());
        }
    }

    private void checkCrossPageAndOverPage(CellDrawInfo cellDrawInfo, long topInPage, ListHeaderForCell lh) {
        if (topInPage + cellDrawInfo.height() > input.pageInfo().bodyArea().bottom()) {
            cellDrawInfo.state(CellDrawInfo.State.OverPage);

            cellDrawInfo.dividedPosition(null);
            cellDrawInfo.startTextColumnIndex(0);
            cellDrawInfo.cellOutput().clearChildControlsCrossingPage();
        } else {
            long tableOuterMarginBottom = currentTableOutput.table().getHeader().getOutterMarginBottom();
            if (lh.getHeight() + topInPage + tableOuterMarginBottom > input.pageInfo().bodyArea().bottom()) {
                long heightWithMargin = input.pageInfo().bodyArea().bottom() - tableOuterMarginBottom - topInPage;
                cellDrawInfo
                        .height(heightWithMargin - (lh.getTopMargin() + lh.getBottomMargin()))
                        .state(CellDrawInfo.State.Divided);
                cellDrawInfo.nextPartHeight(lh.getHeight() - heightWithMargin);
                lh.setHeight(0);
            }
        }
    }
}
