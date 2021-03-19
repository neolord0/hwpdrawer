package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort;

import java.io.UnsupportedEncodingException;

public class UnderLineDrawer {
    private DrawingInfo info;

    private long baseLine;
    private long maxCharHeight;

    private UnderLineSort lineSort;
    private BorderType2 lineShape;
    private long lineColor;
    private long startX;
    private CharShape drawingCharShape;

    public UnderLineDrawer(DrawingInfo info) {
        this.info = info;
    }

    public void initialize(long baseLine, long maxCharHeight) {
        this.baseLine = baseLine;
        this.maxCharHeight = maxCharHeight;

        lineSort = UnderLineSort.None;
        lineShape = BorderType2.Solid;
        lineColor = -1;
        startX = -1;
        drawingCharShape = null;
    }

    public void draw(TextLineDrawer.CharDrawInfo cdi,  boolean endLine) throws UnsupportedEncodingException {
        if (isStartLine(cdi.charShape)) {
            lineSort = cdi.charShape.getProperty().getUnderLineSort();
            lineShape = cdi.charShape.getProperty().getUnderLineShape();
            lineColor = cdi.charShape.getUnderLineColor().getValue();
            startX = cdi.x;
            drawingCharShape = cdi.charShape;
        } else if (isEndLine(cdi.charShape)) {
            drawUnderLine(cdi, false);

            lineSort = cdi.charShape.getProperty().getUnderLineSort();
            if (lineSort == UnderLineSort.None) {
                lineShape = BorderType2.Solid;
                lineColor = -1;
                startX = -1;
                drawingCharShape = null;
            } else {
                lineShape = cdi.charShape.getProperty().getUnderLineShape();
                lineColor = cdi.charShape.getUnderLineColor().getValue();
                startX = cdi.x;
                drawingCharShape = cdi.charShape;
            }
        }
        if (endLine == true && startX != -1) {
            drawUnderLine(cdi, endLine);
        }
    }

    private boolean isStartLine(CharShape charShape) {
        if (changeLineStyle(charShape) && startX == -1) {
            return true;
        }
        return false;
    }

    private boolean changeLineStyle(CharShape charShape) {
        return charShape.getProperty().getUnderLineSort() != lineSort
                || (charShape.getProperty().getUnderLineSort() != UnderLineSort.None
                        && (charShape.getProperty().getUnderLineShape() != lineShape
                                || charShape.getUnderLineColor().getValue() != lineColor));
    }

    private boolean isEndLine(CharShape charShape) {
        if (changeLineStyle(charShape) && startX != -1) {
            return true;
        }
        return false;
    }

    private void drawUnderLine(TextLineDrawer.CharDrawInfo cdi, boolean endLine) {
        long y = (drawingCharShape.getProperty().getUnderLineSort() == UnderLineSort.Top)
                ? baseLine - (maxCharHeight * 4 / 5)
                : baseLine + (maxCharHeight / 5);
        long endX = (endLine == true) ? (long) (cdi.x + cdi.width) : cdi.x;

        info.painter().setLineStyle(drawingCharShape.getProperty().getUnderLineShape().toBorderType(),
                BorderThickness.MM0_15,
                drawingCharShape.getUnderLineColor());
        info.painter().line(startX, y, endX, y);
    }
}
