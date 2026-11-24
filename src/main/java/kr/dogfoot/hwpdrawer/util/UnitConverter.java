package kr.dogfoot.hwpdrawer.util;

/**
 * HWP 단위와 픽셀 간의 변환 유틸리티.
 *
 * HWP 단위: 1 HWP unit = 1/7200 인치
 * 변환 공식: pixel = hwpUnit * zoomRate / 6000
 *   - zoomRate=100일 때 120 DPI에 해당 (A4 → 992x1403 pixels)
 *   - 한글 프로그램의 기본 캡처 해상도와 ��치
 */
public class UnitConverter {
    /**
     * 변환 기준값.
     * hwpUnit * zoomRate / DIVISOR = pixel
     * 120 DPI 기준: 7200 / 120 * 100 = 6000
     */
    private static final double DIVISOR = 6000.0;

    private static int zoomRate = 100;
    private static int offsetX = 0;
    private static int offsetY = 0;

    /**
     * 정수 픽셀 변환의 <b>기준 원점</b>(본문영역 좌상단, HWPUNIT)과 그 스냅된 픽셀값.
     *
     * <p>한글은 좌표를 통째로 반올림하지 않는다. <b>본문 원점을 먼저 정수 픽셀로 스냅한 뒤,
     * 거기서부터의 상대 오프셋을 다시 반올림</b>한다(이중 반올림):</p>
     * <pre>한글px(v) = round(originPx) + round((v - origin) / 60)</pre>
     * <p>실문서 R26의 격자 좌표 <b>78개 전수 78/78</b> 적중(현행 절삭은 21/78).
     * 잉크 임계값 100·128·200 × 커버리지 0.60~0.95 전 조합에서 동일하다.</p>
     */
    private static long originX = 0;
    private static long originY = 0;
    private static int originXpx = 0;
    private static int originYpx = 0;

    /** 페이지마다 본문영역 좌상단을 원점으로 설정한다. */
    public static void setOrigin(long hwpX, long hwpY) {
        originX = hwpX;
        originY = hwpY;
        originXpx = (int) Math.round(hwpX * zoomRate / DIVISOR);
        originYpx = (int) Math.round(hwpY * zoomRate / DIVISOR);
    }

    public static void setZoomRate(int rate) {
        zoomRate = rate;
    }

    public static void setOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }

    public static int zoomRate() {
        return zoomRate;
    }

    /**
     * HWP 단위를 픽셀로 변환.
     */
    public static int toPixel(long hwpUnit) {
        return (int) (hwpUnit * zoomRate / DIVISOR);
    }

    /**
     * HWP 단위를 <b>위치</b> 픽셀로 변환(이중 반올림). 선·테두리·컨트롤 상자가 쓴다.
     *
     * <p>⚠ <b>단순 반올림({@code round(v)})으로 바꾸면 안 된다</b> — 2026-08-11 실험에서
     * 실문서 R06 p1 이 98.46% → 95.22%(-3.24%p)로 붕괴해 보류 결정된 방식이다.
     * 같은 케이스가 이 이중 반올림에서는 98.46% → <b>100.000%</b>(한글과 픽셀 차이 0)이 된다.</p>
     */
    public static int toPixelX(long hwpUnit) {
        return originXpx + (int) Math.round((hwpUnit - originX) * zoomRate / DIVISOR) + offsetX;
    }

    public static int toPixelY(long hwpUnit) {
        return originYpx + (int) Math.round((hwpUnit - originY) * zoomRate / DIVISOR) + offsetY;
    }

    /**
     * HWP 단위를 float 픽셀로 변환 (반올림 오차 없음).
     */
    public static float toPixelF(long hwpUnit) {
        return (float) (hwpUnit * zoomRate / DIVISOR);
    }

    public static float toPixelF(double hwpUnit) {
        return (float) (hwpUnit * zoomRate / DIVISOR);
    }

    public static float toPixelXF(long hwpUnit) {
        return toPixelF(hwpUnit) + offsetX;
    }

    public static float toPixelXF(double hwpUnit) {
        return toPixelF(hwpUnit) + offsetX;
    }

    public static float toPixelYF(long hwpUnit) {
        return toPixelF(hwpUnit) + offsetY;
    }

    public static float toPixelYF(double hwpUnit) {
        return toPixelF(hwpUnit) + offsetY;
    }

    /**
     * HWP 폰트 크기(baseSize)를 포인트로 변환.
     * hwplib에서 baseSize는 HWP 단위와 동�� (1000 = 10pt)
     */
    public static float toPointSize(int baseSize) {
        return baseSize / 100.0f;
    }

    /**
     * HWP 폰트 크기를 화면 픽셀 크기로 변환.
     * baseSize는 HWP 단위와 같은 스케일이므로 동일한 환산 공식 사용.
     */
    public static float toPixelFontSize(int baseSize) {
        return (float) (baseSize * zoomRate / DIVISOR);
    }
}
