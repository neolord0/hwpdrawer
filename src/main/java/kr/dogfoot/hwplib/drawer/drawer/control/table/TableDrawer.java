package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.drawer.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

public class TableDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CellDrawState[] stateForEchoColumn;


    public TableDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public TableDrawResult draw(ControlCharInfo controlCharInfo) throws Exception {
        ControlTable table = (ControlTable) controlCharInfo.control();

        TableOutput tableOutput = output.startTable(table, controlCharInfo.areaWithoutOuterMargin());

        TableDrawResult drawResult = new TableDrawResult(controlCharInfo);
        drawResult.tableOutputForCurrentPage(tableOutput);

        boolean canSplitCell = canSplitCell(table);
        
        stateForEchoColumn = new CellDrawState[table.getTable().getColumnCount()];

        int rowSize = table.getRowList().size();
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            Row row = table.getRowList().get(rowIndex);

            boolean drawingRowCompletely = true;
            boolean stopDrawRow = false;

            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellDrawResult result = drawCell(cell, tableOutput, canSplitCell, null);
                    setStateForEchoColumn(result.split(),  cell);

                    if (result.split()) {
                        drawResult.addSplitCellDrawResult(result);
                        drawingRowCompletely = false;
                    }
                } else {
                    stopDrawRow = true;
                }
            }

            if (drawingRowCompletely == false) {
                drawResult.startRowIndexForNextPage(rowIndex);
            }

            if (stopDrawRow == true) {
                break;
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());
        output.endTable();
        return drawResult;
    }

    private boolean canSplitCell(ControlTable table) {
        return !table.getHeader().getProperty().isLikeWord() &&
                table.getTable().getProperty().getDivideAtPageBoundary() == DivideAtPageBoundary.Divide;
    }

    private boolean canDrawCell(Cell cell) {
        for (int colIndex = cell.getListHeader().getColIndex();
             colIndex < cell.getListHeader().getColIndex() + cell.getListHeader().getColSpan();
             colIndex++) {
            if (!(stateForEchoColumn[colIndex] == null || stateForEchoColumn[colIndex] == CellDrawState.Complete)) {
                return false;
            }
        }
        return true;
    }

    private void setStateForEchoColumn(boolean split, Cell cell) {
        CellDrawState state = (split) ? CellDrawState.Partially : CellDrawState.Complete;
        for (int colIndex = cell.getListHeader().getColIndex();
             colIndex < cell.getListHeader().getColIndex() + cell.getListHeader().getColSpan();
             colIndex++) {
            stateForEchoColumn[colIndex] = state;
        }
    }

    private CellDrawResult drawCell(Cell cell, TableOutput tableOutput, boolean canSplitCell, TextPosition fromPosition) throws Exception {
        long cellTopInPage = tableOutput.cellPosition().currentCellTop(cell.getListHeader().getColIndex())  + tableOutput.areaWithoutOuterMargin().top();

        CellDrawResult result = null;
        CellOutput cellOutput = output.startCell(cell, tableOutput);

        if (cell.getParagraphList() != null) {
            cellOutput
                    .textMargin(
                            cell.getListHeader().getLeftMargin(),
                            cell.getListHeader().getTopMargin(),
                            cell.getListHeader().getRightMargin(),
                            cell.getListHeader().getBottomMargin())
                    .verticalAlignment(cell.getListHeader().getProperty().getTextVerticalAlignment());

            result = new ParaListDrawer(input, output).drawForCell(cell.getParagraphList(),
                    cellOutput.textBoxArea(),
                    canSplitCell,
                    cellTopInPage,
                    tableOutput.table().getHeader().getOutterMarginBottom(),
                    fromPosition);

            cellOutput.calculatedContentHeight(result.height());
        }

        tableOutput.addCell(cellOutput);
        output.endCell();

        result.cell(cell);
        return result;
    }

    public TableDrawResult drawSplitTable(TableDrawResult splitTableDrawResult) throws Exception {
        setSplitTableTop(splitTableDrawResult);

        ControlTable table = splitTableDrawResult.table();
        TableOutput tableOutput = output.startTable(table, splitTableDrawResult.areaWithoutOuterMargin());

        boolean canSplitCell = canSplitCell(table);

        TableDrawResult drawResult = new TableDrawResult(splitTableDrawResult.controlCharInfo());
        drawResult.tableOutputForCurrentPage(tableOutput);

        stateForEchoColumn = new CellDrawState[table.getTable().getColumnCount()];

        int rowSize = table.getRowList().size();
        for (int rowIndex = splitTableDrawResult.startRowIndexForNextPage(); rowIndex < rowSize; rowIndex++) {
            Row row = table.getRowList().get(rowIndex);

            boolean drawingRowCompletely = true;
            boolean stopDrawRow = false;
            TextPosition splitPosition = null;

            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellDrawResult cellDrawResult = splitTableDrawResult.splitCellDrawResult(cell);
                    if (cellDrawResult != null) {
                        splitPosition = cellDrawResult.splitPosition();
                    } else {
                        splitPosition = null;
                    }

                    CellDrawResult result = drawCell(cell, tableOutput, canSplitCell, splitPosition);
                    setStateForEchoColumn(result.split(),  cell);

                    if (result.split()) {
                        drawResult.addSplitCellDrawResult(result);
                        drawingRowCompletely = false;
                    }
                } else {
                    stopDrawRow = true;
                }
            }

            if (drawingRowCompletely == false) {
                drawResult.startRowIndexForNextPage(rowIndex);
            }

            if (stopDrawRow == true) {
                break;
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());
        output.endTable();
        return drawResult;
    }

    private void setSplitTableTop(TableDrawResult splitTableDrawResult) {
        splitTableDrawResult.areaWithOuterMargin()
                .top(input.pageInfo().bodyArea().top())
                .height(0);
        splitTableDrawResult.areaWithoutOuterMargin()
                .top(input.pageInfo().bodyArea().top() + splitTableDrawResult.table().getHeader().getOutterMarginTop())
                .height(0);
    }

    public static CellOutput drawCell(Cell cell, DrawingInput input, InterimOutput output) throws Exception {
        CellOutput cellOutput = output.startCell(cell, null);

        if (cell.getParagraphList() != null) {
            cellOutput
                    .textMargin(
                            cell.getListHeader().getLeftMargin(),
                            cell.getListHeader().getTopMargin(),
                            cell.getListHeader().getRightMargin(),
                            cell.getListHeader().getBottomMargin())
                    .verticalAlignment(cell.getListHeader().getProperty().getTextVerticalAlignment());

            CellDrawResult result = new ParaListDrawer(input, output)
                                .drawForCell(cell.getParagraphList(),
                                        cellOutput.textBoxArea(),
                                        false,
                                        0,
                                        0,
                                        new TextPosition(0, 0, 0));

            cellOutput.calculatedContentHeight(result.height());
        }

        output.endCell();
        return cellOutput;
    }

    public enum CellDrawState {
        Complete,
        Partially,
        No
    }
}
