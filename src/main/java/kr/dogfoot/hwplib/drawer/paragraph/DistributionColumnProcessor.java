package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;

public class DistributionColumnProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;

    public DistributionColumnProcessor(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
    }

    public void process() throws Exception {
        int textLineCountOfColumn = output.currentOutput().content().textLineCount();

        long minMultiColumnHeight = -1;
        int textLineCountOfColumnAtMinHeight = -1;

        for (int textLineCountOfColumn2 = 1 ; textLineCountOfColumn2 < textLineCountOfColumn - 1; textLineCountOfColumn2++) {
            TextLine forSecondColumn = output.currentOutput().content().hideTextLineIndex(textLineCountOfColumn2);

            input.gotoParaCharPosition(forSecondColumn.paraIndex(), forSecondColumn.firstChar().index(), forSecondColumn.firstChar().prePosition());

            paraListDrawer.nextColumn();

            boolean redraw = true;
            while (redraw || input.nextPara()) {
                try {
                    paraListDrawer.paragraph(redraw);
                    redraw = false;
                } catch (RedrawException e) {
                    redraw = true;
                }
            }

            long multiColumnHeight = output.currentOutput().content().multiColumnHeight();

            if (minMultiColumnHeight == -1 ||
                    minMultiColumnHeight >= multiColumnHeight) {
                minMultiColumnHeight = multiColumnHeight;
                textLineCountOfColumnAtMinHeight = textLineCountOfColumn2;
            }

            output.currentOutput().content().clearColumn();
            paraListDrawer.previousColumn();
        }

        TextLine forSecondColumn = output.currentOutput().content().hideTextLineIndex(textLineCountOfColumnAtMinHeight);

        input.gotoParaCharPosition(forSecondColumn.paraIndex(),
                forSecondColumn.firstChar().index(),
                forSecondColumn.firstChar().prePosition());
        paraListDrawer.nextColumn();

        boolean redraw = true;
        while (redraw || input.nextPara()) {
            try {
                paraListDrawer.paragraph(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            }
        }
    }

}
