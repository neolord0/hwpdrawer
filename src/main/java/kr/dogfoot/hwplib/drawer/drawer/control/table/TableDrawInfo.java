package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.*;

public class TableDrawInfo {
    public static TableDrawInfo[] ZeroArray = new TableDrawInfo[0];

    private ArrayList<TableOutput> tableOutputs;
    private TableOutput currentTableOutput;

    private Map<Cell, CellDrawInfo> cellDrawInfos;
    private int splitStartRowIndex;
    private boolean drawContinually;

    public TableDrawInfo() {
        tableOutputs = new ArrayList<>();

        cellDrawInfos = new HashMap<>();
    }

    public TableDrawInfo addTableOutput(TableOutput tableOutput) {
        tableOutputs.add(tableOutput);
        currentTableOutput = tableOutput;
        return this;
    }

    public TableOutput currentTableOutput() {
        return currentTableOutput;
    }

    public Queue<TableOutput> tableOutputQueue() {
        Queue<TableOutput> tableOutputQueue = new LinkedList<>();
        for (TableOutput tableOutput : tableOutputs) {
            tableOutputQueue.add(tableOutput);
        }
        return tableOutputQueue;
    }

    public boolean drawContinually() {
        return drawContinually;
    }

    public TableDrawInfo drawContinually(boolean drawContinually) {
        this.drawContinually = drawContinually;
        return this;
    }

    public int splitStartRowIndex() {
        return splitStartRowIndex;
    }

    public void resetAtDrawing() {
        splitStartRowIndex = -1;
    }

    public void splitStartRowIndex(int splitStartRowIndex) {
        if (this.splitStartRowIndex == -1) {
            this.splitStartRowIndex = splitStartRowIndex;
        } else {
            this.splitStartRowIndex = Math.min(splitStartRowIndex, this.splitStartRowIndex);
        }
    }

    public void addCellDrawInfo(CellDrawInfo cellDrawInfo) {
        cellDrawInfos.put(cellDrawInfo.cell(), cellDrawInfo);
    }

    public CellDrawInfo cellCellDrawInfo(Cell cell) {
        return cellDrawInfos.get(cell);
    }

}
