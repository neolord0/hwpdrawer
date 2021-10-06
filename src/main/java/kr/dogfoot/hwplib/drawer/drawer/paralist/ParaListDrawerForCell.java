package kr.dogfoot.hwplib.drawer.drawer.paralist;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.RedrawException;
import kr.dogfoot.hwplib.drawer.drawer.control.table.CellDrawInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.CharPosition;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawerForCell extends ParaListDrawer {
    public ParaListDrawerForCell(DrawingInput input, InterimOutput output) {
        super(input, output);
    }

    public CellDrawInfo draw(ParagraphListInterface paraList,
                                    Area textBoxArea,
                                    boolean canSplit,
                                    long topInPage,
                                    long bottomMargin,
                                    CharPosition fromPosition,
                                    int startTextColumnIndex,
                                    ControlOutput[] childControlsCrossingPage) throws Exception {

        boolean split = fromPosition != null;
        input.startCellParaList(textBoxArea, paraList, canSplit, topInPage, bottomMargin, split, startTextColumnIndex);

        if (split) {
            if (!input.currentColumnsInfo().isParallelMultiColumn()) {
                input.currentColumnsInfo().processLikeDistributionMultiColumn(true);
                output.nextRow(input.currentColumnsInfo());
            }
            input.gotoParaWithIgnoreNextPara(fromPosition);
        }

        CellDrawInfo cellDrawInfo = new CellDrawInfo();

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                if (fromPosition != null && fromPosition.paraIndex() == input.paraIndex()) {
                    paraDrawer.draw(redraw, fromPosition, childControlsCrossingPage);
                } else {
                    paraDrawer.draw(redraw);
                }
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
                    redraw = true;
                } else if (e.type().isForOverPage()) {
                    cellDrawInfo
                            .state(CellDrawInfo.State.Split)
                            .splitPosition(e.position())
                            .startTextColumnIndex(e.columnIndex());
                    break;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!input.currentColumnsInfo().isLastColumn()) {
                if (input.currentColumnsInfo().isDistributionMultiColumn()
                        || input.currentColumnsInfo().processLikeDistributionMultiColumn()){
                    distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
                }
            }
        }

        if (input.currentColumnsInfo().isParallelMultiColumn()
                && input.currentColumnsInfo().isFirstColumn()
                && split) {
            input.parallelMultiColumnInfo()
                    .addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());
        }


        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            input.parallelMultiColumnInfo()
                    .addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());
        }

        input.endCellParaList();


        cellDrawInfo
                .cellOutput((CellOutput) output.currentOutput())
                .cell(((CellOutput) output.currentOutput()).cell())
                .height(output.currentContent().height());
        return cellDrawInfo;
    }
}
