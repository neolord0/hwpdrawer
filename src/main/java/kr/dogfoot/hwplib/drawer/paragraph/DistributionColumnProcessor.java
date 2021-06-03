package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;

public class DistributionColumnProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;

    private int textLineCounts[];
    private long maxPositionAtOverPage;
    private int[] textLineCountsOfColumnAtMaxPosition;
    private long minMultiColumnHeight;
    private int[] textLineCountsOfColumnAtMinHeight;

    private boolean endProcess;

    public DistributionColumnProcessor(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
    }

    public void test() throws Exception {
        if (input.columnsInfo().currentColumnIndex() == 0) {
            reset();
        }

        paraListDrawer.forDistributionColumn(true);

        testFromCurrentColumn();

        if (endProcess) {
            paraListDrawer.forDistributionColumn(false);
            output.currentOutput().content().rearrangedForDistributionColumn(true);
            redraw();
        }
    }

    private void testFromCurrentColumn() throws Exception {
        int textLineCountOfCurrentColumn = output.currentOutput().content().textLineCount();
        for (int textLineCount = 1; textLineCount < textLineCountOfCurrentColumn; textLineCount++) {
            setTextLineCount(input.columnsInfo().currentColumnIndex(), textLineCount);

            testNextColumn();
        }
        if (input.columnsInfo().currentColumnIndex() == 0) {
            endProcess = true;
        }
    }

    private void testNextColumn() throws Exception {
        TextLine firstLine = output.currentOutput().content().hideTextLineIndex(textLineCount(input.columnsInfo().currentColumnIndex()));

        paraListDrawer.nextColumn();
        input.gotoLineFirstChar(firstLine);
        output.currentOutput().content().clearColumn();

        boolean overPage = false;
        long positionAtOverPage = -1;

        try {
            paraListDrawer.redrawParaList();
        } catch (BreakingDrawException e) {
            if (e.type().isForDistributionColumn()) {
                overPage = true;
                positionAtOverPage = e.paraIndex() << 32 +e.charIndex();
            }
        }

        setTextLineCount(input.columnsInfo().currentColumnIndex(),
                output.currentOutput().content().textLineCount());

        setMinimumColumnHeight(overPage,
                positionAtOverPage,
                output.currentOutput().content().multiColumnHeight());

        output.currentOutput().content().clearColumn();
        paraListDrawer.previousColumn();
    }

    private void setMinimumColumnHeight(boolean overPage, long positionAtOverPage, long multiColumnHeight) {
        if (input.columnsInfo().lastColumn()) {
            if (overPage == true) {
                if (maxPositionAtOverPage == -1 || maxPositionAtOverPage <= positionAtOverPage) {
                    maxPositionAtOverPage = positionAtOverPage;
                    textLineCountsOfColumnAtMaxPosition = textLineCounts.clone();
                }
            } else {
                if (minMultiColumnHeight == -1 ||
                        minMultiColumnHeight >= multiColumnHeight) {
                    minMultiColumnHeight = multiColumnHeight;
                    textLineCountsOfColumnAtMinHeight = textLineCounts.clone();
                }
            }
        }
    }

    private int textLineCount(int columnIndex) {
        return textLineCounts[columnIndex];
    }

    private void setTextLineCount(int columnIndex, int textLineCount) {
        textLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
    }

    private void redraw()  {
        if (textLineCountsOfColumnAtMinHeight == null) {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMaxPosition);
        } else {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMinHeight);
        }

        output.currentOutput().content().hideTextLineIndex(-1);
        TextLine firstLine = output.currentOutput().content().deleteTextLineIndex(input.columnsInfo().limitedTextLineCount());

        paraListDrawer.nextColumn();
        input.gotoLineFirstChar(firstLine);

        try {
            paraListDrawer.redrawParaList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reset() {
        textLineCounts = new int[input.columnsInfo().columnCount()];

        maxPositionAtOverPage = -1;
        textLineCountsOfColumnAtMaxPosition = null;
        minMultiColumnHeight = -1;
        textLineCountsOfColumnAtMinHeight = null;

        endProcess = false;
    }

}
