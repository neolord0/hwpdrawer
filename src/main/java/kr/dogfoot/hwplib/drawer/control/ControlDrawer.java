package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
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

    private TopBottomControls topBottomControls;
    private SquareControls squareControls;

    public ControlDrawer(HWPDrawer drawer) {
        this.drawer = drawer;

        positionCalculator = new PositionCalculator();

        controlInfosForFront = new TreeSet<>();
        controlInfosForBehind = new TreeSet<>();
        topBottomControls = new TopBottomControls();
        squareControls = new SquareControls();
    }


    public ControlDrawer controlList(ArrayList<Control> controlList, DrawingInfo info) {
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

            addControlSortedByZOrder(headerGso, c, info);
            topBottomControls.sortTopBottomAreas();
        }

        return this;
    }

    private void addControlSortedByZOrder(CtrlHeaderGso headerGso, Control control, DrawingInfo info) {
        if (headerGso.getProperty().isLikeWord()) {
            return;
        }

        ControlInfo controlInfo = new ControlInfo(control, headerGso, positionCalculator.absoluteArea(headerGso, info));
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
        drawer.painter().testBackStyle();
        drawer.painter().rectangle(controlInfo.absoluteArea, true);
        drawer.painter().setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        drawer.painter().rectangle(controlInfo.absoluteArea, false);
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

    public ControlDrawer drawControlsForSquare() {
        for (ControlInfo controlInfo : squareControls.controls()) {
            testControl(controlInfo);
        }
        return this;
    }

    public void removeControlsForSquare() {
        squareControls.clear();
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

    public TextFlowCheckResult checkTextFlow(Area textLineArea) {
        Area tempTextLineArea = new Area(textLineArea);
        long offsetY = topBottomControls.checkTextFlow(tempTextLineArea);
        tempTextLineArea.moveY(offsetY);

        TextFlowCheckResult result = squareControls.checkTextFlow(tempTextLineArea);
        if (result.dividedAreas == null) {
            result.nextState = ParagraphDrawer.DrawingState.StartingRedrawing;
        } else if (result.dividedAreas().length == 1 && result.dividedAreas()[0].equals(tempTextLineArea)) {
            result.nextState = ParagraphDrawer.DrawingState.Normal;
        } else {
            result.nextState = ParagraphDrawer.DrawingState.StartingRecalculating;
        }
        result.offsetY += offsetY;
        return result;
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

    public static class TextFlowCheckResult {
        private Area[] dividedAreas;
        private long offsetY;
        private ParagraphDrawer.DrawingState nextState;

        public TextFlowCheckResult(Area[] dividedAreas, long offsetY) {
            this.dividedAreas = dividedAreas;
            this.offsetY = offsetY;
        }

        public Area[] dividedAreas() {
            return dividedAreas;
        }

        public long offsetY() {
            return offsetY;
        }

        public ParagraphDrawer.DrawingState nextState() {
            return nextState;
        }
    }

}
