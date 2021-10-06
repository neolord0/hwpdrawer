package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.util.CharPosition;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class CellDrawInfo {
    public final static CellDrawInfo[] Zero_Array = new CellDrawInfo[0];

    private State state;
    private Cell cell;
    private long height;
    private CharPosition splitPosition;
    private long nextPartHeight;
    private CellOutput cellOutput;
    private int startTextColumnIndex;

    public CellDrawInfo() {
        state = State.Normal;
        nextPartHeight = 0;
        startTextColumnIndex = -1;
    }

    public State state() {
        return state;
    }

    public CellDrawInfo state(State state) {
        this.state = state;
        return this;
    }

    public Cell cell() {
        return cell;
    }

    public CellDrawInfo cell(Cell cell) {
        this.cell = cell;
        return this;
    }

    public long height() {
        return height;
    }

    public CellDrawInfo height(long height) {
        this.height = height;
        return this;
    }

    public CharPosition splitPosition() {
        return splitPosition;
    }

    public CellDrawInfo splitPosition(CharPosition splitPosition) {
        this.splitPosition = splitPosition;
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

    public CellDrawInfo cellOutput(CellOutput cellOutput) {
        this.cellOutput = cellOutput;
        return this;
    }

    public int startTextColumnIndex() {
        return startTextColumnIndex;
    }

    public void startTextColumnIndex(int startTextColumnIndex) {
        this.startTextColumnIndex = startTextColumnIndex;
    }

    public enum State {
        Normal,
        Split,
        OverPage,
        ;

        public boolean isSplit() {
            return this == Split;
        }

        public boolean isNormal() {
            return this == Normal;
        }
    }
}


