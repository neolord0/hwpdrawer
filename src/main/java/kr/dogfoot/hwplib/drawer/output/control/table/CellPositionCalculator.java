package kr.dogfoot.hwplib.drawer.output.control.table;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.TreeSet;

public class CellPositionCalculator {
    private final long[] currentCellTop;

    private final long[] cellXs;

    private final RowHeight[] rowHeights;

    private final ArrayList<RowInfo> rowInfos;

    private final TreeSet<ColInfo> colInfos;

    public CellPositionCalculator(int columnCount, int rowCount) {
        currentCellTop = new long[columnCount];

        cellXs = new long[columnCount + 1];
        cellXs[0] = 0;

        rowHeights = new RowHeight[rowCount];
        for (int index = 0; index < rowCount; index++) {
            rowHeights[index] = new RowHeight();
        }

        colInfos = new TreeSet<>();
        rowInfos = new ArrayList<>();
    }

    public void reset() {
        for (int index = 0; index < currentCellTop.length; index++) {
            currentCellTop[index] = 0;
        }
        for (int index = 0; index < cellXs.length; index++) {
            cellXs[index] = 0;
        }
        for (int index = 0; index < rowHeights.length; index++) {
            rowHeights[index].calculatedHeight = -1;
        }
        colInfos.clear();
        rowInfos.clear();
    }


    public void addInfo(int colIndex, int colSpan, int rowIndex, int rowSpan, long width, long height, long originalHeight) {
        addColumnInfo(colIndex, colSpan, width);
        addRowInfo(rowIndex, rowSpan, height, originalHeight);
        setCurrentCellTop(colIndex, colSpan, height);
    }


    private CellPositionCalculator addColumnInfo(int colIndex, int colSpan, long width) {
        for (ColInfo colInfo : colInfos) {
            if (colInfo.index == colIndex && colInfo.span == colSpan) {
                colInfo.width = Math.max(colInfo.width, width);
                return this;
            }
        }
        colInfos.add(new ColInfo(colIndex, colSpan, width));
        return this;
    }

    private CellPositionCalculator addRowInfo(int rowIndex, int rowSpan, long height, long originalHeight) {
        for (RowInfo rowInfo : rowInfos) {
            if (rowInfo.index == rowIndex && rowInfo.span == rowSpan) {
                rowInfo.height = Math.max(rowInfo.height, height);
                rowInfo.originalHeight = originalHeight;
                return this;
            }
        }
        rowInfos.add(new RowInfo(rowIndex, rowSpan, height, originalHeight));
        return this;
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
        calculateColXs();
        calculateRowHeights();
    }

    private void calculateColXs() {
        for (ColInfo colInfo : colInfos) {
            cellXs[colInfo.index + colInfo.span] = colInfo.width + cellXs[colInfo.index];
        }
    }

    private void calculateRowHeights() {
        ArrayList<RowInfo> removingObjects = new ArrayList<>();

        ArrayList<RowInfo> clonedRowInfos = (ArrayList<RowInfo>) rowInfos.clone();
        int span = 1;
        while (clonedRowInfos.size() > 0) {
            for (RowInfo rowInfo : clonedRowInfos) {
                if (rowInfo.span == span) {
                    set(rowInfo);
                    removingObjects.add(rowInfo);
                }
            }

            clonedRowInfos.removeAll(removingObjects);
            removingObjects.clear();

            span++;
        }


        for (RowInfo rowInfo : rowInfos) {
            if (notSet(rowInfo)) {
                for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++){
                    if (rowInfo.index == index) {
                        rowHeights[index].calculatedHeight = rowInfo.height - rowInfo.span + 1;
                    } else {
                        rowHeights[index].calculatedHeight = 1;
                    }
                }
            }
        }
    }

    private boolean notSet(RowInfo rowInfo) {
        for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++) {
            if (rowHeights[index].calculatedHeight != -1) {
                return false;
            }
        }
        return true;
    }


    private void set(RowInfo rowInfo) {
        if (rowInfo.span == 1) {
            set(rowInfo.index, rowInfo.height, rowInfo.originalHeight);
        } else {
            int emptyIndex = emptyIndex(rowInfo);
            if (emptyIndex >= 0) {
                set(emptyIndex, rowInfo.height - height(rowInfo.index, rowInfo.span), -1);
            } else if (emptyIndex == -1) {
                int zeroCellHeightIndex = zeroCellHeightIndex(rowInfo);
                if (zeroCellHeightIndex >= 0) {
                    set(zeroCellHeightIndex, rowInfo.height - heightExceptedZeroCellHeight(rowInfo.index, rowInfo.span), -1);
                }

                if (rowInfo.height > height(rowInfo.index, rowInfo.span)) {
                    set(rowInfo.index + rowInfo.span - 1, rowInfo.height - height(rowInfo.index, rowInfo.span - 1), -1);
                }
            }
        }
    }

    private int emptyIndex(RowInfo rowInfo) {
        int count = 0;
        int emptyIndex = -1;
        for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++) {
            if (rowHeights[index].calculatedHeight == -1) {
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

    private void set(int rowIndex, long height, long originalHeight) {
        if (height > rowHeights[rowIndex].calculatedHeight) {
            rowHeights[rowIndex].calculatedHeight = height;
            rowHeights[rowIndex].cellHeight = originalHeight;
        }
    }

    private int zeroCellHeightIndex(RowInfo rowInfo) {
        int count = 0;
        int zeroCellHeightIndex = -1;
        for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++) {
            if (rowHeights[index].cellHeight == 0) {
                count++;
                zeroCellHeightIndex = index;
            }
        }
        if (count == 1) {
            return zeroCellHeightIndex;
        } else {
            return -1;
        }
    }

    public long height(int rowIndex, int rowSpan) {
        long height = 0;
        for (int index = rowIndex; index < rowIndex + rowSpan; index++) {
            if (rowHeights[index].calculatedHeight != -1) {
                height += rowHeights[index].calculatedHeight;
            }
        }
        return height;
    }

    private long heightExceptedZeroCellHeight(int rowIndex, int rowSpan) {
        long height = 0;
        for (int index = rowIndex; index < rowIndex + rowSpan; index++) {
            if (rowHeights[index].calculatedHeight != -1 && rowHeights[index].cellHeight != 0) {
                height += rowHeights[index].calculatedHeight;
            }
        }
        return height;
    }

    public long y(int rowIndex) {
        long height = 0;
        for (int index = 0; index < rowIndex; index++) {
            height += rowHeights[index].calculatedHeight;
        }
        return height;
    }


    public long x(int colIndex) {
        return cellXs[colIndex];
    }

    public long totalHeight() {
        long totalHeight = 0;
        for (RowHeight rowHeight : rowHeights) {
            totalHeight += rowHeight.calculatedHeight;
        }
        return totalHeight;
    }

    private class ColInfo implements Comparable<ColInfo>{
        int index;
        int span;
        long width;

        public ColInfo(int index, int span, long width) {
            this.index = index;
            this.span = span;
            this.width = width;
        }

        @Override
        public int compareTo(@NotNull CellPositionCalculator.ColInfo other) {
            if (index < other.index) {
                return -1;
            } else if (index > other.index) {
                return 1;
            } else /*if (index == other.index)*/ {
                if (span < other.span) {
                    return  -1;
                } else if (span > other.span) {
                    return 1;
                } else /*if (span == other.span)*/{
                    return 0;
                }
            }
        }
    }

    private static class RowInfo {
        int index;
        int span;
        long height;
        long originalHeight;

        public RowInfo(int index, int span, long height, long originalHeight) {
            this.index = index;
            this.span = span;
            this.height = height;
            this.originalHeight = originalHeight;
        }
    }

    private static class RowHeight {
        long calculatedHeight;
        long cellHeight;

        RowHeight() {
            calculatedHeight = -1;
            cellHeight = -1;
        }
    }
}
