package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.DrawingInfo;
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
    private DrawingInfo info;
    private PositionCalculator positionCalculator;

    private TreeSet<ControlInfo> controlInfosForFront;
    private TreeSet<ControlInfo> controlInfosForBehind;
    private TopBottomControls topBottomControls;
    private SquareControls squareControls;

    public ControlDrawer(DrawingInfo info) {
        this.info = info;

        positionCalculator = new PositionCalculator(info);

        controlInfosForFront = new TreeSet<>();
        controlInfosForBehind = new TreeSet<>();
        topBottomControls = new TopBottomControls();
        squareControls = new SquareControls();
    }


    public ControlDrawer controlList(ArrayList<Control> controlList) {
        if (controlList == null) {
            return this;
        }

        for (Control c : controlList) {
            CtrlHeaderGso headerGso = null;
            switch (c.getType()) {
                case Table:
                    headerGso = ((ControlTable) c).getHeader();
                    break;
                case Gso:
                    headerGso = ((GsoControl) c).getHeader();
                    break;
            }

            if (headerGso == null) {
                continue;
            }

            addControlSortedByZOrder(headerGso, c);
            topBottomControls.sortTopBottomAreas();
        }

        return this;
    }

    private void addControlSortedByZOrder(CtrlHeaderGso headerGso, Control control) {
        if (headerGso.getProperty().isLikeWord()) {
            return;
        }

        Area absoluteArea = positionCalculator.absoluteArea(headerGso);
        System.out.println("input : " + absoluteArea.toString() + " " + headerGso.getzOrder() + " " + headerGso.getProperty().getTextFlowMethod());

        ControlInfo controlInfo = new ControlInfo(control, headerGso, positionCalculator.absoluteArea(headerGso));
        if (headerGso.getProperty().getTextFlowMethod() == 0) {
            squareControls.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 1) {
            topBottomControls.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 2) {
            controlInfosForBehind.add(controlInfo);
        } else if (headerGso.getProperty().getTextFlowMethod() == 3) {
            controlInfosForFront.add(controlInfo);
        }
    }

    public ControlDrawer drawControlsForBehind() {
        for (ControlInfo controlInfo : controlInfosForBehind) {
            testControl(controlInfo);
        }
        return this;
    }

    private void testControl(ControlInfo controlInfo) {
        info.painter().testBackStyle();
        info.painter().rectangle(controlInfo.absoluteArea, true);
        info.painter().setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        info.painter().rectangle(controlInfo.absoluteArea, false);
    }

    public void removeControlsForBehind() {
        controlInfosForBehind.clear();
    }

    public ControlDrawer drawControlsForFront() {
        for (ControlInfo controlInfo : controlInfosForFront) {
            testControl(controlInfo);
        }
        return this;
    }

    public void removeControlsForFront() {
        controlInfosForFront.clear();
    }

    public ControlDrawer drawControlsForTopBottom() {
        for (ControlInfo controlInfo : topBottomControls.controls()) {
            testControl(controlInfo);
        }
        return this;
    }

    public void removeControlsForTopBottom() {
        topBottomControls.clear();
    }

    public String toTest() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n");
        sb.append("Square {\r\n");
        for (ControlInfo info : squareControls.controls()) {
            sb
                    .append("\t")
                    .append(info.absoluteArea)
                    .append(" ")
                    .append(info.headerGso.getzOrder())
                    .append(" ")
                    .append(info.headerGso.getProperty().getTextFlowMethod())
                    .append("\r\n");
        }
        sb.append("}\r\n");

        sb.append("TopBottom {\r\n");
        for (ControlInfo info : topBottomControls.controls()) {
            sb
                    .append("\t")
                    .append(info.absoluteArea)
                    .append(" ")
                    .append(info.headerGso.getzOrder())
                    .append(" ")
                    .append(info.headerGso.getProperty().getTextFlowMethod())
                    .append("\r\n");
        }
        sb.append("}\r\n");

        sb.append("Front {\r\n");
        for (ControlInfo info : controlInfosForFront) {
            sb
                    .append("\t")
                    .append(info.absoluteArea)
                    .append(" ")
                    .append(info.headerGso.getzOrder())
                    .append(" ")
                    .append(info.headerGso.getProperty().getTextFlowMethod())
                    .append("\r\n");
        }
        sb.append("}\r\n");

        sb.append("Behind {\r\n");
        for (ControlInfo info : controlInfosForBehind) {
            sb
                    .append("\t")
                    .append(info.absoluteArea)
                    .append(" ")
                    .append(info.headerGso.getzOrder())
                    .append(" ")
                    .append(info.headerGso.getProperty().getTextFlowMethod())
                    .append("\r\n");
        }
        sb.append("}\r\n");

        return sb.toString();
    }

    public long checkTopBottomTextFlow(Area textLineArea) {
        return topBottomControls.checkTopBottomTextFlow(textLineArea);
    }

    public Area[] checkSquareTextFlow(Area textLineArea) {
        return squareControls.checkSquareTextFlow(textLineArea);
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
