package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class ColumnStates {
    private CellDrawState[] states;
    private int[] endRowIndexes;

    public ColumnStates(int columnCount) {
        states = new CellDrawState[columnCount];
        endRowIndexes = new int[columnCount];
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            states[columnIndex] = CellDrawState.Nothing;
            endRowIndexes[columnIndex] = 0;
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
        for (int colIndex = cellDrawInfo.cell().getListHeader().getColIndex();
             colIndex < cellDrawInfo.cell().getListHeader().getColIndex() + cellDrawInfo.cell().getListHeader().getColSpan();
             colIndex++) {
            states[colIndex] = state;
            endRowIndexes[colIndex] = cellDrawInfo.cell().getListHeader().getRowIndex() + cellDrawInfo.cell().getListHeader().getRowSpan() - 1;
        }
    }

    public enum CellDrawState {
        Nothing,
        Complete,
        Partially
    }
}
