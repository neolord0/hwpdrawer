package kr.dogfoot.hwplib.drawer.paragraph.control.table;

import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.paragraph.control.table.CellDrawResult;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TableDrawResult {
    public static TableDrawResult[] ZeroArray = new TableDrawResult[0];

    private TableOutput tableOutputForCurrentPage;
    private Area controlArea;

    private ControlTable table;
    private Map<Cell, CellDrawResult> splitCellDrawResults;
    private int startRowIndexForNextPage;

    public TableDrawResult(ControlTable table, Area controlArea) {
        tableOutputForCurrentPage = null;

        this.table = table;
        this.controlArea = new Area(controlArea);
        splitCellDrawResults = new HashMap<>();
        startRowIndexForNextPage = -1;
    }

    public TableOutput tableOutputForCurrentPage() {
        return tableOutputForCurrentPage;
    }

    public void tableOutputForCurrentPage(TableOutput tableOutputForCurrentPage) {
        this.tableOutputForCurrentPage = tableOutputForCurrentPage;
    }

    public ControlTable table() {
        return table;
    }

    public Area controlArea() {
        return controlArea;
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
