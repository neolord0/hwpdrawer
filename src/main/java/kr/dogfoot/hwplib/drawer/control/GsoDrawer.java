package kr.dogfoot.hwplib.drawer.control;

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

public class GsoDrawer {
    private DrawingInfo info;

    public GsoDrawer() {
    }

    public void info(DrawingInfo info) {
        this.info = info;
    }

    public void line(ControlLine line, Area area) {
    }

    public void rectangle(ControlRectangle rectangle, Area area) throws Exception {
        Painter.singleObject().testBackStyle();
        Painter.singleObject().rectangle(area, true);
        Painter.singleObject().setLineStyle(BorderType.Solid, BorderThickness.MM0_12, new Color4Byte(0, 0 , 0, 0));
        Painter.singleObject().rectangle(area, false);
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

        info.startControlParagraphList(textArea);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer();

        for (Paragraph paragraph : textBox.getParagraphList()) {
            paragraphDrawer.draw(paragraph, info);
        }

        info.endParagraphList();
    }

}
