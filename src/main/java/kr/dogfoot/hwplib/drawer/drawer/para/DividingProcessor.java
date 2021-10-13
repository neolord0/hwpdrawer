package kr.dogfoot.hwplib.drawer.drawer.para;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.paralist.DistributionMultiColumnRearranger;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;

public class DividingProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawer paraDrawer;
    private final CharAdder charAdder;
    private DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    public DividingProcessor(DrawingInput input,
                             InterimOutput output,
                             ParaDrawer paraDrawer,
                             CharAdder charAdder) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
        this.charAdder = charAdder;
    }

    public void distributionMultiColumnRearranger(DistributionMultiColumnRearranger distributionMultiColumnRearranger) {
        this.distributionMultiColumnRearranger = distributionMultiColumnRearranger;
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
        input.sectionDefine(charAdder.sectionDefine());
        input.columnsInfo(charAdder.columnDefine(), input.pageInfo().bodyArea().top());

        input.nextPage();
        output.nextPage(input);

        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            startParallelMultiColumnRow();
        }
    }

    private void startParallelMultiColumnRow() {
        input.parallelMultiColumnInfo()
                .startParallelMultiColumn(output.currentRowIndex(), output.currentOutput(), input.currentParaListInfo().cellInfo());
    }

    private void onDividingRow() throws Exception {
        if (!output.hadRearrangedDistributionMultiColumn()
                && input.currentColumnsInfo().columnCount() > 1
                && output.textLineCount() > 1) {
            if (!distributionMultiColumnRearranger.testing()) {
                if (input.sortOfText().isForBody()) {
                    paraDrawer.gotoFirstColumn();
                    distributionMultiColumnRearranger.rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
                } else {
                    if (output.currentRow().calculationCount() == 0) {
                        input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                        paraDrawer.gotoStartCharOfCurrentRow();
                        output.currentRow().increaseCalculationCount();
                    } else {
                        paraDrawer.gotoFirstColumn();
                        distributionMultiColumnRearranger.rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
                    }
                }
            } else {
                distributionMultiColumnRearranger.endingParaIndex(input.paraIndex() - 1);
                throw new BreakDrawingException().forDividingColumn();
            }
        } else {
            if (output.currentOutput().type().isGso()) {
                setCurrentRowBottomToContentHeight();
            }

            paraDrawer.nextRow();
            if (input.currentColumnsInfo().isParallelMultiColumn()) {
                startParallelMultiColumnRow();
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

    private void onDividingPage() throws Exception {
        if (input.currentColumnsInfo().isDistributionMultiColumn()) {
            if (!output.hadRearrangedDistributionMultiColumn() && output.textLineCount() > 1) {
                paraDrawer.gotoFirstColumn();
                distributionMultiColumnRearranger.rearrangeFromCurrentColumnUntilEndingPara(input.paraIndex() - 1);
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
                if (input.currentColumnsInfo().isLastColumn()) {
                    paraDrawer.nextPage();
                } else {
                    paraDrawer.nextColumn();
                }
                break;
            case Distribution:
                onDividingRow();
                break;
            case Parallel:
                input.parallelMultiColumnInfo()
                        .addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());

                if (input.currentColumnsInfo().isLastColumn()) {
                    paraDrawer.nextRow();
                    startParallelMultiColumnRow();
                } else {
                    paraDrawer.nextColumn();
                }
                break;
        }
    }

}
