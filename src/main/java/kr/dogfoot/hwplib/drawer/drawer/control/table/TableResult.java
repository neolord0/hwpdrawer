package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.HashMap;
import java.util.Map;

public class TableResult {
    public static TableResult[] ZeroArray = new TableResult[0];

    private TableOutput tableOutputForCurrentPage;

    private Map<Cell, CellResult> cellResults;
    private int splitStartRowIndex;

    public TableResult() {
        tableOutputForCurrentPage = null;

        cellResults = new HashMap<>();
        splitStartRowIndex = -1;
    }

    public TableOutput tableOutputForCurrentPage() {
        return tableOutputForCurrentPage;
    }

    public TableResult tableOutputForCurrentPage(TableOutput tableOutputForCurrentPage) {
        this.tableOutputForCurrentPage = tableOutputForCurrentPage;
        return this;
    }

    public ControlTable table() {
        return tableOutputForCurrentPage.table();
    }

    public Area areaWithoutOuterMargin() {
        return tableOutputForCurrentPage.controlCharInfo().areaWithoutOuterMargin();
    }

    public Area areaWithOuterMargin() {
        return tableOutputForCurrentPage.controlCharInfo().areaWithOuterMargin();
    }

    public boolean split() {
        return splitStartRowIndex >= 0;
    }

    public int splitStartRowIndex() {
        return splitStartRowIndex;
    }

    public void splitStartRowIndex(int splitStartRowIndex) {
        if (this.splitStartRowIndex == -1) {
            this.splitStartRowIndex = splitStartRowIndex;
        } else {
            this.splitStartRowIndex = Math.min(splitStartRowIndex, this.splitStartRowIndex);
        }
    }

    public void addCellResult(CellResult cellResult) {
        cellResults.put(cellResult.cell(), cellResult);
    }

    public CellResult cellResult(Cell cell) {
        return cellResults.get(cell);
    }
}
