package kr.dogfoot.hwplib.drawer.interimoutput.text;
import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;

public class MultiColumn {
    public final static MultiColumn[] Zero_Array = new MultiColumn[0];

    private Area area;
    private final ArrayList<Column> columns;
    private int currentColumnIndex;

    public MultiColumn(ColumnsInfo columnsInfo) {
        columns = new ArrayList<>();
        if (columnsInfo == null) {
            addNewColumn(null);
        } else {
            for (Area columnArea : columnsInfo.columnAreas())  {
                addNewColumn(columnArea);
            }
        }

        currentColumnIndex = 0;
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

    public Column[] columns() {
        return columns.toArray(Column.Zero_Array);
    }

    public Column currentColumn() {
        return columns.get(currentColumnIndex);
    }

    public void nextColumn() {
        currentColumnIndex++;
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

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        for (Column column : columns) {
            sb.tab(tabCount).append("Column - {\n");
            sb.append(column.test(tabCount + 1));
            sb.tab(tabCount).append("Column - }\n");
        }
        return sb.toString();
    }
}
