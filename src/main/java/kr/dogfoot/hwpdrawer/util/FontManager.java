package kr.dogfoot.hwpdrawer.util;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.FaceName;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * HWP 파일의 폰트 정보를 관리하고, Java Font 객체를 생성하는 클래스.
 */
public class FontManager {
    private static FontManager instance = new FontManager();

    public static FontManager instance() {
        return instance;
    }

    private HWPFile hwpFile;
    private Map<String, Font> fontCache = new HashMap<>();
    private Map<CharShape, Font> calculatingFontCache = new HashMap<>();
    private Map<String, Double> hangulHeightScaleCache = new HashMap<>();
    private SyntheticStyle syntheticStyle = new SyntheticStyle();
    private static final FontRenderContext SCALE_FRC =
            new FontRenderContext(null, true, true);
    private static final int SCALE_REF_EM = 1000;
    /** 기준 폰트("가") visual height — 나눔고딕/바탕류가 기준. */
    private static final double REF_HANGUL_VIS_HEIGHT = 922.0;
    /** 좁은 폰트(맑은고딕류)에 적용할 고정 확대 비율. */
    private static final double NARROW_FONT_SCALE = 1.0;

    private FontManager() {}

    public void hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
    }

    /**
     * CharShape에서 한글 폰트 이름을 가져온다.
     */
    public String getHangulFontName(CharShape charShape) {
        try {
            int fontId = charShape.getFaceNameIds().getHangul();
            FaceName faceName = hwpFile.getDocInfo().getHangulFaceNameList().get(fontId);
            return faceName.getName();
        } catch (Exception e) {
            return "맑은 고딕";
        }
    }

    /**
     * CharShape에서 영문 폰트 이름을 가져온다.
     */
    public String getLatinFontName(CharShape charShape) {
        try {
            int fontId = charShape.getFaceNameIds().getLatin();
            FaceName faceName = hwpFile.getDocInfo().getEnglishFaceNameList().get(fontId);
            return faceName.getName();
        } catch (Exception e) {
            return "맑은 고딕";
        }
    }

    /**
     * CharShape 정보로 Java Font 객체를 생성한다.
     * isHangul=true이면 한글 폰트, false이면 영문 폰트를 사용한다.
     */
    public Font getFont(CharShape charShape, boolean isHangul, float pixelSize) {
        String fontName = isHangul ? getHangulFontName(charShape) : getLatinFontName(charShape);
        int style = Font.PLAIN;
        if (charShape.getProperty().isBold()) style |= Font.BOLD;
        if (charShape.getProperty().isItalic()) style |= Font.ITALIC;
        // 굵은 디자인 글꼴(견명조 등)을 보통 굵기 별칭으로 그릴 때는 굵게를 더한다. 그리는 폰트에만 적용한다 —
        // 폭을 재는 calculatingFont는 CharShape 스타일 그대로라 배치(줄바꿈·글자 위치)는 바뀌지 않는다.
        style |= FontLoader.instance().substituteStyle(fontName);

        String cacheKey = fontName + "_" + style;
        Font baseFont = fontCache.get(cacheKey);
        if (baseFont == null) {
            // 스타일별 폰트 파일을 우선 로드 (Bold variant가 있으면 가짜 bold 대신 실제 bold 사용)
            baseFont = FontLoader.instance().loadFont(fontName, style);
            if (baseFont != null) {
                baseFont = baseFont.deriveFont(style, pixelSize);
                fontCache.put(cacheKey, baseFont);
            } else {
                baseFont = new Font(fontName, style, (int) pixelSize);
                fontCache.put(cacheKey, baseFont);
            }
        }
        return baseFont.deriveFont(style, pixelSize);
    }

    /** 하위 호환용 — 한글 폰트 사용 */
    public Font getFont(CharShape charShape, float pixelSize) {
        return getFont(charShape, true, pixelSize);
    }

    /** 폰트에 글리프가 없는 글자(예: 새굴림의 ☐ U+2610)를 위한 대체 폰트 후보. */
    private static final String[] GLYPH_FALLBACK_FONTS = {"함초롬돋움", "함초롬바탕"};

    /**
     * font가 ch를 그릴 수 없으면 글리프를 가진 대체 폰트로 바꾼다 (style·size 유지).
     * 측정(CharMetricsMeasurer)과 렌더링(ImageRenderer)이 같은 대체 폰트를 쓰게 해서
     * advance와 그려지는 글리프가 일치하도록 한다.
     */
    public Font withGlyphFallback(Font font, char ch) {
        if (font.canDisplay(ch)) return font;
        for (String name : GLYPH_FALLBACK_FONTS) {
            Font fallback = FontLoader.instance().loadFont(name, font.getStyle());
            if (fallback != null && fallback.canDisplay(ch)) {
                return fallback.deriveFont(font.getStyle(), font.getSize2D());
            }
        }
        return font;
    }

    /**
     * 틈이 모양의 뜻인 기호({@link GapSensitiveGlyphs} — ◌ 등)를 합성 굵게로 그리게 되면 굵게를 뺀다.
     * 합성 굵게는 틈을 메워 다른 기호(◌ → ○)로 만든다. 실제 굵은 글꼴 파일이면 틈이 디자인돼 있으므로 그대로 둔다.
     *
     * <p>그리는 폰트에만 쓴다 (ImageRenderer). 글자 폭은 굵게 여부와 상관없이 보통 스타일로 재므로 배치는 같다.</p>
     */
    public Font withGapsKept(Font font, int codePoint) {
        if (!font.isBold() || !GapSensitiveGlyphs.contains(codePoint)) return font;
        if (!syntheticStyle.isSynthesized(font)) return font;
        return font.deriveFont(font.getStyle() & ~Font.BOLD);
    }

    /** 코드포인트(보충문자 포함) 버전 글리프 폴백. 네모친 숫자 U+F02Bx 등은 함초롬 계열이 가짐. */
    public Font withGlyphFallback(Font font, int codePoint) {
        if (font.canDisplay(codePoint)) return font;
        for (String name : GLYPH_FALLBACK_FONTS) {
            Font fallback = FontLoader.instance().loadFont(name, font.getStyle());
            if (fallback != null && fallback.canDisplay(codePoint)) {
                return fallback.deriveFont(font.getStyle(), font.getSize2D());
            }
        }
        return font;
    }

    /**
     * 측정용 폰트(calculating font)를 반환한다.
     * 폰트 size를 baseSize 그대로 사용하므로 FontMetrics 결과가 HWP 단위와 직접 일치한다.
     * (old_version 방식)
     */
    public Font calculatingFont(CharShape charShape) {
        Font cached = calculatingFontCache.get(charShape);
        if (cached != null) return cached;

        String fontName = getHangulFontName(charShape);
        int style = Font.PLAIN;
        if (charShape.getProperty().isBold()) style |= Font.BOLD;
        if (charShape.getProperty().isItalic()) style |= Font.ITALIC;

        // 렌더링과 동일하게 좁은 한글 폰트는 폰트 크기를 확대해서 FontMetrics 일관성 유지.
        float size = charShape.getBaseSize() * (float) hangulHeightScale(charShape);

        // 스타일별 폰트 파일을 우선 로드 (Bold variant가 있으면 가짜 bold 대신 실제 bold 사용)
        Font baseFont = FontLoader.instance().loadFont(fontName, style);
        if (baseFont == null) {
            baseFont = new Font(fontName, style, (int) size);
        } else {
            baseFont = baseFont.deriveFont(style, size);
        }
        calculatingFontCache.put(charShape, baseFont);
        return baseFont;
    }

    /**
     * 한글 글리프의 세로 스케일 보정 계수.
     *
     * 폰트마다 "가"의 visual height가 달라 (맑은고딕 ≈ 0.874em, 나눔고딕 ≈ 0.922em)
     * 같은 baseSize에서도 글자 크기가 다르게 보인다. 기준 높이(나눔고딕)에 맞춰
     * 세로 스케일을 적용해서 폰트간 글자 높이를 통일한다.
     * 기준에 가까운 폰트는 1.0, 작은 폰트는 > 1.0.
     */
    public double hangulHeightScale(CharShape charShape) {
        String fontName = getHangulFontName(charShape);
        int style = Font.PLAIN;
        if (charShape.getProperty().isBold()) style |= Font.BOLD;
        if (charShape.getProperty().isItalic()) style |= Font.ITALIC;
        String key = fontName + "_" + style;

        Double cached = hangulHeightScaleCache.get(key);
        if (cached != null) return cached;

        Font baseFont = FontLoader.instance().loadFont(fontName, style);
        if (baseFont == null) baseFont = new Font(fontName, style, SCALE_REF_EM);
        else baseFont = baseFont.deriveFont(style, (float) SCALE_REF_EM);

        GlyphVector gv = baseFont.createGlyphVector(SCALE_FRC, "가");
        Rectangle2D vb = gv.getVisualBounds();
        // 기준보다 작으면 고정 확대 비율 적용, 아니면 그대로.
        double scale = (vb.getHeight() > 0 && vb.getHeight() < REF_HANGUL_VIS_HEIGHT)
                ? NARROW_FONT_SCALE
                : 1.0;
        hangulHeightScaleCache.put(key, scale);
        return scale;
    }

    public void clear() {
        fontCache.clear();
        calculatingFontCache.clear();
        hangulHeightScaleCache.clear();
        syntheticStyle = new SyntheticStyle();
    }
}
