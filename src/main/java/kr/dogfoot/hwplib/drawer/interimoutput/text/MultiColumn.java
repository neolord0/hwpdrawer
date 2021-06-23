package kr.dogfoot.hwplib.drawer.interimoutput.text;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;

public class MultiColumn {
    public final static long Gsp = 1200; // 약 4mm

    public final static MultiColumn[] Zero_Array = new MultiColumn[0];

    private Area area;
    private final ArrayList<Column> columns;
    private int currentColumnIndex;
    private boolean hadRearrangedDistributionMultiColumn;

    public MultiColumn(ColumnsInfo columnsInfo) {
        columns = new ArrayList<>();
        area = new Area(columnsInfo.textBoxArea());

        for (Area columnArea : columnsInfo.columnAreas()) {
            addNewColumn(columnArea);
        }

        currentColumnIndex = 0;
        hadRearrangedDistributionMultiColumn = false;
    }

    public MultiColumn(Area area) {
        columns = new ArrayList<>();
        addNewColumn(area);

        currentColumnIndex = 0;
        hadRearrangedDistributionMultiColumn = false;
    }

    private void addNewColumn(Area columnArea) {
        Column column = new Column(columnArea);
        columns.add(column);
    }

    public Area area() {
        return area;
    }

    public void area(Area area) {
        this.area = area;
    }

    public int columnCount() {
        return columns.size();
    }

    public Column[] columns() {
        return columns.toArray(Column.Zero_Array);
    }

    public Column currentColumn() {
        return columns.get(currentColumnIndex);
    }

    public void nextColumn() {
        currentColumnIndex++;
    }

    public int currentColumnIndex() {
        return currentColumnIndex;
    }

    public void gotoColumnIndex(int columnIndex) {
        this.currentColumnIndex = columnIndex;
    }

    public void previousColumn() {
        currentColumnIndex--;
    }

    public long height() {
        long height = -1;
        for (Column column : columns) {
            long columnHeight = column.calculateHeight();
            height = height > columnHeight ? height : columnHeight;
        }
        return height;
    }

    public long bottom() {
        return area.top() + height();
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return hadRearrangedDistributionMultiColumn;
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        this.hadRearrangedDistributionMultiColumn = hadRearrangedDistributionMultiColumn;
    }

    public boolean empty() {
        for (Column column : columns) {
            if (column.empty() == false) {
                return false;
            }
        }
        return true;
    }


    public String test(int tabCount) {
        return test(tabCount, false);
    }

    public String test(int tabCount, boolean hideLine) {
        MyStringBuilder sb = new MyStringBuilder();
        for (Column column : columns) {
            sb.tab(tabCount).append("Column : ").append(column.area()).append(" -  {\n");
            sb.append(column.test(tabCount + 1, hideLine));
            sb.tab(tabCount).append("Column - }\n");
        }
        return sb.toString();
    }
}
