package kr.dogfoot.hwplib.drawer.input.paralist;

public class ParallelMultiColumnInfo {
    private int startingPageNo;
    private int startingRowIndex;

    public ParallelMultiColumnInfo() {
        startingPageNo = -1;
        startingRowIndex = -1;
    }

    public void startParallelMultiColumn(int pageNo, int rowIndex) {
        startingPageNo = pageNo;
        startingRowIndex = rowIndex;
    }

    public int startingPageNo() {
        return startingPageNo;
    }

    public int startingRowIndex() {
        return startingRowIndex;
    }
}



