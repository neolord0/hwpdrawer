package kr.dogfoot.hwpdrawer.util;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Java가 굵게/기울임을 합성(획을 두껍게, 글자를 기울여)해서 그리는 Font인지 판정한다.
 *
 * <p>굵은 글꼴 파일이 없는 폰트에 굵게를 지정하면 Java는 원래 글리프를 두껍게 만들어 그린다. 쓰는 곳:</p>
 * <ul>
 *   <li>PDF: 원본 파일을 임베드하면 보통 굵기로 보이므로, 이런 글자는 윤곽선으로 그려야 PNG와 같아진다.</li>
 *   <li>렌더링: 점선 원(◌)처럼 틈이 모양의 뜻인 기호는 합성 굵게로 틈이 메워지므로 보통 굵기로 그린다
 *       ({@link GapSensitiveGlyphs}).</li>
 * </ul>
 *
 * <p>파일 이름(bold/black…)으로는 판정할 수 없다 — Java는 OS/2 표의 스타일 비트로 정하므로, 이름에 Black이
 * 있어도 보통 스타일로 표시된 'Arial Black'에는 굵게를 또 합성한다. 그래서 같은 글자를 지정 스타일과 보통
 * 스타일로 만든 윤곽선을 비교한다: Java가 합성할 때만 둘이 다르다.</p>
 */
public final class SyntheticStyle {
    private static final float PROBE_SIZE = 100f;
    private static final String[] PROBE_TEXTS = {"가", "H"};
    private static final FontRenderContext PROBE_CONTEXT = new FontRenderContext(null, true, true);

    /** 키: 폰트 이름 + 스타일. */
    private final Map<String, Boolean> synthesized = new HashMap<>();

    public boolean isSynthesized(Font font) {
        if (font.getStyle() == Font.PLAIN) return false;
        return synthesized.computeIfAbsent(font.getFontName() + "#" + font.getStyle(),
                key -> outlineDiffersFromPlain(font));
    }

    /** 비교할 글자를 그릴 수 없으면 판정할 수 없으므로 합성으로 본다 (윤곽선이 언제나 PNG와 같다). */
    private static boolean outlineDiffersFromPlain(Font font) {
        for (String probe : PROBE_TEXTS) {
            if (font.canDisplayUpTo(probe) == -1) {
                return !outlineBounds(font.deriveFont(PROBE_SIZE), probe)
                        .equals(outlineBounds(font.deriveFont(Font.PLAIN, PROBE_SIZE), probe));
            }
        }
        return true;
    }

    private static Rectangle2D outlineBounds(Font font, String text) {
        return font.createGlyphVector(PROBE_CONTEXT, text).getOutline().getBounds2D();
    }
}
