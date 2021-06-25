package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort;

import java.util.ArrayList;

public class ColumnsInfo {
    private PageInfo pageInfo;
    private ControlColumnDefine columnDefine;
    private Area textBoxArea;

    private ArrayList<Area> areasFromLeft;
    private ArrayList<Area> areasFromRight;
    private int[] limitedTextLineCounts;

    private int currentColumnIndex;

    private boolean processLikeDistributionMultiColumn;

    public ColumnsInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        textBoxArea = new Area();

        areasFromLeft = new ArrayList<>();
        areasFromRight = new ArrayList<>();
        currentColumnIndex = 0;
    }

    public void set(ControlColumnDefine columnDefine, Area textBoxArea) {
        areasFromLeft.clear();
        areasFromRight.clear();

        this.columnDefine = columnDefine;
        this.textBoxArea.set(textBoxArea);

        currentColumnIndex = 0;
        limitedTextLineCounts = null;

        if (columnDefine == null || columnDefine.getHeader().getProperty().getColumnCount() == 1) {
            areasFromLeft.add(textBoxArea);
            areasFromRight.add(currentColumnArea());
        } else {
            switch (columnDefine.getHeader().getProperty().getColumnDirection()) {
                case FromLeft:
                    makeColumnAreasFromLeft();
                    break;
                case FromRight:
                    makeColumnAreasFromRight();
                    break;
                case Both:
                    makeColumnAreasFromLeft();
                    makeColumnAreasFromRight();
                    break;
            }
        }
        processLikeDistributionMultiColumn = false;
    }

    public void setWithPreviousColumnDefine(Area textBoxArea) {
        set(columnDefine, textBoxArea);
    }

    public void set(Area textBoxArea) {
        set(columnDefine, textBoxArea);
    }

    public void setWithPreviousInfo() {
        set(columnDefine, textBoxArea);
    }

    private void makeColumnAreasFromLeft() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (textBoxArea.width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnStartX = textBoxArea.left();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                areasFromLeft.add(new Area(columnStartX,
                        textBoxArea.top(),
                        columnStartX + columnWidth,
                        textBoxArea.bottom()));
                columnStartX += columnWidth + columnGap;
            }
        } else {
            long columnStartX = textBoxArea.left();
            float rate = textBoxArea.width() / (float) sumColumnINfo();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * rate;
                float columnGap = columnInfo.getGap() * rate;

                areasFromLeft.add(new Area(columnStartX,
                        textBoxArea.top(),
                        (long) (columnStartX + columnWidth),
                        textBoxArea.bottom()));
                columnStartX += columnWidth + columnGap;
            }
        }
    }

    private long sumColumnINfo() {
        long sum = 0;
        for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
            sum += columnInfo.getWidth() + columnInfo.getGap();
        }
        return sum;
    }

    private void makeColumnAreasFromRight() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (textBoxArea.width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnEndX = textBoxArea.right();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                areasFromRight.add(new Area(columnEndX - columnWidth,
                        textBoxArea.top(),
                        columnEndX,
                        textBoxArea.bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        } else {
            long columnEndX = textBoxArea.right();
            float rate = textBoxArea.width() / (float) sumColumnINfo();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * rate;
                float columnGap = columnInfo.getGap() * rate;

                areasFromRight.add(new Area((long) (columnEndX - columnWidth),
                        textBoxArea.top(),
                        columnEndX,
                        textBoxArea.bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        }
    }

    public Area textBoxArea() {
        return textBoxArea;
    }

    public Area[] columnAreas() {
        return areaList().toArray(Area.Zero_Array);
    }

    private ArrayList<Area> areaList() {
        if (columnDefine == null) {
            return areasFromLeft;
        }

        switch (columnDefine.getHeader().getProperty().getColumnDirection()) {
            case FromLeft:
                return areasFromLeft;
            case FromRight:
                return areasFromRight;
            case Both:
                if (pageInfo.pageNo() % 2 == 1) {
                    return areasFromLeft;
                } else {
                    return areasFromRight;
                }
        }
        return areasFromLeft;
    }

    public void reset() {
        currentColumnIndex = 0;
    }

    public Area currentColumnArea() {
        return areaList().get(currentColumnIndex);
    }

    public boolean lastColumn() {
        return currentColumnIndex + 1 == areaList().size();
    }

    public void nextColumn() {
        currentColumnIndex++;
    }

    public void previousColumn() {
        currentColumnIndex--;
    }

    public ColumnSort columnSort() {
        return columnDefine.getHeader().getProperty().getColumnSort();
    }

    public boolean processLikeDistributionMultiColumn() {
        return processLikeDistributionMultiColumn;
    }

    public void processLikeDistributionMultiColumn(boolean processLikeDistributionMultiColumn) {
        this.processLikeDistributionMultiColumn = processLikeDistributionMultiColumn;
    }

    public boolean isDistributionMultiColumn() {
        if (columnCount() > 1
                && (columnDefine.getHeader().getProperty().getColumnSort() == ColumnSort.Distribution)) {
            return true;
        }
        return false;
    }

    public boolean isParallelMultiColumn() {
        if (columnCount() > 1
                && columnDefine.getHeader().getProperty().getColumnSort() == ColumnSort.Parallel) {
            return true;
        }
        return false;
    }

    public boolean isNormalMultiColumn() {
        if (columnCount() > 1
                && columnDefine.getHeader().getProperty().getColumnSort() == ColumnSort.Normal) {
            return true;
        }
        return false;
    }


    public int currentColumnIndex() {
        return currentColumnIndex;
    }

    public void currentColumnIndex(int currentColumnIndex) {
        this.currentColumnIndex = currentColumnIndex;
    }

    public int columnCount() {
        return areaList().size();
    }

    public void limitedTextLineCounts(int[] limitedTextLineCounts) {
        this.limitedTextLineCounts = limitedTextLineCounts;
    }

    public int limitedTextLineCount() {
        if (limitedTextLineCounts == null) {
            return -1;
        } else {
            return limitedTextLineCounts[currentColumnIndex];
        }
    }

    public boolean isOverLimitedTextLineCount(int textLineCount) {
        if (limitedTextLineCounts != null &&
                limitedTextLineCounts[currentColumnIndex] <= textLineCount) {
            return true;
        }
        return false;
    }
}
