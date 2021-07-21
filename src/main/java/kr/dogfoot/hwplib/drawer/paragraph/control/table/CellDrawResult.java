package kr.dogfoot.hwplib.drawer.paragraph.control.table;

import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import org.apache.poi.ss.formula.functions.T;

import java.time.format.TextStyle;

public class CellDrawResult {
    public static CellDrawResult[] ZeroArray = new CellDrawResult[0];

    private Cell cell;
    private boolean split;
    private long height;
    private TextPosition splitPosition;

    public CellDrawResult() {
        split = false;
    }

    public boolean split() {
        return split;
    }

    public CellDrawResult split(boolean split) {
        this.split = split;
        return this;
    }

    public long height() {
        return height;
    }

    public CellDrawResult height(long height) {
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

    public void cell(Cell cell) {
        this.cell = cell;
    }
}
