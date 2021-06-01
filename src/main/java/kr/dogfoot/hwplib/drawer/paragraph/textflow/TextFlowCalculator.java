package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

import java.util.LinkedList;
import java.util.Queue;

public class TextFlowCalculator {
    private final ForTakePlace forTakePlace;
    private final ForFitWithText forFitWithText;


    public TextFlowCalculator() {
        forTakePlace = new ForTakePlace();
        forFitWithText = new ForFitWithText();
    }

    public void add(ControlCharInfo controlCharInfo) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            forTakePlace.add(controlCharInfo);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            forFitWithText.add(controlCharInfo);
        }
    }

    public boolean alreadyAdded(ControlCharInfo controlCharInfo) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            return forTakePlace.alreadyAdded(controlCharInfo);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            return forFitWithText.alreadyAdded(controlCharInfo);
        }
        return false;
    }

    public void reset() {
        forTakePlace.reset();
        forFitWithText.reset();
    }

    public TextFlowCalculationResult calculate(Area textLineArea) {
        Area tempTextLineArea = new Area(textLineArea);
        ForTakePlace.Result resultForTopBottom = forTakePlace.calculate(tempTextLineArea);
        tempTextLineArea.moveY(resultForTopBottom.yOffset());

        Result result = forFitWithText.calculate(tempTextLineArea);
        if (result.dividedAreas == null) {
            result.nextState = ParaListDrawer.DrawingState.StartRedrawing;
        } else if (result.dividedAreas().length == 1 && result.dividedAreas()[0].equals(tempTextLineArea)) {
            result.nextState = ParaListDrawer.DrawingState.Normal;
        } else {
            result.nextState = ParaListDrawer.DrawingState.StartRecalculating;
        }
        result.offsetY += resultForTopBottom.yOffset();
        if (resultForTopBottom.yOffset() > 0 && resultForTopBottom.vertRelTo() == VertRelTo.Para) {
            result.cancelNewLine = true;
        }

        return new TextFlowCalculationResult(result, textLineArea);
    }

    public static class Result {
        private final Area[] dividedAreas;
        private long offsetY;
        private ParaListDrawer.DrawingState nextState;
        private boolean cancelNewLine;

        public Result(Area[] dividedAreas, long offsetY) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
            nextState = ParaListDrawer.DrawingState.Normal;
            cancelNewLine = false;
        }

        public Area[] dividedAreas() {
            return dividedAreas;
        }

        public long offsetY() {
            return offsetY;
        }

        public ParaListDrawer.DrawingState nextState() {
            return nextState;
        }

        public boolean cancelNewLine() {
            return cancelNewLine;
        }
    }

}
