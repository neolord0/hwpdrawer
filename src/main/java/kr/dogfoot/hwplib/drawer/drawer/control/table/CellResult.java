package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class CellResult {
    private Cell cell;
    private boolean split;
    private long height;
    private TextPosition splitPosition;
    private long nextPartHeight;

    public CellResult() {
        split = false;
        nextPartHeight = 0;
    }

    public boolean split() {
        return split;
    }

    public CellResult split(boolean split) {
        this.split = split;
        return this;
    }

    public long height() {
        return height;
    }

    public CellResult height(long height) {
        this.height = height;
        return this;
    }

    public TextPosition splitPosition() {
        return splitPosition;
    }

    public void splitPosition(TextPosition splitPosition) {
        this.splitPosition = splitPosition;
    }

    public Cell cell() {
        return cell;
    }

    public CellResult cell(Cell cell) {
        this.cell = cell;
        return this;
    }

    public long nextPartHeight() {
        return nextPartHeight;
    }

    public void nextPartHeight(long nextPartHeight) {
        this.nextPartHeight = nextPartHeight;
    }

}


