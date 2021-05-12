package kr.dogfoot.hwplib.drawer.painter.control;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.GsoOutput;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.*;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponentNormal;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineInfo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

public class GsoPainter {
    private final Painter painter;
    private final DrawingInfo info;

    public GsoPainter(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;
    }

    public void line(ControlLine line, Area area) {
    }

    public void picture(ControlPicture picture, Area area) {
        painter.image(area, info.getImage(picture.getShapeComponentPicture().getPictureInfo().getBinItemID()));
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

    public void line(GsoOutput gsoOutput) {
    }

    public void rectangle(GsoOutput gsoOutput) throws Exception {
        ControlRectangle rectangle = (ControlRectangle) gsoOutput.gso();
        painter.backgroundPainter().paint(((ShapeComponentNormal)(rectangle.getShapeComponent())).getFillInfo(), gsoOutput.controlArea());

        boolean drawLine = setBorderLine(((ShapeComponentNormal)(rectangle.getShapeComponent())).getLineInfo());

        if (drawLine) {
            painter.rectangle(gsoOutput.controlArea(), false);
        }

        gsoOutput.adjustTextAreaAndVerticalAlignment();
        painter.paintContent(gsoOutput.content());

    }

    public void ellipse(GsoOutput gsoOutput) {
    }

    public void arc(GsoOutput gsoOutput) {
    }

    public void polygon(GsoOutput gsoOutput) {
    }

    public void curve(GsoOutput gsoOutput) {
    }

    public void picture(GsoOutput gsoOutput) {
        ControlPicture picture = (ControlPicture) gsoOutput.gso();
        painter.image(gsoOutput.controlArea(), info.getImage(picture.getShapeComponentPicture().getPictureInfo().getBinItemID()));

        boolean drawLine = setBorderLine(picture.getShapeComponentPicture().getBorderProperty().getLineType(),
                picture.getShapeComponentPicture().getBorderThickness(),
                picture.getShapeComponentPicture().getBorderColor());

        if (drawLine) {
            painter.rectangle(gsoOutput.controlArea(), false);
        }
    }

    public void ole(GsoOutput gsoOutput) {
    }

    public void container(GsoOutput gsoOutput) {
    }

    public void objectLinkLine(GsoOutput gsoOutput) {
    }
}
