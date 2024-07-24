package kr.dogfoot.hwpdrawer.painter.image.control;

import kr.dogfoot.hwpdrawer.painter.image.PainterForImage;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlRectangle;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponentNormal;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineInfo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

public class GsoPainter {
    private final DrawingInput input;
    private final PainterForImage painter;

    public GsoPainter(DrawingInput input, PainterForImage painter) {
        this.input = input;
        this.painter = painter;
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
        gsoOutput.applyCalculatedContentHeight();

        ControlRectangle rectangle = (ControlRectangle) gsoOutput.gso();
        painter.backgroundPainter().paint(((ShapeComponentNormal) (rectangle.getShapeComponent())).getFillInfo(), gsoOutput.areaWithoutOuterMargin());

        boolean drawLine = setBorderLine(((ShapeComponentNormal) (rectangle.getShapeComponent())).getLineInfo());

        if (drawLine) {
            painter.rectangle(gsoOutput.areaWithoutOuterMargin(), false);
        }

        gsoOutput.adjustTextBoxAreaAndVerticalAlignment();
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
        painter.image(gsoOutput.areaWithoutOuterMargin(), input.image(picture.getShapeComponentPicture().getPictureInfo().getBinItemID()));

        boolean drawLine = setBorderLine(picture.getShapeComponentPicture().getBorderProperty().getLineType(),
                picture.getShapeComponentPicture().getBorderThickness(),
                picture.getShapeComponentPicture().getBorderColor());

        if (drawLine) {
            painter.rectangle(gsoOutput.areaWithoutOuterMargin(), false);
        }
    }

    public void ole(GsoOutput gsoOutput) {
    }

    public void container(GsoOutput gsoOutput) {
    }

    public void objectLinkLine(GsoOutput gsoOutput) {
    }
}
