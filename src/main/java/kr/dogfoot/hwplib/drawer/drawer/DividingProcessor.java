package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.paralist.ParagraphListInfo;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.Output;
import kr.dogfoot.hwplib.drawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;

public class DividingProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawer paraDrawer;

    public DividingProcessor(DrawingInput input, InterimOutput output, ParaDrawer paraDrawer) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
    }

    public void process() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();
        if (divideSort.isDivideSection()) {
            onDividingSection();
        } else if (divideSort.isDivideMultiColumn()) {
            onDividingRow();
        } else if (divideSort.isDividePage()) {
            onDividingPage();
        } else if (divideSort.isDivideColumn()) {
            onDividingColumn();
        }
    }

    private void onDividingSection() throws Exception {
        paraDrawer
                .setSectionDefine()
                .setColumnDefine(input.pageInfo().bodyArea().top());

        input.nextPage();
        output.nextPage(input);

        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentRowIndex(),
                    output.currentOutput(),
                    input.currentParaListInfo().cellInfo());
        }
    }

    private void onDividingRow() throws Exception {
        if (!output.hadRearrangedDistributionMultiColumn()
                && input.currentColumnsInfo().columnCount() > 1
                && output.textLineCount() > 1) {
            if (!distributionMultiColumnRearranger().testing()) {
                if (input.sortOfText() == ParagraphListInfo.Sort.ForBody) {
                    paraDrawer.gotoFirstColumn();
                    distributionMultiColumnRearranger().rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
                } else {
                    if (output.currentRow().calculationCount() == 0) {
                        input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                        paraDrawer.gotoStartCharOfCurrentRow();
                        output.currentRow().increaseCalculationCount();
                    } else {
                        paraDrawer.gotoFirstColumn();
                        distributionMultiColumnRearranger().rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
                    }
                }
            } else {
                distributionMultiColumnRearranger().endingParaIndex(input.paraIndex() - 1);
                throw new BreakDrawingException().forDividingColumn();
            }
        } else {
            if (output.currentOutput().type() == Output.Type.Gso) {
                setCurrentRowBottomToContentHeight();
            }

            paraDrawer.nextRow();
            if (input.currentColumnsInfo().isParallelMultiColumn()) {
                input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentRowIndex(),
                        output.currentOutput(),
                        input.currentParaListInfo().cellInfo());
            }

            if (output.textLineCount() > 1) {
                throw new BreakDrawingException().forDividingColumn();
            }
        }
    }

    private void setCurrentRowBottomToContentHeight() {
        GsoOutput gsoOutput = (GsoOutput) output.currentOutput();
        gsoOutput.calculatedContentHeight(output.currentContent().height());
        gsoOutput.applyCalculatedContentHeight();

        input.currentColumnsInfo().textBoxArea().bottom(gsoOutput.textBoxArea().height());
    }

    private DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return paraDrawer.distributionMultiColumnRearranger();
    }

    private void onDividingPage() throws Exception {
        if (input.currentColumnsInfo().isDistributionMultiColumn()) {
            if (!output.hadRearrangedDistributionMultiColumn() && output.textLineCount() > 1) {
                paraDrawer.gotoFirstColumn();
                distributionMultiColumnRearranger().rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
            } else {
                paraDrawer.nextPage();
            }
        } else {
            paraDrawer.nextPage();
        }
    }

    private void onDividingColumn() throws Exception {
        switch (input.currentColumnsInfo().columnSort()) {
            case Normal:
                if (input.currentColumnsInfo().lastColumn()) {
                    paraDrawer.nextPage();
                } else {
                    paraDrawer.nextColumn();
                }
                break;
            case Distribution:
                onDividingRow();
                break;
            case Parallel:
                if (input.currentColumnsInfo().lastColumn()) {
                    paraDrawer.nextRow();
                    input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentRowIndex(),
                            output.currentOutput(),
                            input.currentParaListInfo().cellInfo());
                } else {
                    paraDrawer.nextColumn();
                }
                break;
        }
    }
}
