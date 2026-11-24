package kr.dogfoot.hwpdrawer.util;

import kr.dogfoot.hwplib.object.docinfo.CharShape;

/**
 * 위첨자/아래첨자(super/subscript) 렌더링 규칙.
 *
 * <p>HWP 파일은 첨자를 <b>속성 비트 하나로만</b> 표시하고 크기·위치 값은 담지 않는다
 * (실측: 해당 CharShape의 {@code relativeSize=100}, {@code charOffset=0}). 즉 축소 비율과
 * 올림 높이는 한글 프로그램이 적용하는 고정 변환이므로 참조 출력에서 역산해야 한다.</p>
 *
 * <p><b>크기 배율 역산</b> — 실문서 R01 page2
 * "신청인(대리 신청인<sup>1)</sup>)" 의 첨자 런(baseSize=1100 → 11pt).
 * 참조 이미지의 잉크 높이 비로 배율이 바로 나온다:</p>
 * <pre>
 *   글리프   정상 잉크 높이   위첨자 잉크 높이   비
 *   ")"     17px            10px             0.59
 * </pre>
 *
 * <p><b>상승량 역산</b> — 배율과 달리 상승량은 이미지 관찰만으로는 베이스라인 위치와
 * 글리프 extent 가정에 좌우돼 정확히 나오지 않는다. 그래서 <b>실제로 렌더해서 맞췄다</b>:
 * 0.5로 두면 위첨자가 참조보다 2px 높게 그려졌고, 0.4에서 위첨자 "1"의 잉크 y범위가
 * 참조와 정확히 일치했다(둘 다 1233~1240).</p>
 *
 * <p><b>교차검증</b> — 위 두 값은 <b>다른 문서·다른 글자 크기</b>에서 독립적으로 확인됐다.
 * 실문서 R05 는 15pt 위첨자 "*"를 쓰는데, 이 규칙을 켜자 참조 대비 절대오차가
 * page1 −8.4%, page2 −1.7% 로 줄었다. 즉 배율·상승량이 글자 크기에 비례한다는 가정이
 * 11pt·15pt 두 표본에서 성립한다.</p>
 *
 * <p>첨자는 <b>줄 높이를 바꾸지 않는다</b> — 글자 폭과 그리는 크기·위치만 달라진다.
 * 따라서 {@code CharMetrics.height}(= baseSize)에는 배율을 적용하지 않는다.</p>
 */
public final class ScriptStyle {

    /** 첨자 글자 크기 배율. */
    private static final double SCALE = 0.6;

    /** 위첨자 베이스라인 상승량 (baseSize 대비 비율). 렌더 실측으로 맞춘 값 — 클래스 주석 참고. */
    private static final double SUPERSCRIPT_RISE = 0.4;

    /**
     * 아래첨자 하강량 (baseSize 대비 비율).
     *
     * <p><b>미검증</b> — 테스트 코퍼스 286개 문서 중 아래첨자를 쓰는 문서가 0개라
     * 참조 출력으로 확인할 표본이 없다. 위첨자 상승량의 절반으로 두었다.
     * 아래첨자가 있는 실문서를 확보하면 반드시 재측정할 것.</p>
     */
    private static final double SUBSCRIPT_DROP = 0.25;

    private ScriptStyle() {}

    /**
     * CharShape의 첨자 비트와 무관하게 적용할 첨자 규칙.
     *
     * <p>각주 앵커("본문<sup>3)</sup>")가 이 경우다 — 앵커 글자의 CharShape은
     * {@code isSuperScript()==false}인데도 한글은 위첨자로 그린다. 즉 첨자 여부가
     * 파일이 아니라 <b>컨트롤 종류</b>에서 온다. DocInfo의 CharShape은 여러 글자가
     * 공유하는 인스턴스라 비트를 세우면 다른 글자까지 오염되므로, 복제 대신
     * 이 오버라이드를 CharInfo에 실어 보낸다.</p>
     */
    public static final class Script {
        /** 글자 크기 배율. */
        public final double scale;
        /** 베이스라인 상승 비율(baseSize 대비). 위로 올리므로 렌더 시 음수로 적용된다. */
        public final double rise;

        public Script(double scale, double rise) {
            this.scale = scale;
            this.rise = rise;
        }
    }

    /**
     * 각주/미주 앵커 마크의 첨자 규칙.
     *
     * <p>일반 위첨자({@link #SCALE} 0.6 / {@link #SUPERSCRIPT_RISE} 0.4)와 <b>값이 다르다</b> —
     * 실문서 R21 참조 출력 실측:</p>
     * <pre>
     *   크기: 같은 줄 정상 ")" 잉크 높이 17px vs 앵커 ")" 13px → 0.76
     *         (다른 표본 0.732) → 0.75
     *   상승: 앵커 ")" 와 정상 ")" 의 잉크 중심 차 4.17px·4.28px (11pt=1100단위)
     *         → 약 250단위 = 0.23 × baseSize
     * </pre>
     *
     * <p>표본이 이 문서 하나뿐이다. 각주를 쓰는 실문서를 더 확보하면 재측정할 것.</p>
     */
    public static Script forFootnoteMark() {
        return new Script(0.75, 0.23);
    }

    /** 위첨자 또는 아래첨자인가. */
    public static boolean isScript(CharShape charShape) {
        return charShape.getProperty().isSuperScript()
                || charShape.getProperty().isSubScript();
    }

    /** 글자 크기 배율 — 첨자면 축소, 아니면 1.0. */
    public static double sizeScale(CharShape charShape) {
        return isScript(charShape) ? SCALE : 1.0;
    }

    /** 오버라이드가 있으면 그 배율을, 없으면 CharShape 기준 배율을 쓴다. */
    public static double sizeScale(CharShape charShape, Script override) {
        return override != null ? override.scale : sizeScale(charShape);
    }

    /**
     * 베이스라인 이동량(HWP 글자 단위, baseSize와 같은 스케일).
     * 화면 좌표계는 아래로 갈수록 커지므로 위첨자는 음수를 반환한다.
     */
    public static double baselineShift(CharShape charShape) {
        if (charShape.getProperty().isSuperScript()) {
            return -charShape.getBaseSize() * SUPERSCRIPT_RISE;
        }
        if (charShape.getProperty().isSubScript()) {
            return charShape.getBaseSize() * SUBSCRIPT_DROP;
        }
        return 0;
    }

    /** 오버라이드가 있으면 그 상승량을, 없으면 CharShape 기준 이동량을 쓴다. */
    public static double baselineShift(CharShape charShape, Script override) {
        return override != null
                ? -charShape.getBaseSize() * override.rise
                : baselineShift(charShape);
    }
}
