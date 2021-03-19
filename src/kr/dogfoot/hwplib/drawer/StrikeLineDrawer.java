package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;

public class StrikeLineDrawer {
    private DrawingInfo info;

    private long baseLine;

    private boolean strike;
    private BorderType2 lineShape;
    private long lineColor;
    private long charHeight;

    private long startX;
    private CharShape drawingCharShape;

    public StrikeLineDrawer(DrawingInfo info) {
        this.info = info;
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

    public void draw(TextLineDrawer.CharDrawInfo cdi, boolean endLine) {
        if (isStartLine(cdi.charShape)) {
            strike = cdi.charShape.getProperty().isStrikeLine();
            lineShape = cdi.charShape.getProperty().getStrikeLineShape();
            lineColor = cdi.charShape.getStrikeLineColor().getValue();
            charHeight = cdi.charShape.getBaseSize();
            startX = cdi.x;
            drawingCharShape = cdi.charShape;
        } else if (isEndLine(cdi.charShape)) {
            drawStrikeLine(cdi, false);

            strike = cdi.charShape.getProperty().isStrikeLine();
            if (strike == false) {
                lineShape = BorderType2.Solid;;
                lineColor = -1;
                charHeight = -1;
                startX = -1;
                drawingCharShape = null;
            } else {
                lineShape = cdi.charShape.getProperty().getStrikeLineShape();;
                lineColor = cdi.charShape.getStrikeLineColor().getValue();
                charHeight = cdi.charShape.getBaseSize();
                startX = cdi.x;
                drawingCharShape = cdi.charShape;
            }
        }
        if (endLine == true && startX != -1) {
            drawStrikeLine(cdi, endLine);
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
                || (charShape.getProperty().isStrikeLine() == true &&
                        (charShape.getProperty().getStrikeLineShape() != lineShape
                                || charShape.getStrikeLineColor().getValue() != lineColor));
    }

    private boolean changeCharHeight(CharShape charShape) {
        return charShape.getProperty().isStrikeLine() == true
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

    private void drawStrikeLine(TextLineDrawer.CharDrawInfo cdi, boolean endLine) {
        long y = baseLine - (charHeight * 2 / 5);
        long endX = (endLine == true) ?  (long) (cdi.x + cdi.width) : cdi.x;

        info.painter().setLineStyle(drawingCharShape.getProperty().getStrikeLineShape().toBorderType(),
                BorderThickness.MM0_15,
                drawingCharShape.getStrikeLineColor());
        info.painter().line(startX, y, endX, y);
    }
}
