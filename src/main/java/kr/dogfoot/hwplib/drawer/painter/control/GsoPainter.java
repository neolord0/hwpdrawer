package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

public class GsoPainter {
    private Painter painter;
    private DrawingInfo info;

    public GsoPainter(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;
    }

    public void line(ControlLine line, Area areaWithoutOuterMargin) {
    }

    public void rectangle(ControlRectangle rectangle, Area areaWithoutOuterMargin) throws Exception {
        painter.testBackStyle();
        painter.rectangle(areaWithoutOuterMargin, true);
        drawText(areaWithoutOuterMargin, rectangle.getTextBox());
        painter.setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        painter.rectangle(areaWithoutOuterMargin, false);
    }

    public void ellipse(ControlEllipse ellipse, Area areaWithoutOuterMargin) {
    }

    public void arc(ControlArc arc, Area areaWithoutOuterMargin) {
    }

    public void polygon(ControlPolygon polygon, Area areaWithoutOuterMargin) {
    }

    public void curve(ControlCurve curve, Area areaWithoutOuterMargin) {
    }

    public void picture(ControlPicture picture, Area areaWithoutOuterMargin) {
    }

    public void ole(ControlOLE ole, Area areaWithoutOuterMargin) {
    }

    public void container(ControlContainer container, Area areaWithoutOuterMargin) {
    }

    public void objectLinkLine(ControlObjectLinkLine objectLinkLine, Area areaWithoutOuterMargin) {
    }

    private void drawText(Area areaWithoutOuterMargin, TextBox textBox) throws Exception {
        if (textBox == null) {
            return;
        }

        Area textArea = new Area(areaWithoutOuterMargin).applyMargin(
                textBox.getListHeader().getLeftMargin(),
                textBox.getListHeader().getTopMargin(),
                textBox.getListHeader().getRightMargin(),
                textBox.getListHeader().getBottomMargin());

        info
                .newControlText(textArea)
                .startControlParagraphList(textArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : textBox.getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        info.endParagraphList();

        info.controlContent()
                .adjustVerticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());

        paintControlContent();

    }

    private void paintControlContent() throws Exception {
        painter.controlPainter().paintControls(info.controlContent().behindControls());
        painter.textDrawer().paintTextParts(info.controlContent().textParts());
        painter.controlPainter().paintControls(info.controlContent().notBehindControls());

    }
}
