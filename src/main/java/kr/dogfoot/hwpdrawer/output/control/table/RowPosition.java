package kr.dogfoot.hwpdrawer.output.control.table;

import java.util.ArrayList;

public class RowPosition {
    private final ArrayList<RowInfo> rowInfos;
    private final RowHeight[] rowHeights;

    public RowPosition(int rowCount) {
        rowInfos = new ArrayList<>();

        rowHeights = new RowHeight[rowCount];
        for (int index = 0; index < rowCount; index++) {
            rowHeights[index] = new RowHeight();
        }
    }

    public void reset() {
        rowInfos.clear();
        for (int index = 0; index < rowHeights.length; index++) {
            rowHeights[index].calculatedHeight = -1;
        }
    }

    public void addInfo(int rowIndex, int rowSpan, long height, long originalHeight) {
        for (RowInfo rowInfo : rowInfos) {
            if (rowInfo.index == rowIndex && rowInfo.span == rowSpan) {
                rowInfo.height = Math.max(rowInfo.height, height);
                rowInfo.originalHeight = originalHeight;
                return;
            }
        }
        rowInfos.add(new RowInfo(rowIndex, rowSpan, height, originalHeight));
    }

    public void calculate() {
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

    private void set(int rowIndex, long height, long originalHeight) {
        if (height > rowHeights[rowIndex].calculatedHeight) {
            rowHeights[rowIndex].calculatedHeight = height;
            rowHeights[rowIndex].cellHeight = originalHeight;
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

    private boolean notSet(RowInfo rowInfo) {
        for (int index = rowInfo.index; index < rowInfo.index + rowInfo.span; index++) {
            if (rowHeights[index].calculatedHeight != -1) {
                return false;
            }
        }
        return true;
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
        long y = 0;
        for (int index = 0; index < rowIndex; index++) {
            y += rowHeights[index].calculatedHeight;
        }
        return y;
    }

    public long totalHeight() {
        long totalHeight = 0;
        for (RowHeight rowHeight : rowHeights) {
            totalHeight += rowHeight.calculatedHeight;
        }
        return totalHeight;
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
