package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextHorzArrange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeSet;

public class ForSquare {
    private TreeSet<ControlDrawer.ControlInfo> controlInfos;

    public ForSquare() {
    }

    public void controls(TreeSet<ControlDrawer.ControlInfo> controlInfos) {
        this.controlInfos = controlInfos;
    }

    public TextFlowCalculator.Result calculate(Area textLineArea) {
        LinkedList<Area> dividedAreas = new LinkedList<>();
        ArrayList<Area> addingAreas  = new ArrayList<>();
        ArrayList<Area> removingAreas = new ArrayList<>();

        boolean intersected;
        dividedAreas.add(textLineArea);
        for (ControlDrawer.ControlInfo controlInfo : controlInfos) {
            if (controlInfo.absoluteArea().intersects(textLineArea)) {
                addingAreas.clear();
                removingAreas.clear();

                intersected = false;
                for (Area area : dividedAreas) {
                    if (controlInfo.absoluteArea().intersects(area)) {
                        divideArea(area,
                                controlInfo.absoluteArea(),
                                controlInfo.headerGso().getProperty().getTextHorzArrange(),
                                addingAreas);
                        removingAreas.add(area);
                        intersected = true;
                    }
                }

                if (intersected == false) {
                    for (Area area : dividedAreas) {
                        switch (controlInfo.headerGso().getProperty().getTextHorzArrange()) {
                            case LeftOnly:
                                if (!isLeft(area, controlInfo.absoluteArea())) {
                                    removingAreas.add(area);
                                }
                                break;
                            case RightOnly:
                                if (!isRight(area, controlInfo.absoluteArea())) {
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
        long minBottom = 0;

        for (ControlDrawer.ControlInfo controlInfo : controlInfos) {
            if (controlInfo.absoluteArea().intersects(textLineArea)) {
                minBottom = (minBottom == 0 || minBottom > controlInfo.absoluteArea().bottom()) ?
                        controlInfo.absoluteArea().bottom() : minBottom;
            }
        }
        return minBottom - textLineArea.bottom();
    }
}
