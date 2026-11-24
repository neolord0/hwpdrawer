package kr.dogfoot.hwpdrawer.engine.para.nolineseg.obstacle;

import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextHorzArrange;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * FitWithText(어울림) 컨트롤 영역 집합. old_version {@code ForFitWithText}를 이식.
 *
 * <p>주어진 Y 범위와 교차하는 컨트롤들로 한 라인을 여러 수평 세그먼트로 나눈다.
 * {@link TextHorzArrange}에 따라 세그먼트를 선별한다(BothSides/Left/Right/LargestOnly).</p>
 */
public final class ParaObstacles {

    /** 한 라인 내 사용 가능한 수평 밴드. */
    public static final class Segment {
        public final long left;
        public final long right;
        public Segment(long left, long right) {
            this.left = left;
            this.right = right;
        }
        public long width() { return right - left; }
    }

    private static final class Obstacle {
        final Area area;
        final TextHorzArrange horzArrange;
        final int zOrder;

        Obstacle(Area area, TextHorzArrange horzArrange, int zOrder) {
            this.area = area;
            this.horzArrange = horzArrange;
            this.zOrder = zOrder;
        }
    }

    private final List<Obstacle> list = new ArrayList<>();

    public void add(Area area, TextHorzArrange horzArrange, int zOrder) {
        // 동일 장애물 이중 등록 방지 — Paper/Page-relative 컨트롤이 preRegisterPaperAbsoluteObstacles와
        // preRegisterObstacles 양쪽에서 등록될 때, computeSegments의 non-intersection 분기가
        // 이미 잘린 세그먼트를 잘못 제거하는 것을 막는다.
        for (Obstacle o : list) {
            if (o.zOrder == zOrder && o.horzArrange == horzArrange
                    && o.area.left() == area.left() && o.area.top() == area.top()
                    && o.area.right() == area.right() && o.area.bottom() == area.bottom()) {
                return;  // 중복 — 등록 건너뜀
            }
        }
        list.add(new Obstacle(area, horzArrange, zOrder));
        list.sort(Comparator.comparingInt(o -> o.zOrder));
    }

    /** 페이지/컬럼 교체 시 기존 obstacles는 유효하지 않다. */
    public void clear() {
        list.clear();
    }

    /** shadow ctx로 현재 obstacle을 복제. Area 참조를 공유하므로 shadow 측 수정 금지. */
    public void copyFrom(ParaObstacles other) {
        list.clear();
        list.addAll(other.list);
    }

    public boolean isEmpty() { return list.isEmpty(); }

    public boolean hasIntersecting(long top, long bottom) {
        for (Obstacle o : list) {
            if (verticallyOverlaps(o.area, top, bottom)) return true;
        }
        return false;
    }

    /**
     * [top, bottom] 범위에서 [lineLeft, lineRight] 라인 영역을 장애물로 잘라낸 세그먼트 목록.
     * 빈 리스트면 모든 영역이 막힘. 결과는 left 오름차순.
     */
    public List<Segment> computeSegments(long top, long bottom, long lineLeft, long lineRight) {
        LinkedList<Area> divided = new LinkedList<>();
        Area lineArea = new Area(lineLeft, top, lineRight, bottom);
        divided.add(lineArea);

        for (Obstacle o : list) {
            if (!verticallyOverlaps(o.area, top, bottom)) continue;

            List<Area> adding = new ArrayList<>();
            List<Area> removing = new ArrayList<>();
            boolean intersected = false;
            for (Area a : divided) {
                if (horizontallyOverlaps(o.area, a)) {
                    divide(a, o.area, o.horzArrange, adding);
                    removing.add(a);
                    intersected = true;
                }
            }
            if (!intersected) {
                // 현재 어떤 세그먼트와도 교차하지 않지만 LeftOnly/RightOnly는 반대편 세그먼트 제거.
                for (Area a : divided) {
                    if (o.horzArrange == TextHorzArrange.LeftOnly && a.right() >= o.area.left()) {
                        removing.add(a);
                    } else if (o.horzArrange == TextHorzArrange.RightOnly && a.left() <= o.area.right()) {
                        removing.add(a);
                    }
                }
            }
            divided.removeAll(removing);
            divided.addAll(adding);
        }

        divided.sort(Comparator.comparingLong(Area::left));
        List<Segment> out = new ArrayList<>();
        for (Area a : divided) {
            if (a.right() - a.left() > 0) out.add(new Segment(a.left(), a.right()));
        }
        return out;
    }

    /** [top, bottom]과 겹치는 컨트롤 중 가장 위에서 끝나는 bottom. 없으면 -1. */
    public long nextSkipY(long top, long bottom) {
        long minBot = -1;
        for (Obstacle o : list) {
            if (!verticallyOverlaps(o.area, top, bottom)) continue;
            if (minBot == -1 || o.area.bottom() < minBot) minBot = o.area.bottom();
        }
        return minBot;
    }

    /**
     * 라인 박스와 장애물이 수직으로 겹치는지 — box-box. old_version {@code ForFitWithText#calculate}와
     * 동일. 라인 박스 높이는 실제 텍스트 높이(baseSize)로 좁게 잡아야 line-spacing gap에 걸리는
     * 장애물을 오탐하지 않는다.
     */
    private static boolean verticallyOverlaps(Area a, long top, long bottom) {
        return a.top() < bottom && a.bottom() > top;
    }

    private static boolean horizontallyOverlaps(Area a, Area b) {
        return a.left() < b.right() && a.right() > b.left();
    }

    private static void divide(Area lineSeg, Area ctrl, TextHorzArrange arrange, List<Area> out) {
        Area left, right;
        if (ctrl.left() <= lineSeg.left() && ctrl.right() < lineSeg.right()) {
            left = null;
            right = new Area(ctrl.right(), lineSeg.top(), lineSeg.right(), lineSeg.bottom());
        } else if (ctrl.right() >= lineSeg.right() && ctrl.left() > lineSeg.left()) {
            left = new Area(lineSeg.left(), lineSeg.top(), ctrl.left(), lineSeg.bottom());
            right = null;
        } else {
            left = new Area(lineSeg.left(), lineSeg.top(), ctrl.left(), lineSeg.bottom());
            right = new Area(ctrl.right(), lineSeg.top(), lineSeg.right(), lineSeg.bottom());
        }
        switch (arrange) {
            case BothSides:
                if (left != null) out.add(left);
                if (right != null) out.add(right);
                break;
            case LeftOnly:
                if (left != null) out.add(left);
                break;
            case RightOnly:
                if (right != null) out.add(right);
                break;
            case LargestOnly: {
                long lw = (left != null) ? left.width() : -1;
                long rw = (right != null) ? right.width() : -1;
                if (lw >= rw) {
                    if (left != null) out.add(left);
                } else {
                    if (right != null) out.add(right);
                }
                break;
            }
        }
    }
}
