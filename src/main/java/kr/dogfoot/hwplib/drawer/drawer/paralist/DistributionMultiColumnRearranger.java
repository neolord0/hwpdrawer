package kr.dogfoot.hwplib.drawer.drawer.paralist;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.para.ParaDrawer;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;

public class DistributionMultiColumnRearranger {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;
    private final ParaDrawer paraDrawer;
    private TextFlowCalculator textFlowCalculator;

    private int endingParaIndex;
    private boolean testing;

    private int testingLineCounts[];

    private int paraIndexAtLatestPosition;
    private int charIndexAtLatestPosition;
    private int[] lineCountsOfColumnAtMaxPosition;

    private long minRowHeight;
    private int[] lineCountsOfColumnAtMinHeight;

    private boolean endProcess;

    public DistributionMultiColumnRearranger(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer, ParaDrawer paraDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
        this.paraDrawer = paraDrawer;
        this.endingParaIndex = -1;
    }

    public void textFlowCalculator(TextFlowCalculator textFlowCalculator) {
        this.textFlowCalculator = textFlowCalculator;
    }

    public void resetEndingParaIndex() {
        endingParaIndex = -1;
    }

    public void endingParaIndex(int endingParaIndex) {
        this.endingParaIndex = endingParaIndex;
    }

    public void rearrangeFromCurrentColumnUntilEndingPara(int endingParaIndex) throws Exception {
        this.endingParaIndex = endingParaIndex;
        rearrangeFromCurrentColumn();
    }

    public void rearrangeFromCurrentColumn() throws Exception {
        if (input.currentColumnsInfo().isFirstColumn()) {
            if (output.textLineCount() <= 1) {
                return;
            }
            testing = true;
            reset();
        }

        testFromCurrentColumn();

        if (endProcess) {
            testing = false;
            endingParaIndex = -1;
            output.hadRearrangedDistributionMultiColumn(true);
            redraw();
        }
    }

    private void reset() {
        testingLineCounts = new int[input.currentColumnsInfo().columnCount()];

        paraIndexAtLatestPosition = -1;
        charIndexAtLatestPosition = -1;
        lineCountsOfColumnAtMaxPosition = null;
        minRowHeight = -1;
        lineCountsOfColumnAtMinHeight = null;

        endProcess = false;
    }

    private void testFromCurrentColumn() throws Exception {
        int textLineCount = output.textLineCount();
        for (int testingTextLineCount = 1; testingTextLineCount <= textLineCount; testingTextLineCount++) {
            setTestingLineCount(testingTextLineCount);
            testNextColumn();
        }
        if (input.currentColumnsInfo().isFirstColumn()) {
            endProcess = true;
        }
    }

    private void setTestingLineCount(int textLineCount) {
        testingLineCounts[input.currentColumnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.currentColumnsInfo().currentColumnIndex() + 1; index < testingLineCounts.length; index++) {
            testingLineCounts[index] = 0;
        }
    }

    private void testNextColumn() throws Exception {
        TextLine firstLine = output.hideTextLine(testingLineCounts[input.currentColumnsInfo().currentColumnIndex()] - 1);
        TextLine secondLine = output.currentColumn().nextLine(firstLine);
        if (secondLine != null && secondLine.firstChar() != null) {
            input.gotoParaCharPosition(secondLine.firstChar().position());
        } else {
            if (output.currentColumn().nextCharPosition() != null) {
                input.gotoParaCharPosition(output.currentColumn().nextCharPosition());
            }
        }

        paraDrawer.nextColumn();
        output.clearColumn();

        boolean overPage = false;
        int paraIndexAtOverPage = -1;
        int charIndexAtOverPage = -1;
        boolean endingPara = false;

        try {
            paraListDrawer.redraw(endingParaIndex);
        } catch (BreakDrawingException e) {
            switch (e.type()) {
                case ForNewPage:
                    paraIndexAtOverPage = e.position().paraIndex();
                    ;
                    charIndexAtOverPage = e.position().charIndex();
                    overPage = true;
                    break;
                case ForEndingPara:
                    endingPara = true;
                    break;
                case ForEndingTest:
                    break;
            }
        }


        if (input.currentColumnsInfo().isLastColumn() || endingPara || overPage) {
            setTestingLineCount2(output.textLineCount(), overPage);

            setResultLineCount(overPage,
                    endingPara,
                    paraIndexAtOverPage,
                    charIndexAtOverPage,
                    output.rowHeight());
        }

        output.clearColumn();
        paraDrawer.previousColumn();
    }

    private void setTestingLineCount2(int textLineCount, boolean overPage) {
        testingLineCounts[input.currentColumnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.currentColumnsInfo().currentColumnIndex() + 1; index < testingLineCounts.length; index++) {
            testingLineCounts[index] = -1;
        }

        TextColumn[] columns = output.currentContent().currentRow().columns();
        int count = testingLineCounts.length;
        for (int index = 0; index < count; index++) {
            columns[index].topLineIndexForHiding(testingLineCounts[index]);
        }
    }

    private void setResultLineCount(boolean overPage, boolean endingPara, int paraIndexAtOverPage, int charIndexAtOverPage, long rowHeight) {
        if (input.currentColumnsInfo().isLastColumn() || endingPara) {
            if (overPage == true) {
                if (isLatePosition(paraIndexAtOverPage, charIndexAtOverPage)) {
                    lineCountsOfColumnAtMaxPosition = testingLineCounts.clone();

                    paraIndexAtLatestPosition = paraIndexAtOverPage;
                    charIndexAtLatestPosition = charIndexAtOverPage;
                }
            } else {
                if (minRowHeight == -1 ||
                        minRowHeight >= rowHeight) {
                    lineCountsOfColumnAtMinHeight = testingLineCounts.clone();

                    minRowHeight = rowHeight;
                }
            }
        }
    }

    private boolean isLatePosition(int paraIndexAtOverPage, int charIndexAtOverPage) {
        if (paraIndexAtLatestPosition == -1 && charIndexAtLatestPosition == -1) {
            return true;
        }

        if (paraIndexAtOverPage > paraIndexAtLatestPosition) {
            return true;
        } else if (paraIndexAtOverPage == paraIndexAtLatestPosition) {
            return charIndexAtOverPage > charIndexAtLatestPosition;
        } else {
            return false;
        }
    }

    private void redraw() {
        setLimitedTextCounts();

        output.resetHidingTextLineIndex();
        TextColumn.ResultDeleteTextLineIndex result = output.deleteTextLineIndex(input.currentColumnsInfo().limitedTextLineCount());
        if (result != null) {
            input.gotoParaCharPosition(result.topLine().firstChar().position());
            for (CharInfoControl controlCharInfo : result.deletedControls()) {
                textFlowCalculator.delete(controlCharInfo);
            }
        } else {
            input.gotoParaCharPosition(output.currentColumn().nextCharPosition());
        }
        paraDrawer.nextColumn();

        try {
            paraListDrawer.redraw(-1);
        } catch (Exception e) {
        }
    }

    private void setLimitedTextCounts() {
        if (lineCountsOfColumnAtMinHeight == null) {
            input.currentColumnsInfo().limitedTextLineCounts(lineCountsOfColumnAtMaxPosition);
        } else {
            input.currentColumnsInfo().limitedTextLineCounts(lineCountsOfColumnAtMinHeight);
        }
    }

    public boolean testing() {
        return testing;
    }

    public boolean hasEmptyColumn() {
        if (testingLineCounts[input.currentColumnsInfo().currentColumnIndex()] == 0) {
            return true;
        }
        return false;
    }

}
