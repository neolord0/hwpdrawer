package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class ColumnStates {
    private CellDrawState[] states;

    public ColumnStates() {
        states = null;
    }

    public void init(int columnCount) {
        states = new CellDrawState[columnCount];
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            states[columnIndex] = CellDrawState.Nothing;
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
        for (int colIndex = cell.getListHeader().getColIndex();
             colIndex < cell.getListHeader().getColIndex() + cell.getListHeader().getColSpan();
             colIndex++) {
            if (states[colIndex] == CellDrawState.Partially) {
                return false;
            }
        }
        return true;
    }

    public void setState(CellDrawInfo result) {
        CellDrawState state = (result.split()) ? CellDrawState.Partially : CellDrawState.Complete;
        for (int colIndex = result.cell().getListHeader().getColIndex();
             colIndex < result.cell().getListHeader().getColIndex() + result.cell().getListHeader().getColSpan();
             colIndex++) {
            states[colIndex] = state;
        }
    }

    public enum CellDrawState {
        Nothing,
        Complete,
        Partially
    }

}

