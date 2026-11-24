package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/** 표(Table) 컨트롤 아이템. 각 셀의 위치와 레이아웃 결과를 담는다. */
public class TableItem extends ControlItem {
    private final ControlTable table;
    private final List<CellLayout> cells = new ArrayList<>();

    public TableItem(ControlTable table, Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
        this.table = table;
    }

    public ControlTable table() { return table; }
    public List<CellLayout> cells() { return cells; }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        TableItem copy = new TableItem(table, newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        copy.cells.addAll(this.cells);
        return copy;
    }

    public static class CellLayout {
        private final Area area;
        private final Document cellDocument;
        private final Cell cell;
        private long verticalAlignmentOffset;

        public CellLayout(Area area, Document cellDocument, Cell cell) {
            this.area = area;
            this.cellDocument = cellDocument;
            this.cell = cell;
        }

        public Area area() { return area; }
        public Document cellDocument() { return cellDocument; }
        public Cell cell() { return cell; }
        public long verticalAlignmentOffset() { return verticalAlignmentOffset; }
        public void verticalAlignmentOffset(long v) { this.verticalAlignmentOffset = v; }
    }
}
