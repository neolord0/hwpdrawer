package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

public class ColumnStates {
    private CellDrawState[] states;
    private int[] skippedCellCounts;
    private int[] endRowIndexes;

    public ColumnStates(int columnCount) {
        states = new CellDrawState[columnCount];
        endRowIndexes = new int[columnCount];
        skippedCellCounts = new int[columnCount];
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            states[columnIndex] = CellDrawState.Nothing;
            endRowIndexes[columnIndex] = 0;
            skippedCellCounts[columnIndex] = 0;
        }
    }

    public boolean stopDraw() {
        for (CellDrawState state : states) {
            if (state != CellDrawState.Partially) {
                return false;
            }
        }
        return true;
    }

    public boolean canDraw(Cell cell) {
        if (states == null) {
            return true;
        }
        for (int colIndex = cell.getListHeader().getColIndex();
             colIndex < cell.getListHeader().getColIndex() + cell.getListHeader().getColSpan();
             colIndex++) {
            if (states[colIndex] == CellDrawState.Partially) {
                return false;
            }
        }
        return true;
    }

    public boolean canDivideByRow(int rowIndex) {
        for (int endRowIndex : endRowIndexes) {
            if (endRowIndex != rowIndex) {
                return false;
            }
        }
        return true;
    }

    public void setStates(RowDrawInfo rowDrawInfo) {
        for (CellDrawInfo cellDrawInfo : rowDrawInfo.cellDrawInfos()) {
            setState(cellDrawInfo);
        }
    }

    public void setState(CellDrawInfo cellDrawInfo) {
        CellDrawState state = (cellDrawInfo.state().isDivided()) ? CellDrawState.Partially : CellDrawState.Complete;
        ListHeaderForCell lh = cellDrawInfo.cell().getListHeader();
        for (int colIndex = lh.getColIndex(); colIndex < lh.getColIndex() + lh.getColSpan(); colIndex++) {
            states[colIndex] = state;
            endRowIndexes[colIndex] = cellDrawInfo.cell().getListHeader().getRowIndex() + cellDrawInfo.cell().getListHeader().getRowSpan() - 1;
        }
    }

    public void increaseSkippedCellCount(Cell cell) {
        ListHeaderForCell lh = cell.getListHeader();
        for (int colIndex = lh.getColIndex(); colIndex < lh.getColIndex() + lh.getColSpan(); colIndex++) {
            skippedCellCounts[colIndex] += lh.getRowSpan();
        }
    }

    public int skippedCellCount(Cell cell) {
        int result = 0;
        ListHeaderForCell lh = cell.getListHeader();
        for (int colIndex = lh.getColIndex(); colIndex < lh.getColIndex() + lh.getColSpan(); colIndex++) {
            result = Math.max(result, skippedCellCounts[colIndex]);
        }
        return result;
    }

    public void clearSkippedCellCount(Cell cell) {
        ListHeaderForCell lh = cell.getListHeader();
        for (int colIndex = lh.getColIndex(); colIndex < lh.getColIndex() + lh.getColSpan(); colIndex++) {
            skippedCellCounts[colIndex] = 0;
        }
    }

    public enum CellDrawState {
        Nothing,
        Complete,
        Partially
    }
}
