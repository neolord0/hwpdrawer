package kr.dogfoot.hwpdrawer.output;

import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.MyStringBuilder;
import kr.dogfoot.hwpdrawer.input.paralist.ColumnsInfo;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.text.TextLine;
import kr.dogfoot.hwpdrawer.output.text.TextRow;

import java.util.ArrayList;

public class Content {
    private final ArrayList<TextRow> rows;
    private int currentRowIndex;

    public Content(Area area) {
        rows = new ArrayList<>();

        TextRow row = new TextRow(area);
        rows.add(row);

        currentRowIndex = 0;
    }

    public Content(ColumnsInfo columnsInfo) {
        rows = new ArrayList<>();

        TextRow row = new TextRow(columnsInfo);
        rows.add(row);

        currentRowIndex = 0;
    }

    public void nextRow(ColumnsInfo columnsInfo) {
        if (currentRow() != null && currentRow().empty()) {
            deleteCurrentRow();
        }

        currentRowIndex++;
        if (currentRowIndex >= rows.size()) {
            TextRow row = new TextRow(columnsInfo);
            rows.add(row);
        }
    }

    private void deleteCurrentRow() {
        rows.remove(currentRowIndex);
        currentRowIndex--;
    }

    public TextRow firstRow() {
        return rows.get(0);
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

    public boolean empty() {
        for (TextRow row : rows) {
            if (!row.empty()) {
                return false;
            }
        }
        return true;
    }

    public boolean removeControl(ControlOutput controlOutput) {
        for (TextRow row : rows) {
            if (row.removeControl(controlOutput)) {
                return true;
            }
        }
        return false;
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

