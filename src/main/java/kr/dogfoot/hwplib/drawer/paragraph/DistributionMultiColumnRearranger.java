package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.Column;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;

public class DistributionMultiColumnRearranger {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;
    private final ParaDrawer paraDrawer;

    private int endParaIndex;
    private boolean testing;

    private int testingLineCounts[];

    private int paraIndexAtLatestPosition;
    private int charIndexAtLatestPosition;
    private int[] lineCountsOfColumnAtMaxPosition;

    private long minMultiColumnHeight;
    private int[] lineCountsOfColumnAtMinHeight;

    private boolean endProcess;

    public DistributionMultiColumnRearranger(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer, ParaDrawer paraDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
        this.paraDrawer = paraDrawer;
        this.endParaIndex = -1;
    }

    public void endParaIndex(int endParaIndex) {
        this.endParaIndex = endParaIndex;
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
            endParaIndex = -1;
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
        minMultiColumnHeight = -1;
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
            input.gotoChar(secondLine.firstChar());
        } else {
            if (output.currentColumn().nextChar() != null) {
                input.gotoChar(output.currentColumn().nextChar());
            }
        }

        paraDrawer.nextColumn();
        output.clearColumn();

        boolean overPage = false;
        int paraIndexAtOverPage = -1;
        int charIndexAtOverPage = -1;
        boolean endingPara = false;

        try {
            paraListDrawer.redraw(endParaIndex);
        } catch (BreakDrawingException e) {
            switch (e.type()) {
                case ForNewPage:
                    paraIndexAtOverPage = e.paraIndex();
                    charIndexAtOverPage = e.charIndex();
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
                    output.multiColumnHeight());
        }

        output.clearColumn();
        paraDrawer.previousColumn();
    }

    private void setTestingLineCount2(int textLineCount, boolean overPage) {
        testingLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.columnsInfo().currentColumnIndex() + 1; index < testingLineCounts.length; index++) {
            testingLineCounts[index] = -1;
        }

        Column[] columns = output.currentContent().currentMultiColumn().columns();
        int count = testingLineCounts.length;
        for (int index = 0; index < count; index++) {
            columns[index].topLineIndexForHiding(testingLineCounts[index]);
        }
    }

    private void setResultLineCount(boolean overPage, boolean endingPara, int paraIndexAtOverPage, int charIndexAtOverPage, long multiColumnHeight) {
        if (input.columnsInfo().lastColumn() || endingPara) {
            if (overPage == true) {
                if (isLatePosition(paraIndexAtOverPage, charIndexAtOverPage) ) {
                    lineCountsOfColumnAtMaxPosition = testingLineCounts.clone();

                    paraIndexAtLatestPosition = paraIndexAtOverPage;
                    charIndexAtLatestPosition = charIndexAtOverPage;
                }
            } else {
                if (minMultiColumnHeight == -1 ||
                        minMultiColumnHeight >= multiColumnHeight) {
                    lineCountsOfColumnAtMinHeight = testingLineCounts.clone();

                    minMultiColumnHeight = multiColumnHeight;
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

        TextLine firstLine = output.deleteTextLineIndex(input.columnsInfo().limitedTextLineCount());
        if (firstLine != null) {
            input.gotoChar(firstLine.firstChar());
        } else {
            input.gotoChar(output.currentColumn().nextChar());
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


