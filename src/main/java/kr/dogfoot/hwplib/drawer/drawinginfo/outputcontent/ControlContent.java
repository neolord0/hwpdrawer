package kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public abstract class ControlContent extends OutputContent  implements Comparable<ControlContent>  {
    protected Area controlArea;

    protected ControlContent() {
    }

    public abstract int zOrder();

    public abstract int textFlowMethod();

    public abstract Area calculatedControlArea();

    public abstract void adjustTextAreaAndVerticalAlignment();

    public abstract VertRelTo vertRelTo();

    public Area controlArea() {
        return controlArea;
    }

    public void controlArea(Area controlArea) {
        this.controlArea = controlArea;
    }

    public void moveY(long offsetY) {
        controlArea.moveY(offsetY);

        for (TextPart textPart : textParts) {
            textPart.area()
                    .moveY(offsetY);
        }

        for (ControlContent controlContent : behindChildContents) {
            if (controlContent.vertRelTo() == VertRelTo.Para) {
                controlContent.moveY(offsetY);
            }
        }

        for (ControlContent controlContent : nonBehindChildContents) {
            if (controlContent.vertRelTo() == VertRelTo.Para) {
                controlContent.moveY(offsetY);
            }
        }
    }

    @Override
    public int compareTo(ControlContent o) {
        if (zOrder() > o.zOrder())
            return 1;
        else if (zOrder() == o.zOrder())
            return 0;
        else
            return -1;
    }
}
