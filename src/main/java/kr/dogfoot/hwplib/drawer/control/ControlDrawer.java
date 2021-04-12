package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.util.ArrayList;
import java.util.TreeSet;

public class ControlDrawer {
    private HWPDrawer drawer;
    private PositionCalculator positionCalculator;

    private TreeSet<ControlInfo> controlInfosForFront;
    private TreeSet<ControlInfo> controlInfosForBehind;
    private TreeSet<ControlInfo> controlInfosForTopBottom;
    private TreeSet<ControlInfo> controlInfosForSquare;

    public ControlDrawer(HWPDrawer drawer) {
        this.drawer = drawer;

        positionCalculator = new PositionCalculator();

        controlInfosForFront = new TreeSet<>();
        controlInfosForBehind = new TreeSet<>();
        controlInfosForTopBottom = new TreeSet();
        controlInfosForSquare = new TreeSet();
    }

    public ControlDrawer controlList(ArrayList<Control> controlList, DrawingInfo info) {
        if (controlList == null) {
            return this;
        }

        for (Control control : controlList) {
            CtrlHeaderGso headerGso = null;
            switch (control.getType()) {
                case Table:
                    headerGso = ((ControlTable) control).getHeader();
                    break;
                case Gso:
                    headerGso = ((GsoControl) control).getHeader();
                    break;
            }

            if (headerGso == null || headerGso.getProperty().isLikeWord() == true) {
                continue;
            }

            addControlSortedByZOrder(headerGso, control, info);
        }

        return this;
    }

    private void addControlSortedByZOrder(CtrlHeaderGso headerGso, Control control, DrawingInfo info) {
        if (headerGso.getProperty().isLikeWord()) {
            return;
        }

        ControlInfo controlInfo = new ControlInfo(control, headerGso, positionCalculator.absoluteArea(headerGso, info));
        if (headerGso.getProperty().getTextFlowMethod() == 0) {
            controlInfosForSquare.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 1) {
            controlInfosForTopBottom.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 2) {
            controlInfosForBehind.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 3) {
            controlInfosForFront.add(controlInfo);
        }
    }

    public ControlDrawer drawControlsForBehind() {
        for (ControlInfo controlInfo : controlInfosForBehind) {
            draw(controlInfo);
        }
        return this;
    }

    public void removeControlsForBehind() {
        controlInfosForBehind.clear();
    }

    public ControlDrawer drawControlsForFront() {
        for (ControlInfo controlInfo : controlInfosForFront) {
            draw(controlInfo);
        }
        return this;
    }

    public void removeControlsForFront() {
        controlInfosForFront.clear();
    }

    public ControlDrawer drawControlsForTopBottom() {
        for (ControlInfo controlInfo : controlInfosForTopBottom) {
            draw(controlInfo);
        }
        return this;
    }

    public void removeControlsForTopBottom() {
        controlInfosForTopBottom.clear();
    }

    public ControlDrawer drawControlsForSquare() {
        for (ControlInfo controlInfo : controlInfosForSquare) {
            draw(controlInfo);
        }
        return this;
    }

    public void removeControlsForSquare() {
        controlInfosForSquare.clear();
    }

    public TreeSet<ControlInfo> controlsForSquare() {
        return controlInfosForSquare;
    }

    public TreeSet<ControlInfo> controlsForTopBottom() {
        return controlInfosForTopBottom;
    }

    private void draw(ControlInfo controlInfo) {
        drawer.painter().testBackStyle();
        drawer.painter().rectangle(controlInfo.absoluteArea, true);
        drawer.painter().setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        drawer.painter().rectangle(controlInfo.absoluteArea, false);
    }

    public static class ControlInfo implements Comparable<ControlInfo> {
        public static ControlInfo[] Zero_Array = new ControlInfo[0];

        private Control control;
        private CtrlHeaderGso headerGso;
        private Area absoluteArea;

        public ControlInfo(Control control, CtrlHeaderGso headerGso, Area absoluteArea) {
            this.control = control;
            this.headerGso = headerGso;
            this.absoluteArea = absoluteArea;
        }

        public int compareTo(ControlInfo o) {
            if(headerGso.getzOrder() > o.headerGso.getzOrder())
                return 1;
            else if (headerGso.getzOrder() == o.headerGso.getzOrder())
                return 0;
            else
                return -1;
        }

        public Control control() {
            return control;
        }

        public CtrlHeaderGso headerGso() {
            return headerGso;
        }

        public Area absoluteArea() {
            return absoluteArea;
        }
    }
}
