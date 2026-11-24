package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 한 페이지(본문, 또는 셀·글상자 안의 페이지) 안에서 문단과 컨트롤을 읽는 순서로 놓는다.
 *
 * <p>본문 글자는 행(TextRow) → 단(TextColumn) → 단 안의 줄 순서다. 표·글상자는 본문 흐름 밖에서 페이지
 * 좌표로만 놓이므로, 자리를 보고 단에 넣는다: 컨트롤 위쪽이 들어가는 행, 그 행에서 가로 가운데가 들어가는 단.
 * 단 안에서는 위쪽 좌표 순서로 문단 사이에 끼운다 — 같은 높이면 문단이 먼저다 (글자처럼 취급한 표는
 * 자기를 담은 줄 안에 있으므로 그 문단 다음에 온다).</p>
 *
 * <p>쪽·단을 넘어 이어지는 문단 조각은 여기서 따로 나오고, {@link BlockList}가 다시 잇는다.</p>
 */
final class ReadingOrder {
    private static final Comparator<ControlItem> BY_POSITION =
            Comparator.<ControlItem>comparingLong(c -> c.area().top()).thenComparingLong(c -> c.area().left());

    private ReadingOrder() {
    }

    /** @param backdrop 이 흐름의 글자 뒤 바탕 (보이지 않는 글자를 빼는 데 쓴다) */
    static List<FlowItem> of(Content content, List<ControlItem> controls, Backdrop backdrop) {
        List<ColumnFlow> columns = columnsOf(content);
        List<ControlItem> textControls = textControlsOf(controls);
        if (columns.isEmpty()) {
            List<FlowItem> items = new ArrayList<>();
            for (ControlItem control : textControls) items.add(FlowItem.of(control));
            return items;
        }
        for (ControlItem control : textControls) {
            columnFor(control, columns).add(control);
        }
        List<FlowItem> items = new ArrayList<>();
        for (ColumnFlow column : columns) {
            items.addAll(column.items(backdrop));
        }
        return items;
    }

    /**
     * 글자를 담을 수 있는 컨트롤만, 위→아래 순서로. 비슷한 높이로 나란히 놓인 것들(접수 상자 | 결재 상자 — 세로로
     * 긴 쪽 높이의 절반 넘게 겹침)은 한 띠로 보고 왼쪽→오른쪽으로.
     */
    static List<ControlItem> textControlsOf(List<ControlItem> controls) {
        List<ControlItem> sorted = new ArrayList<>();
        for (ControlItem control : controls) {
            if (hasText(control)) sorted.add(control);
        }
        sorted.sort(BY_POSITION);
        List<ControlItem> result = new ArrayList<>();
        List<ControlItem> band = new ArrayList<>();
        for (ControlItem control : sorted) {
            if (!band.isEmpty() && !overlapsAll(band, control)) {
                addBand(band, result);
            }
            band.add(control);
        }
        addBand(band, result);
        return result;
    }

    private static void addBand(List<ControlItem> band, List<ControlItem> result) {
        band.sort(Comparator.comparingLong(c -> c.area().left()));
        result.addAll(band);
        band.clear();
    }

    /** 띠의 모든 컨트롤과 세로로 많이 겹치는가 — 키 큰 틀이 그 안에 위아래로 쌓인 상자들을 한 띠로 묶지 않게. */
    private static boolean overlapsAll(List<ControlItem> band, ControlItem control) {
        for (ControlItem member : band) {
            if (!overlapsMostly(member, control)) return false;
        }
        return true;
    }

    /** 세로로 겹친 길이가 긴 쪽 높이의 절반을 넘는가. */
    private static boolean overlapsMostly(ControlItem a, ControlItem b) {
        long overlap = Math.min(a.area().bottom(), b.area().bottom()) - Math.max(a.area().top(), b.area().top());
        long taller = Math.max(a.area().height(), b.area().height());
        return taller > 0 && overlap * 2 > taller;
    }

    private static boolean hasText(ControlItem control) {
        if (control instanceof TableItem) return true;
        if (control instanceof GsoTextBoxItem) return ((GsoTextBoxItem) control).innerDocument() != null;
        if (control instanceof GsoContainerItem) {
            for (ControlItem child : ((GsoContainerItem) control).children()) {
                if (hasText(child)) return true;
            }
        }
        return false;
    }

    /**
     * 줄이 있는 단들. 한 TextColumn에 시각적인 단 여러 개가 차례로 들어 있는 경우가 있어(배분 다단, 셀 안 다단 —
     * 첫 단 줄들 뒤에 둘째 단 줄들이 다시 위에서부터), 줄 위쪽이 거슬러 올라가는 곳에서 단을 나눈다.
     */
    private static List<ColumnFlow> columnsOf(Content content) {
        List<ColumnFlow> columns = new ArrayList<>();
        for (int r = 0; r < content.rows().size(); r++) {
            TextRow row = content.rows().get(r);
            for (TextColumn column : row.columns()) {
                List<TextLine> run = new ArrayList<>();
                for (TextLine line : column.textLines()) {
                    if (!run.isEmpty() && line.area().top() < run.get(run.size() - 1).area().top()) {
                        columns.add(new ColumnFlow(r, run));
                        run = new ArrayList<>();
                    }
                    run.add(line);
                }
                if (!run.isEmpty()) columns.add(new ColumnFlow(r, run));
            }
        }
        return columns;
    }

    /**
     * 컨트롤 위쪽이 들어가는 마지막 행(없으면 첫 행)에서, 가로 가운데가 들어가거나 가장 가까운 단.
     * 가로로 같은 단이 여럿이면(겹쳐 놓인 흐름) 컨트롤 위쪽이 세로 범위에 드는 단.
     */
    private static ColumnFlow columnFor(ControlItem control, List<ColumnFlow> columns) {
        long top = control.area().top();
        int row = columns.get(0).row;
        for (ColumnFlow column : columns) {
            if (rowTopOf(column.row, columns) <= top) row = column.row;
        }
        long centerX = (control.area().left() + control.area().right()) / 2;
        ColumnFlow best = null;
        for (ColumnFlow column : columns) {
            if (column.row != row) continue;
            if (best == null || column.isCloserThan(best, centerX, top)) best = column;
        }
        return best;
    }

    private static long rowTopOf(int row, List<ColumnFlow> columns) {
        long top = Long.MAX_VALUE;
        for (ColumnFlow column : columns) {
            if (column.row == row) top = Math.min(top, column.top);
        }
        return top;
    }

    /** 줄이 있는 단 하나와 그 단에 끼울 컨트롤들. */
    private static final class ColumnFlow {
        private final int row;
        private final List<TextLine> lines;
        private final long top;
        private final long bottom;
        private final long left;
        private final long right;
        private final List<ControlItem> controls = new ArrayList<>();

        /** @param lines 위→아래 순서의 줄 */
        ColumnFlow(int row, List<TextLine> lines) {
            this.row = row;
            this.lines = lines;
            long left = Long.MAX_VALUE;
            long right = Long.MIN_VALUE;
            long bottom = Long.MIN_VALUE;
            for (TextLine line : lines) {
                left = Math.min(left, line.area().left());
                right = Math.max(right, line.area().right());
                bottom = Math.max(bottom, line.area().bottom());
            }
            this.top = lines.get(0).area().top();
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        void add(ControlItem control) {
            controls.add(control);
        }

        /** 가로 거리가 더 가깝거나, 같으면 세로 거리가 더 가깝다. */
        boolean isCloserThan(ColumnFlow other, long x, long y) {
            long dx = distance(x, left, right);
            long otherDx = distance(x, other.left, other.right);
            if (dx != otherDx) return dx < otherDx;
            return distance(y, top, bottom) < distance(y, other.top, other.bottom);
        }

        private static long distance(long value, long from, long to) {
            if (value < from) return from - value;
            if (value > to) return value - to;
            return 0;
        }

        /** 문단과 컨트롤을 위쪽 좌표 순서로 (컨트롤은 이미 위쪽 순서). */
        List<FlowItem> items(Backdrop backdrop) {
            List<FlowItem> items = new ArrayList<>();
            int next = 0;
            for (ParagraphText paragraph : paragraphs(backdrop)) {
                while (next < controls.size() && controls.get(next).area().top() < paragraph.top()) {
                    items.add(FlowItem.of(controls.get(next++)));
                }
                items.add(FlowItem.of(paragraph));
            }
            while (next < controls.size()) {
                items.add(FlowItem.of(controls.get(next++)));
            }
            return items;
        }

        /** 같은 문단의 연속된 줄끼리 묶는다. */
        private List<ParagraphText> paragraphs(Backdrop backdrop) {
            List<ParagraphText> paragraphs = new ArrayList<>();
            List<TextLine> current = new ArrayList<>();
            for (TextLine line : lines) {
                boolean startsNew = line.firstInPara()
                        || (!current.isEmpty() && current.get(0).paraIndex() != line.paraIndex());
                if (startsNew && !current.isEmpty()) {
                    paragraphs.add(ParagraphText.of(current, backdrop));
                    current = new ArrayList<>();
                }
                current.add(line);
            }
            if (!current.isEmpty()) paragraphs.add(ParagraphText.of(current, backdrop));
            return paragraphs;
        }
    }
}
