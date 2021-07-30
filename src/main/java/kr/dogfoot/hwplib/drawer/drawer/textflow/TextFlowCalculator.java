package kr.dogfoot.hwplib.drawer.drawer.textflow;

import kr.dogfoot.hwplib.drawer.drawer.ParaDrawer;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class TextFlowCalculator {
    private final ForTakePlace forTakePlace;
    private final ForFitWithText forFitWithText;

    public TextFlowCalculator() {
        forTakePlace = new ForTakePlace();
        forFitWithText = new ForFitWithText();
    }

    public void add(ControlCharInfo controlCharInfo, Area areaWithOuterMargin) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            forTakePlace.add(controlCharInfo, areaWithOuterMargin);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            forFitWithText.add(controlCharInfo, areaWithOuterMargin);
        }
    }

    public void delete(ControlCharInfo controlCharInfo) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            forTakePlace.delete(controlCharInfo);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            forFitWithText.delete(controlCharInfo);
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

    public void resetForNewPage() {
        forFitWithText.reset();
    }

    public void resetForNewColumn() {
        forTakePlace.reset();
    }

    public TextFlowCalculationResult calculate(Area textLineArea) {
        Area tempTextLineArea = new Area(textLineArea);
        ForTakePlace.Result resultForTakePlace = forTakePlace.calculate(tempTextLineArea);
        tempTextLineArea.moveY(resultForTakePlace.yOffset());

        Result result = forFitWithText.calculate(tempTextLineArea);
        if (result.dividedAreas == null) {
            result.nextState = ParaDrawer.DrawingState.StartRedrawing;
        } else if (result.dividedAreas().length == 1 && result.dividedAreas()[0].equals(tempTextLineArea)) {
            result.nextState = ParaDrawer.DrawingState.Normal;
        } else {
            result.nextState = ParaDrawer.DrawingState.StartRecalculating;
        }
        result.offsetY += resultForTakePlace.yOffset();
        if (resultForTakePlace.yOffset() > 0 && resultForTakePlace.vertRelTo() == VertRelTo.Para) {
            result.cancelNewLine = true;
        }

        return new TextFlowCalculationResult(result);
    }

    public static class Result {
        private final Area[] dividedAreas;
        private long offsetY;
        private ParaDrawer.DrawingState nextState;
        private boolean cancelNewLine;

        public Result(Area[] dividedAreas, long offsetY) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
            nextState = ParaDrawer.DrawingState.Normal;
            cancelNewLine = false;
        }

        public Area[] dividedAreas() {
            return dividedAreas;
        }

        public long offsetY() {
            return offsetY;
        }

        public ParaDrawer.DrawingState nextState() {
            return nextState;
        }

        public boolean cancelNewLine() {
            return cancelNewLine;
        }
    }
}
