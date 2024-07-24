package kr.dogfoot.hwpdrawer.drawer.para.nolineseg.textflow;

import kr.dogfoot.hwpdrawer.drawer.para.nolineseg.ParaDrawingState;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

import java.util.LinkedList;
import java.util.Queue;

public class TextFlowCalculationResult {
    private final long offsetY;
    private final long nextStartY;
    private final boolean cancelNewLine;
    private final ParaDrawingState nextState;

    private final Queue<Area> textPartAreas;
    private final Area storedTextLineArea;

    public TextFlowCalculationResult(ForFitWithText.Result resultForFitWithText, ForTakePlace.Result resultForTakePlace) {
        offsetY = resultForFitWithText.offsetY() + resultForTakePlace.offsetY();
        nextStartY = resultForFitWithText.nextStartY();

        if (resultForTakePlace.offsetY() > 0 && resultForTakePlace.vertRelTo() == VertRelTo.Para) {
            cancelNewLine = true;
        } else {
            cancelNewLine = resultForFitWithText.cancelNewLine();
        }
        nextState = resultForFitWithText.nextState();

        textPartAreas = new LinkedList<>();
        storedTextLineArea = new Area(0, 0, 0, 0);
        if (nextState.isStartRecalculating()) {
            textPartAreas(resultForFitWithText.dividedAreas());
        }
    }

    public Area nextArea() {
        return textPartAreas.poll();
    }

    public long startX(Area currentTextLineArea) {
        return currentTextLineArea.left() - storedTextLineArea.left();
    }

    public boolean lastTextPart() {
        return textPartAreas.size() == 0;
    }

    public Area storedTextLineArea() {
        return storedTextLineArea;
    }

    public TextFlowCalculationResult storeTextLineArea(Area textLineArea) {
        storedTextLineArea.set(textLineArea);
        return this;
    }

    public void textPartAreas(Area[] areas) {
        for (Area area : areas) {
            textPartAreas.offer(area);
        }
    }

    public long offsetY() {
        return offsetY;
    }

    public long nextStartY() {
        return nextStartY;
    }

    public boolean cancelNewLine() {
        return cancelNewLine;
    }

    public ParaDrawingState nextState() {
        return nextState;
    }
}
