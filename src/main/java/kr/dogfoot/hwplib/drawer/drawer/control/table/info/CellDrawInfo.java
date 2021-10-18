package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.util.CharPosition;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class CellDrawInfo {
    public final static CellDrawInfo[] Zero_Array = new CellDrawInfo[0];

    private State state;
    private Cell cell;
    private long height;
    private CharPosition dividedPosition;
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

    public CharPosition dividedPosition() {
        return dividedPosition;
    }

    public CellDrawInfo dividedPosition(CharPosition dividedPosition) {
        this.dividedPosition = dividedPosition;
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
        Divided,
        OverPage,
        ;

        public boolean isDivided() {
            return this == Divided;
        }

        public boolean isNormal() {
            return this == Normal;
        }

        public boolean isOverPage() {
            return this == OverPage;
        }
    }
}


