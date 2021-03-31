package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;

public class PositionCalculator {
    private CtrlHeaderGso header;
    private DrawingInfo info;

    public PositionCalculator() {
    }

    public Area absoluteArea(CtrlHeaderGso header, DrawingInfo info) {
        this.header = header;
        this.info = info;

        Area area = new Area(0, 0, width(), height());
        area.moveX(xOffset(area.width()));
        area.moveY(yOffset(area.height()));
        return area;
    }

    private long width() {
        long width = header.getWidth();
        switch (header.getProperty().getWidthCriterion()) {
            case Paper:
                return info.paperArea().width() * width / 10000;
            case Page:
                return info.pageArea().width() * width / 10000;
            case Column:
                // todo
                return info.paragraphArea().width() * width / 10000;
            case Para:
                return info.paragraphArea().width() * width / 10000;
            case Absolute:
                return width;
        }
        return width;
    }

    private long height() {
        long height = header.getHeight();
        switch (header.getProperty().getHeightCriterion()) {
            case Paper:
                return info.paperArea().height() * height / 10000;
            case Page:
                return info.pageArea().height() * height / 10000;
            case Absolute:
                return height;
        }
        return height;
    }

    private long xOffset(long width) {
        Area criterionArea = null;
        switch (header.getProperty().getHorzRelTo()) {
            case Paper:
                criterionArea = info.paperArea();
                break;
            case Page:
                criterionArea = info.pageArea();
                break;
            case Column:
                // todo
                criterionArea = info.paragraphArea();
                break;
            case Para:
                // todo
                criterionArea = info.paragraphArea();
                break;
        }
        if (criterionArea == null) {
            return 0;
        }
        long xOffset = header.getxOffset();
        switch (header.getProperty().getHorzRelativeArrange()) {
            case TopOrLeft:
            case Inside:
                return criterionArea.left() + xOffset;
            case Center:
                return criterionArea.left() + (criterionArea.width() - width) / 2 + xOffset;
            case BottomOrRight:
            case Outside:
                return criterionArea.right() - width + xOffset;
        }
        return criterionArea.left() + xOffset;
    }

    private long yOffset(long height) {
        Area criterionArea = null;
        switch (header.getProperty().getVertRelTo()) {
            case Paper:
                criterionArea = info.paperArea();
                break;
            case Page:
                criterionArea = info.pageArea();
                break;
            case Para:
                criterionArea = info.paragraphArea();
                break;
        }
        if (criterionArea == null) {
            return 0;
        }
        long yOffset = header.getyOffset();
        switch (header.getProperty().getVertRelativeArrange()) {
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
