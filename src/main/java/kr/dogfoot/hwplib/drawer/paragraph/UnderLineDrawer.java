package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort;

import java.io.UnsupportedEncodingException;

public class UnderLineDrawer {
    private HWPDrawer drawer;

    private long baseLine;
    private long maxCharHeight;

    private UnderLineSort lineSort;
    private BorderType2 lineShape;
    private long lineColor;
    private long startX;
    private CharShape drawingCharShape;

    public UnderLineDrawer(HWPDrawer drawer) {
        this.drawer = drawer;
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

    public void draw(CharInfo charInfo, boolean endLine) throws UnsupportedEncodingException {
        if (isStartLine(charInfo.charShape())) {
            lineSort = charInfo.charShape().getProperty().getUnderLineSort();
            lineShape = charInfo.charShape().getProperty().getUnderLineShape();
            lineColor = charInfo.charShape().getUnderLineColor().getValue();
            startX = charInfo.x();
            drawingCharShape = charInfo.charShape();
        } else if (isEndLine(charInfo.charShape())) {
            drawUnderLine(charInfo, false);

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
        if (endLine == true && startX != -1) {
            drawUnderLine(charInfo, endLine);
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

    private void drawUnderLine(CharInfo charInfo, boolean endLine) {
        long y = (drawingCharShape.getProperty().getUnderLineSort() == UnderLineSort.Top)
                ? baseLine - (maxCharHeight * 4 / 5)
                : baseLine + (maxCharHeight / 5);
        long endX = (endLine == true) ? (long) (charInfo.x() + charInfo.width()) : charInfo.x();

        drawer.painter().setLineStyle(drawingCharShape.getProperty().getUnderLineShape().toBorderType(),
                BorderThickness.MM0_15,
                drawingCharShape.getUnderLineColor());
        drawer.painter().line(startX, y, endX, y);
    }
}
