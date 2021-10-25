package kr.dogfoot.hwplib.drawer.output.control.table;

import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.TreeSet;

public class CellPositionCalculator {
    private final long[] currentCellTop;

    private final RowPosition rowPosition;
    private final ColumnPosition columnPosition;

    public CellPositionCalculator(int columnCount, int rowCount) {
        currentCellTop = new long[columnCount];

        rowPosition = new RowPosition(rowCount);
        columnPosition = new ColumnPosition(columnCount);
    }

    public void reset() {
        for (int index = 0; index < currentCellTop.length; index++) {
            currentCellTop[index] = 0;
        }
        columnPosition.reset();
        rowPosition.reset();
    }

    public void addInfo(int colIndex, int colSpan, int rowIndex, int rowSpan, long width, long height, long originalHeight) {
        columnPosition.addInfo(colIndex, colSpan, width);
        rowPosition.addInfo(rowIndex, rowSpan, height, originalHeight);
        setCurrentCellTop(colIndex, colSpan, height);
    }


    private void setCurrentCellTop(int colIndex, int colSpan, long height) {
        for (int index = colIndex; index < colIndex + colSpan; index++) {
            currentCellTop[index] += height;
        }
    }

    public long currentCellTop(int colIndex) {
        return currentCellTop[colIndex];
    }

    public void calculate() {
        columnPosition.calculate();
        rowPosition.calculate();
    }

    public long height(int rowIndex, int rowSpan) {
        return rowPosition.height(rowIndex, rowSpan);
    }

    public long y(int rowIndex) {
        return rowPosition.y(rowIndex);
    }

    public long x(int colIndex) {
        return columnPosition.x(colIndex);
    }

    public long totalHeight() {
        return rowPosition.totalHeight();
    }
}
