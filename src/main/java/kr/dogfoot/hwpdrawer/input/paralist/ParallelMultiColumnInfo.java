package kr.dogfoot.hwpdrawer.input.paralist;

import kr.dogfoot.hwpdrawer.output.Output;

import java.util.ArrayList;

public class ParallelMultiColumnInfo {
    private ArrayList<ParentInfo> parentInfos;
    private int currentParentIndex;

    private int startingRowIndex;

    public ParallelMultiColumnInfo() {
        parentInfos = new ArrayList<>();
        startingRowIndex = -1;
    }

    public void startParallelMultiColumn(int rowIndex, Output parentOutput, ParagraphListInfo.CellInfo cellInfo) {
        parentInfos.clear();
        currentParentIndex = 0;
        startingRowIndex = rowIndex;
        addParentInfo(parentOutput, cellInfo);
    }

    public int startingRowIndex() {
        return startingRowIndex;
    }

    public void addParentInfo(Output parentOutput, ParagraphListInfo.CellInfo cellInfo) {
        if (!alreadyAdded(parentOutput)) {
            parentInfos.add(new ParentInfo(parentOutput, cellInfo));
        }
    }

    private boolean alreadyAdded(Output parentOutput) {
        for (ParentInfo parentInfo : parentInfos) {
            if (parentInfo.output == parentOutput) {
                return true;
            }
        }
        return false;
    }

    public void nextColumn() {
        currentParentIndex = 0;
    }

    public ParentInfo nextParentInfo() {
        if (currentParentIndex >= parentInfos.size()) {
            return null;
        }
        ParentInfo parentInfo = parentInfos.get(currentParentIndex);
        currentParentIndex++;
        return parentInfo;
    }

    public static class ParentInfo {
        private Output output;
        private ParagraphListInfo.CellInfo cellInfo;

        public ParentInfo(Output output, ParagraphListInfo.CellInfo cellInfo) {
            this.output = output;
            this.cellInfo = cellInfo;
        }

        public Output output() {
            return output;
        }

        public ParagraphListInfo.CellInfo cellInfo() {
            return cellInfo;
        }
    }
}



