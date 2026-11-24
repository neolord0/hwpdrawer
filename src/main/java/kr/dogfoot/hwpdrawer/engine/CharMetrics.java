package kr.dogfoot.hwpdrawer.engine;

/**
 * 한 글자의 레이아웃 측정 결과.
 * 모든 값은 HWP 단위 (1/7200 인치).
 *
 * <p>정밀도를 위해 double을 사용 — 누적 반올림 오차를 피함.
 * 렌더링 시점에 long/int로 변환.</p>
 */
public class CharMetrics {
    public double width;
    public double height;
    public double descent;
    public double baseSize;
}
