package kr.dogfoot.hwpdrawer.util;

/**
 * 획 사이의 틈이 모양의 뜻인 기호 — 점선 원, 점선 사각형, 점선 괘선, 음영 블록.
 *
 * <p>Java의 합성 굵게는 획을 사방으로 두껍게 하므로 이런 기호의 틈을 메운다. 실문서 R16에서 굵은 33px
 * '◌◌◌◌'(빈칸 표시)가 실선 원 '○○○○'로 그려졌다 — 주 글꼴(HY중고딕)에 글리프가 없어 함초롬돋움으로 대체했고,
 * 함초롬돋움에는 굵은 글꼴 파일이 없어 굵기를 합성했기 때문이다. 이런 기호는 합성 굵게 대신 보통 굵기로 그린다
 * ({@link FontManager#withGapsKept}).</p>
 */
public final class GapSensitiveGlyphs {
    private static final int DOTTED_CIRCLE = 0x25CC;
    private static final int DOTTED_SQUARE = 0x2B1A;
    /** ┄┅┆┇┈┉┊┋ 점선 괘선 (3·4점). */
    private static final int DASHED_BOX_DRAWINGS_FIRST = 0x2504;
    private static final int DASHED_BOX_DRAWINGS_LAST = 0x250B;
    /** ╌╍╎╏ 점선 괘선 (2점). */
    private static final int DOUBLE_DASH_BOX_DRAWINGS_FIRST = 0x254C;
    private static final int DOUBLE_DASH_BOX_DRAWINGS_LAST = 0x254F;
    /** ░▒▓ 음영 블록 — 무늬의 틈이 농도다. */
    private static final int SHADE_FIRST = 0x2591;
    private static final int SHADE_LAST = 0x2593;

    private GapSensitiveGlyphs() {}

    public static boolean contains(int codePoint) {
        return codePoint == DOTTED_CIRCLE
                || codePoint == DOTTED_SQUARE
                || between(codePoint, DASHED_BOX_DRAWINGS_FIRST, DASHED_BOX_DRAWINGS_LAST)
                || between(codePoint, DOUBLE_DASH_BOX_DRAWINGS_FIRST, DOUBLE_DASH_BOX_DRAWINGS_LAST)
                || between(codePoint, SHADE_FIRST, SHADE_LAST);
    }

    private static boolean between(int codePoint, int first, int last) {
        return codePoint >= first && codePoint <= last;
    }
}
