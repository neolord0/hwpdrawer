package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.MultiColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;

public class Content {
    private final ArrayList<MultiColumn> multiColumns;
    private int currentMultiColumnIndex;

    public Content(Area area) {
        multiColumns = new ArrayList<>();

        MultiColumn multiColumn = new MultiColumn(area);
        multiColumns.add(multiColumn);

        currentMultiColumnIndex = 0;
    }

    public Content(ColumnsInfo columnsInfo) {
        multiColumns = new ArrayList<>();

        MultiColumn multiColumn = new MultiColumn(columnsInfo);
        multiColumns.add(multiColumn);

        currentMultiColumnIndex = 0;
    }

    public void nextMultiColumn(ColumnsInfo columnsInfo) {
        if (currentMultiColumn() != null && currentMultiColumn().empty()) {
            deleteCurrentMultiColumn();
        }

        currentMultiColumnIndex++;
        if (currentMultiColumnIndex >= multiColumns.size()) {
            MultiColumn multiColumn = new MultiColumn(columnsInfo);
            multiColumns.add(multiColumn);
        }
    }

    private void deleteCurrentMultiColumn() {
        multiColumns.remove(currentMultiColumnIndex);
        currentMultiColumnIndex--;
    }

    public int currentMultiColumnIndex() {
        return currentMultiColumnIndex;
    }

    public MultiColumn gotoMultiColumn(int multiColumnIndex) {
        currentMultiColumnIndex = multiColumnIndex;
        return multiColumns.get(multiColumnIndex);
    }

    public void gotoLastMultiColumn() {
        currentMultiColumnIndex = multiColumns.size() - 1;
    }

    public MultiColumn[] multiColumns() {
        return multiColumns.toArray(MultiColumn.Zero_Array);
    }

    public MultiColumn currentMultiColumn() {
        return multiColumns.get(currentMultiColumnIndex);
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return currentMultiColumn().hadRearrangedDistributionMultiColumn();
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        currentMultiColumn().hadRearrangedDistributionMultiColumn(hadRearrangedDistributionMultiColumn);
    }

    public ControlOutput[] behindChildOutputs() {
        return currentMultiColumn().currentColumn().behindChildOutputs().toArray(ControlOutput.Zero_Array);
    }


    public ControlOutput[] nonBehindChildOutputs() {
        return currentMultiColumn().currentColumn().nonBehindChildOutputs().toArray(ControlOutput.Zero_Array);
    }

    public TextLine[] textLines() {
        return currentMultiColumn().currentColumn().textLines();
    }

    public long multiColumnHeight() {
        return currentMultiColumn().height();
    }

    public long multiColumnBottom() {
        return currentMultiColumn().bottom();
    }

    public long height() {
        long height = 0;
        for (MultiColumn multiColumn : multiColumns) {
            if (multiColumn.columnCount() > 0) {
                height += multiColumn.height() + MultiColumn.Gsp;
            }
        }
        height -= MultiColumn.Gsp;
        return height;
    }

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        for (MultiColumn multiColumn : multiColumns) {
            sb.tab(tabCount).append("MultiColumn - {\n");
            sb.append(multiColumn.test(tabCount + 1));
            sb.tab(tabCount).append("MultiColumn - }\n");
        }
        return sb.toString();
    }
}

