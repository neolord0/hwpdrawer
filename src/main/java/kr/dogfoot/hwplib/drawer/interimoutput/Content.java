package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.Column;
import kr.dogfoot.hwplib.drawer.interimoutput.text.MultiColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;
import java.util.Set;

public class Content {
    private final ArrayList<MultiColumn> multiColumns;
    private MultiColumn currentMultiColumn;

    public Content(Area area) {
        multiColumns = new ArrayList<>();
        addNewMultiColumn(area);
    }

    private void addNewMultiColumn(Area area) {
        MultiColumn multiColumn = new MultiColumn(area);
        multiColumns.add(multiColumn);
        currentMultiColumn = multiColumn;
    }

    public Content(ColumnsInfo columnsInfo) {
        multiColumns = new ArrayList<>();
        addNewMultiColumn(columnsInfo);
    }

    public MultiColumn addNewMultiColumn(ColumnsInfo columnsInfo) {
        MultiColumn multiColumn = new MultiColumn(columnsInfo);
        multiColumns.add(multiColumn);
        currentMultiColumn = multiColumn;
        return multiColumn;
    }

    public MultiColumn[] multiColumns() {
        return multiColumns.toArray(MultiColumn.Zero_Array);
    }

    public MultiColumn currentMultiColumn() {
        return currentMultiColumn;
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return currentMultiColumn.hadRearrangedDistributionMultiColumn();
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        currentMultiColumn.hadRearrangedDistributionMultiColumn(hadRearrangedDistributionMultiColumn);
    }

    public ControlOutput[] behindChildOutputs() {
        return currentMultiColumn.currentColumn().behindChildOutputs().toArray(ControlOutput.Zero_Array);
    }


    public ControlOutput[] nonBehindChildOutputs() {
        return currentMultiColumn.currentColumn().nonBehindChildOutputs().toArray(ControlOutput.Zero_Array);
    }

    public TextLine[] textLines() {
        return currentMultiColumn.currentColumn().textLines();
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

    public long multiColumnHeight() {
        return currentMultiColumn.height();
    }

    public long multiColumnBottom() {
        return currentMultiColumn.bottom();
    }
}

