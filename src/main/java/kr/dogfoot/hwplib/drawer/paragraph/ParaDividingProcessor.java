package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;

public class ParaDividingProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawer paraDrawer;

    public ParaDividingProcessor(DrawingInput input, InterimOutput output, ParaDrawer paraDrawer) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
    }

    public void process() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();

        if (divideSort.isDivideSection()) {
            onDividingSection();
        } else if (divideSort.isDivideMultiColumn()) {
            onDividingMultiColumn();
        } else if (divideSort.isDividePage()) {
            onDividingPage();
        } else if (divideSort.isDivideColumn()) {
            onDividingColumn();
        }
    }

    private void onDividingSection() throws Exception {
        paraDrawer.setSectionDefine();
        paraDrawer.setColumnDefine(input.pageInfo().bodyArea().top());

        input.nextPage();
        output.nextPage(input);

        if (input.columnsInfo().isParallelMultiColumn()) {
            input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentMultiColumnIndex());
        }
    }

    private void onDividingMultiColumn() throws Exception {
        if (!output.hadRearrangedDistributionMultiColumn()
                && input.columnsInfo().columnCount() > 1
                && output.textLineCount() > 1) {
            if (!distributionMultiColumnRearranger().testing()) {
                gotoZeroColumn();

                distributionMultiColumnRearranger().endParaIndex(input.paraIndex() - 1);
                distributionMultiColumnRearranger().rearrangeFromCurrentColumn();
            } else {
                distributionMultiColumnRearranger().endParaIndex(input.paraIndex() - 1);
                throw new BreakDrawingException().forDividingColumn();
            }
        } else {
            paraDrawer.nextMultiColumn();

            if (input.columnsInfo().isParallelMultiColumn()) {
                input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentMultiColumnIndex());
            }

            if (output.textLineCount() > 1) {
                throw new BreakDrawingException().forDividingColumn();
            }
        }
    }

    private DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return paraDrawer.distributionMultiColumnRearranger();
    }

    private void gotoZeroColumn() {
        while (input.columnsInfo().currentColumnIndex() > 0) {
            input.previousColumn();
            output.previousColumn();
        }
    }

    private void onDividingPage() throws Exception {
        if (input.columnsInfo().isDistributionMultiColumn()) {
            if (!output.hadRearrangedDistributionMultiColumn() && output.textLineCount() > 1) {
                gotoZeroColumn();

                distributionMultiColumnRearranger().endParaIndex(input.paraIndex() - 1);
                distributionMultiColumnRearranger().rearrangeFromCurrentColumn();
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
                onDividingMultiColumn();
                break;
            case Parallel:
                if (input.columnsInfo().lastColumn()) {
                    paraDrawer.nextMultiColumn();
                    input.parallelMultiColumnInfo().startParallelMultiColumn(output.currentPage().pageNo(), output.currentMultiColumnIndex());
                } else {
                    paraDrawer.nextColumn();
                }
                break;
        }
    }
}
