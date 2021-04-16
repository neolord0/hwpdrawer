package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
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

    public void line(ControlLine line, Area area) {
    }

    public void rectangle(ControlRectangle rectangle, Area area) throws Exception {
        painter.testBackStyle();
        painter.rectangle(area, true);
        painter.setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        painter.rectangle(area, false);
        drawText(area, rectangle.getTextBox());
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
    }

    public void ole(ControlOLE ole, Area area) {
    }

    public void container(ControlContainer container, Area area) {
    }

    public void objectLinkLine(ControlObjectLinkLine objectLinkLine, Area area) {
    }

    private void drawText(Area area, TextBox textBox) throws Exception {
        if (textBox == null) {
            return;
        }

        Area textArea = new Area(area).applyMargin(
                textBox.getListHeader().getLeftMargin(),
                textBox.getListHeader().getTopMargin(),
                textBox.getListHeader().getRightMargin(),
                textBox.getListHeader().getBottomMargin());

        info
                .newControlText(textArea, textBox.getListHeader().getProperty().getTextVerticalAlignment())
                .startControlParagraphList(textArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : textBox.getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        info.controlContent().adjustVerticalAlignment();

        paintControlContent();

        info.endParagraphList();
    }

    private void paintControlContent() throws Exception {
        painter.controlPainter().paintControls(info.controlContent().behindControls());
        painter.textDrawer().paintTextParts(info.controlContent().textParts());
        painter.controlPainter().paintControls(info.controlContent().notBehindControls());

    }
}
