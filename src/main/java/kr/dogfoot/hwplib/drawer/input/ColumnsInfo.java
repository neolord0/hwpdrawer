package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnInfo;

import java.util.ArrayList;

public class ColumnsInfo {
    private static final float ColumnInfoRate = 1.297607421875f;

    private PageInfo pageInfo;

    private ControlColumnDefine columnDefine;
    private ArrayList<Area> columnAreasFromLeft;
    private ArrayList<Area> columnAreasFromRight;

    private int currentColumnIndex;

    public ColumnsInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;

        columnAreasFromLeft = new ArrayList<>();
        columnAreasFromRight = new ArrayList<>();

        currentColumnIndex = 0;
    }

    public void columnDefine(ControlColumnDefine columnDefine) {
        columnAreasFromLeft.clear();
        columnAreasFromRight.clear();

        this.columnDefine = columnDefine;
        if (columnDefine.getHeader().getProperty().getColumnCount() == 1) {
            columnAreasFromLeft.add(pageInfo.bodyArea());
            columnAreasFromRight.add(pageInfo.columnArea());
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
        currentColumnIndex = 0;
    }

    private void makeColumnAreasFromLeft() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (pageInfo.bodyArea().width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnStartX = pageInfo.bodyArea().left();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                columnAreasFromLeft.add(new Area(columnStartX,
                        pageInfo.bodyArea().top(),
                        columnStartX + columnWidth,
                        pageInfo.bodyArea().bottom()));
                columnStartX += columnWidth + columnGap;
            }
        } else {
            long columnStartX = pageInfo.bodyArea().left();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * ColumnInfoRate;
                float columnGap = columnInfo.getGap() * ColumnInfoRate;

                columnAreasFromLeft.add(new Area(columnStartX,
                        pageInfo.bodyArea().top(),
                        (long) (columnStartX + columnWidth),
                        pageInfo.bodyArea().bottom()));
                columnStartX += columnWidth + columnGap;
            }
        }
    }

    private void makeColumnAreasFromRight() {
        if (columnDefine.getHeader().getProperty().isSameWidth()) {
            long columnWidth = (pageInfo.bodyArea().width()
                    - columnDefine.getHeader().getGapBetweenColumn() * (columnDefine.getHeader().getProperty().getColumnCount() - 1)) / columnDefine.getHeader().getProperty().getColumnCount();
            long columnGap = columnDefine.getHeader().getGapBetweenColumn();

            long columnEndX = pageInfo.bodyArea().right();
            for (int index = 0; index < columnDefine.getHeader().getProperty().getColumnCount(); index++) {
                columnAreasFromRight.add(new Area(columnEndX - columnWidth,
                        pageInfo.bodyArea().top(),
                        columnEndX,
                        pageInfo.bodyArea().bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        } else {
            long columnEndX = pageInfo.bodyArea().right();
            for (ColumnInfo columnInfo : columnDefine.getHeader().getColumnInfoList()) {
                float columnWidth = columnInfo.getWidth() * ColumnInfoRate;
                float columnGap = columnInfo.getGap() * ColumnInfoRate;

                columnAreasFromRight.add(new Area((long) (columnEndX - columnWidth),
                        pageInfo.bodyArea().top(),
                        columnEndX,
                        pageInfo.bodyArea().bottom()));
                columnEndX -= columnWidth + columnGap;
            }
        }
    }

    public Area[] columnAreas() {
        return columnAreasList().toArray(Area.Zero_Array);
    }

    private ArrayList<Area> columnAreasList() {
        switch (columnDefine.getHeader().getProperty().getColumnDirection()) {
            case FromLeft:
                return columnAreasFromLeft;
            case FromRight:
                return columnAreasFromRight;
            case Both:
                if (pageInfo.pageNo() % 2 == 1) {
                    return columnAreasFromLeft;
                } else {
                    return columnAreasFromRight;
                }
        }
        return columnAreasFromLeft;
    }

    public void resetColumn() {
        currentColumnIndex = 0;
    }

    public Area columnArea() {
        return columnAreasList().get(currentColumnIndex);
    }

    public boolean lastColumn() {
        return currentColumnIndex + 1 == columnAreasList().size();
    }

    public void nextColumn() {
        currentColumnIndex++;
    }
}
