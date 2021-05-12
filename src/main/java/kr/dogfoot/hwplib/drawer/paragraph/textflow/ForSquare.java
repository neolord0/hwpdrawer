package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextHorzArrange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeSet;

public class ForSquare {
    private final TreeSet<SquareArea> squareAreas;

    public ForSquare() {
        squareAreas = new TreeSet<>();
    }

    public void add(ControlCharInfo controlCharInfo) {
        squareAreas.add(new SquareArea(controlCharInfo.areaWithOuterMargin(),
                controlCharInfo.header()));
    }

    public TextFlowCalculator.Result calculate(Area textLineArea) {
        LinkedList<Area> dividedAreas = new LinkedList<>();
        ArrayList<Area> addingAreas = new ArrayList<>();
        ArrayList<Area> removingAreas = new ArrayList<>();

        boolean intersected;
        dividedAreas.add(textLineArea);
        for (SquareArea squareArea : squareAreas) {
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
            return new TextFlowCalculator.Result(null, offsetY(textLineArea));
        } else {
            return new TextFlowCalculator.Result(dividedAreas.toArray(Area.Zero_Array), 0);
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
            case LargestOnly: {
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

        for (SquareArea squareArea : squareAreas) {
            if (squareArea.area.intersects(textLineArea)) {
                minBottom = (minBottom == -1) ? squareArea.area.bottom() : Math.min(squareArea.area.bottom(), minBottom);
            }
        }
        return minBottom - textLineArea.bottom();
    }

    public void reset() {
        squareAreas.clear();
    }

    private static class SquareArea implements Comparable<SquareArea> {
        public Area area;
        public int zOrder;
        public TextHorzArrange textHorzArrange;

        public SquareArea(Area area, CtrlHeaderGso ctrlHeaderGso) {
            this.area = area;
            zOrder = ctrlHeaderGso.getzOrder();
            textHorzArrange = ctrlHeaderGso.getProperty().getTextHorzArrange();
        }

        @Override
        public int compareTo(SquareArea o) {
            if (zOrder > o.zOrder)
                return 1;
            else if (zOrder == o.zOrder)
                return 0;
            else
                return -1;
        }
    }
}
