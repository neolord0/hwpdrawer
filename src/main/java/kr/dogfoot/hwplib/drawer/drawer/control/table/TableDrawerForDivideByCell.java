package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.CellDrawInfo;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.ColumnStates;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.RowDrawInfo;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.RowDrawInfoList;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.Queue;

public class TableDrawerForDivideByCell extends TableDrawer {
    private RowDrawInfoList rowDrawInfoList;

    public TableDrawerForDivideByCell(DrawingInput input, InterimOutput output, CellDrawer cellDrawer) {
        super(input, output, cellDrawer);
    }

    public Queue<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        init(controlCharInfo);

        rowDrawInfoList = new RowDrawInfoList(table);

        drawInEachPage(false);
        addCurrentTableOutputToTableDrawInfo();
        while (currentTableOutput.divided()) {
            setTableAreaToPageTop();
            drawInEachPage(true);
            addCurrentTableOutputToTableDrawInfo();
        }

        return tableDrawInfo.tableOutputQueue();
    }

    private void drawInEachPage(boolean divided) throws Exception {
        columnStates = new ColumnStates(table.getTable().getColumnCount());
        rowDrawInfoList.clear();

        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);
        cellDrawer.currentTableOutput(currentTableOutput);

        boolean emptyTable = true;

        int rowSize = currentTableOutput.table().getRowList().size();
        int rowStart = (divided) ? tableDrawInfo.dividingStartRowIndex() : 0;
        tableDrawInfo.dividingStartRowIndex(-1);

        for (int rowIndex = rowStart; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo = drawRow(rowIndex);
            rowDrawInfoList.add(rowDrawInfo);

            columnStates.setStates(rowDrawInfo);

            if (columnStates.canDivideByRow(rowIndex)) {
                if (!rowDrawInfoList.overPage()) {
                    emptyTable = false;
                    addCellInRows(rowDrawInfoList.rowDrawInfos());
                } else {
                    if (emptyTable) {
                        addCellInRows(rowDrawInfoList.rowDrawInfos());

                        if (rowIndex + 1 < rowSize - 1) {
                            currentTableOutput.divided(true);
                            tableDrawInfo.dividingStartRowIndex(rowIndex + 1);
                        }
                    } else {
                        currentTableOutput.divided(true);
                        tableDrawInfo.dividingStartRowIndex(rowDrawInfoList.startRowIndex());
                    }
                    break;
                }
                rowDrawInfoList.clear();
            }
        }

        setHeightAndCharInfo();

        output.endTable();
    }

    private RowDrawInfo drawRow(int rowIndex) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);
        CellDrawInfo cellDrawInfo;

        for (Cell cell : row.getCellList()) {
            cellDrawInfo = cellDrawer.draw(cell,
                    null,
                    -1,
                    ControlOutput.Zero_Array,
                    false,
                    rowDrawInfoList.cellPosition().currentCellTop(cell.getListHeader().getColIndex()));

            rowDrawInfo.addCellDrawInfo(cellDrawInfo);

            if (cellDrawInfo.state() == CellDrawInfo.State.OverPage) {
                rowDrawInfo.overPage(true);
            }
        }
        return rowDrawInfo;
    }

    private void addCellInRows(RowDrawInfo[] rowDrawInfos) {
        for (RowDrawInfo rowDrawInfo : rowDrawInfos) {
            addCellInRow(rowDrawInfo);
        }
    }

    private void addCurrentTableOutputToTableDrawInfo() {
        tableDrawInfo
                .addTableOutput(currentTableOutput);
    }
}
