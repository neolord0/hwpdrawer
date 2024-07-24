package kr.dogfoot.hwpdrawer.output.text;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.MyStringBuilder;
import kr.dogfoot.hwpdrawer.input.paralist.ColumnsInfo;

import java.util.ArrayList;

public class TextRow {
    public final static long Gsp = 1200; // 약 4mm
    public final static TextRow[] Zero_Array = new TextRow[0];

    private Area area;
    private final ArrayList<TextColumn> columns;
    private int currentColumnIndex;
    private boolean hadRearrangedDistributionMultiColumn;
    private int calculationCount;

    public TextRow(ColumnsInfo columnsInfo) {
        area = new Area(columnsInfo.textBoxArea());
        columns = new ArrayList<>();

        for (Area columnArea : columnsInfo.columnAreas()) {
            addNewColumn(columnArea);
        }

        currentColumnIndex = 0;
        hadRearrangedDistributionMultiColumn = false;
        calculationCount = 0;
    }

    public TextRow(Area area) {
        this.area = null;
        columns = new ArrayList<>();

        addNewColumn(area);

        currentColumnIndex = 0;
        hadRearrangedDistributionMultiColumn = false;
        calculationCount = 0;
    }

    private void addNewColumn(Area columnArea) {
        TextColumn column = new TextColumn(columnArea);
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

    public TextColumn[] columns() {
        return columns.toArray(TextColumn.Zero_Array);
    }


    public TextColumn currentColumn() {
        return columns.get(currentColumnIndex);
    }

    public void nextColumn() {
        currentColumnIndex++;
    }

    public void gotoColumn(int columnIndex) {
        this.currentColumnIndex = columnIndex;
    }

    public void previousColumn() {
        currentColumnIndex--;
    }

    public long height() {
        long height = -1;
        for (TextColumn column : columns) {
            long columnHeight = column.calculateHeight();
            height = height > columnHeight ? height : columnHeight;
        }
        return height;
    }

    public long bottom() {
        if (area == null) {
            return -1;
        }
        return area.top() + height();
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return hadRearrangedDistributionMultiColumn;
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        this.hadRearrangedDistributionMultiColumn = hadRearrangedDistributionMultiColumn;
    }

    public boolean empty() {
        for (TextColumn column : columns) {
            if (column.empty() == false) {
                return false;
            }
        }
        return true;
    }

    public CharInfo firstChar() {
        if (columns.get(0).firstLine() != null) {
            return columns.get(0).firstLine().firstChar();
        } else {
            return null;
        }
    }

    public void clear() {
        for (TextColumn column : columns) {
            column.clear();
        }
    }

    public int calculationCount() {
        return calculationCount;
    }

    public void increaseCalculationCount() {
        calculationCount++;
    }

    public boolean removeControl(ControlOutput controlOutput) {
        for (TextColumn column : columns) {
            if (column.removeControl(controlOutput)) {
                return true;
            }
        }
        return false;
    }


    public String test(int tabCount) {
        return test(tabCount, false);
    }

    public String test(int tabCount, boolean hideLine) {
        MyStringBuilder sb = new MyStringBuilder();
        for (TextColumn column : columns) {
            sb.tab(tabCount).append("Column : ").append(column.area()).append(" -  {\n");
            sb.append(column.test(tabCount + 1, hideLine));
            sb.tab(tabCount).append("Column - }\n");
        }
        return sb.toString();
    }
}
