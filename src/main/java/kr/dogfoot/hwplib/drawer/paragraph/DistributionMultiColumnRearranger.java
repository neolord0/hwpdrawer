package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.Column;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import org.apache.poi.ss.formula.functions.Columns;

public class DistributionMultiColumnRearranger {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;

    private int endParaIndex;
    private boolean testing;

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
        this.endParaIndex = -1;
    }

    public void endParaIndex(int endParaIndex) {
        this.endParaIndex = endParaIndex;
    }

    public void rearrangeFromCurrentColumn() throws Exception {
        if (input.columnsInfo().currentColumnIndex() == 0) {
            paraListDrawer.forDistributionMultiColumn(true);
            reset();
            testing = true;
        }

        testFromCurrentColumn();

        if (endProcess) {
            testing = false;
            endParaIndex = -1;
            paraListDrawer.forDistributionMultiColumn(false);
            output.hadRearrangedDistributionMultiColumn(true);
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
        for (int index = input.columnsInfo().currentColumnIndex() + 1; index < textLineCounts.length; index++) {
            textLineCounts[index] = 0;
        }
    }

    private void testNextColumn() throws Exception {
        TextLine firstLine = output.hideTextLine(textLineCount(input.columnsInfo().currentColumnIndex()));
        input.gotoLineFirstChar(firstLine);

        paraListDrawer.nextColumn();
        output.clearColumn();

        boolean overPage = false;
        boolean endingPara = false;
        long positionAtOverPage = -1;

        try {
            paraListDrawer.redrawParaList(endParaIndex);
        } catch (BreakingDrawException e) {
            switch (e.type()) {
                case ForDistributionColumn:
                    overPage = true;
                    positionAtOverPage = e.paraIndex() << 32 + e.charIndex();
                    break;
                case ForEndingPara:
                    endingPara = true;
                    break;
            }
        }

        setTextLineCount2(output.textLineCount(), overPage);
        setMinimumHeightOfMultiColumn(overPage,
                endingPara,
                positionAtOverPage,
                output.multiColumnHeight());

        output.clearColumn();
        paraListDrawer.previousColumn();
    }

    private void setTextLineCount2(int textLineCount, boolean overPage) {
        textLineCounts[input.columnsInfo().currentColumnIndex()] = textLineCount;
        for (int index = input.columnsInfo().currentColumnIndex() + 1; index < textLineCounts.length; index++) {
            textLineCounts[index] = -1;
        }

        Column[] columns = output.currentContent().currentMultiColumn().columns();
        int count = textLineCounts.length;
        for (int index = 0; index < count; index++) {
            columns[index].topLineIndexForHiding(textLineCounts[index]);
        }
    }

    private void setMinimumHeightOfMultiColumn(boolean overPage, boolean endingPara, long positionAtOverPage, long multiColumnHeight) {
        if (input.columnsInfo().lastColumn() || endingPara) {
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


    public boolean testing() {
        return testing;
    }

    private void redraw() {
        setLimitedTextCounts();

        output.resetHidingTextLineIndex();

        TextLine firstLine = output.deleteTextLineIndex(input.columnsInfo().limitedTextLineCount());
        input.gotoLineFirstChar(firstLine);

        paraListDrawer.nextColumn();

        try {
            paraListDrawer.redrawParaList(-1);
        } catch (Exception e) {
        }
    }

    private void setLimitedTextCounts() {
        if (textLineCountsOfColumnAtMinHeight == null) {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMaxPosition);
        } else {
            input.columnsInfo().limitedTextLineCounts(textLineCountsOfColumnAtMinHeight);
        }
    }

    public boolean hasEmptyColumn() {
        if (textLineCounts[input.columnsInfo().currentColumnIndex()] == 0) {
            return true;
        }
        return false;
    }
}


