package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** 표 하나의 셀들. 쪽마다 나뉜 셀 내용(cellDocument)을 셀별로 쪽 순서대로 모은다. */
final class TableCells {
    /** 셀 다섯 개 중 합친 셀이 하나 이하면 규칙적인 표. 셀을 많이 합친 서식은 보통 30% 이상이다. */
    private static final int MAX_MERGED_PER_REGULAR = 5;

    private final int rowCount;
    private final int columnCount;
    private final List<CellContent> cells;

    private TableCells(int rowCount, int columnCount, List<CellContent> cells) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.cells = cells;
    }

    /** @param segments 같은 표의 조각들 (쪽 순서) */
    static TableCells of(List<TableItem> segments) {
        Map<Cell, CellContent> byCell = new IdentityHashMap<>();
        List<CellContent> cells = new ArrayList<>();
        for (TableItem segment : segments) {
            for (TableItem.CellLayout layout : segment.cells()) {
                CellContent content = byCell.get(layout.cell());
                if (content == null) {
                    content = new CellContent(layout.cell());
                    byCell.put(layout.cell(), content);
                    cells.add(content);
                }
                content.add(layout.cellDocument(), segment, layout.verticalAlignmentOffset());
            }
        }
        cells.sort(Comparator.comparingInt(CellContent::row).thenComparingInt(CellContent::column));

        int rowCount = segments.get(0).table().getTable().getRowCount();
        int columnCount = segments.get(0).table().getTable().getColumnCount();
        for (CellContent cell : cells) {
            rowCount = Math.max(rowCount, cell.row() + 1);
            columnCount = Math.max(columnCount, cell.column() + 1);
        }
        return new TableCells(rowCount, columnCount, cells);
    }

    int rowCount() {
        return rowCount;
    }

    int columnCount() {
        return columnCount;
    }

    /** 셀 수 (빈 셀 포함, 합친 셀은 하나). */
    int cellCount() {
        return cells.size();
    }

    /** 위→아래, 왼쪽→오른쪽 순서. */
    List<CellContent> cells() {
        return cells;
    }

    /**
     * 셀을 거의 합치지 않은 표인가 (합친 셀이 20% 이하). 이런 표는 빈 칸이 많아도 칸 줄이 뜻을 가진다
     * (금액·날짜를 적을 빈 칸이 있는 자료 표).
     */
    boolean isRegular() {
        int merged = 0;
        for (CellContent cell : cells) {
            if (cell.isMerged()) merged++;
        }
        return merged * MAX_MERGED_PER_REGULAR <= cells.size();
    }

    /** 셀 하나: 표 안 자리(왼쪽 위 칸)와 쪽마다 나뉜 내용. */
    static final class CellContent {
        private final ListHeaderForCell header;
        private final List<Document> documents = new ArrayList<>();
        /** documents와 같은 순서: 그 내용이 놓인 표 조각, 세로 정렬로 밀려 그려지는 양. */
        private final List<TableItem> segments = new ArrayList<>();
        private final List<Long> verticalOffsets = new ArrayList<>();

        CellContent(Cell cell) {
            this.header = cell.getListHeader();
        }

        private void add(Document document, TableItem segment, long verticalOffset) {
            if (document == null) return;
            for (Document each : documents) {
                if (each == document) return;
            }
            documents.add(document);
            segments.add(segment);
            verticalOffsets.add(verticalOffset);
        }

        int row() {
            return header.getRowIndex();
        }

        int column() {
            return header.getColIndex();
        }

        int columnSpan() {
            return Math.max(1, header.getColSpan());
        }

        /** 셀 테두리/배경 (문서 정보의 BorderFill 번호). */
        int borderFillId() {
            return header.getBorderFillId();
        }

        int rowSpan() {
            return Math.max(1, header.getRowSpan());
        }

        boolean isMerged() {
            return header.getRowSpan() > 1 || header.getColSpan() > 1;
        }

        List<Document> documents() {
            return documents;
        }

        /** documents의 i번째 내용이 놓인 표 조각. */
        TableItem segmentOf(int index) {
            return segments.get(index);
        }

        long verticalOffsetOf(int index) {
            return verticalOffsets.get(index);
        }
    }
}
