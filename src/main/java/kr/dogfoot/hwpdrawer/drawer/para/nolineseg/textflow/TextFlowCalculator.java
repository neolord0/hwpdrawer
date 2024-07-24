package kr.dogfoot.hwpdrawer.drawer.para.nolineseg.textflow;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;

public class TextFlowCalculator {
    private final ForTakePlace forTakePlace;
    private final ForFitWithText forFitWithText;

    public TextFlowCalculator() {
        forTakePlace = new ForTakePlace();
        forFitWithText = new ForFitWithText();
    }

    public void add(CharInfoControl controlCharInfo, Area areaWithOuterMargin) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            forTakePlace.add(controlCharInfo, areaWithOuterMargin);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            forFitWithText.add(controlCharInfo, areaWithOuterMargin);
        }
    }

    public void addForTakePlace(CharInfoControl controlCharInfo, Area areaWithOuterMargin) {
        forTakePlace.add(controlCharInfo, areaWithOuterMargin);
    }


    public void delete(CharInfoControl controlCharInfo) {
        if (controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            forTakePlace.delete(controlCharInfo);
        } else if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText) {
            forFitWithText.delete(controlCharInfo);
        }
    }

    public boolean alreadyAdded(CharInfoControl controlCharInfo) {
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
        tempTextLineArea.moveY(resultForTakePlace.offsetY());
        ForFitWithText.Result resultForFitWithText = forFitWithText.calculate(tempTextLineArea);

        return new TextFlowCalculationResult(resultForFitWithText, resultForTakePlace);
    }
}
