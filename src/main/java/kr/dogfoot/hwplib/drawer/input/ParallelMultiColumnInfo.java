package kr.dogfoot.hwplib.drawer.input;

public class ParallelMultiColumnInfo {
    private int startingPageNo;
    private int startingMultiColumnIndex;

    public ParallelMultiColumnInfo() {
        startingPageNo = -1;
        startingMultiColumnIndex = -1;
    }

    public void startParallelMultiColumn(int pageNo, int multiColumnIndex) {
        startingPageNo = pageNo;
        startingMultiColumnIndex = multiColumnIndex;
    }

    public int startingPageNo() {
        return startingPageNo;
    }

    public int startingMultiColumnIndex() {
        return startingMultiColumnIndex;
    }
}



