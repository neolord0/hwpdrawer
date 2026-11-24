package kr.dogfoot.hwpdrawer.export.image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 표 격자선 위에서 <b>같은 점선이 연속으로 이어지는 구간(run)</b>의 시작 좌표를 알려준다.
 *
 * <p>한글은 점선 격자선을 셀 경계·병합셀 경계와 무관하게 <b>run 전체를 하나의 dash 위상</b>으로
 * 그린다(실문서 R03 page13 한글 출력: 가로선 y=198의 점 107개가 전부 {@code (x−122)%7 == 0}이고,
 * 열 경계 188·804를 리셋 없이 통과). 우리는 셀마다 자기 좌표에서 패턴을 시작했는데, 하나의
 * 격자선은 위 셀의 bottom·아래 셀들의 top으로 두 번 이상 그려지므로 위상이 어긋난 점열이 겹쳐
 * 점 길이가 구간마다 달라졌다(1px→2px, 3px→5px).</p>
 *
 * <p>표 좌상단을 원점으로 쓰면 안 된다 — 점선 run이 표 왼쪽 끝에서 시작하지 않는 격자선
 * (실문서 R02 page2 y=402, R03 page12 y=1226)에서 한글과 위상이 어긋난다.</p>
 *
 * <p>좌표계는 축에 무관하다: 가로 격자선이면 {@code line}=y·{@code pos}=x, 세로 격자선이면
 * {@code line}=x·{@code pos}=y.</p>
 */
final class DashRunOrigins {

    /** 한 격자선 위의 점선 구간. {@code style}이 다르면 이어져 있어도 같은 run이 아니다. */
    private static final class Run {
        final Object style;
        int from, to;
        Run(Object style, int from, int to) { this.style = style; this.from = from; this.to = to; }
        boolean joins(Object s, int f, int t) { return style.equals(s) && f <= to && t >= from; }
    }

    private final Map<Integer, List<Run>> runsByLine = new HashMap<>();

    /** 격자선 {@code line}의 {@code [from, to]} 구간에 {@code style} 점선이 있음을 등록한다. */
    void add(int line, int from, int to, Object style) {
        List<Run> runs = runsByLine.computeIfAbsent(line, k -> new ArrayList<>());
        for (Run r : runs) {
            if (r.joins(style, from, to)) {
                r.from = Math.min(r.from, from);
                r.to = Math.max(r.to, to);
                mergeInto(runs, r);
                return;
            }
        }
        runs.add(new Run(style, from, to));
    }

    /** {@code r}이 커지면서 새로 맞닿게 된 run들을 흡수한다. */
    private static void mergeInto(List<Run> runs, Run r) {
        runs.removeIf(other -> other != r && other.joins(r.style, r.from, r.to)
                && absorb(r, other));
    }

    private static boolean absorb(Run into, Run other) {
        into.from = Math.min(into.from, other.from);
        into.to = Math.max(into.to, other.to);
        return true;
    }

    /**
     * 격자선 {@code line}에서 {@code pos}를 품은 run의 시작 좌표.
     * 등록된 run이 없으면 {@code fallback}(보통 세그먼트 자신의 시작)을 돌려준다.
     */
    int originAt(int line, int pos, Object style, int fallback) {
        List<Run> runs = runsByLine.get(line);
        if (runs == null) return fallback;
        for (Run r : runs) {
            if (r.style.equals(style) && pos >= r.from && pos <= r.to) return r.from;
        }
        return fallback;
    }

    /** 점선 변의 스타일 키 — 두께와 dash 패턴이 같아야 한 run으로 잇는다. */
    static Object styleOf(BorderRenderer.Side side) {
        StringBuilder key = new StringBuilder().append(side.dashStroke.getLineWidth());
        for (float v : side.dashStroke.getDashArray()) key.append(',').append(v);
        return key.append('|').append(side.color.getRGB()).toString();
    }
}
