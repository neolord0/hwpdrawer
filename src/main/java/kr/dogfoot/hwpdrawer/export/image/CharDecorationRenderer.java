package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.docinfo.charshape.UnderLineSort;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * 밑줄과 취소선 렌더링을 전담한다.
 *
 * <p>Y 위치 공식 — HWP 비율(ascent = baseSize × 17/20, descent = baseSize × 3/20):</p>
 * <ul>
 *   <li>Bottom underline: {@code baseLine + bs × 3/20}</li>
 *   <li>Top underline: {@code baseLine - bs × 17/20}</li>
 *   <li>Middle underline / Strike: {@code baseLine - bs × 7/20} (시각 중앙)</li>
 * </ul>
 */
final class CharDecorationRenderer {
    // HWP 비율: ascent = 17/20 baseSize, descent = 3/20 baseSize, 중앙 = 7/20 baseSize.
    private static final int ASCENT_NUM = 17;
    private static final int DESCENT_NUM = 3;
    private static final int MIDDLE_NUM = 7;
    private static final int DENOM = 20;

    private final Graphics2D g2d;

    CharDecorationRenderer(Graphics2D g2d) {
        this.g2d = g2d;
    }

    /**
     * 밑줄을 그린다. 가운데 밑줄과 취소선이 동시 존재하면 취소선이 우선하므로 건너뛴다.
     */
    void drawUnderLine(CharShape charShape, CharInfo charInfo, double lineLeft, long baseLine) {
        UnderLineSort sort = charShape.getProperty().getUnderLineSort();
        if (sort == UnderLineSort.None) return;
        if (!drawableCharLineShape(charShape.getProperty().getUnderLineShape())) return;
        if (sort == UnderLineSort.Middle && charShape.getProperty().isStrikeLine()) return;

        long y = underLineY(sort, Math.round(charInfo.baseSize()), baseLine);
        drawHorizontalLine(
                charInfo, lineLeft, y,
                colorOf(charShape.getUnderLineColor()),
                StrokeFactory.forCharBorder(charShape.getProperty().getUnderLineShape()));
    }

    /**
     * 취소선을 그린다. 글자의 시각적 수직 중앙에 위치 (charOffset과 무관).
     */
    void drawStrikeLine(CharShape charShape, CharInfo charInfo, double lineLeft, long baseLine) {
        if (!charShape.getProperty().isStrikeLine()) return;
        if (!drawableCharLineShape(charShape.getProperty().getStrikeLineShape())) return;

        long y = Math.round(baseLine - charInfo.baseSize() * MIDDLE_NUM / DENOM);
        drawHorizontalLine(
                charInfo, lineLeft, y,
                colorOf(charShape.getStrikeLineColor()),
                StrokeFactory.forCharBorder(charShape.getProperty().getStrikeLineShape()));
    }

    /**
     * 글자 장식(밑줄/취소선)으로 그릴 수 있는 선 모양인지 판정한다.
     * 3D 계열(Thick3D/Solid3D 등)은 표 테두리 전용 값으로, 변환된 옛 문서의 잔재 비트에서
     * 나타난다 — 한글은 이런 장식을 그리지 않는다 (실문서 R02 표 머리글: 취소선모양=Solid3D 무시).
     */
    private static boolean drawableCharLineShape(BorderType2 shape) {
        switch (shape) {
            case Thick3D:
            case Thick3DReverseLighting:
            case Solid3D:
            case Solid3DReverseLighting:
                return false;
            default:
                return true;
        }
    }

    private static long underLineY(UnderLineSort sort, long bs, long baseLine) {
        switch (sort) {
            case Bottom: return baseLine + bs * DESCENT_NUM / DENOM;
            case Top:    return baseLine - bs * ASCENT_NUM / DENOM;
            case Middle: return baseLine - bs * MIDDLE_NUM / DENOM;
            default:     return baseLine + bs * DESCENT_NUM / DENOM;
        }
    }

    private void drawHorizontalLine(CharInfo charInfo, double lineLeft, long y,
                                     Color color, Stroke stroke) {
        Color savedColor = g2d.getColor();
        Stroke savedStroke = g2d.getStroke();

        try {
            g2d.setColor(color);
            g2d.setStroke(stroke);
            int x1 = (int) UnitConverter.toPixelXF(lineLeft + charInfo.x());
            int x2 = (int) UnitConverter.toPixelXF(lineLeft + charInfo.x() + charInfo.width());
            int py = UnitConverter.toPixelY(y);
            g2d.drawLine(x1, py, x2, py);
        } finally {
            g2d.setColor(savedColor);
            g2d.setStroke(savedStroke);
        }
    }

    private static Color colorOf(kr.dogfoot.hwplib.object.etc.Color4Byte color) {
        return new Color(color.getR(), color.getG(), color.getB());
    }
}
