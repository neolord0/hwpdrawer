package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.CellDrawInfo;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.ColumnStates;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.RowDrawInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.Queue;

public class TableDrawerForDivide extends TableDrawer {
    public TableDrawerForDivide(DrawingInput input, InterimOutput output, CellDrawer cellDrawer) {
        super(input, output, cellDrawer);
    }

    public Queue<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        init(controlCharInfo);

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

        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);
        cellDrawer.currentTableOutput(currentTableOutput);

        int rowSize = currentTableOutput.table().getRowList().size();
        int rowStart = (divided) ? tableDrawInfo.dividingStartRowIndex() : 0;
        tableDrawInfo.dividingStartRowIndex(-1);
        for (int rowIndex = rowStart; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo;
            if (divided) {
                rowDrawInfo = drawRowInDividedTable(rowIndex);
            } else {
                rowDrawInfo = drawRow(rowIndex);
            }
            columnStates.setStates(rowDrawInfo);

            addCellInRow(rowDrawInfo);

            if (rowDrawInfo.divided()) {
                currentTableOutput.divided(true);
                tableDrawInfo.dividingStartRowIndex(rowIndex);
            }

            if (columnStates.stopDraw()) {
                break;
            }
        }

        setHeightAndCharInfo();

        output.endTable();
    }

    private RowDrawInfo drawRowInDividedTable(int rowIndex) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);
        CellDrawInfo oldCellDrawInfo;
        CellDrawInfo cellDrawInfo;

        for (Cell cell : row.getCellList()) {
            if (columnStates.canDraw(cell)) {
                oldCellDrawInfo = tableDrawInfo.cellCellDrawInfo(cell);
                if (oldCellDrawInfo != null) {
                    if (oldCellDrawInfo.state().isNormal()) {
                        break;
                    } else {
                        cell.getListHeader().setHeight(oldCellDrawInfo.nextPartHeight());
                    }

                    cellDrawInfo = cellDrawer.draw(oldCellDrawInfo.cell(),
                            oldCellDrawInfo.dividedPosition(),
                            oldCellDrawInfo.startTextColumnIndex(),
                            oldCellDrawInfo.cellOutput().childControlsCrossingPage(), canDivideCell(table));
                } else {
                    cellDrawInfo = cellDrawer.draw(cell,
                            null,
                            -1,
                            ControlOutput.Zero_Array,
                            canDivideCell(table));
                }

                rowDrawInfo.addCellDrawInfo(cellDrawInfo);

                switch (cellDrawInfo.state()) {
                    case Divided:
                        rowDrawInfo.divided(true);
                        break;
                }
            }
        }
        return rowDrawInfo;
    }

    private RowDrawInfo drawRow(int rowIndex) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);
        CellDrawInfo cellDrawInfo;

        for (Cell cell : row.getCellList()) {
            if (columnStates.canDraw(cell)) {
                cellDrawInfo = cellDrawer.draw(cell,
                        null,
                        -1,
                        ControlOutput.Zero_Array,
                        canDivideCell(table));

                rowDrawInfo.addCellDrawInfo(cellDrawInfo);

                switch (cellDrawInfo.state()) {
                    case Divided:
                        rowDrawInfo.divided(true);
                        break;
                }
            }
        }
        return rowDrawInfo;
    }

    private boolean canDivideCell(ControlTable table) {
        return !table.getHeader().getProperty().isLikeWord();
    }

    private void addCurrentTableOutputToTableDrawInfo() {
        tableDrawInfo
                .addTableOutput(currentTableOutput);
    }
}
