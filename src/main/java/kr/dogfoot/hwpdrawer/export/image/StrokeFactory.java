package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.charshape.BorderType2;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;

import java.awt.BasicStroke;
import java.awt.Stroke;

/**
 * HWP의 다양한 border/line 스타일 enum을 {@link Stroke}로 변환하는 중앙 팩토리.
 *
 * <p>Clean Code: dash 패턴 매직 상수를 한 곳에 모아 중복을 제거한다.</p>
 */
public final class StrokeFactory {
    private StrokeFactory() {}

    private static final float DEFAULT_WIDTH = 1.0f;
    private static final float THICK_WIDTH = 2.0f;

    // ── 자주 쓰이는 dash 패턴 (픽셀 단위) ──
    private static final float[] DASH      = {6, 4};
    private static final float[] DASH_LONG = {12, 4};
    private static final float[] DASH_DOT  = {6, 3, 1, 3};
    private static final float[] DASH_2DOT = {6, 2, 1, 2, 1, 2};
    private static final float[] DOT       = {0.5f, 3};
    private static final float[] CIRCLE    = {0.1f, 4};

    // ── 단 구분선 전용 패턴 (한글 실측, 표/문단 테두리와 다르다) ──
    private static final float[] COL_SEP_DASH_DOT = {10, 2, 1, 2};   // 주기 15
    private static final float[] COL_SEP_DOT      = {1, 4};          // 주기 5

    /** 문자 꾸미기용 BorderType2 → Stroke. (밑줄/취소선 등) */
    public static Stroke forCharBorder(BorderType2 type) {
        if (type == null) return new BasicStroke(DEFAULT_WIDTH);
        switch (type) {
            case Solid:         return solid();
            case Dash:          return dash(DEFAULT_WIDTH, DASH);
            case Dot:           return roundedDash(DEFAULT_WIDTH, DOT);
            case DashDot:       return dash(DEFAULT_WIDTH, DASH_DOT);
            case DashDotDot:    return dash(DEFAULT_WIDTH, DASH_2DOT);
            case LongDash:      return dash(DEFAULT_WIDTH, DASH_LONG);
            case CircleDot:     return roundedDash(1.5f, CIRCLE);
            case Double:        return solid(); // 이중선: 추후 별도 처리
            case Thick3D:
            case Solid3D:
            case Thick3DReverseLighting:
            case Solid3DReverseLighting:
                return new BasicStroke(THICK_WIDTH);
            default:            return solid();
        }
    }

    /** 문단 테두리용 BorderFill의 BorderType → Stroke. (기본 두께 1px, dash 패턴 원본 크기) */
    public static Stroke forParaBorder(BorderType type) {
        if (type == null) return new BasicStroke(DEFAULT_WIDTH);
        switch (type) {
            case None:        return null;
            case Solid:       return solid();
            case Dash:        return dash(DEFAULT_WIDTH, DASH);
            case Dot:         return roundedDash(DEFAULT_WIDTH, DOT);
            case DashDot:     return dash(DEFAULT_WIDTH, DASH_DOT);
            case DashDotDot:  return dash(DEFAULT_WIDTH, DASH_2DOT);
            case LongDash:    return dash(DEFAULT_WIDTH, DASH_LONG);
            case CircleDot:   return roundedDash(1.5f, CIRCLE);
            default:          return solid();
        }
    }

    /**
     * 테두리용 BorderType → Stroke. 선 두께(width, px)와 dash 패턴 길이 배율(scale)을 반영한다.
     *
     * <p>표 셀 테두리처럼 두께·zoom을 반영해야 하는 경우 쓴다. {@code width}는 보통
     * {@code BorderRenderer.scaledThickness(...)}(zoom 반영 픽셀 두께), {@code scale}은
     * {@code zoomRate/100}이다 — zoom이 커지면 dash 간격도 함께 커진다. 인자 없는
     * {@link #forParaBorder(BorderType)}는 두께 1px·패턴 원본 크기로 동작한다.</p>
     */
    public static BasicStroke forParaBorder(BorderType type, float width, float scale) {
        float w = Math.max(DEFAULT_WIDTH, width);
        if (type == null) return new BasicStroke(w);
        switch (type) {
            case None:        return null;
            case Solid:       return new BasicStroke(w);
            case Dash:        return dash(w, scaled(DASH, scale));
            case Dot:         return dash(w, borderDotPattern(w));
            case DashDot:     return dash(w, scaled(DASH_DOT, scale));
            case DashDotDot:  return dash(w, scaled(DASH_2DOT, scale));
            case LongDash:    return dash(w, scaled(DASH_LONG, scale));
            case CircleDot:   return roundedDash(w, scaled(CIRCLE, scale));
            default:          return new BasicStroke(w);
        }
    }

    /**
     * <b>단 구분선</b>(ControlColumnDefine의 divideLine) 전용 Stroke.
     *
     * <p>한글은 같은 선 종류라도 컨텍스트마다 점선 규칙이 다르다. 두께가 같은 MM0_12인데도
     * 실측 주기가 이렇게 갈린다 — 단 구분선 Dot = 5, 표 셀 테두리 Dot = 3, 문단 테두리 Dot = 3.
     * 그래서 표/문단이 쓰는 공유 상수({@link #DOT}·{@link #DASH_DOT})와 분리한다.</p>
     *
     * <p>한글 캡처 실측(zoom 100 = 120DPI, 두께 MM0_12):</p>
     * <ul>
     *   <li>DashDot: 대시 10px · 공백 2px · 점 1px · 공백 2px (주기 15)
     *       — 다단/일반/방향/맞쪽 시험 문서의 한글 출력 x=354</li>
     *   <li>Dot: 점 1px · 공백 4px (주기 5) — 다단/일반/기본/같은크기 시험 문서의 한글 출력 x=369</li>
     *   <li>Solid: 1px 실선 — 다단/일반/다단설정나누기/1 시험 문서</li>
     * </ul>
     *
     * <p>둥근 끝({@link #roundedDash})을 쓰면 안 된다 — 안티에일리어싱이 꺼진 단 구분선 경로에서
     * {@code {0.5,3}}의 점 절반이 사라져 주기가 7로 벌어지던 것이 기존 결함이었다.
     * 표본이 없는 나머지 종류(Dash·LongDash·DashDotDot·CircleDot)는 기존 동작을 유지한다.</p>
     */
    public static Stroke forColumnSeparator(BorderType type) {
        if (type == null) return new BasicStroke(DEFAULT_WIDTH);
        switch (type) {
            case Dot:     return dash(DEFAULT_WIDTH, COL_SEP_DOT);
            case DashDot: return dash(DEFAULT_WIDTH, COL_SEP_DASH_DOT);
            default:      return forParaBorder(type);
        }
    }

    /**
     * GSO 도형(선/타원/사각형) 테두리용 LineType → Stroke. 선 두께(width, px)를 반영한다.
     * dash 패턴은 zoom 100 기준 고정 px(테스트 렌더 기준)이며, Solid/Double/입체선 등은 실선.
     */
    public static Stroke forGsoLine(LineType type, float width) {
        float w = Math.max(DEFAULT_WIDTH, width);
        if (type == null) return new BasicStroke(w);
        switch (type) {
            case Dash:        return dash(w, DASH);
            case Dot:         return roundedDash(w, DOT);
            case DashDot:     return dash(w, DASH_DOT);
            case DashDotDot:  return dash(w, DASH_2DOT);
            case LongDash:    return dash(w, DASH_LONG);
            case CircleDot:   return roundedDash(w, CIRCLE);
            default:          return new BasicStroke(w);  // Solid/Double/ThinBold 등: 실선
        }
    }

    /**
     * 테두리 점선(Dot)의 dash 패턴 — 점 길이·간격이 <b>선 두께에 비례</b>한다.
     *
     * <p>한글 캡처 측정: 두께 1px(MM0_1·MM0_12)은 점 1px·간격 2px(주기 3), 두께 2px(MM0_4)는
     * 점 3px·간격 4px(주기 7). 즉 점 = 2t−1, 간격 = 2t다. 고정 패턴({@link #DOT})을 쓰면
     * 두꺼운 점선의 주기가 얇은 점선과 같아져 한글보다 촘촘해진다 — 실문서 R13
     * 절취선(Dot/MM0_4)이 주기 3.5로 그려져 참조의 주기 7과 어긋났다.</p>
     *
     * <p>{@code t}는 이미 zoom이 반영된 픽셀 두께이므로 패턴을 zoom으로 다시 스케일하지 않는다.</p>
     *
     * <p><b>표본 한계:</b> 실측은 t=1·t=2 둘뿐이다. t≥3(MM0_7 이상) 점선 표본이 테스트
     * 문서에 없어 그 구간은 외삽이다.</p>
     */
    private static float[] borderDotPattern(float t) {
        return new float[]{Math.max(1f, 2f * t - 1f), Math.max(1f, 2f * t)};
    }

    /** dash 패턴의 각 구간을 scale배(보통 zoomRate/100)로 늘린다. BasicStroke가 0 길이를 거부하므로 하한을 둔다. */
    private static float[] scaled(float[] pattern, float scale) {
        if (scale == 1.0f) return pattern;
        float[] out = new float[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            out[i] = Math.max(0.01f, pattern[i] * scale);
        }
        return out;
    }

    private static Stroke solid() {
        return new BasicStroke(DEFAULT_WIDTH);
    }

    private static BasicStroke dash(float width, float[] pattern) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10, pattern, 0);
    }

    private static BasicStroke roundedDash(float width, float[] pattern) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER,
                10, pattern, 0);
    }
}
