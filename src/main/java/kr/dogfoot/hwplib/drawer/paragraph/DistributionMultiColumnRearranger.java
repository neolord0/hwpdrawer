package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;

public class DistributionMultiColumnRearranger {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;

    private int textLineCounts[];
    private long maxPositionAtOverPage;
    private int[] textLineCountsOfColumnAtMaxPosition;
    private long minMultiColumnHeight;
    private int[] textLineCountsOfColumnAtMinHeight;

    private boolean endProcess;

    public DistributionMultiColumnRearranger(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
    }

    public void rearrange() throws Exception {
        if (input.columnsInfo().currentColumnIndex() == 0) {
            paraListDrawer.forDistributionMultiColumn(true);
            reset();
        }

        testFromCurrentColumn();

        if (endProcess) {
            paraListDrawer.forDistributionMultiColumn(false);
            output.hadRearrangedDistributionMultiColumn(true);
            setResult();
            redraw();
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

    private void testFromCurrentColumn() throws Exception {
        int textLineCountOfCurrentColumn = output.textLineCount();
        for (int textLineCount = 1; textLineCount < textLineCountOfCurrentColumn; textLineCount++) {
            setTextLineCount(textLineCount);

            testNextColumn();
        }
        if (input.columnsInfo().currentColumnIndex() == 0) {
            endProcess = true;
        }
    }

    private void setTextLineCount(int textLineCount) {
        textLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
    }

    private void testNextColumn() throws Exception {
        TextLine firstLine = output.hideTextLine(textLineCount(input.columnsInfo().currentColumnIndex()));
        input.gotoLineFirstChar(firstLine);

        paraListDrawer.nextColumn();

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

        setTextLineCount(output.textLineCount());

        setMinimumHeightOfMultiColumn(overPage,
                positionAtOverPage,
                output.multiColumnHeight());

        output.clearColumn();

        paraListDrawer.previousColumn();
    }

    private void setMinimumHeightOfMultiColumn(boolean overPage, long positionAtOverPage, long multiColumnHeight) {
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

    private void setResult() {
        if (textLineCountsOfColumnAtMinHeight == null) {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMaxPosition);
        } else {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMinHeight);
        }
    }

    private void redraw()  {
        output.resetHidingTextLineIndex();

        TextLine firstLine = output.deleteTextLineIndex(input.columnsInfo().limitedTextLineCount());
        input.gotoLineFirstChar(firstLine);

        paraListDrawer.nextColumn();

        try {
            paraListDrawer.redrawParaList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
