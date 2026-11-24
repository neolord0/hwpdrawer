package kr.dogfoot.hwpdrawer.export.text;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 표의 머리 행. 보통은 첫 행이다.
 *
 * <p>여러 줄 머리(묶음 칸 아래 하위 칸 — "제출대상"이 세 열에 걸치고 그 아래 "사단 | 재단 | 제출여부")는 한 줄로
 * 합쳐 열마다 "제출대상 &gt; 사단"으로 쓴다. 하위 칸을 본문 첫 행으로 두면 값처럼 읽히고 묶음 아래 열은 이름이 없다.
 * 묶음 이름을 열마다 되풀이하므로 되풀이한 글자를 {@link #repeatedText()}에 남긴다.</p>
 *
 * <p>하위 머리 행 판정은 {@link #isSubHeaderRow} — 묶음 칸 아래의 본문 행과 가려야 한다. 본문이 다 빈 서식 표는
 * 머리 행만 남는다.</p>
 */
final class MarkdownHeader {
    private static final String LEVEL_SEPARATOR = " > ";
    /** 하위 머리 칸은 짧은 이름이다: 문단 3개, 글자 40개 이하, 목록 표시로 시작하지 않음. */
    private static final int MAX_LABEL_PARAGRAPHS = 3;
    private static final int MAX_LABEL_LENGTH = 40;
    private static final String LIST_MARKERS = "-•▪◆◇□■※*▶▷◦∘❍○●·";

    private final CellText[][] grid;
    private final List<Integer> rows;
    private final List<Integer> columns;

    private MarkdownHeader(CellText[][] grid, List<Integer> rows, List<Integer> columns) {
        this.grid = grid;
        this.rows = rows;
        this.columns = columns;
    }

    /**
     * @param grid    칸에 놓인 셀 (합친 셀은 한 칸에만, 합친 범위는 셀이 안다)
     * @param rows    표 행 (첫 행부터)
     * @param columns 쓰는 열
     */
    static Result of(CellText[][] grid, List<Integer> rows, List<Integer> columns) {
        return new MarkdownHeader(grid, rows, columns).build();
    }

    private Result build() {
        List<Integer> headerRows = new ArrayList<>();
        headerRows.add(rows.get(0));
        while (headerRows.size() < rows.size() && isSubHeaderRow(rows.get(headerRows.size()), headerRows)) {
            headerRows.add(rows.get(headerRows.size()));
        }
        return headerRows.size() == 1 ? singleRow(headerRows.get(0)) : merged(headerRows);
    }

    private Result singleRow(int row) {
        List<String> labels = new ArrayList<>();
        for (int column : columns) {
            CellText cell = grid[row][column];
            labels.add(cell == null ? "" : cell.asMarkdownCell(false));
        }
        return new Result(1, labels, "");
    }

    /** 열마다 위에서 아래로 그 열을 덮는 머리 칸들을 " > "로. 하위 칸 없이 여러 열에 걸친 칸은 놓인 칸에만. */
    private Result merged(List<Integer> headerRows) {
        List<String> labels = new ArrayList<>();
        Map<CellText, Integer> uses = new IdentityHashMap<>();
        for (int column : columns) {
            List<CellText> chain = new ArrayList<>();
            for (int row : headerRows) {
                CellText cell = coveringCell(row, column);
                if (cell != null && (chain.isEmpty() || chain.get(chain.size() - 1) != cell)) chain.add(cell);
            }
            dropUnplacedDeepest(chain, column);
            List<String> texts = new ArrayList<>();
            for (CellText cell : chain) {
                texts.add(cell.asMarkdownCell(false));
                uses.merge(cell, 1, Integer::sum);
            }
            labels.add(String.join(LEVEL_SEPARATOR, texts));
        }
        return new Result(headerRows.size(), labels, repeatedTextOf(uses));
    }

    /**
     * 맨 아래 칸이 이 열에 놓인 칸이 아니면(하위 칸 없이 여러 열에 걸친 칸) 뺀다 — 그 칸 이름은 놓인 열에만 쓴다.
     * 하위 칸이 있는 묶음은 그 아래 칸이 맨 아래라 남는다.
     */
    private void dropUnplacedDeepest(List<CellText> chain, int column) {
        if (chain.isEmpty()) return;
        CellText deepest = chain.get(chain.size() - 1);
        if (grid[deepest.row()][column] != deepest) chain.remove(chain.size() - 1);
    }

    private static String repeatedTextOf(Map<CellText, Integer> uses) {
        StringBuilder repeated = new StringBuilder();
        for (Map.Entry<CellText, Integer> use : uses.entrySet()) {
            for (int i = 1; i < use.getValue(); i++) repeated.append(use.getKey().asMarkdownCell(false));
        }
        return repeated.toString();
    }

    /**
     * row가 하위 머리 행인가. 묶음 칸 아래의 본문 행도 '묶음 아래에 있다'는 점은 같으므로 더 따진다:
     * <ol>
     *   <li>글자 든 칸이 모두 바로 위 머리 행의 묶음 칸(여러 열에 걸치고 row 전에 끝남) 아래에 있다.</li>
     *   <li>그 묶음 칸 하나를 둘 이상의 칸으로 쪼갠다 (본문은 쪼개지 않고 아래로 이어진다).</li>
     *   <li>칸이 모두 짧은 이름이다 (목록 표시로 시작하지 않는 짧은 글).</li>
     *   <li>첫 머리 행에 row까지 내려오는 칸이 있거나(구분·비고 같은 세로 머리), 첫 머리 행이 모두 묶음 칸이다.</li>
     * </ol>
     */
    private boolean isSubHeaderRow(int row, List<Integer> headerRows) {
        boolean any = false;
        for (int column : columns) {
            CellText cell = grid[row][column];
            if (cell == null) continue;
            if (!isUnderGroup(row, column, headerRows) || !isLabel(cell)) return false;
            any = true;
        }
        return any && splitsGroupOf(row, headerRows.get(headerRows.size() - 1))
                && isHeaderShaped(headerRows.get(0), row);
    }

    /** 바로 위 머리 행에서 이 열을 덮는 칸이 row 전에 끝나는 묶음 칸인가 (더 위 행의 묶음은 이미 하위 칸으로 나뉘었다). */
    private boolean isUnderGroup(int row, int column, List<Integer> headerRows) {
        CellText group = coveringCell(headerRows.get(headerRows.size() - 1), column);
        return group != null && group.columnSpan() > 1 && group.row() + group.rowSpan() <= row;
    }

    /**
     * previous 행을 덮고 row 전에 끝나는 묶음 칸 가운데, row의 글자 든 칸 둘 이상이 그 아래에 있는 것이 있는가.
     * (묶음이 빈 행까지 걸쳐 끝날 수 있다 — 빈 행은 rows에서 빠졌다.)
     */
    private boolean splitsGroupOf(int row, int previous) {
        Map<CellText, Integer> children = new IdentityHashMap<>();
        for (int column : columns) {
            if (grid[row][column] == null) continue;
            CellText group = coveringCell(previous, column);
            if (group != null && group.columnSpan() > 1 && group.row() + group.rowSpan() <= row) {
                if (children.merge(group, 1, Integer::sum) > 1) return true;
            }
        }
        return false;
    }

    private boolean isHeaderShaped(int firstRow, int row) {
        boolean allGroups = true;
        for (CellText cell : grid[firstRow]) {
            if (cell == null) continue;
            if (cell.row() + cell.rowSpan() > row) return true;
            if (cell.columnSpan() < 2) allGroups = false;
        }
        return allGroups;
    }

    private static boolean isLabel(CellText cell) {
        List<String> paragraphs = cell.paragraphs();
        if (paragraphs.size() > MAX_LABEL_PARAGRAPHS) return false;
        int length = 0;
        for (String paragraph : paragraphs) length += paragraph.length();
        return length <= MAX_LABEL_LENGTH && LIST_MARKERS.indexOf(paragraphs.get(0).charAt(0)) < 0;
    }

    /**
     * (row, column) 칸을 합친 범위로 덮는, 칸에 놓인 셀. 칸에 놓인 것을 본다 — 잘못 만든 표에서 한 칸에 두 셀을
     * 합쳐 놓았으면 합친 셀이어야 글자를 잃지 않는다. 이 열에 놓인 셀을 먼저 본다.
     */
    private CellText coveringCell(int row, int column) {
        CellText found = null;
        for (int r = 0; r <= row; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                CellText cell = grid[r][c];
                if (cell == null || !covers(cell, row, column)) continue;
                if (c == column) return cell;
                if (found == null) found = cell;
            }
        }
        return found;
    }

    private static boolean covers(CellText cell, int row, int column) {
        return cell.row() <= row && row < cell.row() + cell.rowSpan()
                && cell.column() <= column && column < cell.column() + cell.columnSpan();
    }

    /** 머리 행 수(본문은 그 다음 행부터), 열마다의 머리 글자, 되풀이한 글자. */
    static final class Result {
        private final int rowCount;
        private final List<String> labels;
        private final String repeatedText;

        private Result(int rowCount, List<String> labels, String repeatedText) {
            this.rowCount = rowCount;
            this.labels = labels;
            this.repeatedText = repeatedText;
        }

        int rowCount() {
            return rowCount;
        }

        List<String> labels() {
            return labels;
        }

        String repeatedText() {
            return repeatedText;
        }
    }
}
