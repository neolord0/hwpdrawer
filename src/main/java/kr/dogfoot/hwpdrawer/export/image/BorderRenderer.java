package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BackSlashDiagonalShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderFillProperty;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;
import kr.dogfoot.hwplib.object.docinfo.borderfill.SlashDiagonalShape;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * 표 셀 테두리를 그린다.
 *
 * <p>HWP의 다중선 테두리({@link BorderType#Double 이중선}, {@link BorderType#ThinThick},
 * {@link BorderType#ThickThin}, {@link BorderType#ThinThickThin 삼중선})는 한글과 동일하게
 * <b>중첩된 사각형(nested rectangle)</b>으로 렌더링한다. 각 가닥(ply)을 셀 네 모서리에서
 * 직교하는 변의 같은 순위 가닥과 만나도록 연장(miter)해, 모서리·교차점에서 선이 어긋나
 * 엉키지 않고 자연스러운 격자(windowpane)를 이룬다.</p>
 *
 * <p><b>절대 방향(absolute orientation):</b> 가닥은 변 중심선을 기준으로 좌표축의
 * 음(−)→양(+) 방향(가로변은 위→아래, 세로변은 왼→오른)으로 배치한다. 비대칭 다중선
 * (ThinThick/ThickThin)도 "이름 그대로" −쪽이 첫 가닥이다 — 예: ThinThick은 −쪽이 thin,
 * +쪽이 thick. 이는 변을 그리는 셀과 무관하므로, 두 인접 셀이 공유 격자선을 양쪽에서
 * 다시 그려도 같은 픽셀을 칠한다(겹쳐도 동일 → 교차선이 메워지는 현상 없음). 이 절대
 * 방향은 한글 캡처 측정과도 일치한다(아래 변 바깥쪽에 thick, 위 변 바깥쪽에 thin).</p>
 */
final class BorderRenderer {
    private BorderRenderer() {}

    /** 표 셀의 네 변 테두리를 중첩 사각형으로 그린다. 점선 위상 원점은 셀 자신이다(표 밖 단독 호출용). */
    static void drawCellBorders(Graphics2D g, BorderFill bf, int x1, int y1, int x2, int y2) {
        drawCellBorders(g, bf, x1, y1, x2, y2, null, null);
    }

    /**
     * 표 셀의 네 변 테두리를 중첩 사각형으로 그린다. (x1,y1)-(x2,y2)는 셀 영역 픽셀.
     *
     * <p>{@code hRuns}/{@code vRuns}는 <b>점선 위상의 원점</b>을 알려준다 — 셀마다 자기 좌표에서
     * 패턴을 새로 시작하면 한 격자선이 여러 위상으로 그려져 점 길이가 들쭉날쭉해진다
     * ({@link DashRunOrigins} 참고). null이면 셀 자신을 원점으로 쓴다.</p>
     */
    static void drawCellBorders(Graphics2D g, BorderFill bf, int x1, int y1, int x2, int y2,
                                DashRunOrigins hRuns, DashRunOrigins vRuns) {
        if (bf == null) return;
        Side top    = nonWhite(Side.of(bf.getTopBorder()));
        Side bottom = nonWhite(Side.of(bf.getBottomBorder()));
        Side left   = nonWhite(Side.of(bf.getLeftBorder()));
        Side right  = nonWhite(Side.of(bf.getRightBorder()));

        // outerAtStart: 변의 "바깥쪽"이 −축(위/왼, plies[0]) 쪽이면 true.
        //  top·left = 바깥이 −축(true), bottom·right = 바깥이 +축(false).
        drawHorizontalSide(g, top,    left, right, x1, x2, y1, true,  origin(hRuns, y1, x1, top));
        drawHorizontalSide(g, bottom, left, right, x1, x2, y2, false, origin(hRuns, y2, x1, bottom));
        drawVerticalSide(g,   left,   top, bottom, y1, y2, x1, true,  origin(vRuns, x1, y1, left));
        drawVerticalSide(g,   right,  top, bottom, y1, y2, x2, false, origin(vRuns, x2, y1, right));
    }

    /** 이 변이 속한 점선 run의 시작 좌표. run 정보가 없으면 세그먼트 자신의 시작. */
    private static int origin(DashRunOrigins runs, int line, int segStart, Side side) {
        if (runs == null || !side.dashed) return segStart;
        return runs.originAt(line, segStart, DashRunOrigins.styleOf(side), segStart);
    }

    /** 셀 네 변 중 점선인 변을 {@code hRuns}/{@code vRuns}에 등록한다. */
    static void collectDashRuns(BorderFill bf, int x1, int y1, int x2, int y2,
                                DashRunOrigins hRuns, DashRunOrigins vRuns) {
        if (bf == null) return;
        addRun(hRuns, y1, x1, x2, nonWhite(Side.of(bf.getTopBorder())));
        addRun(hRuns, y2, x1, x2, nonWhite(Side.of(bf.getBottomBorder())));
        addRun(vRuns, x1, y1, y2, nonWhite(Side.of(bf.getLeftBorder())));
        addRun(vRuns, x2, y1, y2, nonWhite(Side.of(bf.getRightBorder())));
    }

    private static void addRun(DashRunOrigins runs, int line, int from, int to, Side side) {
        if (side.dashed) runs.add(line, from, to, DashRunOrigins.styleOf(side));
    }

    /**
     * 흰색 테두리는 그리지 않는다(없는 변으로 취급).
     *
     * <p>셀은 저마다 자기 네 변을 그리므로, 두 셀이 맞닿은 변은 두 번 그려진다. 나중에
     * 그려지는 셀의 <b>흰색</b> 변이 먼저 그려진 이웃의 검은 변을 지워 버린다.
     * 실문서 R03 page12 '표 양식' 표가 그 예다 — '휴먼명조, 13~14p' 셀(bf30)은 네 변이
     * 모두 Dot 인데, 바로 아래 '임의설정 ↓' 셀(bf6)의 위 변이 {@code Solid FFFFFF} 라
     * 아래쪽 점선이 통째로 지워졌다.</p>
     *
     * <p>한글은 이 경우 검은 점선을 남긴다. 격자(다중선) 경로인 {@link TableBorderRenderer}
     * 에도 이미 "검정만 칠하고 흰색은 안 덮어 배경 보존" 규칙이 있는데, 셀별 경로에만
     * 빠져 있었다. 배경은 테두리보다 먼저 칠해지므로 흰 변을 그리지 않아도 잃는 것이 없다.</p>
     */
    private static Side nonWhite(Side s) {
        if (s != null && s.color != null && s.color.getRGB() == Color.WHITE.getRGB()) {
            return new Side();   // 빈 Side = 그리지 않음
        }
        return s;
    }

    // ── 셀 대각선 ────────────────────────────────────────────────────────

    /**
     * 표 셀의 대각선(슬래시 "/" · 백슬래시 "\")을 그린다. (x1,y1)-(x2,y2)는 셀 영역 픽셀.
     *
     * <p>대각선의 <b>모양</b>은 {@link BorderFillProperty}의 slash/backSlash 값이, 선
     * <b>종류·두께·색</b>은 {@link BorderFill#getDiagonalBorder()}가 정한다. 두 정보 중
     * 하나라도 없으면(None) 그리지 않는다.</p>
     *
     * <p>모서리에서 마주보는 모서리까지 가는 온전한 대각선(Slash/BackSlash) 외에, 한 모서리
     * 에서 마주보는 <b>변의 중점</b>으로 뻗는 변형이 있다 — 셀을 3분할하는 머리 셀에서 두 줄이
     * 부채꼴로 갈라지는 형태(…ToBottomEdge / …ToLeftEdge / 둘 다인 …ToBottomLeftEdge).
     * 180° 회전 플래그는 셀 중심 기준 점대칭이다.</p>
     */
    static void drawCellDiagonals(Graphics2D g, BorderFill bf, int x1, int y1, int x2, int y2) {
        if (bf == null) return;
        BorderFillProperty prop = bf.getProperty();
        EachBorder diagonal = bf.getDiagonalBorder();
        if (prop == null || diagonal == null) return;
        BorderType type = diagonal.getType();
        if (type == null || type == BorderType.None) return;

        List<int[]> lines = new ArrayList<>();
        addSlashLines(lines, prop, x1, y1, x2, y2);
        addBackSlashLines(lines, prop, x1, y1, x2, y2);
        if (lines.isEmpty()) return;

        Color savedColor = g.getColor();
        Stroke savedStroke = g.getStroke();
        g.setColor(colorOf(diagonal));
        g.setStroke(StrokeFactory.forParaBorder(type,
                (float) scaledSingleThickness(diagonal.getThickness()),
                (float) (UnitConverter.zoomRate() / 100.0)));
        try {
            for (int[] l : lines) g.drawLine(l[0], l[1], l[2], l[3]);
        } finally {
            g.setColor(savedColor);
            g.setStroke(savedStroke);
        }
    }

    /** 슬래시("/") 계열: 오른쪽 위 모서리에서 뻗는다. */
    private static void addSlashLines(List<int[]> out, BorderFillProperty prop,
                                      int x1, int y1, int x2, int y2) {
        SlashDiagonalShape shape = prop.getSlashDiagonalShape();
        if (shape == null || shape == SlashDiagonalShape.None) return;

        boolean rotated = prop.isRotateSlashDiagonal180();
        int midX = (x1 + x2) / 2, midY = (y1 + y2) / 2;
        switch (shape) {
            case Slash:
                addDiagonal(out, rotated, x1, y1, x2, y2, x2, y1, x1, y2);
                break;
            case RightTopToBottomEdge:
                addDiagonal(out, rotated, x1, y1, x2, y2, x2, y1, midX, y2);
                break;
            case RightTopToLeftEdge:
                addDiagonal(out, rotated, x1, y1, x2, y2, x2, y1, x1, midY);
                break;
            case RightTopToBottomLeftEdge:
                addDiagonal(out, rotated, x1, y1, x2, y2, x2, y1, midX, y2);
                addDiagonal(out, rotated, x1, y1, x2, y2, x2, y1, x1, midY);
                break;
            default:
                break;
        }
    }

    /** 백슬래시("\") 계열: 왼쪽 위 모서리에서 뻗는다. */
    private static void addBackSlashLines(List<int[]> out, BorderFillProperty prop,
                                          int x1, int y1, int x2, int y2) {
        BackSlashDiagonalShape shape = prop.getBackSlashDiagonalShape();
        if (shape == null || shape == BackSlashDiagonalShape.None) return;

        boolean rotated = prop.isRotateBackSlashDiagonal180();
        int midX = (x1 + x2) / 2, midY = (y1 + y2) / 2;
        switch (shape) {
            case BackSlash:
                addDiagonal(out, rotated, x1, y1, x2, y2, x1, y1, x2, y2);
                break;
            case LeftTopToBottomEdge:
                addDiagonal(out, rotated, x1, y1, x2, y2, x1, y1, midX, y2);
                break;
            case LeftTopToRightEdg:
                addDiagonal(out, rotated, x1, y1, x2, y2, x1, y1, x2, midY);
                break;
            case LeftTopToBottomRightEdge:
                addDiagonal(out, rotated, x1, y1, x2, y2, x1, y1, midX, y2);
                addDiagonal(out, rotated, x1, y1, x2, y2, x1, y1, x2, midY);
                break;
            default:
                break;
        }
    }

    /** 선분 하나를 추가한다. rotated면 셀 중심 기준 점대칭(180° 회전)한 위치로 넣는다. */
    private static void addDiagonal(List<int[]> out, boolean rotated,
                                    int x1, int y1, int x2, int y2,
                                    int sx, int sy, int ex, int ey) {
        if (rotated) {
            int sumX = x1 + x2, sumY = y1 + y2;
            sx = sumX - sx; sy = sumY - sy;
            ex = sumX - ex; ey = sumY - ey;
        }
        out.add(new int[]{sx, sy, ex, ey});
    }

    /**
     * 가로 변(top/bottom)을 그린다. 가닥 위치는 절대 방향(yc = edgeY + axisOffset)이라
     * top·bottom 이 같은 공유선을 같은 픽셀에 칠한다.
     *
     * @param outerAtStart 이 변의 바깥쪽이 −y(위)면 true(top), +y(아래)면 false(bottom)
     */
    private static void drawHorizontalSide(Graphics2D g, Side side, Side leftPerp, Side rightPerp,
                                           int x1, int x2, int edgeY, boolean outerAtStart,
                                           int phaseOriginX) {
        if (side.isEmpty()) return;
        if (side.dashed) {
            drawDashedLine(g, side, x1, edgeY, x2, edgeY, phaseOriginX);
            return;
        }
        int n = side.plies.size();
        for (int i = 0; i < n; i++) {
            Ply p = side.plies.get(i);
            int frameRank = outerAtStart ? i : (n - 1 - i);   // 바깥에서 몇 번째 사각형 테두리인가
            double yc = edgeY + p.axisOffset;
            double xL = perpOuterEdge(leftPerp,  frameRank, x1, true,  -1); // 왼쪽 변: 바깥=−x
            double xR = perpOuterEdge(rightPerp, frameRank, x2, false, +1); // 오른쪽 변: 바깥=+x
            if ((int) Math.round(xR - xL) <= 0) continue;
            fillBar(g, side.color, xL, yc - p.width / 2.0, xR - xL, p.width);
        }
    }

    /**
     * 세로 변(left/right)을 그린다.
     *
     * @param outerAtStart 이 변의 바깥쪽이 −x(왼)면 true(left), +x(오른)면 false(right)
     */
    private static void drawVerticalSide(Graphics2D g, Side side, Side topPerp, Side bottomPerp,
                                         int y1, int y2, int edgeX, boolean outerAtStart,
                                         int phaseOriginY) {
        if (side.isEmpty()) return;
        if (side.dashed) {
            drawDashedLine(g, side, edgeX, y1, edgeX, y2, phaseOriginY);
            return;
        }
        int n = side.plies.size();
        for (int i = 0; i < n; i++) {
            Ply p = side.plies.get(i);
            int frameRank = outerAtStart ? i : (n - 1 - i);
            double xc = edgeX + p.axisOffset;
            double yT = perpOuterEdge(topPerp,    frameRank, y1, true,  -1); // 위 변: 바깥=−y
            double yB = perpOuterEdge(bottomPerp, frameRank, y2, false, +1); // 아래 변: 바깥=+y
            if ((int) Math.round(yB - yT) <= 0) continue;
            fillBar(g, side.color, xc - p.width / 2.0, yT, p.width, yB - yT);
        }
    }

    /**
     * 직교 변(perp)에서 바깥쪽으로 frameRank 번째 가닥의 바깥 끝 좌표.
     * 그 가닥과 만나도록 현재 변의 가닥을 연장(miter)해 중첩 사각형 모서리를 만든다.
     * perp 가닥이 없으면(None) 셀 모서리에서 멈춘다.
     *
     * @param perpOuterAtStart perp 변의 바깥쪽이 plies[0](−축)이면 true
     * @param outwardSign      바깥쪽 부호(−1: 위/왼, +1: 아래/오른)
     */
    private static double perpOuterEdge(Side perp, int frameRank, int edge,
                                        boolean perpOuterAtStart, int outwardSign) {
        int n = perp.plies.size();
        if (n == 0) return edge;
        int idx = perpOuterAtStart ? frameRank : (n - 1 - frameRank);
        if (idx < 0) idx = 0;
        else if (idx >= n) idx = n - 1;
        Ply p = perp.plies.get(idx);
        return edge + p.axisOffset + outwardSign * p.width / 2.0;
    }

    private static void fillBar(Graphics2D g, Color color, double x, double y, double w, double h) {
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int iw = Math.max(1, (int) Math.round(w));
        int ih = Math.max(1, (int) Math.round(h));
        g.setColor(color);
        g.fillRect(ix, iy, iw, ih);
    }

    /**
     * 점선/파선 변을 그린다. 실선과 같은 {@link #fillBar}(픽셀 정렬 채우기)로 칠한다.
     *
     * <p>이전에는 {@code g.drawLine} + dash Stroke였는데, 안티에일리어싱 때문에 점 하나가
     * 패턴보다 1px 길게·선이 두께보다 1행 두껍게 번졌다(패턴 {3,4}가 화면에서 4/3으로,
     * 두께 2px가 3행으로). 한글은 점 경계가 픽셀에 딱 맞으므로 실선과 같은 채우기를 쓴다.</p>
     */
    static void drawDashedLine(Graphics2D g, Side side, int x1, int y1, int x2, int y2,
                               double phaseOrigin) {
        // 둥근 끝(CircleDot)은 캡의 반원이 점 모양 자체다 — 사각 채우기로 바꾸면 뭉개진다.
        if (side.dashStroke.getEndCap() == BasicStroke.CAP_ROUND) {
            g.setColor(side.color);
            g.setStroke(side.dashStroke);
            g.drawLine(x1, y1, x2, y2);
            return;
        }
        boolean horizontal = y1 == y2;
        double thickness = side.dashStroke.getLineWidth();
        double crossStart = (horizontal ? y1 : x1) - thickness / 2.0;
        double from = horizontal ? x1 : y1;
        double to = horizontal ? x2 : y2;

        // 가로 점선은 왼쪽 세로 테두리의 <b>바깥</b> 모서리에서 패턴이 시작한다 — 한글은 모서리를
        // 가로선이 차지한다. 원점을 두께의 절반만큼 왼쪽으로 당긴다(t=1이면 보정 0).
        // 실측 11줄(실문서 R03 p5 H130·H319·H320·H901·H1072, p6 H605·H784·H785, p13 H198·H1239·H1240)에서
        // 우리 시작 x가 한글보다 정확히 +1이었고, 보정 후 일치한다.
        // 세로는 반대 부호(한글이 안쪽에서 시작)로 관측됐으나 표본이 1줄뿐이라 보정하지 않는다.
        double origin = horizontal ? phaseOrigin - Math.floor(thickness / 2.0) : phaseOrigin;

        for (double[] seg : dashSegments(origin, from, to, side.dashStroke.getDashArray())) {
            double len = seg[1] - seg[0];
            if (horizontal) fillBar(g, side.color, seg[0], crossStart, len, thickness);
            else            fillBar(g, side.color, crossStart, seg[0], thickness, len);
        }
    }

    /**
     * {@code origin}에서 시작한 dash 패턴(켜짐/꺼짐 길이 교대)을 {@code [from, to)}로 잘라
     * "켜짐" 구간들을 만든다.
     *
     * <p><b>위상 원점을 세그먼트 시작이 아니라 격자선 전체의 시작으로 잡는 것이 핵심이다.</b>
     * 한글은 격자선 하나를 셀 경계·병합셀 경계와 무관하게 하나의 위상으로 그린다(실문서 R03 p13
     * ok: 점 107개가 전부 (x−122)%7 == 0, 열 경계 188·804를 그대로 통과). 우리는 셀마다
     * 위상을 리셋했는데, 하나의 격자선은 위 셀의 bottom·아래 셀들의 top으로 두 번 이상 그려지므로
     * 위상이 어긋난 점열이 겹쳐 <b>점 길이가 구간마다 달라졌다</b> — 실문서 R04 p1 y=651에서
     * 원점 99(mod 3 = 0)와 원점 274(mod 3 = 1)가 겹쳐 x≥273 구간의 점이 1px→2px가 됐고,
     * 실문서 R03 p13 y=198에서는 위상차 2 때문에 3px→5px가 됐다.</p>
     */
    private static List<double[]> dashSegments(double origin, double from, double to, float[] pattern) {
        List<double[]> segments = new ArrayList<>();
        double period = 0;
        for (float p : pattern) period += p;
        if (period <= 0) return segments;

        // from을 품는 주기의 시작으로 되감는다. 패턴 길이가 짝수라 주기 경계는 항상 "켜짐"부터다.
        double pos = origin + Math.floor((from - origin) / period) * period;
        for (int i = 0; pos < to; i++) {
            double next = pos + pattern[i % pattern.length];
            if (i % 2 == 0) {
                double on = Math.max(pos, from), off = Math.min(next, to);
                if (off > on) segments.add(new double[]{on, off});
            }
            pos = next;
        }
        return segments;
    }

    // ── 한 변(Side) 모델 ─────────────────────────────────────────────────

    /**
     * 한 변의 가닥(ply) 목록. 좌표축 −→+ 순서(가로변=위→아래, 세로변=왼→오른).
     * 표 전체 격자 렌더러({@link TableBorderRenderer})가 재사용한다.
     */
    static final class Side {
        final List<Ply> plies = new ArrayList<>();
        Color color = Color.BLACK;
        boolean dashed = false;
        /** 점선/파선 변의 두께(getLineWidth)와 dash 패턴(getDashArray)을 담는다. */
        BasicStroke dashStroke;

        boolean isEmpty() { return plies.isEmpty() && !dashed; }

        /** 가닥 개수. */
        int plyCount() { return plies.size(); }

        /**
         * <b>주(主) 가닥</b>(교차점에서 직교 분리선을 관통하는 가닥)의 인덱스. 없으면 −1.
         *
         * <p>한글은 다중선에서 <b>가장 굵은 가닥</b>(원래 단일선에 해당하는 본선)을 교차점에서
         * 관통시키고, 더 얇은 가닥들은 직교 가닥에서 멈춘다. 삼중선 ThinThickThin·ThinThick·
         * ThickThin은 굵은 가닥 하나가 유일한 최대폭 → 그 가닥이 관통. 이중선(Double)은 두
         * 가닥 폭이 같아 주 가닥이 없다(둘 다 교차점에서 끊김). 단일선은 그 한 가닥이 주.</p>
         */
        int centerIndex() {
            int best = -1;
            double bestW = -1;
            boolean unique = false;
            for (int i = 0; i < plies.size(); i++) {
                double w = plies.get(i).width;
                if (w > bestW + 0.5) { bestW = w; best = i; unique = true; }
                else if (Math.abs(w - bestW) <= 0.5) { unique = false; }
            }
            return unique ? best : -1;
        }

        /** 변 중심선 기준 전체 폭의 −쪽 끝(가장 바깥 −축 가닥의 바깥 모서리). */
        double spanStart() {
            if (plies.isEmpty()) return 0;
            Ply p = plies.get(0);
            return p.axisOffset - p.width / 2.0;
        }

        /** 변 중심선 기준 전체 폭의 +쪽 끝. */
        double spanEnd() {
            if (plies.isEmpty()) return 0;
            Ply p = plies.get(plies.size() - 1);
            return p.axisOffset + p.width / 2.0;
        }

        static Side of(EachBorder b) {
            Side s = new Side();
            if (b == null) return s;
            BorderType type = b.getType();
            if (type == null || type == BorderType.None) return s;
            s.color = colorOf(b);

            double thick = isMultiPly(type)
                    ? scaledThickness(b.getThickness())
                    : scaledSingleThickness(b.getThickness());
            if (isDashed(type)) {
                s.dashed = true;
                s.dashStroke = dashedStroke(type, b.getThickness());
                return s;
            }

            double[] widths = widthsFor(type, thick);   // −→+ 순서 (이름 그대로)
            double gap = Math.max(1.0, gapBaseFor(type) * UnitConverter.zoomRate() / 100.0);
            double total = gap * (widths.length - 1);
            for (double w : widths) total += w;

            // axisOffset: 변 중심선 기준 가닥 중심의 부호 위치(−축→+축). plies[0]이 가장 −쪽.
            double cum = 0;
            for (double w : widths) {
                double axisOffset = cum + w / 2.0 - total / 2.0;
                s.plies.add(new Ply(w, axisOffset));
                cum += w + gap;
            }
            return s;
        }
    }

    /** 한 가닥: width(px)와 변 중심선 기준 중심의 축방향 위치 axisOffset(px, −축→+축). */
    static final class Ply {
        final double width;
        final double axisOffset;
        Ply(double width, double axisOffset) { this.width = width; this.axisOffset = axisOffset; }

        double width() { return width; }
        double axisOffset() { return axisOffset; }
    }

    // ── 가닥 폭 / 간격 / 두께 매핑 ─────────────────────────────────────────

    /**
     * 다중선 타입별 가닥 폭(좌표축 −→+ 순서, 이름 그대로).
     * 단일선/기타는 단일 가닥(두께 T).
     *
     * <p><b>이중선 가닥은 내림(floor)</b>이다 — 반올림하면 t=3(MM0_7)에서 가닥이 2px가 돼
     * 한글보다 두꺼워진다. 한글 캡처 측정: Double/MM0_5(t=2)·Double/MM0_7(t=3) 모두
     * 가닥 1px + 공백 2px + 가닥 1px = 4px (실문서 R12 page12·page13, R02·R03).
     * 같은 MM0_7이라도 ThickThin은 3px+1px로 t를 그대로 쓰므로({@link #baseThicknessPx}는
     * 옳다), 반씩 쪼개는 이중선에서만 내림이 필요하다.</p>
     *
     * <p><b>표본 한계:</b> 관측된 Double은 MM0_5·MM0_7(t=2,3) 둘뿐이고 둘 다 가닥 1px이라,
     * 한글의 규칙이 floor(t/2)인지 "이중선 가닥은 항상 1px"인지는 구분되지 않는다. 둘이
     * 갈리는 t≥4(MM1_0 이상)의 Double 표본이 테스트 문서에 없다 — 그런 문서가 생기면
     * 재확인이 필요하다.</p>
     */
    private static double[] widthsFor(BorderType type, double t) {
        double thin = Math.max(1.0, Math.round(t / 4.0));
        double doubleW = Math.max(1.0, Math.floor(t / 2.0));
        switch (type) {
            case Double:
            case DoubleWave:
                return new double[]{doubleW, doubleW};
            case ThinThick:
                return new double[]{thin, t};
            case ThickThin:
                return new double[]{t, thin};
            case ThinThickThin:
                return new double[]{thin, t, thin};
            default:
                return new double[]{t};
        }
    }

    /**
     * 가닥 사이 공백(px, zoom 100 기준). 한글 캡처 측정값: 이중선/삼중선 = 2px,
     * 비대칭(ThinThick/ThickThin) = 1px.
     */
    private static double gapBaseFor(BorderType type) {
        switch (type) {
            case ThinThick:
            case ThickThin:
                return 1.0;
            default:
                return 2.0;
        }
    }

    /** 다중선(가닥 2개 이상으로 분해되는) 타입 여부. {@link #widthsFor}의 분해 대상과 일치. */
    private static boolean isMultiPly(BorderType type) {
        switch (type) {
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

    private static boolean isDashed(BorderType type) {
        switch (type) {
            case Dash:
            case Dot:
            case DashDot:
            case DashDotDot:
            case LongDash:
            case CircleDot:
                return true;
            default:
                return false;
        }
    }

    /**
     * 점선/파선 테두리용 Stroke. 실선 가닥과 동일하게 두께는 {@link #scaledThickness}(zoom 반영
     * 픽셀 두께)로, dash 패턴 길이는 zoom 배율(zoomRate/100)로 스케일한다. 이전에는
     * {@code StrokeFactory.forParaBorder(type)}가 고정 1px·고정 패턴이라 두께·zoom을 무시했다.
     */
    static BasicStroke dashedStroke(BorderType type, BorderThickness thickness) {
        float width = (float) scaledThickness(thickness);
        float scale = (float) (UnitConverter.zoomRate() / 100.0);
        return StrokeFactory.forParaBorder(type, width, scale);
    }

    /** {@link BorderThickness} → zoom 반영 픽셀 두께. 한글 캡처 측정값에 맞춘 단조 곡선. */
    private static double scaledThickness(BorderThickness t) {
        double base = baseThicknessPx(t);
        return Math.max(1.0, Math.round(base * UnitConverter.zoomRate() / 100.0));
    }

    /**
     * 단일선(Solid 등) → zoom 반영 픽셀 두께. 다중선 가닥과 달리 <b>선형 mm→px</b>로 환산한다.
     * 한글 캡처 측정(실문서 R06): Solid/MM1_5 = 7px = 1.5mm × 4.724(120dpi). 다중선도 패턴
     * <b>전체 폭</b>은 선형값과 일치하므로(ThinThick/MM1_5 = 1+1+5 = 7px, ThinThickThin/MM3_0
     * = 2+2+6+2+2 = 14px), 압축 곡선({@link #baseThicknessPx})은 가닥 분해 폭에만 적용한다.
     */
    private static double scaledSingleThickness(BorderThickness t) {
        double mm = thicknessMm(t);
        double px = UnitConverter.toPixelF(mm * 7200.0 / 25.4); // 1mm = 7200/25.4 HWPUNIT
        return Math.max(1.0, Math.round(px));
    }

    /** {@link BorderThickness} → mm 값. */
    private static double thicknessMm(BorderThickness t) {
        if (t == null) return 0.12;
        switch (t) {
            case MM0_1:  return 0.1;
            case MM0_12: return 0.12;
            case MM0_15: return 0.15;
            case MM0_2:  return 0.2;
            case MM0_25: return 0.25;
            case MM0_3:  return 0.3;
            case MM0_4:  return 0.4;
            case MM0_5:  return 0.5;
            case MM0_6:  return 0.6;
            case MM0_7:  return 0.7;
            case MM1_0:  return 1.0;
            case MM1_5:  return 1.5;
            case MM2_0:  return 2.0;
            case MM3_0:  return 3.0;
            case MM4_0:  return 4.0;
            case MM5_0:  return 5.0;
            default:     return 0.5;
        }
    }

    /**
     * zoom 100 기준 두께 픽셀. mm→px(0.5→2.4, 1.5→7, 3.0→14)를 그대로 쓰면 굵은 가닥이
     * 셀을 덮어버리므로, 한글 캡처에서 관측한 두께(0.7mm→3, 1.5mm→5, 3.0mm→6 등)에
     * 맞춘 압축 곡선을 사용한다.
     */
    private static double baseThicknessPx(BorderThickness t) {
        if (t == null) return 1.0;
        switch (t) {
            case MM0_1:
            case MM0_12:
            case MM0_15:
            case MM0_2:
            case MM0_25: return 1.0;
            case MM0_3:
            case MM0_4:
            case MM0_5:
            case MM0_6:  return 2.0;
            case MM0_7:  return 3.0;
            case MM1_0:  return 4.0;
            case MM1_5:
            case MM2_0:  return 5.0;
            case MM3_0:  return 6.0;
            case MM4_0:  return 7.0;
            case MM5_0:  return 8.0;
            default:     return 2.0;
        }
    }

    private static Color colorOf(EachBorder border) {
        if (border.getColor() == null) return Color.BLACK;
        return new Color(
                border.getColor().getR(),
                border.getColor().getG(),
                border.getColor().getB());
    }
}
