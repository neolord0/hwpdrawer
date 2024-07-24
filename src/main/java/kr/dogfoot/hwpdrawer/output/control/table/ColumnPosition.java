package kr.dogfoot.hwpdrawer.output.control.table;

import java.util.ArrayList;

public class ColumnPosition {
    private final long[] colWidths;
    private final ArrayList<ColInfo> colInfos;

    public ColumnPosition(int columnCount) {
         colInfos = new ArrayList<>();

        colWidths = new long[columnCount];
        for (int index = 0; index < columnCount; index++) {
            colWidths[index] = -1;
        }
    }

    public void reset() {
        for (int index = 0; index < colWidths.length; index++) {
            colWidths[index] = -1;
        }
        colInfos.clear();
    }

    public void addInfo(int colIndex, int colSpan, long width) {
        for (ColInfo colInfo : colInfos) {
            if (colInfo.index == colIndex && colInfo.span == colSpan) {
                return;
            }
        }
        colInfos.add(new ColInfo(colIndex, colSpan, width));
    }

    public void calculate() {
        ArrayList<ColInfo> removingObjects = new ArrayList<>();


        ArrayList<ColInfo> clonedRowInfos = (ArrayList<ColInfo>) colInfos.clone();
        int span = 1;
        while (span <= colWidths.length && clonedRowInfos.size() > 0) {

            for (ColInfo colInfo : clonedRowInfos) {;
                if (colInfo.span <= span) {
                    if (set(colInfo)) {
                        removingObjects.add(colInfo);
                    }
                }
            }
            clonedRowInfos.removeAll(removingObjects);
            removingObjects.clear();
            span++;
        }
   }

    private boolean set(ColInfo colInfo) {
        if (colInfo.span == 1) {
            set(colInfo.index, colInfo.width);
            return true;
        } else {
            int emptyIndex = emptyIndex(colInfo);
            if (emptyIndex >= 0) {
                set(emptyIndex, colInfo.width - width(colInfo.index, colInfo.span));
                return true;
            } else if (emptyIndex == -1) {
                return true;
            } else {
                for (int colIndex = colInfo.index; colIndex < colInfo.index + colInfo.span; colIndex++) {
                    if (colIndex == colInfo.index) {
                        setTemporary(colInfo.index, colInfo.width - colInfo.span + 1);
                    } else {
                        setTemporary(colInfo.index, 1);
                    }
                }
                return false;
            }
        }
    }

    private void set(int index, long width) {
        colWidths[index] = width;
    }

    private void setTemporary(int index, long width) {
        if (colWidths[index] == -1) {
            colWidths[index] = width;
        }
    }

    private int emptyIndex(ColInfo colInfo) {
        int count = 0;
        int emptyIndex = -1;
        for (int index = colInfo.index; index < colInfo.index + colInfo.span; index++) {
            if (colWidths[index] == -1) {
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


    private long width(int colIndex, int colSpan) {
        long width = 0;
        for (int index = colIndex; index < colIndex + colSpan; index++) {
            if (colWidths[index] != -1) {
                width += colWidths[index];
            }
        }
        return width;
    }

    public long x(int colIndex) {
        long x = 0;
        for (int index = 0; index < colIndex; index++) {
            x += colWidths[index];
        }
        return x;
    }

    private class ColInfo {
        int index;
        int span;
        long width;

        public ColInfo(int index, int span, long width) {
            this.index = index;
            this.span = span;
            this.width = width;
        }
    }
}
