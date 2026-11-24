package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * 표 전체의 셀 테두리를 <b>하나의 격자(lattice)</b>로 렌더링한다.
 *
 * <p>셀마다 {@link BorderRenderer#drawCellBorders}를 독립 호출하면, 인접한 두 셀이
 * 공유 격자선을 서로 다른 순위(rank)로 두 번 그려 다중선(이중/삼중) 교차점이 과도하게
 * 메워진다(over-fill). 한글은 다중선 교차점을 다음 규칙으로 그린다 — 이를 재현한다.</p>
 *
 * <ul>
 *   <li><b>내부 교차점</b>: 분리선의 <b>중앙(원래 단일선 위치) 가닥</b>은 직교 분리선을
 *       관통한다. <b>바깥(얇은) 가닥</b>은 가장 가까운 직교 가닥에서 멈춰, 직교 분리선의
 *       공백(gap)이 흰색으로 관통한다(가닥×가닥 교차점에서만 칠해짐).</li>
 *   <li><b>바깥 모서리</b>: 직교 테두리의 y-범위 밖에 있는 가닥(표 상/하단 가장자리)은
 *       바깥 모서리까지 연장돼 중첩 사각형(nested rectangle) 모서리를 만든다.</li>
 *   <li><b>T자(분리선이 바깥 테두리에 닿음)</b>: 중앙 가닥은 직교 테두리의 중앙까지,
 *       바깥 가닥은 가장 안쪽 직교 가닥까지 — 위 두 규칙의 자연스러운 결합.</li>
 * </ul>
 *
 * <p>오직 테두리 색(검정)만 칠하고 흰색을 덮어쓰지 않으므로 셀 배경/채우기를 보존한다.</p>
 */
final class TableBorderRenderer {

    /** 픽셀 좌표 셀 사각형 + BorderFill. */
    static final class Cell {
        final int x1, y1, x2, y2;
        final BorderFill bf;
        Cell(int x1, int y1, int x2, int y2, BorderFill bf) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.bf = bf;
        }
    }

    /** BorderFill에 다중선(이중/삼중) 테두리가 하나라도 있으면 true. */
    static boolean hasMultiLine(BorderFill bf) {
        if (bf == null) return false;
        return isMulti(bf.getTopBorder()) || isMulti(bf.getBottomBorder())
                || isMulti(bf.getLeftBorder()) || isMulti(bf.getRightBorder());
    }

    private static boolean isMulti(EachBorder b) {
        if (b == null || b.getType() == null) return false;
        switch (b.getType()) {
            case Double:
            case DoubleWave:
            case ThinThick:
            case ThickThin:
            case ThinThickThin:
                return true;
            default:
                return false;
        }
    }

    // ── 격자 모델 ─────────────────────────────────────────────────────────

    private final Graphics2D g;
    private int[] xs;          // 정렬된 세로 격자선 x 좌표
    private int[] ys;          // 정렬된 가로 격자선 y 좌표
    private BorderRenderer.Side[][] hEdge;  // hEdge[r][c]: y=ys[r], x∈[xs[c],xs[c+1]] 가로 테두리
    private BorderRenderer.Side[][] vEdge;  // vEdge[c][r]: x=xs[c], y∈[ys[r],ys[r+1]] 세로 테두리
    private int[] vSpanTop, vSpanBot;       // 세로 격자선 c의 y-범위
    private int[] hSpanLeft, hSpanRight;    // 가로 격자선 r의 x-범위

    private TableBorderRenderer(Graphics2D g) { this.g = g; }

    static void render(Graphics2D g, List<Cell> cells) {
        if (cells.isEmpty()) return;
        TableBorderRenderer r = new TableBorderRenderer(g);
        r.buildGrid(cells);
        r.draw();
    }

    private void buildGrid(List<Cell> cells) {
        TreeSet<Integer> xsSet = new TreeSet<>();
        TreeSet<Integer> ysSet = new TreeSet<>();
        for (Cell c : cells) {
            xsSet.add(c.x1); xsSet.add(c.x2);
            ysSet.add(c.y1); ysSet.add(c.y2);
        }
        xs = toArray(xsSet);
        ys = toArray(ysSet);
        int nCols = xs.length - 1;   // 셀 열 개수
        int nRows = ys.length - 1;   // 셀 행 개수

        hEdge = new BorderRenderer.Side[ys.length][Math.max(nCols, 1)];
        vEdge = new BorderRenderer.Side[xs.length][Math.max(nRows, 1)];

        for (Cell c : cells) {
            int c1 = indexOf(xs, c.x1), c2 = indexOf(xs, c.x2);
            int r1 = indexOf(ys, c.y1), r2 = indexOf(ys, c.y2);
            if (c1 < 0 || c2 < 0 || r1 < 0 || r2 < 0 || c.bf == null) continue;

            BorderRenderer.Side top = BorderRenderer.Side.of(c.bf.getTopBorder());
            BorderRenderer.Side bottom = BorderRenderer.Side.of(c.bf.getBottomBorder());
            BorderRenderer.Side left = BorderRenderer.Side.of(c.bf.getLeftBorder());
            BorderRenderer.Side right = BorderRenderer.Side.of(c.bf.getRightBorder());

            for (int cc = c1; cc < c2; cc++) {
                putH(r1, cc, top);
                putH(r2, cc, bottom);
            }
            for (int rr = r1; rr < r2; rr++) {
                putV(c1, rr, left);
                putV(c2, rr, right);
            }
        }

        // 격자선 범위 계산
        vSpanTop = new int[xs.length];
        vSpanBot = new int[xs.length];
        for (int c = 0; c < xs.length; c++) {
            int min = -1, max = -1;
            for (int rr = 0; rr < nRows; rr++) {
                if (nonEmpty(vEdge[c][rr])) { if (min < 0) min = rr; max = rr; }
            }
            vSpanTop[c] = min < 0 ? ys[0] : ys[min];
            vSpanBot[c] = max < 0 ? ys[ys.length - 1] : ys[max + 1];
        }
        hSpanLeft = new int[ys.length];
        hSpanRight = new int[ys.length];
        for (int r = 0; r < ys.length; r++) {
            int min = -1, max = -1;
            for (int cc = 0; cc < nCols; cc++) {
                if (nonEmpty(hEdge[r][cc])) { if (min < 0) min = cc; max = cc; }
            }
            hSpanLeft[r] = min < 0 ? xs[0] : xs[min];
            hSpanRight[r] = max < 0 ? xs[xs.length - 1] : xs[max + 1];
        }
    }

    /** 공유 모서리는 가닥이 더 많은(또는 비어있지 않은) Side를 우선한다. */
    private void putH(int r, int c, BorderRenderer.Side s) {
        if (!nonEmpty(s)) return;
        if (!nonEmpty(hEdge[r][c]) || s.plyCount() > hEdge[r][c].plyCount()) hEdge[r][c] = s;
    }

    private void putV(int c, int r, BorderRenderer.Side s) {
        if (!nonEmpty(s)) return;
        if (!nonEmpty(vEdge[c][r]) || s.plyCount() > vEdge[c][r].plyCount()) vEdge[c][r] = s;
    }

    // ── 렌더링 ────────────────────────────────────────────────────────────

    private void draw() {
        int nCols = xs.length - 1;
        int nRows = ys.length - 1;

        // 가로 테두리
        for (int r = 0; r < ys.length; r++) {
            for (int c = 0; c < nCols; c++) {
                BorderRenderer.Side side = hEdge[r][c];
                if (!nonEmpty(side)) continue;
                drawHorizontal(side, r, c);
            }
        }
        // 세로 테두리
        for (int c = 0; c < xs.length; c++) {
            for (int r = 0; r < nRows; r++) {
                BorderRenderer.Side side = vEdge[c][r];
                if (!nonEmpty(side)) continue;
                drawVertical(side, c, r);
            }
        }
    }

    private void drawHorizontal(BorderRenderer.Side side, int r, int c) {
        int y = ys[r];
        int xa = xs[c], xb = xs[c + 1];
        if (side.dashed) {
            // 위상 원점은 이 가로 격자선의 왼쪽 끝 — 열마다 리셋하면 점 길이가 어긋난다.
            BorderRenderer.drawDashedLine(g, side, xa, y, xb, y, hSpanLeft[r]);
            return;
        }
        boolean continuesLeft = c > 0 && nonEmpty(hEdge[r][c - 1]);
        boolean continuesRight = c < xs.length - 2 && nonEmpty(hEdge[r][c + 1]);
        int center = side.centerIndex();

        for (int i = 0; i < side.plyCount(); i++) {
            BorderRenderer.Ply p = side.plies.get(i);
            double sy = y + p.axisOffset();
            boolean centeredPly = i == center && isAxisCentered(p);
            double xL = hPlyEnd(xa, sy, p, centeredPly, side.plyCount(), c, r,
                    continuesLeft, continuesLeft ? hEdge[r][c - 1] : null,
                    vSpanTop[c], vSpanBot[c], true);
            double xR = hPlyEnd(xb, sy, p, centeredPly, side.plyCount(), c + 1, r,
                    continuesRight, continuesRight ? hEdge[r][c + 1] : null,
                    vSpanTop[c + 1], vSpanBot[c + 1], false);
            fillBar(side.color, xL, sy - p.width() / 2.0, xR - xL, p.width());
        }
    }

    /**
     * 가로 가닥의 세로 격자선 {@code cLine} 쪽 끝 좌표 — 한글 측정 규칙(세로 우선):
     *
     * <ul>
     *   <li><b>中央 대칭 가닥</b>(삼중선 중앙·단일선)은 세로(위/아래)의 中央 주 가닥과
     *       결합(join)해 그 먼 모서리까지 그리고, 반대편 가로 세그먼트도 中央 주 가닥을
     *       가질 때만 전체 관통한다(삼중선×삼중선 전관통; 표7·표8 전환은 中央 결합까지).</li>
     *   <li>그 외 가닥은 <b>같은 쪽</b>(격자선 −/+쪽) 세로 패턴의 첫 간격에서 끊긴다 —
     *       단 그 간격이 <b>반대쪽 세로에서도 흰색으로 이어질 때만</b>(흰 채널: 표2·6·7·8).
     *       채널이 반대쪽 가닥에 막히면(전환 교차: 표5 Solid×Double·표6 ThickThin↔ThinThick)
     *       끊지 않고 위/아래 세로 폭의 바깥 끝까지 관통한다. 전환 교차점(위·아래 세로의
     *       가닥 수가 다름)에서는 추가로 <b>가로 가닥 수가 간격 쪽 세로와 같을 때만</b> 끊긴다
     *       (표8 r0/r1 삼중선×삼중선 끊김 vs 표7 r3/r4 ThickThin×삼중선 관통).</li>
     * </ul>
     */
    private double hPlyEnd(int edge, double sy, BorderRenderer.Ply p, boolean centeredPly,
                           int hPlyCount, int cLine, int r,
                           boolean continuesBeyond, BorderRenderer.Side beyondSeg,
                           int spanLo, int spanHi, boolean atStart) {
        BorderRenderer.Side above = r > 0 ? vEdge[cLine][r - 1] : null;
        BorderRenderer.Side below = r < ys.length - 1 ? vEdge[cLine][r] : null;
        BorderRenderer.Side same = p.axisOffset() < 0 ? above : below;
        BorderRenderer.Side other = p.axisOffset() < 0 ? below : above;

        if (centeredPly) {
            BorderRenderer.Side centered = hasCenteredMain(above) ? above
                    : hasCenteredMain(below) ? below : null;
            if (centered != null) {
                boolean full = continuesBeyond && hasCenteredMain(beyondSeg);
                return perpEnd(edge, sy, centered, spanLo, spanHi, true, full, false, atStart);
            }
        }

        BorderRenderer.Side perp = nonEmpty(same) ? same : other;
        if (!nonEmpty(perp) || perp.dashed) return edge;
        // 표 가장자리 바깥 가닥은 바깥 모서리까지 연장(중첩 사각형) — 기존 모서리 규칙.
        if (sy < spanLo - 0.5 || sy > spanHi + 0.5) {
            return edge + (atStart ? perp.spanStart() : perp.spanEnd());
        }
        // 가로가 여기서 끝나고(T자) 세로가 다른 패턴으로 전환하며 관통하면: 같은 쪽 세로의
        // 主 가닥(없으면 가장 가까운 가닥)에 결합 — 표8 오른쪽 바깥 r0/r1·r3/r4 측정.
        // 균일 T자(외곽 테두리, 표2~4)는 아래 채널 규칙(안쪽 첫 가닥에서 끊김)이 맞으므로
        // 제외 — 主 결합으로 하면 흰 간격이 메워진다. 모서리(세로 한쪽만)도 기존 규칙.
        if (!continuesBeyond && nonEmpty(same) && nonEmpty(other)
                && same.plyCount() != other.plyCount()) {
            return perpEnd(edge, sy, same, spanLo, spanHi, same.centerIndex() >= 0, false, true, atStart);
        }
        boolean clipEligible = !nonEmpty(other)
                || other.plyCount() == (nonEmpty(same) ? same.plyCount() : -1)
                || (nonEmpty(same) && hPlyCount == same.plyCount());
        if (nonEmpty(same) && clipEligible && gapChannelOpen(same, other, atStart)) {
            return perpEnd(edge, sy, same, spanLo, spanHi, false, false, true, atStart);
        }
        // 관통: 위/아래 세로 폭을 합친 바깥 끝까지.
        double ext = atStart ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        if (nonEmpty(above)) {
            ext = atStart ? Math.min(ext, above.spanStart()) : Math.max(ext, above.spanEnd());
        }
        if (nonEmpty(below)) {
            ext = atStart ? Math.min(ext, below.spanStart()) : Math.max(ext, below.spanEnd());
        }
        return edge + ext;
    }

    /**
     * {@code same}(가닥과 같은 쪽 세로)의 접근 방향 첫 간격이 {@code other}(반대쪽 세로)
     * 에서도 흰색인가 — other 가 없으면 열림, other 의 가닥이 간격 구간과 겹치면 막힘.
     */
    private static boolean gapChannelOpen(BorderRenderer.Side same, BorderRenderer.Side other,
                                          boolean atStart) {
        int n = same.plyCount();
        if (n < 2) return false; // 간격 없음(단일 가닥) → 끊을 흰 채널이 없다
        BorderRenderer.Ply a = atStart ? same.plies.get(n - 2) : same.plies.get(0);
        BorderRenderer.Ply b = atStart ? same.plies.get(n - 1) : same.plies.get(1);
        double lo = a.axisOffset() + a.width() / 2.0;
        double hi = b.axisOffset() - b.width() / 2.0;
        if (!nonEmpty(other) || other.dashed) return true;
        for (BorderRenderer.Ply q : other.plies) {
            double qLo = q.axisOffset() - q.width() / 2.0;
            double qHi = q.axisOffset() + q.width() / 2.0;
            if (qLo < hi - 0.25 && qHi > lo + 0.25) return false;
        }
        return true;
    }

    /**
     * 세로 가닥이 가로 격자선 {@code rLine}과 만날 때 기준이 되는 가로 Side —
     * 가닥이 격자선의 왼(−)쪽이면 왼 세그먼트, 오른(+)쪽이면 오른 세그먼트.
     */
    private BorderRenderer.Side perpForVerticalPly(double axisOffset, int rLine, int c) {
        BorderRenderer.Side leftS = c > 0 ? hEdge[rLine][c - 1] : null;
        BorderRenderer.Side rightS = c < xs.length - 1 ? hEdge[rLine][c] : null;
        BorderRenderer.Side preferred = axisOffset < 0 ? leftS : rightS;
        BorderRenderer.Side other = axisOffset < 0 ? rightS : leftS;
        return nonEmpty(preferred) ? preferred : other;
    }

    /** 가닥이 격자선 중심에 대칭으로 걸쳐 있는가(원래 단일선 위치). */
    private static boolean isAxisCentered(BorderRenderer.Ply p) {
        return Math.abs(p.axisOffset()) < 0.26;
    }

    /** 분리선에 격자선 중심 대칭의 주(유일 최대폭) 가닥이 있는가(삼중선 중앙·단일선). */
    private static boolean hasCenteredMain(BorderRenderer.Side s) {
        if (!nonEmpty(s) || s.dashed) return false;
        int ci = s.centerIndex();
        return ci >= 0 && isAxisCentered(s.plies.get(ci));
    }

    private void drawVertical(BorderRenderer.Side side, int c, int r) {
        int x = xs[c];
        int ya = ys[r], yb = ys[r + 1];
        if (side.dashed) {
            // 위상 원점은 이 세로 격자선의 위쪽 끝 — 행마다 리셋하면 점 길이가 어긋난다.
            BorderRenderer.drawDashedLine(g, side, x, ya, x, yb, vSpanTop[c]);
            return;
        }
        boolean adjUp = r > 0 && nonEmpty(vEdge[c][r - 1]);
        boolean adjDown = r < ys.length - 2 && nonEmpty(vEdge[c][r + 1]);
        // 같은 가닥 수로 이어질 때만 연속선(관통). 가닥 수가 다른 전환 교차점은 가닥별
        // rank 결합(rankJoin) — 한글 측정(표7 r0/r1: ThickThin→삼중선).
        int n = side.plyCount();
        boolean sameUp = adjUp && vEdge[c][r - 1].plyCount() == n;
        boolean sameDown = adjDown && vEdge[c][r + 1].plyCount() == n;
        // 세로 가닥은 자기 쪽 가로 세그먼트와 <b>타입(가닥 폭 시퀀스)이 같을 때</b> 그 가로의
        // 간격 행에서 끊긴다(windowpane 맞물림). count-match 가 아니라 타입 일치 — 한글 측정:
        // 표2·3(균일)은 비대칭 主(thick)도 끊김(간격 행이 표 폭 전체 흰색), 표6 0/1열은
        // v ThickThin × h ThinThick(미러)라 thick 연속, 1/2열은 h ThickThin(동일)이라 끊김.
        boolean lT = c > 0 && interlocks(hEdge[r][c - 1], side);
        boolean rT = c < xs.length - 1 && interlocks(hEdge[r][c], side);
        boolean lB = c > 0 && interlocks(hEdge[r + 1][c - 1], side);
        boolean rB = c < xs.length - 1 && interlocks(hEdge[r + 1][c], side);
        int center = side.centerIndex();

        for (int i = 0; i < side.plyCount(); i++) {
            BorderRenderer.Ply p = side.plies.get(i);
            double sx = x + p.axisOffset();
            boolean isCenter = i == center;
            // 中央 대칭 主(삼중선 中央·단일선)만 간격 행을 관통(표4) — 비대칭 主는 예외 없음.
            boolean passT = isCenter && isAxisCentered(p);
            boolean passB = passT;
            // 직교 기하는 가닥이 있는 쪽(격자선의 −/+쪽) 세그먼트 패턴 기준 — 한글 측정
            // (표6: 세로 얇은 가닥이 자기 쪽(오른쪽) 가로 패턴의 간격 행에서 끊김).
            BorderRenderer.Side topH = perpForVerticalPly(p.axisOffset(), r, c);
            BorderRenderer.Side botH = perpForVerticalPly(p.axisOffset(), r + 1, c);
            // 가닥이 격자선의 왼쪽(−x)이면 왼 세그먼트가, 오른쪽이면 오른 세그먼트가 가로막는다.
            boolean blockedT = p.axisOffset() < 0 ? lT : rT;
            boolean blockedB = p.axisOffset() < 0 ? lB : rB;
            double yT = adjUp && !sameUp
                    ? rankJoin(ya, side, i, p.axisOffset(), r, c, true)
                    : perpEnd(ya, sx, topH, hSpanLeft[r], hSpanRight[r],
                            passT, sameUp, blockedT, true);
            double yB = adjDown && !sameDown
                    ? rankJoin(yb, side, i, p.axisOffset(), r + 1, c, false)
                    : perpEnd(yb, sx, botH, hSpanLeft[r + 1], hSpanRight[r + 1],
                            passB, sameDown, blockedB, false);
            fillBar(side.color, sx - p.width() / 2.0, yT, p.width(), yB - yT);
        }
    }

    /**
     * 전환 교차점(인접 세로 세그먼트의 가닥 수가 다름)에서 세로 가닥 {@code i}의 끝 좌표 —
     * 가닥 수가 같은 쪽 가로 패턴의 <b>같은 rank 가닥</b> 먼 모서리에 결합한다(한글 측정,
     * 표7 r0/r1: 삼중선 세그먼트의 바깥 thin→가로 thin 띠 바깥(y483), 中央→가로 中央
     * 상단(y487), ThickThin 세그먼트는 같은 수(2)의 ThickThin 쪽과 rank 결합). 매칭되는
     * 쪽이 없으면 가장 가까운 가닥의 먼 모서리(기존 T자 처리).
     */
    private double rankJoin(int edge, BorderRenderer.Side side, int i, double axisOffset,
                            int rLine, int c, boolean atStart) {
        BorderRenderer.Side leftS = c > 0 ? hEdge[rLine][c - 1] : null;
        BorderRenderer.Side rightS = c < xs.length - 1 ? hEdge[rLine][c] : null;
        BorderRenderer.Side own = axisOffset < 0 ? leftS : rightS;
        int n = side.plyCount();
        BorderRenderer.Side perp = nonEmpty(own) && own.plyCount() == n ? own
                : nonEmpty(leftS) && leftS.plyCount() == n ? leftS
                : nonEmpty(rightS) && rightS.plyCount() == n ? rightS
                : null;
        BorderRenderer.Ply np;
        if (perp != null) {
            // frameRank 페어링(중첩 사각형 모서리). 매칭 박스가 세로의 오른쪽이면(perp가
            // 오른쪽 세그먼트) 세로는 박스의 왼쪽 변(plies[0]=바깥), 왼쪽 박스면 오른쪽 변
            // (plies[last]=바깥). 가로 띠는 박스 위 경계(atStart)면 plies[0]=바깥, 아래
            // 경계면 plies[last]=바깥 — 표7 r0/r1·r3/r4 양쪽 세로(0/1열·1/2열) 측정.
            boolean boxToRight = perp == rightS;
            np = perp.plies.get(atStart == boxToRight ? i : n - 1 - i);
        } else {
            perp = nonEmpty(own) ? own : nonEmpty(leftS) ? leftS : rightS;
            if (!nonEmpty(perp) || perp.dashed) return edge;
            np = atStart ? perp.plies.get(perp.plyCount() - 1) : perp.plies.get(0);
        }
        return atStart ? edge + np.axisOffset() - np.width() / 2.0
                       : edge + np.axisOffset() + np.width() / 2.0;
    }

    /**
     * 한 가닥이 직교 테두리 {@code perp}(격자선 좌표 {@code edge})를 만나는 끝 좌표.
     *
     * @param sCoord    이 가닥의 직교축 위치(세그먼트와 수직인 좌표). perp의 범위
     *                  [{@code spanLo},{@code spanHi}] 밖이면 바깥 모서리 처리.
     * @param atStart   세그먼트가 perp의 +쪽(이 끝에서 −방향으로 연장)이면 true.
     * @param continues 직교 테두리 반대편에 같은 방향 세그먼트가 이어지면 true(중앙 가닥 관통·연결).
     * @param blocked   이 가닥과 같은 쪽(격자선의 − 또는 +쪽)에 가닥 수가 같은 직교 테두리가 있어
     *                  windowpane 으로 끊겨야 하면 true. T자/가닥수 불일치 쪽은 false라 이어진다.
     */
    private double perpEnd(int edge, double sCoord, BorderRenderer.Side perp,
                           int spanLo, int spanHi, boolean isCenter,
                           boolean continues, boolean blocked, boolean atStart) {
        if (!nonEmpty(perp) || perp.dashed) return edge;

        double outerEdge = atStart ? edge + perp.spanStart() : edge + perp.spanEnd();

        // perp의 직교 범위 밖(표 가장자리) → 바깥 모서리까지 연장 (중첩 사각형 모서리).
        if (sCoord < spanLo - 0.5 || sCoord > spanHi + 0.5) return outerEdge;

        if (isCenter) {
            if (continues) return outerEdge;     // 주 가닥은 직교 분리선 관통
            int ci = perp.centerIndex();          // 경계: perp 의 주 가닥까지
            if (ci < 0) return edge;
            BorderRenderer.Ply cp = perp.plies.get(ci);
            return atStart ? edge + cp.axisOffset() - cp.width() / 2.0
                           : edge + cp.axisOffset() + cp.width() / 2.0;
        }

        if (!blocked) return outerEdge;           // T자 교차/가닥수 불일치 → 끊기지 않고 이어짐

        // 얇은(바깥) 가닥이 막힘: 세그먼트에서 가장 가까운 perp 가닥의 먼 쪽 모서리까지(windowpane)
        BorderRenderer.Ply np = atStart
                ? perp.plies.get(perp.plyCount() - 1)   // +x/+y쪽 세그먼트 → 가장 +쪽 가닥
                : perp.plies.get(0);                    // −쪽 세그먼트 → 가장 −쪽 가닥
        return atStart ? edge + np.axisOffset() - np.width() / 2.0
                       : edge + np.axisOffset() + np.width() / 2.0;
    }

    private static boolean nonEmpty(BorderRenderer.Side s) {
        return s != null && !s.isEmpty();
    }

    /**
     * 직교 테두리 {@code s}가 {@code side}와 <b>타입이 같은가</b>(가닥 폭 시퀀스 일치) —
     * windowpane 이 맞물려 가닥이 끊기는 조건. ThinThick×ThickThin 처럼 가닥 수만 같고
     * 폭 순서가 거울상이면 안 맞물린다(표6 0/1열 측정).
     */
    private static boolean interlocks(BorderRenderer.Side s, BorderRenderer.Side side) {
        if (!nonEmpty(s) || s.dashed || s.plyCount() != side.plyCount()) return false;
        for (int i = 0; i < s.plyCount(); i++) {
            if (Math.abs(s.plies.get(i).width() - side.plies.get(i).width()) > 0.5) return false;
        }
        return true;
    }

    /**
     * 양 끝 경계를 각각 반올림해 채운다. 시작점·폭을 따로 반올림하면(round(x)+round(w))
     * 끝 경계가 1px 밀려 직교 간격을 침범할 수 있다(표7 좌 ThickThin이 세로 삼중선 간격을
     * 1px 침범하던 버그). 정수 폭 가닥의 배치는 동일하고 클립된 바의 끝만 정확해진다.
     */
    private void fillBar(Color color, double x, double y, double w, double h) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int iw = (int) Math.round(x + w) - ix;
        int ih = (int) Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;
        g.setColor(color);
        g.fillRect(ix, iy, iw, ih);
    }

    private static int[] toArray(TreeSet<Integer> set) {
        int[] a = new int[set.size()];
        int i = 0;
        for (int v : set) a[i++] = v;
        return a;
    }

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }
}
