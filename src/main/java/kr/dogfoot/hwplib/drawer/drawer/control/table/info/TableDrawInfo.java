package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

import java.util.*;

public class TableDrawInfo {
    private final ArrayList<TableOutput> tableOutputs;

    private final Map<Cell, CellDrawInfo> cellDrawInfos;
    private final Map<Cell, CellDrawInfo> oldCellDrawInfos;

    private int dividingStartRowIndex;

    public TableDrawInfo() {
        tableOutputs = new ArrayList<>();
        cellDrawInfos = new HashMap<>();
        oldCellDrawInfos = new HashMap<>();
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

    public CellDrawInfo oldCellDrawInfo(Cell cell) {
        return oldCellDrawInfos.get(cell);
    }

    public void correctStateOfCellWithSameRow() {
        ArrayList<CellDrawInfo> dividedCellDrawInfos = new ArrayList<>();
        for(CellDrawInfo cellDrawInfo1 : cellDrawInfos.values()) {
            if (cellDrawInfo1.state() == CellDrawInfo.State.Divided) {
                dividedCellDrawInfos.add(cellDrawInfo1);
            }
        }

        for (CellDrawInfo dividedCellDrawInfo : dividedCellDrawInfos) {
            for (CellDrawInfo cellDrawInfo1 : cellDrawInfos.values()) {
                if (dividedCellDrawInfo != cellDrawInfo1 && cellDrawInfo1.state() != CellDrawInfo.State.Divided) {
                    ListHeaderForCell lh = dividedCellDrawInfo.cell().getListHeader();
                    ListHeaderForCell lh2 = cellDrawInfo1.cell().getListHeader();

                    if (lh.getRowIndex() == lh2.getRowIndex() && lh.getRowSpan() == lh2.getRowSpan()) {
                        cellDrawInfo1.state(CellDrawInfo.State.Divided);
                    }
                }
            }
        }
    }

    public void saveCellDrawInfo() {
        oldCellDrawInfos.putAll(cellDrawInfos);
        cellDrawInfos.clear();
    }
}
