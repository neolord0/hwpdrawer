package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.paralist.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextRow;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;

public class Content {
    private final ArrayList<TextRow> rows;
    private int currentRowIndex;

    public Content(Area area) {
        rows = new ArrayList<>();

        TextRow multiColumn = new TextRow(area);
        rows.add(multiColumn);

        currentRowIndex = 0;
    }

    public Content(ColumnsInfo columnsInfo) {
        rows = new ArrayList<>();

        TextRow multiColumn = new TextRow(columnsInfo);
        rows.add(multiColumn);

        currentRowIndex = 0;
    }

    public void nextRow(ColumnsInfo columnsInfo) {
        if (currentRow() != null && currentRow().empty()) {
            deleteCurrentRow();
        }

        currentRowIndex++;
        if (currentRowIndex >= rows.size()) {
            TextRow multiColumn = new TextRow(columnsInfo);
            rows.add(multiColumn);
        }
    }

    private void deleteCurrentRow() {
        rows.remove(currentRowIndex);
        currentRowIndex--;
    }

    public int currentRowIndex() {
        return currentRowIndex;
    }

    public TextRow gotoRow(int rowIndex) {
        currentRowIndex = rowIndex;
        return rows.get(rowIndex);
    }

    public void gotoLastRow() {
        currentRowIndex = rows.size() - 1;
    }

    public TextRow[] rows() {
        return rows.toArray(TextRow.Zero_Array);
    }

    public TextRow currentRow() {
        return rows.get(currentRowIndex);
    }

    public ControlOutput[] behindChildOutputs() {
        return currentRow().currentColumn().behindChildOutputs().toArray(ControlOutput.Zero_Array);
    }

    public ControlOutput[] nonBehindChildOutputs() {
        return currentRow().currentColumn().nonBehindChildOutputs().toArray(ControlOutput.Zero_Array);
    }

    public TextLine[] textLines() {
        return currentRow().currentColumn().textLines();
    }

    public long rowHeight() {
        return currentRow().height();
    }

    public long rowBottom() {
        return currentRow().bottom();
    }

    public int rowCount() {
        return rows.size();
    }

    public long height() {
        long height = 0;
        for (TextRow row : rows) {
            if (row.columnCount() > 0) {
                height += row.height() + TextRow.Gsp;
            }
        }
        height -= TextRow.Gsp;
        return height;
    }

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        for (TextRow row : rows) {
            sb.tab(tabCount).append("Row - {\n");
            sb.append(row.test(tabCount + 1));
            sb.tab(tabCount).append("Row - }\n");
        }
        return sb.toString();
    }
}

