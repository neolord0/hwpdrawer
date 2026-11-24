package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.FontManager;
import kr.dogfoot.hwpdrawer.util.ScriptStyle;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Graphics2D를 사용하여 Document의 Content를 렌더링한다.
 *
 * <p>Clean Code: 역할별로 작은 도우미 클래스로 위임.</p>
 * <ul>
 *   <li>{@link ParaBorderRenderer}: 문단 테두리</li>
 *   <li>{@link CharDecorationRenderer}: 밑줄/취소선</li>
 *   <li>{@link StrokeFactory}: dash/dot 패턴 중앙 관리</li>
 *   <li>{@link CharShapeAccess}: CharShape 한글/영문 분기 접근자</li>
 * </ul>
 */
public class ImageRenderer {
    private final Graphics2D g2d;
    private final ParaBorderRenderer paraBorderRenderer;
    private final CharDecorationRenderer decorationRenderer;

    public ImageRenderer(Graphics2D g2d, HWPFileReader reader) {
        this.g2d = g2d;
        this.paraBorderRenderer = new ParaBorderRenderer(g2d, reader);
        this.decorationRenderer = new CharDecorationRenderer(g2d);
    }

    /** Content 전체를 렌더링한다. */
    public void renderContent(Content content) {
        for (TextRow row : content.rows()) {
            for (TextColumn column : row.columns()) {
                renderColumn(column);
            }
        }
    }

    // ───── 렌더링 순서 (배경 → 전경) ─────

    private void renderColumn(TextColumn column) {
        // 문단 테두리는 텍스트 뒤에 배경으로 먼저 그린다.
        paraBorderRenderer.drawAll(column);
        for (TextLine line : column.textLines()) {
            renderLine(line);
        }
    }

    private void renderLine(TextLine line) {
        for (TextPart part : line.parts()) {
            renderPart(part, line);
        }
    }

    // ───── 한 TextPart 렌더링 ─────

    private void renderPart(TextPart part, TextLine line) {
        double lineLeft = part.startX();
        long baseLine = line.area().top() + line.maxCharBaseSize();
        CharShape prevCharShape = null;
        Font prevFont = null;

        // 글자 음영(형광펜)은 글자보다 먼저, part 단위로 한 번에 칠한다.
        // 글자 루프 안에서 글자마다 칠하면 앞 글자의 안티에일리어싱된 오른쪽 가장자리를
        // 다음 글자의 음영 사각형이 덮어버린다.
        drawCharShades(part, line, lineLeft, baseLine);

        for (CharInfo charInfo : part.charInfos()) {
            if (charInfo.dotLeader()) {
                drawDotLeader(charInfo, lineLeft, baseLine);
                continue;
            }
            if (isInvisible(charInfo)) continue;

            CharShape charShape = charInfo.charShape();
            // 글리프 폴백 때문에 같은 CharShape 안에서도 글자마다 폰트가 달라질 수 있다.
            Font font = charInfo.text() != null
                    ? fontForCodePoint(charShape, charInfo.codePoint(), charInfo.script())
                    : fontFor(charShape, charInfo.ch(), charInfo.script());
            if (!font.equals(prevFont)) {
                g2d.setFont(font);
                prevFont = font;
            }
            if (charShape != prevCharShape) {
                g2d.setColor(charColorOf(charShape));
                prevCharShape = charShape;
            }

            drawChar(charInfo, charShape, lineLeft, baseLine);
            decorationRenderer.drawUnderLine(charShape, charInfo, lineLeft, baseLine);
            decorationRenderer.drawStrikeLine(charShape, charInfo, lineLeft, baseLine);
        }
    }

    private static boolean isInvisible(CharInfo charInfo) {
        return charInfo.ch() == 0 || (charInfo.isSpace() && charInfo.ch() == '\t');
    }

    // ───── 글자 음영(형광펜) ─────

    /** 음영 사각형의 baseline 아래쪽 깊이 비율(3/20) — 밑줄·취소선이 쓰는 값과 같다. */
    private static final int SHADE_DESCENT_NUM = 3;
    private static final int SHADE_DESCENT_DEN = 20;

    /**
     * 글자 음영(형광펜) 배경을 칠한다. 같은 음영색이 이어지는 글자들을 하나의 구간으로 묶어
     * 한 번에 칠해, 글자 사이에 이음매가 생기지 않게 한다.
     */
    private void drawCharShades(TextPart part, TextLine line, double lineLeft, long baseLine) {
        List<CharInfo> chars = part.charInfos();
        Color saved = g2d.getColor();
        try {
            int i = 0;
            while (i < chars.size()) {
                Color shade = shadeColorOf(chars.get(i).charShape());
                if (shade == null) { i++; continue; }

                int start = i;
                while (i < chars.size() && shade.equals(shadeColorOf(chars.get(i).charShape()))) i++;
                fillShade(chars, start, i - 1, shade, line, lineLeft, baseLine);
            }
        } finally {
            g2d.setColor(saved);
        }
    }

    /** chars[from..to] 구간을 음영색으로 칠한다. */
    private void fillShade(List<CharInfo> chars, int from, int to, Color shade,
                           TextLine line, double lineLeft, long baseLine) {
        CharInfo first = chars.get(from);
        CharInfo last = chars.get(to);
        int px0 = Math.round(UnitConverter.toPixelXF(lineLeft + first.x()));
        int px1 = Math.round(UnitConverter.toPixelXF(lineLeft + last.x() + last.width()));
        if (px1 < px0) return;

        // 세로 범위는 밑줄/취소선과 같은 기준을 쓴다 — 위는 라인의 고정 ascent(17/20),
        // 아래는 baseline + 3/20. 폰트 실제 ascent를 쓰면 한글보다 한 행씩 커진다.
        double bs = first.charShape() != null ? first.charShape().getBaseSize() : first.baseSize();
        int py0 = Math.round(UnitConverter.toPixelYF(baseLine - line.maxCharBaseSize()));
        int py1 = Math.round(UnitConverter.toPixelYF(
                baseLine + bs * SHADE_DESCENT_NUM / (double) SHADE_DESCENT_DEN));
        if (py1 < py0) return;

        g2d.setColor(shade);
        g2d.fillRect(px0, py0, px1 - px0 + 1, py1 - py0 + 1);
    }

    /** CharShape의 음영색. 음영 없음('색 없음' 0xFFFFFFFF)이면 null. */
    private static Color shadeColorOf(CharShape charShape) {
        if (charShape == null) return null;
        return HwpColors.paintOf(charShape.getShadeColor());
    }

    /**
     * 탭 채우기를 그린다. charInfo.tabFillSort()에 따라:
     * - Dash: 점선 (-------)
     * - Solid: 실선 (____________)
     * - null: 점 (…….)
     * 색은 항목 글자색.
     */
    private void drawDotLeader(CharInfo charInfo, double lineLeft, long baseLine) {
        double x1 = lineLeft + charInfo.x();
        double x2 = x1 + charInfo.width();
        if (x2 - x1 < 1) return;

        float px1 = UnitConverter.toPixelXF(x1);
        float px2 = UnitConverter.toPixelXF(x2);
        float py = UnitConverter.toPixelYF(baseLine - charInfo.height() * 0.35);

        Color saved = g2d.getColor();
        if (charInfo.charShape() != null) g2d.setColor(charColorOf(charInfo.charShape()));

        kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType fillSort = charInfo.tabFillSort();
        if (fillSort == kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType.Dash) {
            drawDashLeader(px1, px2, py, charInfo.height());
        } else if (fillSort == kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType.Solid) {
            drawSolidLeader(px1, px2, py, charInfo.height());
        } else {
            drawDotLeaderLine(px1, px2, py, charInfo.height());
        }

        g2d.setColor(saved);
    }

    private void drawDotLeaderLine(float px1, float px2, float py, double height) {
        Object savedAA = g2d.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            float step = Math.max(2f, UnitConverter.toPixelXF(height * 0.257));
            float d = 2.7f;
            for (float x = px1 + step / 2f; x < px2 - d / 2f; x += step) {
                g2d.fill(new java.awt.geom.Ellipse2D.Float(x - d / 2f, py - d / 2f, d, d));
            }
        } finally {
            if (savedAA != null) g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, savedAA);
        }
    }

    private void drawDashLeader(float px1, float px2, float py, double height) {
        float dashLength = Math.max(2f, UnitConverter.toPixelXF(height * 0.3));
        float gapLength = Math.max(1f, dashLength / 2f);
        float lineWidth = Math.max(0.5f, UnitConverter.toPixelXF(height * 0.05));

        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{dashLength, gapLength}, 0f);
        g2d.setStroke(stroke);
        g2d.drawLine((int)px1, (int)py, (int)px2, (int)py);
    }

    private void drawSolidLeader(float px1, float px2, float py, double height) {
        float lineWidth = Math.max(0.5f, UnitConverter.toPixelXF(height * 0.05));
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine((int)px1, (int)py, (int)px2, (int)py);
    }

    private static Font fontFor(CharShape charShape, char ch, ScriptStyle.Script script) {
        int baseSize = charShape.getBaseSize();
        short relativeSize = CharShapeAccess.relativeSize(charShape, ch);
        double effectiveSize = baseSize * relativeSize / 100.0
                * ScriptStyle.sizeScale(charShape, script);
        // 맑은고딕처럼 좁은 한글 폰트는 폰트 크기를 고정 비율만큼 키워 다른 폰트와 높이/폭 맞춤.
        if (kr.dogfoot.hwpdrawer.reader.HWPFileReader.isHangul(ch)) {
            effectiveSize *= FontManager.instance().hangulHeightScale(charShape);
        }
        float pixelSize = Math.max(1, UnitConverter.toPixelFontSize((int) effectiveSize));
        Font font = FontManager.instance().getFont(charShape, pixelSize);
        return FontManager.instance().withGapsKept(FontManager.instance().withGlyphFallback(font, ch), ch);
    }

    /** 보충문자(코드포인트) 렌더용 폰트. 코드포인트로 글리프 폴백(네모친 숫자 → 함초롬). */
    private static Font fontForCodePoint(CharShape charShape, int codePoint, ScriptStyle.Script script) {
        int baseSize = charShape.getBaseSize();
        short relativeSize = charShape.getRelativeSizes().getHangul();
        double effectiveSize = baseSize * relativeSize / 100.0
                * ScriptStyle.sizeScale(charShape, script);
        float pixelSize = Math.max(1, UnitConverter.toPixelFontSize((int) effectiveSize));
        Font font = FontManager.instance().getFont(charShape, pixelSize);
        return FontManager.instance().withGapsKept(FontManager.instance().withGlyphFallback(font, codePoint), codePoint);
    }

    private static Color charColorOf(CharShape charShape) {
        return new Color(
                charShape.getCharColor().getR(),
                charShape.getCharColor().getG(),
                charShape.getCharColor().getB());
    }

    /**
     * 한 글자를 Graphics2D에 그린다. 장평(ratio)이 100이 아니면 transform으로 수평 스케일.
     */
    private void drawChar(CharInfo charInfo, CharShape charShape, double lineLeft, long baseLine) {
        double charX = lineLeft + charInfo.x();
        long charY = baseLine + CharShapeAccess.charOffsetY(charShape, charInfo.ch())
                + Math.round(ScriptStyle.baselineShift(charShape, charInfo.script()));

        float px = UnitConverter.toPixelXF(charX);
        float py = UnitConverter.toPixelYF(charY);
        String text = charInfo.text() != null ? charInfo.text() : String.valueOf(charInfo.ch());

        short ratio = CharShapeAccess.ratio(charShape, charInfo.ch());
        if (ratio == 100) {
            g2d.drawString(text, px, py);
        } else {
            drawCharWithHorizontalScale(text, px, py, ratio);
        }
    }

    private void drawCharWithHorizontalScale(String text, float px, float py, short ratio) {
        Font savedFont = g2d.getFont();
        AffineTransform scaleAT = AffineTransform.getScaleInstance(ratio / 100.0, 1.0);
        g2d.setFont(savedFont.deriveFont(scaleAT));
        try {
            g2d.drawString(text, px, py);
        } finally {
            g2d.setFont(savedFont);
        }
    }
}
