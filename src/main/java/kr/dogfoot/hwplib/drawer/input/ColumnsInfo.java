package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort;

import java.util.ArrayList;

public class ColumnsInfo {
    private static final float ColumnInfoRate = 1.297607421875f;

    private PageInfo pageInfo;
    private ControlColumnDefine columnDefine;
    private Area multiColumnArea;

    private ArrayList<Area> areasFromLeft;
    private ArrayList<Area> areasFromRight;
    private int[] limitedTextLineCounts;

    private int currentColumnIndex;

    public ColumnsInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        multiColumnArea = new Area();

        areasFromLeft = new ArrayList<>();
        areasFromRight = new ArrayList<>();
        currentColumnIndex = 0;
    }

    public void set(ControlColumnDefine columnDefine, Area multiColumnArea) {
        ;
        areasFromLeft.clear();
        areasFromRight.clear();

        this.columnDefine = columnDefine;
        this.multiColumnArea = multiColumnArea;

        currentColumnIndex = 0;
        limitedTextLineCounts = null;

        if (columnDefine.getHeader().getProperty().getColumnCount() == 1) {
            areasFromLeft.add(multiColumnArea);
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
    }

    public void setWithSameColumnDefine(Area multiColumnArea) {
        set(columnDefine, multiColumnArea);
    }

    public void set(Area multiColumnArea) {
        ;
        set(columnDefine, multiColumnArea);
    }

    public void set() {
        set(columnDefine, multiColumnArea);
    }

    private void makeColumnAreasFromLeft() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (multiColumnArea.width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnStartX = multiColumnArea.left();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                areasFromLeft.add(new Area(columnStartX,
                        multiColumnArea.top(),
                        columnStartX + columnWidth,
                        multiColumnArea.bottom()));
                columnStartX += columnWidth + columnGap;
            }
        } else {
            long columnStartX = multiColumnArea.left();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * ColumnInfoRate;
                float columnGap = columnInfo.getGap() * ColumnInfoRate;

                areasFromLeft.add(new Area(columnStartX,
                        multiColumnArea.top(),
                        (long) (columnStartX + columnWidth),
                        multiColumnArea.bottom()));
                columnStartX += columnWidth + columnGap;
            }
        }
    }

    private void makeColumnAreasFromRight() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (multiColumnArea.width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnEndX = multiColumnArea.right();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                areasFromRight.add(new Area(columnEndX - columnWidth,
                        multiColumnArea.top(),
                        columnEndX,
                        multiColumnArea.bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        } else {
            long columnEndX = multiColumnArea.right();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * ColumnInfoRate;
                float columnGap = columnInfo.getGap() * ColumnInfoRate;

                areasFromRight.add(new Area((long) (columnEndX - columnWidth),
                        multiColumnArea.top(),
                        columnEndX,
                        multiColumnArea.bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        }
    }

    public Area multiColumnArea() {
        return multiColumnArea;
    }

    public Area[] columnAreas() {
        return areaList().toArray(Area.Zero_Array);
    }

    private ArrayList<Area> areaList() {
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

    public boolean isDistributionMultiColumn() {
        if (areasFromLeft.size() > 1
                && columnDefine.getHeader().getProperty().getColumnSort() == ColumnSort.Distribution) {
            return true;
        }
        return false;
    }

    public boolean isParallelMultiColumn() {
        if (areasFromLeft.size() > 1
                && columnDefine.getHeader().getProperty().getColumnSort() == ColumnSort.Parallel) {
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
