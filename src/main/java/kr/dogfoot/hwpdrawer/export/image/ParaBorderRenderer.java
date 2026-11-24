package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * 문단 테두리(paragraph border) 렌더러.
 *
 * <p>같은 paraIndex + borderFillId를 가진 연속 라인들을 하나의 그룹으로 묶고, 그룹의 상단/하단/좌/우를
 * 감싸는 사각형을 그린다. 테두리 박스는 문단의 {@code topParaSpace}, {@code bottomParaSpace},
 * 마지막 라인의 {@code lineGap}을 포함해서 그려진다.</p>
 */
final class ParaBorderRenderer {
    private final Graphics2D g2d;
    private final HWPFileReader reader;

    ParaBorderRenderer(Graphics2D g2d, HWPFileReader reader) {
        this.g2d = g2d;
        this.reader = reader;
    }

    /** 컬럼의 모든 문단 테두리를 그린다. */
    void drawAll(TextColumn column) {
        List<TextLine> lines = column.textLines();
        int i = 0;
        while (i < lines.size()) {
            TextLine first = lines.get(i);
            int borderId = first.borderFillId();
            if (borderId <= 0) { i++; continue; }

            int last = findLastLineOfGroup(lines, i, first.paraIndex(), borderId);
            drawOneParaBorder(first, lines.get(last), borderId);
            i = last + 1;
        }
    }

    private static int findLastLineOfGroup(List<TextLine> lines, int start, int paraIndex, int borderId) {
        int last = start;
        while (last + 1 < lines.size()
                && lines.get(last + 1).paraIndex() == paraIndex
                && lines.get(last + 1).borderFillId() == borderId) {
            last++;
        }
        return last;
    }

    private void drawOneParaBorder(TextLine first, TextLine last, int borderFillId) {
        BorderFill bf = borderFillById(borderFillId);
        if (bf == null) return;

        BorderRect rect = computeBorderRect(first, last);
        if (rect == null) return;

        drawRect(bf, rect);
    }

    private BorderFill borderFillById(int borderFillId) {
        try {
            return reader.hwpFile().getDocInfo().getBorderFillList().get(borderFillId - 1);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 테두리 사각형의 픽셀 좌표를 계산한다.
     *
     * <p>top = first.area.top - topParaSpace (문단 위 여백 포함)<br>
     * bottom = last.area.bottom + bottomParaSpace<br>
     * — lineGap은 문단 내 라인 사이의 간격이므로 마지막 라인 이후에는 더하지 않는다.
     *   이 값을 더하면 다음 문단의 border.top과 어긋나 두 줄의 가로선이 된다.</p>
     */
    private static BorderRect computeBorderRect(TextLine first, TextLine last) {
        long left = first.paraLeft();
        long right = first.paraRight();
        long top = first.area().top() - first.paraTopSpace();
        long bottom = last.area().bottom() + last.paraBottomSpace();
        if (right <= left || bottom <= top) return null;

        return new BorderRect(
                UnitConverter.toPixelX(left),
                UnitConverter.toPixelY(top),
                UnitConverter.toPixelX(right),
                UnitConverter.toPixelY(bottom));
    }

    private void drawRect(BorderFill bf, BorderRect rect) {
        Color savedColor = g2d.getColor();
        Stroke savedStroke = g2d.getStroke();
        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            drawSide(bf.getTopBorder(),    rect.xL, rect.yT, rect.xR, rect.yT);
            drawSide(bf.getBottomBorder(), rect.xL, rect.yB, rect.xR, rect.yB);
            drawSide(bf.getLeftBorder(),   rect.xL, rect.yT, rect.xL, rect.yB);
            drawSide(bf.getRightBorder(),  rect.xR, rect.yT, rect.xR, rect.yB);
        } finally {
            g2d.setColor(savedColor);
            g2d.setStroke(savedStroke);
            if (savedAA != null) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
            }
        }
    }

    private void drawSide(EachBorder border, int x1, int y1, int x2, int y2) {
        if (border == null) return;
        BorderType type = border.getType();
        if (type == null || type == BorderType.None) return;

        g2d.setColor(colorOf(border));
        g2d.setStroke(StrokeFactory.forParaBorder(type));
        g2d.drawLine(x1, y1, x2, y2);
    }

    private static Color colorOf(EachBorder border) {
        if (border.getColor() == null) return Color.BLACK;
        return new Color(
                border.getColor().getR(),
                border.getColor().getG(),
                border.getColor().getB());
    }

    /** 픽셀 좌표 사각형. */
    private static final class BorderRect {
        final int xL, yT, xR, yB;
        BorderRect(int xL, int yT, int xR, int yB) {
            this.xL = xL;
            this.yT = yT;
            this.xR = xR;
            this.yB = yB;
        }
    }
}
