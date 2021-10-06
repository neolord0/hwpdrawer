package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.*;

public class TableDrawInfo {
    private ArrayList<TableOutput> tableOutputs;

    private Map<Cell, CellDrawInfo> cellDrawInfos;
    private int splitStartRowIndex;

    public TableDrawInfo() {
        tableOutputs = new ArrayList<>();
        cellDrawInfos = new HashMap<>();
        splitStartRowIndex = -1;
    }

    public TableDrawInfo addTableOutput(TableOutput tableOutput) {
        tableOutputs.add(tableOutput);
        return this;
    }


    public Queue<TableOutput> tableOutputQueue() {
        Queue<TableOutput> tableOutputQueue = new LinkedList<>();
        for (TableOutput tableOutput : tableOutputs) {
            tableOutputQueue.add(tableOutput);
        }
        return tableOutputQueue;
    }

    public int splitStartRowIndex() {
        return splitStartRowIndex;
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
