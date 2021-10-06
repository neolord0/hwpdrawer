package kr.dogfoot.hwplib.drawer.drawer.control.table;

import java.util.ArrayList;

public class RowDrawInfo {
    public final static RowDrawInfo[] Zero_Array = new RowDrawInfo[0];

    private int rowIndex;
    private ArrayList<CellDrawInfo> cellDrawInfos;
    private boolean split;
    private boolean overPage;

    public RowDrawInfo(int rowIndex) {
        this.rowIndex = rowIndex;
        cellDrawInfos = new ArrayList<>();
        split = false;
        overPage = false;
    }

    public int rowIndex() {
        return rowIndex;
    }

    public void addCellDrawInfo(CellDrawInfo cellDrawInfo) {
        cellDrawInfos.add(cellDrawInfo);
    }

    public CellDrawInfo[] cellDrawInfos() {
        return cellDrawInfos.toArray(CellDrawInfo.Zero_Array);
    }

    public boolean split() {
        return split;
    }

    public void split(boolean split) {
        this.split = split;
    }

    public boolean overPage() {
        return overPage;
    }

    public void overPage(boolean overPage) {
        this.overPage = overPage;
    }
}
