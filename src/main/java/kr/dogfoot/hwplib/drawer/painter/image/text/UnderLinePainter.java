package kr.dogfoot.hwplib.drawer.painter.image.text;

import kr.dogfoot.hwplib.drawer.painter.image.PainterForImage;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort;

public class UnderLinePainter {
    private final PainterForImage painter;

    private long baseLine;
    private long maxCharHeight;

    private UnderLineSort lineSort;
    private BorderType2 lineShape;
    private long lineColor;
    private long startX;
    private CharShape drawingCharShape;

    public UnderLinePainter(PainterForImage painter) {
        this.painter = painter;
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

    public void paint(CharInfo charInfo, boolean endLine) {
        if (isStartLine(charInfo.charShape())) {
            lineSort = charInfo.charShape().getProperty().getUnderLineSort();
            lineShape = charInfo.charShape().getProperty().getUnderLineShape();
            lineColor = charInfo.charShape().getUnderLineColor().getValue();
            startX = charInfo.x();
            drawingCharShape = charInfo.charShape();
        } else if (isEndLine(charInfo.charShape())) {
            paintUnderLine(charInfo, false);

            lineSort = charInfo.charShape().getProperty().getUnderLineSort();
            if (lineSort == UnderLineSort.None) {
                lineShape = BorderType2.Solid;
                lineColor = -1;
                startX = -1;
                drawingCharShape = null;
            } else {
                lineShape = charInfo.charShape().getProperty().getUnderLineShape();
                lineColor = charInfo.charShape().getUnderLineColor().getValue();
                startX = charInfo.x();
                drawingCharShape = charInfo.charShape();
            }
        }
        if (endLine && startX != -1) {
            paintUnderLine(charInfo, true);
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

    private void paintUnderLine(CharInfo charInfo, boolean endLine) {
        long y = (drawingCharShape.getProperty().getUnderLineSort() == UnderLineSort.Top)
                ? baseLine - (maxCharHeight * 4 / 5)
                : baseLine + (maxCharHeight / 5);
        long endX = (endLine) ? (long) (charInfo.x() + charInfo.width()) : charInfo.x();

        painter.setLineStyle(drawingCharShape.getProperty().getUnderLineShape().toBorderType(),
                BorderThickness.MM0_15,
                drawingCharShape.getUnderLineColor());
        painter.line(startX, y, endX, y);
    }
}
