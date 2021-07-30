package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
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

        if (input.columnsInfo().isParallelMultiColumn()) {
            input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentRowIndex());
        }
    }

    private void onDividingRow() throws Exception {
        if (!output.hadRearrangedDistributionMultiColumn()
                && input.columnsInfo().columnCount() > 1
                && output.textLineCount() > 1) {
            if (!distributionMultiColumnRearranger().testing()) {
                if (input.isBodyText()) {
                    paraDrawer.gotoFirstColumn();
                    distributionMultiColumnRearranger().rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
                } else {
                    if (output.currentRow().calculationCount() == 0) {
                        input.columnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
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

            if (input.columnsInfo().isParallelMultiColumn()) {
                input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentRowIndex());
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

        input.columnsInfo().textBoxArea().bottom(gsoOutput.textBoxArea().height());
    }

    private DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return paraDrawer.distributionMultiColumnRearranger();
    }

    private void onDividingPage() throws Exception {
        if (input.columnsInfo().isDistributionMultiColumn()) {
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
        switch (input.columnsInfo().columnSort()) {
            case Normal:
                if (input.columnsInfo().lastColumn()) {
                    paraDrawer.nextPage();
                } else {
                    paraDrawer.nextColumn();
                }
                break;
            case Distribution:
                onDividingRow();
                break;
            case Parallel:
                if (input.columnsInfo().lastColumn()) {
                    paraDrawer.nextRow();
                    input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentRowIndex());
                } else {
                    paraDrawer.nextColumn();
                }
                break;
        }
    }
}
