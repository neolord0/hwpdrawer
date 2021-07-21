package kr.dogfoot.hwplib.drawer.interimoutput.control.table;

import java.util.ArrayList;

public class CellPositionCalculator {
    private final long[] currentCellTop;

    private final long[] cellXs;

    private final long[] rowHeights;
    private final ArrayList<RowInfo> rowInfos;

    public CellPositionCalculator(int columnCount, int rowCount) {
        currentCellTop = new long[columnCount];

        cellXs = new long[columnCount + 1];
        cellXs[0] = 0;

        rowHeights = new long[rowCount];

        rowInfos = new ArrayList<>();
        for (int index = 0; index < rowCount; index++) {
            rowHeights[index] = -1;
        }
    }

    public void addInfo(int colIndex, int colSpan, int rowIndex, int rowSpan, long width, long height) {
        addColumnInfo(colIndex, colSpan, width);
        addRowInfo(rowIndex, rowSpan, height);
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

    private CellPositionCalculator addRowInfo(int rowIndex, int rowSpan, long height) {
        rowInfos.add(new RowInfo(rowIndex, rowSpan, height));
        return this;
    }

    public void calculate() {
        ArrayList<RowInfo> deletings = new ArrayList<>();
        while (rowInfos.size() > 0) {
            for (RowInfo rowInfo : rowInfos) {
                int emptyIndex = emptyIndex(rowInfo);
                if (emptyIndex >= 0) {
                    set(emptyIndex, rowInfo.height - height(rowInfo.index, rowInfo.span));
                    deletings.add(rowInfo);
                } else if (emptyIndex == -1) {
                    if (rowInfo.height > height(rowInfo.index, rowInfo.span)) {
                        set(rowInfo.index + rowInfo.span - 1, rowInfo.height - height(rowInfo.index, rowInfo.span - 1));
                    }
                    deletings.add(rowInfo);
                }
            }
            rowInfos.removeAll(deletings);
            deletings.clear();
        }
    }

    private int emptyIndex(RowInfo rowInfo) {
        int count = 0;
        int emptyIndex = -1;
        for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++) {
            if (rowHeights[index] == -1) {
                count++;
                emptyIndex = index;
            }
        }
        if (count == 0) {
            return -1;
        } else if (count > 1) {
            return -2;
        } else {
            return emptyIndex;
        }
    }

    private void set(int rowIndex, long height) {
        if (height > rowHeights[rowIndex]) {
            rowHeights[rowIndex] = height;
        }
    }

    public long height(int rowIndex, int rowSpan) {
        long height = 0;
        for (int index = rowIndex; index < rowIndex + rowSpan; index++) {
            if (rowHeights[index] != -1) {
                height += rowHeights[index];
            }
        }
        return height;
    }

    public long y(int rowIndex) {
        long height = 0;
        for (int index = 0; index < rowIndex; index++) {
            height += rowHeights[index];
        }
        return height;
    }

    private CellPositionCalculator addColumnInfo(int colIndex, int colSpan, long width) {
        cellXs[colIndex + colSpan] = width + cellXs[colIndex];
        return this;
    }

    public long x(int colIndex) {
        return cellXs[colIndex];
    }

    public long totalHeight() {
        long totalHeight = 0;
        for (long height : rowHeights) {
            totalHeight += height;
        }
        return totalHeight;
    }

    private static class RowInfo {
        int index;
        int span;
        long height;

        public RowInfo(int index, int span, long height) {
            this.index = index;
            this.span = span;
            this.height = height;
        }
    }
}
