package kr.dogfoot.hwpdrawer.export.text;

import java.util.ArrayList;
import java.util.List;

/**
 * 표의 칸 격자를 글자 블록으로 쓴다. 합친 셀은 왼쪽 위 칸에만 글자를 두고, 칸이 모두 빈 행·열은 뺀다
 * (서식 문서의 여백용 행·열).
 *
 * <p>칸에 넣을 수 없는 셀({@link CellText#isComplex()} — 안쪽 표가 있는 셀, 본문을 담은 쪽 틀)은 그 자리에 블록으로
 * 쓴다. 쪽 틀이 있는 행에서는 표를 끊고 앞뒤 행들을 따로따로 표로 본다. 표 조각마다:</p>
 * <ul>
 *   <li>앞쪽의 '칸 하나가 표 폭 전체를 덮는' 행들(첫 행은 가운데·오른쪽에 혼자 있는 칸도)은 표 제목으로 위에 쓴다.</li>
 *   <li>자료 표 — 셀을 거의 합치지 않았거나, 칸 격자 절반 넘게 또는 셀 거의 다 글자가 있는 표: Markdown 표.
 *       첫 행이 머리 행이다 (여러 줄 머리는 {@link MarkdownHeader}가 합친다). 위 본문 행에서 내려온 합친 셀이 덮은 칸에는 "〃"(같음)을 쓴다 — 그 행만 읽어도 어느
 *       구분인지 알게. 나란한 여러 칸의 목록
 *       (산업명 | 분류번호)은 빈 문단을 넣어야 줄 수가 같아지면 빈 문단까지 줄을 맞춘다.</li>
 *   <li>서식 — 그 밖의 표, 행이나 열이 하나뿐인 표: 행마다 글자가 든 칸만 {@code " | "}로 이어 한 줄로 쓴다
 *       ({@link CellText#asFormCell()}). 칸이 하나뿐인 행은 {@link CellText#asOnlyCellOfRow()}.</li>
 * </ul>
 */
final class TableGrid {
    /** 셀을 많이 합친 표에서 칸 격자의 이 비율 이하만 글자가 있으면 서식으로 본다. */
    private static final double MAX_FILL_FOR_FORM = 0.5;
    /** 셀을 많이 합친 표라도 셀이 이 비율 이상 글자를 가졌으면 자료 표다 (두 단 머리 행의 자료 표). */
    private static final double MIN_FILLED_CELLS_OF_DATA_TABLE = 0.9;
    private static final String FORM_CELL_SEPARATOR = " | ";
    /** Markdown 표에서 위 행의 합친 셀이 덮은 칸. */
    private static final String SAME_AS_ABOVE = "〃";

    private final int rowCount;
    private final int columnCount;
    private final int cellCount;
    private final boolean regular;
    private final List<CellText> cells = new ArrayList<>();
    private CellText[][] grid;

    /**
     * @param cellCount 표의 셀 수 (빈 셀 포함)
     * @param regular   셀을 거의 합치지 않은 표인가 ({@link TableCells#isRegular()})
     */
    TableGrid(int rowCount, int columnCount, int cellCount, boolean regular) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.cellCount = cellCount;
        this.regular = regular;
    }

    /** 글자가 든 셀만 넣는다. */
    void put(CellText cell) {
        cells.add(cell);
    }

    /** 표를 쓴 블록들. 글자가 하나도 없으면 빈 목록. */
    List<TextBlock> render() {
        grid = layout();
        List<TextBlock> blocks = new ArrayList<>();
        List<Integer> part = new ArrayList<>();
        for (int row = 0; row < rowCount; row++) {
            if (isEmptyRow(row)) continue;
            if (hasPageFrame(row)) {
                renderPart(part, blocks);
                part.clear();
                formRows(singleton(row), columnsUsedBy(singleton(row)), blocks);
            } else {
                part.add(row);
            }
        }
        renderPart(part, blocks);
        addDroppedText(blocks);
        return blocks;
    }

    /** 칸에 쓴 셀들이 보이지 않아 뺀 글자를 글자 없는 블록 하나로 남긴다 (빠짐·중복 검사용). */
    private void addDroppedText(List<TextBlock> blocks) {
        StringBuilder dropped = new StringBuilder();
        for (CellText cell : cells) dropped.append(cell.droppedText());
        if (dropped.length() > 0) blocks.add(TextBlock.droppedOnly(dropped.toString()));
    }

    /**
     * 표 조각 하나: 앞쪽 제목 행들을 떼고, 나머지를 자료 표 또는 서식으로. 칸에 넣을 수 없는 셀(안쪽 표)이 있으면
     * 서식 행으로 쓰고 그 셀은 그 자리에 블록으로.
     */
    private void renderPart(List<Integer> rows, List<TextBlock> blocks) {
        int first = 0;
        while (first < rows.size() - 1
                && isCaption(rows.get(first), columnsUsedBy(rows.subList(first, rows.size())), first == 0)) {
            first++;
        }
        if (first > 0) formRows(rows.subList(0, first), columnsUsedBy(rows.subList(0, first)), blocks);
        List<Integer> body = rows.subList(first, rows.size());
        if (body.isEmpty()) return;
        List<Integer> columns = columnsUsedBy(body);
        if (body.size() > 1 && columns.size() > 1 && !hasComplexCell(body)
                && (regular || isDataTable(body, columns))) {
            blocks.add(markdown(body, columns));
        } else {
            formRows(body, columns, blocks);
        }
    }

    /**
     * 셀을 많이 합친 표 가운데 자료 표: 칸 격자의 절반 넘게 글자가 있거나, 셀이 거의 다(90%) 글자를 가졌다.
     * 셀 비율만 보면 라벨이 많은 큰 서식도 자료 표가 된다 (빈 칸은 적지만 합친 칸이 격자를 성기게 만든다).
     */
    private boolean isDataTable(List<Integer> rows, List<Integer> columns) {
        int filledSlots = 0;
        for (int row : rows) {
            for (int column : columns) {
                if (grid[row][column] != null) filledSlots++;
            }
        }
        return filledSlots > MAX_FILL_FOR_FORM * rows.size() * columns.size()
                || cells.size() >= MIN_FILLED_CELLS_OF_DATA_TABLE * Math.max(cellCount, cells.size());
    }

    /**
     * 셀을 칸에 놓는다. 여러 열에 걸친 셀은 걸친 열 가운데 다른 행의 더 좁은 셀이 시작하는 첫 열에 놓는다 — 빈 여백
     * 열과 내용 열에 걸친 머리 칸("산 업 명")이 여백 열에 놓이면 내용 열이 머리 없는 열이 된다. 자리가 이미 차
     * 있으면(잘못 만든 표) 걸친 열의 빈 자리에, 그것도 없으면 그 칸의 글자에 잇는다 — 글자를 잃지 않게.
     */
    private CellText[][] layout() {
        CellText[][] placed = new CellText[rowCount][columnCount];
        for (CellText cell : cells) {
            int column = freeColumnOf(cell, placed);
            if (column >= 0) {
                placed[cell.row()][column] = cell;
            } else {
                int at = Math.min(cell.column(), columnCount - 1);
                placed[cell.row()][at] = placed[cell.row()][at].joinedWith(cell);
            }
        }
        return placed;
    }

    private int freeColumnOf(CellText cell, CellText[][] placed) {
        int anchor = anchorColumnOf(cell);
        if (placed[cell.row()][anchor] == null) return anchor;
        for (int column = cell.column(); column < spanEnd(cell); column++) {
            if (placed[cell.row()][column] == null) return column;
        }
        return -1;
    }

    private int anchorColumnOf(CellText cell) {
        for (int column = cell.column(); column < spanEnd(cell); column++) {
            for (CellText other : cells) {
                if (other.row() != cell.row() && other.column() == column && other.columnSpan() < cell.columnSpan()) {
                    return column;
                }
            }
        }
        return Math.min(cell.column(), columnCount - 1);
    }

    private int spanEnd(CellText cell) {
        return Math.min(cell.column() + cell.columnSpan(), columnCount);
    }

    /** 머리 행({@link MarkdownHeader} — 여러 줄 머리는 합친다) + 구분 줄 + 본문 행. */
    private TextBlock markdown(List<Integer> rows, List<Integer> columns) {
        MarkdownHeader.Result header = MarkdownHeader.of(grid, rows, columns);
        StringBuilder out = new StringBuilder("|");
        for (String label : header.labels()) out.append(' ').append(label).append(" |");
        out.append("\n|");
        for (int i = 0; i < columns.size(); i++) out.append(" --- |");
        for (int i = header.rowCount(); i < rows.size(); i++) {
            out.append('\n');
            appendMarkdownRow(out, rows.get(i), columns);
        }
        return TextBlock.fixed(out.toString(), header.repeatedText());
    }

    /** 본문 행 하나. 위 행에서 내려온 합친 셀이 덮은 칸은 "〃". */
    private void appendMarkdownRow(StringBuilder out, int row, List<Integer> columns) {
        boolean aligned = alignsParallelLists(row, columns);
        out.append('|');
        for (int column : columns) {
            CellText cell = grid[row][column];
            String text;
            if (cell != null) {
                text = cell.asMarkdownCell(aligned);
            } else {
                text = isCoveredFromAbove(row, column) ? SAME_AS_ABOVE : "";
            }
            out.append(' ').append(text).append(" |");
        }
    }

    /**
     * 이 행의 나란한 목록(문단이 여러 개인 칸이 둘 이상)을 빈 문단 자리까지 맞춰 쓸까. 빈 문단을 빼면 줄 수가 서로
     * 다르고 넣으면 같아질 때만 — 빈 문단 자리가 칸마다 다르면(신·구 대비표) 넣는 편이 오히려 짝을 어긋낸다.
     */
    private boolean alignsParallelLists(int row, List<Integer> columns) {
        int lists = 0;
        int alignedLength = -1;
        boolean sameLength = true;
        boolean sameAlignedLength = true;
        int length = -1;
        for (int column : columns) {
            CellText cell = grid[row][column];
            if (cell == null || !cell.isList()) continue;
            lists++;
            if (length >= 0 && length != cell.paragraphs().size()) sameLength = false;
            if (alignedLength >= 0 && alignedLength != cell.alignedParagraphs().size()) sameAlignedLength = false;
            length = cell.paragraphs().size();
            alignedLength = cell.alignedParagraphs().size();
        }
        return lists > 1 && !sameLength && sameAlignedLength;
    }

    /** 위 행에서 시작한 합친 셀(글자 있음)이 이 칸을 덮는가 — 첫 행이 실은 자료인 표도 있어 머리 행에서 내려온 칸도. */
    private boolean isCoveredFromAbove(int row, int column) {
        for (int above = row - 1; above >= 0; above--) {
            CellText cell = grid[above][column];
            if (cell != null) return cell.row() + cell.rowSpan() > row;
        }
        return false;
    }

    /** 행마다 한 줄. 칸에 넣을 수 없는 셀이 나오면 줄 블록을 끊고 그 셀의 블록을 넣는다. */
    private void formRows(List<Integer> rows, List<Integer> columns, List<TextBlock> blocks) {
        List<String> lines = new ArrayList<>();
        for (int row : rows) {
            List<CellText> rowCells = new ArrayList<>();
            for (int column : columns) {
                CellText cell = grid[row][column];
                if (cell == null) continue;
                if (cell.isComplex()) {
                    addFormLine(lines, rowCells);
                    rowCells.clear();
                    flush(lines, blocks);
                    blocks.addAll(cell.blocks());
                } else {
                    rowCells.add(cell);
                }
            }
            addFormLine(lines, rowCells);
        }
        flush(lines, blocks);
    }

    private static void addFormLine(List<String> lines, List<CellText> rowCells) {
        if (rowCells.isEmpty()) return;
        if (rowCells.size() == 1) {
            lines.addAll(rowCells.get(0).asOnlyCellOfRow());
            return;
        }
        List<String> texts = new ArrayList<>();
        for (CellText cell : rowCells) texts.add(cell.asFormCell());
        lines.add(String.join(FORM_CELL_SEPARATOR, texts));
    }

    private static void flush(List<String> lines, List<TextBlock> blocks) {
        if (lines.isEmpty()) return;
        blocks.add(TextBlock.fixed(String.join("\n", lines)));
        lines.clear();
    }

    /**
     * 표 제목 행인가: 글자가 든 칸이 하나뿐이고, 그 칸이 남은 열 전체에 걸치거나, 표 첫 행에서 첫 열이 아닌 자리에
     * 혼자 있다 (가운데·오른쪽의 편지머리 "사단법인 ○○○○"). 첫 열의 칸 하나("기업체명 :" + 빈 칸)는 이름표다.
     */
    private boolean isCaption(int row, List<Integer> columns, boolean firstRow) {
        CellText only = null;
        for (int column : columns) {
            if (grid[row][column] == null) continue;
            if (only != null) return false;
            only = grid[row][column];
        }
        if (only == null || columns.size() < 2) return false;
        boolean spansAll = only.column() <= columns.get(0)
                && only.column() + only.columnSpan() - 1 >= columns.get(columns.size() - 1);
        return spansAll || (firstRow && grid[row][columns.get(0)] == null);
    }

    /** rows 가운데 글자가 든 칸이 있는 열. */
    private List<Integer> columnsUsedBy(List<Integer> rows) {
        List<Integer> columns = new ArrayList<>();
        for (int column = 0; column < columnCount; column++) {
            for (int row : rows) {
                if (grid[row][column] != null) {
                    columns.add(column);
                    break;
                }
            }
        }
        return columns;
    }

    private boolean hasPageFrame(int row) {
        for (CellText cell : grid[row]) {
            if (cell != null && cell.isPageFrame()) return true;
        }
        return false;
    }

    private boolean hasComplexCell(List<Integer> rows) {
        for (int row : rows) {
            for (CellText cell : grid[row]) {
                if (cell != null && cell.isComplex()) return true;
            }
        }
        return false;
    }

    private boolean isEmptyRow(int row) {
        for (CellText cell : grid[row]) {
            if (cell != null) return false;
        }
        return true;
    }

    private static List<Integer> singleton(int row) {
        List<Integer> rows = new ArrayList<>();
        rows.add(row);
        return rows;
    }
}
