package kr.dogfoot.hwplib.drawer.paragraph.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.ControlContent;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.GsoContent;
import kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent.TableContent;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextBox;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

public class ControlDrawer {
    private DrawingInfo info;

    public ControlDrawer(DrawingInfo info) {
        this.info = info;
    }

    public ControlContent draw(ControlCharInfo controlCharInfo) throws Exception {
        switch (controlCharInfo.control().getType()) {
            case Gso:
                GsoControl gso = (GsoControl) controlCharInfo.control();

                switch (gso.getGsoType()) {
                    case Line:
                        return line((ControlLine) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Rectangle:
                        return rectangle((ControlRectangle) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Ellipse:
                        return ellipse((ControlEllipse) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Arc:
                        return arc((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Polygon:
                        return polygon((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Curve:
                        return curve((ControlArc) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Picture:
                        return picture((ControlPicture) gso, controlCharInfo.areaWithoutOuterMargin());
                    case OLE:
                        return ole((ControlOLE) gso, controlCharInfo.areaWithoutOuterMargin());
                    case Container:
                        return container((ControlContainer) gso, controlCharInfo.areaWithoutOuterMargin());
                    case ObjectLinkLine:
                        return objectLinkLine((ControlObjectLinkLine) gso, controlCharInfo.areaWithoutOuterMargin());
                }
                break;
            case Table:
                return table(controlCharInfo, controlCharInfo.areaWithoutOuterMargin());
        }
        return null;
    }

    private GsoContent line(ControlLine control, Area controlArea) {
        return null;
    }

    private GsoContent rectangle(ControlRectangle control, Area controlArea) throws Exception {
        GsoContent content = info.startGsoContent(control, controlArea);
        if (control.getTextBox() != null) {
            long calculatedContentHeight = drawTextBox(control.getTextBox(), content);
            content.calculatedContentHeight(calculatedContentHeight);
        }

        info.endGsoContent();
        return content;
    }


    private GsoContent ellipse(ControlEllipse control, Area controlArea) {
        return null;
    }

    private GsoContent arc(ControlArc control, Area controlArea) {
        return null;
    }

    private GsoContent polygon(ControlArc control, Area controlArea) {
        return null;
    }

    private GsoContent curve(ControlArc control, Area controlArea) {
        return null;
    }

    private GsoContent picture(ControlPicture control, Area controlArea) {
        return null;
    }

    private GsoContent ole(ControlOLE control, Area controlArea) {
        return null;
    }

    private GsoContent container(ControlContainer control, Area controlArea) {
        return null;
    }

    private GsoContent objectLinkLine(ControlObjectLinkLine control, Area controlArea) {
        return null;
    }

    private TableContent table(ControlCharInfo controlCharInfo, Area controlArea) {
        return null;
    }

    private long drawTextBox(TextBox textBox, GsoContent content) throws Exception {
        content
                .textMargin(
                        textBox.getListHeader().getLeftMargin(),
                        textBox.getListHeader().getTopMargin(),
                        textBox.getListHeader().getRightMargin(),
                        textBox.getListHeader().getBottomMargin())
                .verticalAlignment(textBox.getListHeader().getProperty().getTextVerticalAlignment());

        info.startControlParagraphList(content.textArea());

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : textBox.getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        return info.endControlParagraphList();
    }
}
