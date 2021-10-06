package kr.dogfoot.hwplib.drawer.drawer.para;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.paralist.DistributionMultiColumnRearranger;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.paralist.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.util.Area;


public class NewPageColumnChecker {
    private final DrawingInput input;
    private final InterimOutput output;
    private final TextLineDrawer textLineDrawer;
    private DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    private Area currentTextArea;

    public NewPageColumnChecker(DrawingInput input, InterimOutput output, TextLineDrawer textLineDrawer) {
        this.input = input;
        this.output = output;
        this.textLineDrawer = textLineDrawer;
    }

    public void distributionMultiColumnRearranger(DistributionMultiColumnRearranger distributionMultiColumnRearranger) {
        this.distributionMultiColumnRearranger = distributionMultiColumnRearranger;
    }

    public Result check(ParaDrawingState drawingState, Area currentTextArea) throws Exception {
        this.currentTextArea = currentTextArea;

        if (drawingState.isNormal()) {
            switch (input.sortOfText()) {
                case ForBody:
                    return forBodyText();
                case ForControl:
                    return forControl();
                case ForCell:
                    return forCell();
            }
        }
        return Result.Nothing;
    }

    private Result forBodyText() throws Exception {
        if (!isOverColumnBottom(textLineDrawer.maxCharHeight())
                && !input.currentColumnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
            return Result.Nothing;
        }

        if (isOverPageBottom(textLineDrawer.maxCharHeight())
                && (input.currentColumnsInfo().isLastColumn() || input.currentColumnsInfo().isParallelMultiColumn())) {
            return Result.NextPage;
        } else {
            output.currentColumn().nextCharPosition(textLineDrawer.firstDrawingCharPosition());

            if (!input.currentColumnsInfo().isLastColumn()) {
                if (shouldProcessInDistributionMultiColumn()) {
                    return processInDistributionMultiColumn();
                } else {
                    return Result.NextColumn;
                }
            }
        }

        return Result.Nothing;
    }

    private Result forControl() throws Exception {
        if (!isOverColumnBottom(textLineDrawer.maxCharHeight())
                && !input.currentColumnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
            return Result.Nothing;
        }

        output.currentColumn().nextCharPosition(textLineDrawer.firstDrawingCharPosition());

        if (!input.currentColumnsInfo().isLastColumn()) {
            if (shouldProcessInDistributionMultiColumn()) {
                return processInDistributionMultiColumn();
            } else {
                return Result.NextColumn;
            }
        } else {
            if (input.currentColumnsInfo().isNormalMultiColumn()) {
                throw new BreakDrawingException().forOverTextBoxArea();
            }
        }
        return Result.Nothing;
    }

    private Result forCell() throws Exception {
        if (input.currentParaListInfo().cellInfo().canSplit()
                && isOverPageBottomForCell(textLineDrawer.maxCharHeight())) {
            if (input.currentColumnsInfo().isParallelMultiColumn()) {
                if (input.currentColumnsInfo().isFirstColumn()) {
                    throw new BreakDrawingException(textLineDrawer.firstDrawingCharPosition())
                            .forOverPage()
                            .columnIndex(0);
                } else {
                    gotoSplitNextCell();
                    return Result.ResetForNewColumn;
                }
            } else {
                if (input.currentColumnsInfo().isLastColumn()) {
                    throw new BreakDrawingException(textLineDrawer.firstDrawingCharPosition()).forOverPage();
                } else {
                    return Result.NextColumn;
                }
            }
        }
        return forControl();
    }

    private boolean isOverColumnBottom(long addingHeight) {
        return input.currentColumnsInfo().currentColumnArea().bottom() < currentTextArea.top() + addingHeight;
    }

    private boolean isOverPageBottom(long addingHeight) {
        return input.pageInfo().bodyArea().bottom() < currentTextArea.top() + addingHeight;
    }

    private boolean isOverPageBottomForCell(long addingHeight) {
        return input.pageInfo().bodyArea().bottom() - input.currentParaListInfo().cellInfo().bottomMargin()
                < input.currentParaListInfo().cellInfo().topInPage() + currentTextArea.top() + addingHeight;
    }

    private boolean shouldProcessInDistributionMultiColumn() {
        return (distributionMultiColumnRearranger.testing()
                || input.currentColumnsInfo().isDistributionMultiColumn()
                || input.currentColumnsInfo().processLikeDistributionMultiColumn())
                && !output.currentRow().hadRearrangedDistributionMultiColumn();
    }

    private Result processInDistributionMultiColumn() throws Exception {
        distributionMultiColumnRearranger.rearrangeFromCurrentColumn();

        if (distributionMultiColumnRearranger.testing()) {
            throw new BreakDrawingException().forEndingTest();
        } else {
            return Result.StopAddingChar;
        }
    }

    private void gotoSplitNextCell() throws BreakDrawingException {
        ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
        if (parentInfo != null) {
            output.currentOutput(parentInfo.output()).content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
            input.currentParaListInfo().cellInfo(parentInfo.cellInfo());
            input.currentParaListInfo().resetParaStartY(input.currentParaListInfo().textBoxArea().top());
        } else {
            throw new BreakDrawingException(textLineDrawer.firstDrawingCharPosition())
                    .forOverPage()
                    .columnIndex(input.currentColumnsInfo().currentColumnIndex());
        }
    }

    public enum Result {
        NextPage,
        NextColumn,
        ResetForNewColumn,
        StopAddingChar,
        Nothing
    }
}
