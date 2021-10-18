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
    protected boolean firstDraw;

    public TableDrawerForDivide(DrawingInput input, InterimOutput output, CellDrawer cellDrawer) {
        super(input, output, cellDrawer);
    }

    public Queue<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        init(controlCharInfo);

        drawInEachPage(true);
        addCurrentTableOutputToTableDrawInfo();
        while (currentTableOutput.divided()) {
            setTableAreaToPageTop();
            drawInEachPage(false);
            addCurrentTableOutputToTableDrawInfo();
        }

        return tableDrawInfo.tableOutputQueue();
    }

    private void drawInEachPage(boolean firstDraw) throws Exception {
        this.firstDraw = firstDraw;
        columnStates = new ColumnStates(table.getTable().getColumnCount());

        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);
        cellDrawer.currentTableOutput(currentTableOutput);

        int rowSize = currentTableOutput.table().getRowList().size();
        int rowStart = (firstDraw) ? 0 : tableDrawInfo.dividingStartRowIndex();
        tableDrawInfo.dividingStartRowIndex(-1);
        for (int rowIndex = rowStart; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo = drawRow(rowIndex);

            addCellInRow(rowDrawInfo);

            tableDrawInfo.correctStateOfCellWithSameRow();
            columnStates.setStates(rowDrawInfo);

            if (rowDrawInfo.divided()) {
                currentTableOutput.divided(true);
                tableDrawInfo.dividingStartRowIndex(rowIndex);
            }

            if (columnStates.stopDraw()) {
                break;
            }
        }

        setHeightAndCharInfo();

        tableDrawInfo.saveCellDrawInfo();
        output.endTable();
    }

    private RowDrawInfo drawRow(int rowIndex) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);

        for (Cell cell : row.getCellList()) {
            if (columnStates.canDraw(cell)) {
                CellDrawInfo cellDrawInfo = drawCell(cell);
                if (cellDrawInfo != null) {
                    rowDrawInfo.addCellDrawInfo(cellDrawInfo);

                    if (cellDrawInfo.state().isDivided()) {
                        rowDrawInfo.divided(true);
                    }
                }
            }

        }
        return rowDrawInfo;
    }

    private CellDrawInfo drawCell(Cell cell) throws Exception {
        CellDrawInfo cellDrawInfo = null;

        CellDrawInfo oldCellDrawInfo = null;
        if (!firstDraw) {
            oldCellDrawInfo = tableDrawInfo.oldCellDrawInfo(cell);
        }

        if (oldCellDrawInfo != null) {
            changeOldCellDrawInfo(oldCellDrawInfo, cell);

            if (oldCellDrawInfo.state().isDivided()) {
                cellDrawInfo = cellDrawer.draw(oldCellDrawInfo.cell(),
                        oldCellDrawInfo.dividedPosition(),
                        oldCellDrawInfo.startTextColumnIndex(),
                        oldCellDrawInfo.cellOutput().childControlsCrossingPage(),
                        canDivideCell(table));
            }
        } else {
            cellDrawInfo = cellDrawer.draw(cell,
                    null,
                    -1,
                    ControlOutput.Zero_Array,
                    canDivideCell(table));
        }

        return cellDrawInfo;
    }

    private void changeOldCellDrawInfo(CellDrawInfo oldCellDrawInfo, Cell cell) {
        if (oldCellDrawInfo.state().isDivided()) {
            int skippedRowCount = columnStates.skippedCellCount(cell);
            if (skippedRowCount > 0) {
                cell = cell.clone();
                cell.getListHeader().setRowIndex(cell.getListHeader().getRowIndex() - skippedRowCount);
                cell.getListHeader().setRowSpan(cell.getListHeader().getRowSpan() + skippedRowCount);
                columnStates.clearSkippedCellCount(cell);

                oldCellDrawInfo.cell(cell);
            }

            oldCellDrawInfo.cell().getListHeader().setHeight(oldCellDrawInfo.nextPartHeight());
        } else {
            columnStates.increaseSkippedCellCount(oldCellDrawInfo.cell());
        }
    }

    private boolean canDivideCell(ControlTable table) {
        return !table.getHeader().getProperty().isLikeWord();
    }

    private void addCurrentTableOutputToTableDrawInfo() {
        tableDrawInfo
                .addTableOutput(currentTableOutput);
    }
}
