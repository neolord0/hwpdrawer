package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.Column;
import kr.dogfoot.hwplib.drawer.interimoutput.text.MultiColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;
import java.util.Set;

public class Content {
    private final ArrayList<MultiColumn> multiColumns;
    private MultiColumn currentMultiColumn;

    public Content() {
        multiColumns = new ArrayList<>();
        addNewMultiColumn(null);
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

    public boolean rearrangedForDistributionColumn() {
        return currentMultiColumn().rearrangedForDistributionColumn();
    }

    public void rearrangedForDistributionColumn(boolean rearrangedForDistributionColumn) {
        currentMultiColumn().rearrangedForDistributionColumn(rearrangedForDistributionColumn);
    }

    public Column currentColumn() {
        return currentMultiColumn.currentColumn();
    }

    public void nextColumn() {
        currentMultiColumn.nextColumn();
    }

    public void previousColumn() {
        currentMultiColumn.previousColumn();
    }

    public void addChildOutput(ControlOutput output) {
        currentColumn().addChildOutput(output);
    }

    public void setLastTextPartToLastLine() {
        currentColumn().setLastTextPartToLastLine();
    }

    public void addTextLine(TextLine line) {
        currentColumn().addTextLine(line);
    }

    public boolean checkRedrawingTextLine(Area area) {
        return currentColumn().checkRedrawingTextLine(area);
    }

    public TextLine deleteRedrawingTextLine(Area area) {
        return currentColumn().deleteRedrawingTextLine(area);
    }

    public TextLine[] textLines() {
        return currentColumn().textLines();
    }

    public int textLineCount() {
        return currentColumn().textLineCount();
    }

    public TextLine hideTextLineIndex(int topLineIndex) {
        return currentColumn().hideTextLineIndex(topLineIndex);
    }

    public TextLine deleteTextLineIndex(int topLineIndex) {
        return currentColumn().deleteTextLineIndex(topLineIndex);
    }

    public void clearColumn() {
        currentColumn().clear();;
    }

    public Set<ControlOutput> behindChildOutputs() {
        return currentColumn().behindChildOutputs();
    }

    public Set<ControlOutput> nonBehindChildOutputs() {
        return currentColumn().nonBehindChildOutputs();
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

    public void calculateColumnHeight() {
        currentColumn().calculateHeight();
    }

    public long multiColumnHeight() {
        return currentMultiColumn().height();
    }
}

