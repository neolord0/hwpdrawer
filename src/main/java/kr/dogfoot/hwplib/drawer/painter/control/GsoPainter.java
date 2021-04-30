package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.GsoContent;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponentNormal;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineInfo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

public class GsoPainter {
    private Painter painter;
    private DrawingInfo info;

    public GsoPainter(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;
    }

    public void line(ControlLine line, Area area) {
    }

    public void rectangle(ControlRectangle rectangle, Area area) throws Exception {
        painter.backgroundPainter().paint(((ShapeComponentNormal)(rectangle.getShapeComponent())).getFillInfo(), area);
        drawText(rectangle.getTextBox(), area);
        ;
        boolean drawLine = setBorderLine(((ShapeComponentNormal)(rectangle.getShapeComponent())).getLineInfo());
        if (drawLine == true) {
            painter.rectangle(area, false);
        }
    }

    public void ellipse(ControlEllipse ellipse, Area area) {
    }

    public void arc(ControlArc arc, Area area) {
    }

    public void polygon(ControlPolygon polygon, Area area) {
    }

    public void curve(ControlCurve curve, Area area) {
    }

    public void picture(ControlPicture picture, Area area) {
        painter.paintImage(area, info.getImage(picture.getShapeComponentPicture().getPictureInfo().getBinItemID()));
        boolean drawLine = setBorderLine(picture.getShapeComponentPicture().getBorderProperty().getLineType(),
                picture.getShapeComponentPicture().getBorderThickness(),
                picture.getShapeComponentPicture().getBorderColor());
        if (drawLine == true) {
            painter.rectangle(area, false);
        }
    }

    public void ole(ControlOLE ole, Area area) {
    }

    public void container(ControlContainer container, Area area) {
    }

    public void objectLinkLine(ControlObjectLinkLine objectLinkLine, Area area) {
    }


    private void drawText(TextBox textBox, Area controlArea) throws Exception {
        /*
        if (textBox == null) {
            return;
        }

        Area textArea = new Area(controlArea).applyMargin(
                textBox.getListHeader().getLeftMargin(),
                textBox.getListHeader().getTopMargin(),
                textBox.getListHeader().getRightMargin(),
                textBox.getListHeader().getBottomMargin());

        info.startContentContentAndParagraphList(controlArea, textArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : textBox.getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        GsoContent controlContent = info.endControlContentAndParagraphList();

        controlContent.adjustVerticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());
        paintControlContent(controlContent);

         */
    }

    private void paintControlContent(GsoContent controlContent) throws Exception {
        /*
        painter.controlPainter().paintControls(controlContent.behindControls());
        painter.textPainter().paintTextParts(controlContent.textParts());
        painter.controlPainter().paintControls(controlContent.nonBehindControls());

         */
    }

    private boolean setBorderLine(LineInfo lineInfo) {
        return setBorderLine(lineInfo.getProperty().getLineType(), lineInfo.getThickness(), lineInfo.getColor());
    }

    private boolean setBorderLine(LineType type, int thickness, Color4Byte color) {
        if (type == LineType.None) {
            return false;
        }
        painter.setLineStyle(type, thickness, color);
        return true;
    }

    public void line(GsoContent gsoContent) {
    }

    public void rectangle(GsoContent gsoContent) throws Exception {
        ControlRectangle rectangle = (ControlRectangle) gsoContent.control();
        painter.backgroundPainter().paint(((ShapeComponentNormal)(rectangle.getShapeComponent())).getFillInfo(), gsoContent.controlArea());

        gsoContent.adjustTextAreaAndVerticalAlignment();
        painter.paintContent(gsoContent);
        boolean drawLine = setBorderLine(((ShapeComponentNormal)(rectangle.getShapeComponent())).getLineInfo());
        if (drawLine == true) {
            painter.rectangle(gsoContent.controlArea(), false);
        }
    }

    public void ellipse(GsoContent gsoContent) {
    }

    public void arc(GsoContent gsoContent) {
    }

    public void polygon(GsoContent gsoContent) {
    }

    public void curve(GsoContent gsoContent) {
    }

    public void picture(GsoContent gsoContent) {
    }

    public void ole(GsoContent gsoContent) {
    }

    public void container(GsoContent gsoContent) {
    }

    public void objectLinkLine(GsoContent gsoContent) {
    }
}
