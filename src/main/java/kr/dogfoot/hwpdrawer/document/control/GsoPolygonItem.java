package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwpdrawer.util.Area;

/**
 * 다각형(Polygon) GSO 컨트롤 아이템.
 *
 * <p>다각형은 사각형과 달리 꼭짓점 좌표(페이지 HWP 단위)를 직접 갖는다 — 마름모·삼각형 등.
 * 채우기/테두리 색·두께는 {@link #gso()}의 ShapeComponentNormal 에서 읽는다.</p>
 */
public class GsoPolygonItem extends ControlItem {
    private final GsoControl gso;
    private final long[] xs;   // 페이지 좌표 (HWP 단위)
    private final long[] ys;

    public GsoPolygonItem(GsoControl gso, long[] xs, long[] ys,
                          Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
        this.gso = gso;
        this.xs = xs;
        this.ys = ys;
    }

    public GsoControl gso() { return gso; }
    public long[] xs() { return xs; }
    public long[] ys() { return ys; }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        GsoPolygonItem copy = new GsoPolygonItem(gso, xs, ys, newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        return copy;
    }
}
