package kr.dogfoot.hwplib.drawer.painter.image.text;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.painter.image.PainterForImage;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;


public class StrikeLinePainter {
    private final PainterForImage painter;
    private long baseLine;

    private boolean strike;
    private BorderType2 lineShape;
    private long lineColor;
    private long charHeight;

    private long startX;
    private CharShape drawingCharShape;

    public StrikeLinePainter(PainterForImage painter) {
        this.painter = painter;
    }

    public void initialize(long baseLine) {
        this.baseLine = baseLine;

        strike = false;
        lineShape = BorderType2.Solid;
        lineColor = -1;
        charHeight = -1;
        startX = -1;
        drawingCharShape = null;
    }

    public void paint(CharInfo charInfo, boolean endLine) {
        if (isStartLine(charInfo.charShape())) {
            strike = charInfo.charShape().getProperty().isStrikeLine();
            lineShape = charInfo.charShape().getProperty().getStrikeLineShape();
            lineColor = charInfo.charShape().getStrikeLineColor().getValue();
            charHeight = charInfo.charShape().getBaseSize();
            startX = charInfo.x();
            drawingCharShape = charInfo.charShape();
        } else if (isEndLine(charInfo.charShape())) {
            paintStrikeLine(charInfo, false);

            strike = charInfo.charShape().getProperty().isStrikeLine();
            if (!strike) {
                lineShape = BorderType2.Solid;
                lineColor = -1;
                charHeight = -1;
                startX = -1;
                drawingCharShape = null;
            } else {
                lineShape = charInfo.charShape().getProperty().getStrikeLineShape();
                lineColor = charInfo.charShape().getStrikeLineColor().getValue();
                charHeight = charInfo.charShape().getBaseSize();
                startX = charInfo.x();
                drawingCharShape = charInfo.charShape();
            }
        }
        if (endLine && startX != -1) {
            paintStrikeLine(charInfo, true);
        }
    }

    private boolean isStartLine(CharShape charShape) {
        if ((changeLineStyle(charShape)
                || changeCharHeight(charShape))
                && startX == -1) {
            return true;
        }
        return false;
    }

    private boolean changeLineStyle(CharShape charShape) {
        return charShape.getProperty().isStrikeLine() != strike
                || (charShape.getProperty().isStrikeLine() &&
                (charShape.getProperty().getStrikeLineShape() != lineShape
                        || charShape.getStrikeLineColor().getValue() != lineColor));
    }

    private boolean changeCharHeight(CharShape charShape) {
        return charShape.getProperty().isStrikeLine()
                && charShape.getBaseSize() != charHeight;
    }

    private boolean isEndLine(CharShape charShape) {
        if ((changeLineStyle(charShape)
                || changeCharHeight(charShape))
                && startX != -1) {
            return true;
        }
        return false;
    }

    private void paintStrikeLine(CharInfo charInfo, boolean endLine) {
        long y = baseLine - (charHeight * 2 / 5);
        long endX = (endLine) ? (long) (charInfo.x() + charInfo.width()) : charInfo.x();

        painter.setLineStyle(drawingCharShape.getProperty().getStrikeLineShape().toBorderType(),
                BorderThickness.MM0_15,
                drawingCharShape.getStrikeLineColor());
        painter.line(startX, y, endX, y);
    }
}
