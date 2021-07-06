package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.ParaDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.LinkedList;
import java.util.Queue;

public class TextFlowCalculationResult {
    private final TextFlowCalculator.Result result;
    private final Queue<Area> textPartAreas;
    private Area storedTextLineArea;

    public TextFlowCalculationResult(TextFlowCalculator.Result result, Area textLineArea) {
        this.result = result;
        textPartAreas = new LinkedList<>();
        storedTextLineArea = new Area(0, 0, 0, 0);

        if (result.nextState().isStartRecalculating()) {
            textPartAreas(result.dividedAreas());
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
        return result.offsetY();
    }

    public boolean cancelNewLine() {
        return result.cancelNewLine();
    }

    public ParaDrawer.DrawingState nextState() {
        return result.nextState();
    }
}
