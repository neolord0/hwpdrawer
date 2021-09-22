package kr.dogfoot.hwplib.drawer.drawer.textflow;

import kr.dogfoot.hwplib.drawer.drawer.ParaDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

import java.util.LinkedList;
import java.util.Queue;

public class TextFlowCalculationResult {
    private final long offsetY;
    private final boolean cancelNewLine;
    private final ParaDrawer.DrawingState nextState;

    private final Queue<Area> textPartAreas;
    private final Area storedTextLineArea;

    public TextFlowCalculationResult(ForFitWithText.Result resultForFitWithText, ForTakePlace.Result resultForTakePlace) {
        offsetY = resultForFitWithText.offsetY() + resultForTakePlace.offsetY();

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

    public boolean cancelNewLine() {
        return cancelNewLine;
    }

    public ParaDrawer.DrawingState nextState() {
        return nextState;
    }
}
