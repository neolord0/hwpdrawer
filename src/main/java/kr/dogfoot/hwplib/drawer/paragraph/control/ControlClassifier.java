package kr.dogfoot.hwplib.drawer.paragraph.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;

import java.util.ArrayList;
import java.util.TreeSet;

public class ControlClassifier {
    private PositionCalculator positionCalculator;

    private TreeSet<ControlInfo> controlInfosForFront;
    private TreeSet<ControlInfo> controlInfosForBehind;
    private TreeSet<ControlInfo> controlInfosForTopBottom;
    private TreeSet<ControlInfo> controlInfosForSquare;

    public ControlClassifier() {
        positionCalculator = new PositionCalculator();

        controlInfosForFront = new TreeSet<>();
        controlInfosForBehind = new TreeSet<>();
        controlInfosForTopBottom = new TreeSet();
        controlInfosForSquare = new TreeSet();
    }

    public ControlClassifier controlList(ArrayList<Control> controlList, DrawingInfo info) {
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

    public TreeSet<ControlInfo> controlsForBehind() {
        return controlInfosForBehind;
    }

    public TreeSet<ControlInfo> controlsForFront() {
        return controlInfosForFront;
    }

    public TreeSet<ControlInfo> controlsForTopBottom() {
        return controlInfosForTopBottom;
    }

    public TreeSet<ControlInfo> controlsForSquare() {
        return controlInfosForSquare;
    }

    public void removeControlsForBehind() {
        controlInfosForBehind.clear();
    }

    public void removeControlsForFront() {
        controlInfosForFront.clear();
    }

    public void removeControlsForTopBottom() {
        controlInfosForTopBottom.clear();
    }

    public void removeControlsForSquare() {
        controlInfosForSquare.clear();
    }

    public static class ControlInfo implements Comparable<ControlInfo> {
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
