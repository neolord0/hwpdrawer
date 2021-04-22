package kr.dogfoot.hwplib.drawer.painter.control;

import java.util.ArrayList;

public class CellPositionCalculator {
    private long[] cellXs;

    private ArrayList<RowInfo> rowInfos;
    private long[] rowHeights;

    public CellPositionCalculator() {
        rowInfos = new ArrayList<>();
        cellXs = null;
        rowHeights = null;
    }

    public void reset(int columnCount,int rowCount) {
        cellXs = new long[columnCount + 1];
        cellXs[0] = 0;

        rowInfos.clear();
        rowHeights = new long[rowCount];
   }


    public CellPositionCalculator addRowInfo(int rowIndex, int rowSpan, long height) {
        rowInfos.add(new RowInfo(rowIndex, rowSpan, height));
        return this;
    }

    public void calculate() {
        for (RowInfo rowInfo : rowInfos) {
            if (rowInfo.span == 1) {
                set(rowInfo.index, rowInfo.height);
            }
        }
        for (RowInfo rowInfo : rowInfos) {
            if (rowInfo.span > 1) {
                if (rowInfo.height > height(rowInfo.index, rowInfo.span)) {
                    set(rowInfo.index + rowInfo.span - 1, rowInfo.height - height(rowInfo.index, rowInfo.span - 1));
                }
            }
        }
    }

    private void set(int rowIndex, long height) {
        if (height > rowHeights[rowIndex]) {
            rowHeights[rowIndex] = height;
        }
    }


    public long height(int rowIndex, int rowSpan) {
        long height = 0;
        for (int index = rowIndex ; index < rowIndex + rowSpan; index++) {
            height += rowHeights[index];
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


    public CellPositionCalculator addColumnInfo(int colIndex, int colSpan, long width) {
        cellXs[colIndex + colSpan] = width + cellXs[colIndex];
        return this;
    }

    public long x(int colIndex) {
        return cellXs[colIndex];
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
