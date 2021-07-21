package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;

public class DistributionMultiColumnRearranger {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;
    private final ParaDrawer paraDrawer;

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
        if (input.columnsInfo().currentColumnIndex() == 0) {
            if (output.textLineCount() <= 1) {
                return;
            }

            paraDrawer.forDistributionMultiColumn(true);
            reset();
            testing = true;
        }

        testFromCurrentColumn();

        if (endProcess) {
            testing = false;
            endingParaIndex = -1;
            paraDrawer.forDistributionMultiColumn(false);
            output.hadRearrangedDistributionMultiColumn(true);
            redraw();
        }
    }

    private void reset() {
        testingLineCounts = new int[input.columnsInfo().columnCount()];

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
        if (input.columnsInfo().currentColumnIndex() == 0) {
            endProcess = true;
        }
    }

    private void setTestingLineCount(int textLineCount) {
        testingLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.columnsInfo().currentColumnIndex() + 1; index < testingLineCounts.length; index++) {
            testingLineCounts[index] = 0;
        }
    }

    private void testNextColumn() throws Exception {
        TextLine firstLine = output.hideTextLine(testingLineCounts[input.columnsInfo().currentColumnIndex()] - 1);
        TextLine secondLine = output.currentColumn().nextLine(firstLine);
        if (secondLine != null && secondLine.firstChar() != null) {
            input.gotoParaCharPosition(secondLine.firstChar().position());
        } else {
            if (output.currentColumn().nextChar() != null) {
                input.gotoParaCharPosition(output.currentColumn().nextChar().position());
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
                    paraIndexAtOverPage = e.position().paraIndex();;
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


        if (input.columnsInfo().lastColumn() || endingPara || overPage) {
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
        testingLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.columnsInfo().currentColumnIndex() + 1; index < testingLineCounts.length; index++) {
            testingLineCounts[index] = -1;
        }

        TextColumn[] columns = output.currentContent().currentRow().columns();
        int count = testingLineCounts.length;
        for (int index = 0; index < count; index++) {
            columns[index].topLineIndexForHiding(testingLineCounts[index]);
        }
    }

    private void setResultLineCount(boolean overPage, boolean endingPara, int paraIndexAtOverPage, int charIndexAtOverPage, long rowHeight) {
        if (input.columnsInfo().lastColumn() || endingPara) {
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
        TextColumn.ResultDeleteTextLineIndex result = output.deleteTextLineIndex(input.columnsInfo().limitedTextLineCount());
        if (result != null) {
            input.gotoParaCharPosition(result.topLine().firstChar().position());
            for (ControlCharInfo controlCharInfo : result.deletedControls()) {
                paraDrawer.textFlowCalculator().delete(controlCharInfo);
            }
        } else {
            input.gotoParaCharPosition(output.currentColumn().nextChar().position());
        }
        paraDrawer.nextColumn();

        try {
            paraListDrawer.redraw(-1);
        } catch (Exception e) {
        }
    }

    private void setLimitedTextCounts() {
        if (lineCountsOfColumnAtMinHeight == null) {
            input.columnsInfo().limitedTextLineCounts(lineCountsOfColumnAtMaxPosition);
        } else {
            input.columnsInfo().limitedTextLineCounts(lineCountsOfColumnAtMinHeight);
        }
    }

    public boolean testing() {
        return testing;
    }

    public boolean hasEmptyColumn() {
        if (testingLineCounts[input.columnsInfo().currentColumnIndex()] == 0) {
            return true;
        }
        return false;
    }
}


