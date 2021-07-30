package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.HashMap;
import java.util.Map;

public class TableDrawResult {
    public static TableDrawResult[] ZeroArray = new TableDrawResult[0];

    private TableOutput tableOutputForCurrentPage;

    private ControlCharInfo controlCharInfo;
    private Map<Cell, CellDrawResult> splitCellDrawResults;
    private int startRowIndexForNextPage;

    public TableDrawResult(ControlCharInfo controlCharInfo) {
        tableOutputForCurrentPage = null;

        this.controlCharInfo = controlCharInfo;
        splitCellDrawResults = new HashMap<>();
        startRowIndexForNextPage = -1;
    }

    public TableOutput tableOutputForCurrentPage() {
        return tableOutputForCurrentPage;
    }

    public void tableOutputForCurrentPage(TableOutput tableOutputForCurrentPage) {
        this.tableOutputForCurrentPage = tableOutputForCurrentPage;
    }

    public ControlCharInfo controlCharInfo() {
        return controlCharInfo;
    }

    public ControlTable table() {
        return (ControlTable) controlCharInfo.control();
    }

    public Area areaWithoutOuterMargin() {
        return controlCharInfo.areaWithoutOuterMargin();
    }

    public Area areaWithOuterMargin() {
        return controlCharInfo.areaWithOuterMargin();
    }

    public void addSplitCellDrawResult(CellDrawResult splitCellDrawResult) {
        splitCellDrawResults.put(splitCellDrawResult.cell(), splitCellDrawResult);
    }

    public boolean split() {
        return startRowIndexForNextPage > 0;
    }

    public int startRowIndexForNextPage() {
        return startRowIndexForNextPage;
    }

    public void startRowIndexForNextPage(int startRowIndexForNextPage) {
        this.startRowIndexForNextPage = startRowIndexForNextPage;
    }

    public CellDrawResult splitCellDrawResult(Cell cell) {
        return splitCellDrawResults.get(cell);
    }
}
