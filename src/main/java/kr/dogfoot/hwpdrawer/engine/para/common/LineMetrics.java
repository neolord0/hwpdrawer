package kr.dogfoot.hwpdrawer.engine.para.common;

/**
 * 한 라인 내 글자들을 쌓아가면서 누적되는 측정값 (double, HWP 단위).
 */
public final class LineMetrics {
    private double charX;
    private double visibleCharX;
    private double maxDescent;
    /** 이 줄에 공백 아닌 글자가 하나라도 들어왔는가. 후행 밑줄 공백 처리에 쓴다. */
    private boolean hasNonSpace;

    public double charX() { return charX; }

    /**
     * 정렬 기준 가시 폭 — 마지막 가시 글자의 <b>오른쪽 끝</b>이다.
     *
     * <p>자간은 글자 <i>사이</i>에만 들어가므로 마지막 글자 뒤의 자간은 이 폭에 넣지 않는다.
     * 넣으면 가운데정렬 줄이 자간의 절반만큼 왼쪽으로 밀린다(실문서 R11 제목
     * "부동산매매계약서" 8자·자간 50: 참조보다 7px 왼쪽). LineSeg 없는 경로
     * ({@code LineFinisher.computeVisibleCharX})는 원래부터 {@code x + width}로 계산해
     * 자간을 뺐다 — 두 경로를 같은 정의로 맞춘다.</p>
     */
    public double visibleCharX() { return visibleCharX; }
    public double maxDescent() { return maxDescent; }

    public void appendNonSpace(double advance, double descent) {
        appendNonSpace(advance, 0, descent);
    }

    /**
     * 글자를 추가한다. {@code advance}는 글자 폭 + 뒤따르는 자간이고,
     * {@code trailingSpacing}은 그중 자간 몫이다(가시 폭에서 제외된다).
     */
    public void appendNonSpace(double advance, double trailingSpacing, double descent) {
        charX += advance;
        visibleCharX = charX - trailingSpacing;
        hasNonSpace = true;
        if (descent > maxDescent) maxDescent = descent;
    }

    public void appendSpace(double advance, double descent) {
        appendSpace(advance, 0, descent, false);
    }

    /**
     * 공백을 추가한다. 밑줄/취소선이 그어진 공백은 눈에 보이므로({@code visible=true})
     * visibleCharX(정렬 기준 가시 폭)에 포함시킨다 — 그렇지 않으면 밑줄 공백만 있는
     * 가운데 정렬 줄(서명란 등)이 셀 중앙부터 오른쪽으로 밀려 잘린다.
     *
     * <p><b>단, 공백 아닌 글자가 이미 있는 줄에서는 포함하지 않는다.</b> 한글은 글자가 있는
     * 줄에서는 후행 공백을 정렬 폭에서 빼기 때문이다 — 밑줄이 그어져 있어도 마찬가지다
     * (밑줄은 그려지되 중앙 계산에는 안 들어간다). 실문서 R23 제목
     * "촬 영 허 가 신 청 서" 뒤에 <b>밑줄 그은 공백 1개</b>가 있는데, 이걸 폭에 넣으면
     * 공백 절반(20pt → 8.3px)만큼 제목이 왼쪽으로 밀린다(참조 337 vs 우리 328).</p>
     *
     * <p>공백만 있는 줄(실문서 R07 서명란)은 {@code hasNonSpace == false} 라 종전대로 포함된다 —
     * 그 줄은 후행 공백을 뺄 "본문"이 아예 없어 폭이 0이 되면 정렬이 무너진다.</p>
     */
    public void appendSpace(double advance, double trailingSpacing, double descent, boolean visible) {
        charX += advance;
        if (visible && !hasNonSpace) visibleCharX = charX - trailingSpacing;
        if (descent > maxDescent) maxDescent = descent;
    }

    public void shiftX(double delta) {
        charX += delta;
        if (visibleCharX != 0) visibleCharX += delta;
    }

    public void reset() {
        charX = 0;
        visibleCharX = 0;
        maxDescent = 0;
        hasNonSpace = false;
    }
}
