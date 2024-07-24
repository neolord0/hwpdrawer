package kr.dogfoot.hwpdrawer.util;

import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class PositionCalculator {
    private final static PositionCalculator singleObject = new PositionCalculator();

    public static PositionCalculator singleObject() {
        return singleObject;
    }

    private DrawingInput info;
    private CtrlHeaderGso gsoHeader;

    public Area area(CtrlHeaderGso gsoHeader, DrawingInput info, Area currentTextPartArea) {
        this.info = info;
        this.gsoHeader = gsoHeader;

        Area area = new Area(0, 0, width(), height());
        if (!gsoHeader.getProperty().isLikeWord()) {
            area
                    .move(xOffset(area.width()) + gsoHeader.getOutterMarginLeft(),
                            yOffset(area.height(), currentTextPartArea) + gsoHeader.getOutterMarginTop());
        }
        return area;
    }

    private long width() {
        long width = gsoHeader.getWidth();
        switch (gsoHeader.getProperty().getWidthCriterion()) {
            case Paper:
                return info.pageInfo().paperArea().width() * width / 10000;
            case Page:
                return info.pageInfo().bodyArea().width() * width / 10000;
            case Column:
                // todo
                return info.paraArea().width() * width / 10000;
            case Para:
                return info.paraArea().width() * width / 10000;
            case Absolute:
                return width;
        }
        return width;
    }

    private long height() {
        long height = gsoHeader.getHeight();
        switch (gsoHeader.getProperty().getHeightCriterion()) {
            case Paper:
                return info.pageInfo().paperArea().height() * height / 10000;
            case Page:
                return info.pageInfo().bodyArea().height() * height / 10000;
            case Absolute:
                return height;
        }
        return height;
    }

    private long xOffset(long width) {
        Area criterionArea = null;
        switch (gsoHeader.getProperty().getHorzRelTo()) {
            case Paper:
                criterionArea = info.pageInfo().paperArea();
                break;
            case Page:
                criterionArea = info.pageInfo().bodyArea();
                break;
            case Column:
                // todo
                criterionArea = info.paraArea();
                break;
            case Para:
                criterionArea = info.paraArea();
                break;
        }
        if (criterionArea == null) {
            return 0;
        }

        long xOffset = gsoHeader.getxOffset();
        switch (gsoHeader.getProperty().getHorzRelativeArrange()) {
            case TopOrLeft:
            case Inside:
                return criterionArea.left() + xOffset;
            case Center:
                return criterionArea.left() + (criterionArea.width() - width) / 2 + xOffset;
            case BottomOrRight:
                return criterionArea.right() - xOffset - width;
            case Outside:
                return criterionArea.right() - width + xOffset;
        }
        return criterionArea.left() + xOffset;
    }

    private long yOffset(long height, Area currentTextPartArea) {
        Area criterionArea = null;
        switch (gsoHeader.getProperty().getVertRelTo()) {
            case Paper:
                criterionArea = info.pageInfo().paperArea();
                break;
            case Page:
                criterionArea = info.pageInfo().bodyArea();
                break;
            case Para:
                criterionArea = currentTextPartArea;
                break;
        }

        if (criterionArea == null) {
            return 0;
        }
        long yOffset = gsoHeader.getyOffset();
        if (gsoHeader.getProperty().getVertRelTo() == VertRelTo.Para) {
            yOffset = Math.max(0, yOffset);
        }
        switch (gsoHeader.getProperty().getVertRelativeArrange()) {
            case TopOrLeft:
            case Inside:
                return criterionArea.top() + yOffset;
            case Center:
                return criterionArea.top() + (criterionArea.height() - height) / 2 + yOffset;
            case BottomOrRight:
            case Outside:
                return criterionArea.bottom() - height + yOffset;
        }
        return yOffset;
    }
}
