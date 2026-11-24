package kr.dogfoot.hwpdrawer.engine;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.FontManager;
import kr.dogfoot.hwpdrawer.util.ScriptStyle;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

/**
 * old_version의 측정 방식을 따른다.
 *
 * - calculating font: 폰트 size를 baseSize로 직접 사용 → FontMetrics 결과가 HWP 단위와 일치
 * - 공백 폭: baseSize / 2 × relativeSize% × ratio%
 * - 글자 폭: stringWidth(String.valueOf(ch)) × relativeSize% × ratio%
 * - 글자 높이: getHeight() × relativeSize% (ascent + descent + leading)
 * - descent: getDescent() × relativeSize%
 *
 * 따라서 픽셀 ↔ HWP 변환이 필요 없고 measurement가 바로 HWP 단위로 나온다.
 */
public class CharMetricsMeasurer {
    private final Graphics2D metricsGraphics;
    private final FontRenderContext frc;

    public CharMetricsMeasurer() {
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        metricsGraphics = tempImage.createGraphics();
        // 드로잉(ImageExporter)과 같은 텍스트 AA hint + 서브픽셀 메트릭을 적용해서
        // measurement와 drawing의 글자 metric이 일치하도록 한다.
        metricsGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        metricsGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        frc = metricsGraphics.getFontRenderContext();
    }

    public FontRenderContext fontRenderContext() {
        return frc;
    }

    /**
     * 한 글자의 치수를 HWP 단위로 측정한다.
     */
    /**
     * 보충문자(surrogate pair)의 치수를 측정한다. 코드포인트로 글리프 폴백을 하고,
     * 폭은 전각 한 칸(한글과 동일 취급)으로 둔다(네모친 숫자 등은 전각).
     */
    public CharMetrics measureCodePoint(String text, int codePoint, CharShape charShape) {
        CharMetrics m = new CharMetrics();
        Font calcFont = FontManager.instance().calculatingFont(charShape);
        calcFont = FontManager.instance().withGlyphFallback(calcFont, codePoint);
        metricsGraphics.setFont(calcFont);
        FontMetrics fm = metricsGraphics.getFontMetrics();

        short relativeSize = charShape.getRelativeSizes().getHangul();
        short ratio = charShape.getRatios().getHangul();
        // 보충문자 폭은 폰트가 정한다 — 전각인 것도 반각인 것도 있다.
        //
        // 예전에는 baseSize(전각 1em)로 고정했다. 네모친 숫자(U+F0289 등)는 실제로 전각이라
        // 맞았지만, 반각인 기호를 만나면 두 배로 벌어진다. 실문서 R20 제목 아래
        // 가로선은 U+F0827 50개를 이어 붙여 그린 것인데, 전각으로 두면 50자가 350px 대신
        // 711px 로 퍼져 선이 아니라 점선으로 보였다(잉크 조각 50개 vs 참조 1개).
        //
        // 함초롬돋움 실측(9pt=15px 기준): U+F0827 advance 7.28px(반각·글리프가 칸을 꽉 채움),
        // U+F0289 advance 14.55px(전각). 폰트 advance 를 쓰면 둘 다 맞는다.
        // 글리프가 없어 advance 가 0 인 폰트로 폴백된 경우에만 종전대로 전각으로 둔다.
        double advance = calcFont.getStringBounds(text, frc).getWidth();
        double rawWidth = advance > 0 ? advance : charShape.getBaseSize();
        m.width = rawWidth * relativeSize / 100.0 * ratio / 100.0
                * ScriptStyle.sizeScale(charShape);
        m.height = charShape.getBaseSize();
        m.descent = 0;
        m.baseSize = fm.getAscent() * relativeSize / 100.0;
        return m;
    }

    public CharMetrics measure(char ch, CharShape charShape) {
        CharMetrics m = new CharMetrics();

        Font calcFont = FontManager.instance().calculatingFont(charShape);
        // 폰트에 글리프가 없는 글자는 렌더링과 동일한 대체 폰트로 측정한다.
        calcFont = FontManager.instance().withGlyphFallback(calcFont, ch);
        metricsGraphics.setFont(calcFont);
        FontMetrics fm = metricsGraphics.getFontMetrics();

        // 공백은 한글 기준 (old_version과 동일) — Latin 분기로 가면 폭이 틀어짐
        boolean isSpace = (ch == ' ' || ch == '\t');
        short relativeSize = isSpace
                ? charShape.getRelativeSizes().getHangul()
                : relativeSize(charShape, ch);
        short ratio = isSpace
                ? charShape.getRatios().getHangul()
                : ratio(charShape, ch);

        // 폭 계산
        double rawWidth;
        if (isSpace) {
            rawWidth = charShape.getBaseSize() / 2.0;
        } else if (HWPFileReader.isHangul(ch)) {
            // 맑은고딕처럼 natural advance가 1em보다 좁은 폰트(heightScale > 1.0)는
            // 1em 슬롯으로 확장해 한컴과 동일하게 자간이 벌어지게 한다.
            double scale = FontManager.instance().hangulHeightScale(charShape);
            if (scale > 1.0) {
                rawWidth = charShape.getBaseSize();
            } else {
                // Java synthetic bold(별도 Bold TTF 없을 때)는 advance를 Regular보다 부풀린다.
                // HWP bold 한글의 advance = Regular 한글 advance (굵기만 다르고 폭은 동일).
                // bold/italic 시에는 plain 스타일 폰트로 advance를 측정해 합성 확장을 제거한다.
                boolean hasSyntheticStyle = charShape.getProperty().isBold()
                        || charShape.getProperty().isItalic();
                Font widthFont = hasSyntheticStyle ? calcFont.deriveFont(Font.PLAIN) : calcFont;
                rawWidth = widthFont.getStringBounds(String.valueOf(ch), frc).getWidth();
            }
        } else {
            // 영문/숫자: HWP는 정수 격자 기반 advance를 사용하므로 반올림.
            // 한글 bold(위 분기)와 동일하게 Java 합성 bold의 advance 부풀림을 제거한다.
            // 한글은 bold라도 Regular advance를 쓰므로, 이게 없으면 굵은 대시("-")·숫자 등이
            // 넓게 측정돼 탭 채움 대시 줄이 길어지고 뒤 라벨이 오른쪽으로 밀린다
            // (실문서 R12 page7 '제1장 ----18포인트, 진하게' 예시). 실제 Bold TTF가 로드된 경우
            // deriveFont(PLAIN)은 그 폰트의 자연 advance를 주므로 합성 부풀림만 제거된다.
            boolean hasSyntheticStyle = charShape.getProperty().isBold()
                    || charShape.getProperty().isItalic();
            Font widthFont = hasSyntheticStyle ? calcFont.deriveFont(Font.PLAIN) : calcFont;
            rawWidth = Math.round(widthFont.getStringBounds(String.valueOf(ch), frc).getWidth());
        }
        // 첨자는 글자 폭만 줄인다 (줄 높이는 아래에서 baseSize 그대로 유지).
        m.width = rawWidth * relativeSize / 100.0 * ratio / 100.0
                * ScriptStyle.sizeScale(charShape);

        // 글자 높이 = baseSize (relativeSize 무시) — 14/16 통과를 만든 설정.
        m.height = charShape.getBaseSize();
        m.descent = 0;
        m.baseSize = fm.getAscent() * relativeSize / 100.0;

        return m;
    }

    /**
     * CharShape의 ascent (라인 top → baseline 거리)를 반환한다.
     *
     * 한글 프로그램의 LineSeg 정답 데이터를 보면 ascent = baseSize × 17/20 (= 0.85)으로
     * 폰트와 무관한 고정 비율이다. 폰트마다 다른 FontMetrics.getAscent()를 쓰면 라인마다
     * baseline이 어긋난다. relativeSize 또한 무시.
     */
    public double ascent(CharShape charShape) {
        return charShape.getBaseSize() * 17.0 / 20.0;
    }

    /**
     * 자간을 가져온다 (HWP 단위).
     * 자간은 글자 폭의 퍼센트로 적용한다.
     */
    public double charSpacing(CharShape charShape, char ch, double charWidth) {
        // 공백은 한글 기준 (old_version과 동일)
        byte spacing;
        if (ch == ' ' || ch == '\t' || HWPFileReader.isHangul(ch)) {
            spacing = charShape.getCharSpaces().getHangul();
        } else {
            spacing = charShape.getCharSpaces().getLatin();
        }
        return charWidth * spacing / 100.0;
    }

    private short relativeSize(CharShape charShape, char ch) {
        if (HWPFileReader.isHangul(ch)) {
            return charShape.getRelativeSizes().getHangul();
        }
        return charShape.getRelativeSizes().getLatin();
    }

    private short ratio(CharShape charShape, char ch) {
        if (HWPFileReader.isHangul(ch)) {
            return charShape.getRatios().getHangul();
        }
        return charShape.getRatios().getLatin();
    }

    public void dispose() {
        if (metricsGraphics != null) {
            metricsGraphics.dispose();
        }
    }
}
