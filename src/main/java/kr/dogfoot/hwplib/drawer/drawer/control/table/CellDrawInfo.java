package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class CellDrawInfo {
    private Cell cell;
    private boolean split;
    private long height;
    private TextPosition splitPosition;
    private long nextPartHeight;
    private CellOutput cellOutput;
    private int textColumnIndex;

    public CellDrawInfo() {
        split = false;
        nextPartHeight = 0;
        textColumnIndex = -1;
    }

    public boolean split() {
        return split;
    }

    public CellDrawInfo split(boolean split) {
        this.split = split;
        return this;
    }

    public long height() {
        return height;
    }

    public CellDrawInfo height(long height) {
        this.height = height;
        return this;
    }

    public TextPosition splitPosition() {
        return splitPosition;
    }

    public CellDrawInfo splitPosition(TextPosition splitPosition) {
        this.splitPosition = splitPosition;
        return this;
    }

    public Cell cell() {
        return cell;
    }

    public CellDrawInfo cell(Cell cell) {
        this.cell = cell;
        return this;
    }

    public long nextPartHeight() {
        return nextPartHeight;
    }

    public void nextPartHeight(long nextPartHeight) {
        this.nextPartHeight = nextPartHeight;
    }

    public CellOutput cellOutput() {
        return cellOutput;
    }

    public void cellOutput(CellOutput cellOutput) {
        this.cellOutput = cellOutput;
    }

    public int textColumnIndex() {
        return textColumnIndex;
    }

    public void textColumnIndex(int textColumnIndex) {
        this.textColumnIndex = textColumnIndex;
    }

}


