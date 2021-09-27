package kr.dogfoot.hwplib.drawer.drawer.para.textflow;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.para.ParaDrawingState;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextHorzArrange;

import java.util.*;

public class ForFitWithText {
    private final Map<CharInfoControl, Area> charInfos;
    private final TreeSet<FitWithTextArea> fitWithTextAreas;

    public ForFitWithText() {
        charInfos = new HashMap<>();
        fitWithTextAreas = new TreeSet<>();
    }
    public void add(CharInfoControl charInfo, Area areaWithOuterMargin) {
        charInfos.put(charInfo, areaWithOuterMargin);
        fitWithTextAreas.add(new FitWithTextArea(areaWithOuterMargin,
                charInfo.header()));
    }

    public boolean alreadyAdded(CharInfoControl charInfo) {
        return charInfos.containsKey(charInfo);
    }

    public Result calculate(Area textLineArea) {
        LinkedList<Area> dividedAreas = new LinkedList<>();
        ArrayList<Area> addingAreas = new ArrayList<>();
        ArrayList<Area> removingAreas = new ArrayList<>();

        boolean intersected;
        dividedAreas.add(textLineArea);
        for (FitWithTextArea squareArea : fitWithTextAreas) {
            if (squareArea.area.intersects(textLineArea)) {
                addingAreas.clear();
                removingAreas.clear();

                intersected = false;
                for (Area area : dividedAreas) {
                    if (squareArea.area.intersects(area)) {
                        divideArea(area,
                                squareArea.area,
                                squareArea.textHorzArrange,
                                addingAreas);
                        removingAreas.add(area);
                        intersected = true;
                    }
                }

                if (!intersected) {
                    for (Area area : dividedAreas) {
                        switch (squareArea.textHorzArrange) {
                            case LeftOnly:
                                if (!isLeft(area, squareArea.area)) {
                                    removingAreas.add(area);
                                }
                                break;
                            case RightOnly:
                                if (!isRight(area, squareArea.area)) {
                                    removingAreas.add(area);
                                }
                                break;
                        }
                    }
                }

                dividedAreas.removeAll(removingAreas);
                dividedAreas.addAll(addingAreas);
            }
        }
        Collections.sort(dividedAreas);

        if (dividedAreas.size() == 0) {
            return new Result(null, offsetY(textLineArea), textLineArea);
        } else {
            return new Result(dividedAreas.toArray(Area.Zero_Array), 0,  textLineArea);
        }
    }

    private void divideArea(Area textLineArea, Area controlArea, TextHorzArrange textHorzArrange, ArrayList<Area> addingAreas) {
        Area left, right;
        if (controlArea.left() <= textLineArea.left() &&
                controlArea.right() < textLineArea.right()) {
            left = null;
            right = new Area(controlArea.right(), textLineArea.top(), textLineArea.right(), textLineArea.bottom());
        } else if (controlArea.right() >= textLineArea.right() &&
                controlArea.left() > textLineArea.left()) {
            left = new Area(textLineArea.left(), textLineArea.top(), controlArea.left(), textLineArea.bottom());
            right = null;
        } else {
            left = new Area(textLineArea.left(), textLineArea.top(), controlArea.left(), textLineArea.bottom());
            right = new Area(controlArea.right(), textLineArea.top(), textLineArea.right(), textLineArea.bottom());
        }
        switch (textHorzArrange) {
            case BothSides:
                if (left != null) {
                    addingAreas.add(left);
                }
                if (right != null) {
                    addingAreas.add(right);
                }
                break;
            case LeftOnly:
                if (left != null) {
                    addingAreas.add(left);
                }
                break;
            case RightOnly:
                if (right != null) {
                    addingAreas.add(right);
                }
                break;
            case LargestOnly:
                long leftWidth = (left != null) ? left.width() : -1;
                long rightWidth = (right != null) ? right.width() : -1;
                if (leftWidth >= rightWidth) {
                    if (left != null) {
                        addingAreas.add(left);
                    }
                } else {
                    if (right != null) {
                        addingAreas.add(right);
                    }
                }
                break;
        }
    }

    private boolean isLeft(Area area, Area criteriaArea) {
        return area.right() < criteriaArea.left();
    }

    private boolean isRight(Area area, Area criteriaArea) {
        return area.left() > criteriaArea.right();
    }

    private long offsetY(Area textLineArea) {
        long minBottom = -1;

        for (FitWithTextArea squareArea : fitWithTextAreas) {
            if (squareArea.area.intersects(textLineArea)) {
                minBottom = (minBottom == -1) ? squareArea.area.bottom() : Math.min(squareArea.area.bottom(), minBottom);
            }
        }
        return minBottom - textLineArea.bottom();
    }

    public void reset() {
        fitWithTextAreas.clear();
    }

    public void delete(CharInfoControl charInfo) {
        charInfos.remove(charInfo);
        updateFitWithTextAreas();
    }

    private void updateFitWithTextAreas() {
        fitWithTextAreas.clear();
        for (CharInfoControl charInfo : charInfos.keySet()) {
            fitWithTextAreas.add(new FitWithTextArea(charInfos.get(charInfo),
                    charInfo.header()));
        }
    }

    private static class FitWithTextArea implements Comparable<FitWithTextArea> {
        public Area area;
        public int zOrder;
        public TextHorzArrange textHorzArrange;

        public FitWithTextArea(Area area, CtrlHeaderGso ctrlHeaderGso) {
            this.area = area;
            zOrder = ctrlHeaderGso.getzOrder();
            textHorzArrange = ctrlHeaderGso.getProperty().getTextHorzArrange();
        }

        @Override
        public int compareTo(FitWithTextArea o) {
            if (zOrder > o.zOrder)
                return 1;
            else if (zOrder == o.zOrder)
                return 0;
            else
                return -1;
        }
    }

    public static class Result {
        private final Area[] dividedAreas;
        private long offsetY;
        private ParaDrawingState nextState;
        private boolean cancelNewLine;

        public Result(Area[] dividedAreas, long offsetY, Area textLineArea) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
            cancelNewLine = false;

            if (dividedAreas == null) {
                nextState = ParaDrawingState.StartRedrawing;
            } else if (dividedAreas().length == 1 && dividedAreas[0].equals(textLineArea)) {
                nextState = ParaDrawingState.Normal;
            } else {
                nextState = ParaDrawingState.StartRecalculating;
            }
        }

        public Area[] dividedAreas() {
            return dividedAreas;
        }

        public long offsetY() {
            return offsetY;
        }

        public void offsetY(long offsetY) {
            this.offsetY = offsetY;
        }

        public ParaDrawingState nextState() {
            return nextState;
        }

        public Result nextState(ParaDrawingState nextState) {
            this.nextState = nextState;
            return this;
        }

        public boolean cancelNewLine() {
            return cancelNewLine;
        }

        public void cancelNewLine(boolean cancelNewLine) {
            this.cancelNewLine = cancelNewLine;
        }
    }
}
