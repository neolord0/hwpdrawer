package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class TextFlowCalculator {
    private final ForTopBottom forTopBottom;
    private final ForSquare forSquare;

    public TextFlowCalculator() {
        forTopBottom = new ForTopBottom();
        forSquare = new ForSquare();
    }

    public void addForTopBottom(ControlCharInfo controlCharInfo) {
        forTopBottom.addTopBottomArea(controlCharInfo.areaWithOuterMargin(), controlCharInfo.header().getProperty().getVertRelTo());
    }

    public void addForSquare(ControlCharInfo controlCharInfo) {
        forSquare.add(controlCharInfo);
    }

    public void reset() {
        forTopBottom.reset();
        forSquare.reset();
    }

    public Result calculate(Area textLineArea) {
        Area tempTextLineArea = new Area(textLineArea);
        ForTopBottom.Result resultForTopBottom = forTopBottom.calculate(tempTextLineArea);
        tempTextLineArea.moveY(resultForTopBottom.yOffset());

        Result result = forSquare.calculate(tempTextLineArea);
        if (result.dividedAreas == null) {
            result.nextState = ParagraphDrawer.DrawingState.StartRedrawing;
        } else if (result.dividedAreas().length == 1 && result.dividedAreas()[0].equals(tempTextLineArea)) {
            result.nextState = ParagraphDrawer.DrawingState.Normal;
        } else {
            result.nextState = ParagraphDrawer.DrawingState.StartRecalculating;
        }
        result.offsetY += resultForTopBottom.yOffset();
        if (resultForTopBottom.yOffset() > 0 && resultForTopBottom.vertRelTo() == VertRelTo.Para) {
            result.cancelNewLine = true;
        }
        return result;
    }

    public static class Result {
        private final Area[] dividedAreas;
        private long offsetY;
        private ParagraphDrawer.DrawingState nextState;
        private boolean cancelNewLine;

        public Result(Area[] dividedAreas, long offsetY) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
            nextState = ParagraphDrawer.DrawingState.Normal;
            cancelNewLine = false;
        }

        public Area[] dividedAreas() {
            return dividedAreas;
        }

        public long offsetY() {
            return offsetY;
        }

        public ParagraphDrawer.DrawingState nextState() {
            return nextState;
        }

        public boolean cancelNewLine() {
            return cancelNewLine;
        }
    }
}
