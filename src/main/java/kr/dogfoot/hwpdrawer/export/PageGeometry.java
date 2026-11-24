package kr.dogfoot.hwpdrawer.export;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

/**
 * 페이지 용지 크기를 출력 형식마다 필요한 단위로 바꿔 준다.
 *
 * <p>렌더러는 모든 형식에서 같은 픽셀 좌표계(zoomRate 100 = 120 DPI)로 그린다.
 * PNG는 그 픽셀을 그대로 캔버스로 쓰고, 벡터 형식(SVG/PDF)은 같은 좌표계를 실제 용지 크기
 * (mm, pt)로 비례 확대/축소해 보여 준다.</p>
 */
public final class PageGeometry {
    /** 1pt = 1/72인치, HWP 단위 = 1/7200인치. */
    private static final double HWP_UNITS_PER_POINT = 100.0;
    private static final double HWP_UNITS_PER_MM = 7200.0 / 25.4;

    private final long widthHwp;
    private final long heightHwp;

    private PageGeometry(long widthHwp, long heightHwp) {
        this.widthHwp = widthHwp;
        this.heightHwp = heightHwp;
    }

    public static PageGeometry of(Page page) {
        return new PageGeometry(page.paperArea().width(), page.paperArea().height());
    }

    /** PNG 캔버스 폭 (정수 픽셀, 소수점 버림). */
    public int widthPx() {
        return UnitConverter.toPixel(widthHwp);
    }

    /** PNG 캔버스 높이 (정수 픽셀, 소수점 버림). */
    public int heightPx() {
        return UnitConverter.toPixel(heightHwp);
    }

    /** 벡터 형식의 캔버스 폭 (소수 픽셀 — 용지 크기와 정확히 비례). */
    public float exactWidthPx() {
        return UnitConverter.toPixelF(widthHwp);
    }

    public float exactHeightPx() {
        return UnitConverter.toPixelF(heightHwp);
    }

    public float widthPt() {
        return (float) (widthHwp / HWP_UNITS_PER_POINT);
    }

    public float heightPt() {
        return (float) (heightHwp / HWP_UNITS_PER_POINT);
    }

    /** 렌더러 픽셀 1개가 몇 pt인지 (zoomRate 100이면 0.6). */
    public float pointsPerPixel() {
        return 1f / UnitConverter.toPixelF(HWP_UNITS_PER_POINT);
    }

    public double widthMm() {
        return widthHwp / HWP_UNITS_PER_MM;
    }

    public double heightMm() {
        return heightHwp / HWP_UNITS_PER_MM;
    }

    /** 그릴 면적이 있는지. 용지 크기가 0인 페이지는 어떤 형식으로도 내보내지 않는다. */
    public boolean hasArea() {
        return widthPx() > 0 && heightPx() > 0;
    }
}
