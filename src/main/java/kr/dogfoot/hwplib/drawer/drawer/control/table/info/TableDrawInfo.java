package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.*;

public class TableDrawInfo {
    private ArrayList<TableOutput> tableOutputs;

    private Map<Cell, CellDrawInfo> cellDrawInfos;
    private int dividingStartRowIndex;

    public TableDrawInfo() {
        tableOutputs = new ArrayList<>();
        cellDrawInfos = new HashMap<>();
        dividingStartRowIndex = -1;
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

    public int dividingStartRowIndex() {
        return dividingStartRowIndex;
    }

    public void dividingStartRowIndex(int dividingStartRowIndex) {
        if (this.dividingStartRowIndex == -1) {
            this.dividingStartRowIndex = dividingStartRowIndex;
        } else {
            this.dividingStartRowIndex = Math.min(dividingStartRowIndex, this.dividingStartRowIndex);
        }
    }

    public void addCellDrawInfo(CellDrawInfo cellDrawInfo) {
        cellDrawInfos.put(cellDrawInfo.cell(), cellDrawInfo);
    }

    public CellDrawInfo cellCellDrawInfo(Cell cell) {
        return cellDrawInfos.get(cell);
    }
}
