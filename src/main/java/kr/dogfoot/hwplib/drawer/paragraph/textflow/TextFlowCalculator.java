package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;

public class TextFlowCalculator {
    private ForTopBottom forTopBottom;
    private ForSquare forSquare;

    public TextFlowCalculator() {
        forTopBottom = new ForTopBottom();
        forSquare = new ForSquare();
    }

    public void addForTopBottom(ControlCharInfo controlCharInfo) {
        forTopBottom.addTopBottomArea(controlCharInfo.area());
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
        long offsetY = forTopBottom.calculate(tempTextLineArea);
        tempTextLineArea.moveY(offsetY);

        Result result = forSquare.calculate(tempTextLineArea);
        if (result.dividedAreas == null) {
            result.nextState = ParagraphDrawer.DrawingState.StartRedrawing;
        } else if (result.dividedAreas().length == 1 && result.dividedAreas()[0].equals(tempTextLineArea)) {
            result.nextState = ParagraphDrawer.DrawingState.Normal;
        } else {
            result.nextState = ParagraphDrawer.DrawingState.StartRecalculating;
        }
        result.offsetY += offsetY;
        return result;
    }

    public static class Result {
        private Area[] dividedAreas;
        private long offsetY;
        private ParagraphDrawer.DrawingState nextState;

        public Result(Area[] dividedAreas, long offsetY) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
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
    }
}
