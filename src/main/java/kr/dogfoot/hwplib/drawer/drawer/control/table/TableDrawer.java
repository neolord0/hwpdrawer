package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.drawer.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.ArrayList;

public class TableDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private TableOutput tableOutput;
    private CellDrawState[] statesForEchoColumn;

    public TableDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public ArrayList<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        ArrayList<TableOutput> tableOutputs = new ArrayList<>();
        TableResult result = drawFirst(controlCharInfo);
        tableOutputs.add(result.tableOutputForCurrentPage());
        while (result.split()) {
            result = drawSplit(result);
            tableOutputs.add(result.tableOutputForCurrentPage());
        }
        return tableOutputs;
    }


    public TableResult drawFirst(CharInfoControl controlCharInfo) throws Exception {
        initializeStatesForEchoColumn((ControlTable) controlCharInfo.control());

        tableOutput = output.startTable((ControlTable) controlCharInfo.control(), controlCharInfo.areaWithoutOuterMargin());
        TableResult result = new TableResult().tableOutputForCurrentPage(tableOutput);

        int rowSize = tableOutput.table().getRowList().size();
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            if (stopDrawRow()) {
                break;
            }

            boolean drawingRowCompletely = true;

            Row row = tableOutput.table().getRowList().get(rowIndex);
            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellResult cellResult = drawCell(cell, null, ControlOutput.Zero_Array);
                    setStatesForEchoColumn(cellResult);

                    result.addCellResult(cellResult);

                    if (cellResult.split()) {
                        drawingRowCompletely = false;
                    }
                }
            }

            if (drawingRowCompletely == false) {
                result.splitStartRowIndex(rowIndex);
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());

        output.endTable();

        tableOutput.controlCharInfo(new CharInfoControl(controlCharInfo));
        tableOutput.controlCharInfo().output(tableOutput);
        return result;
    }

    private void initializeStatesForEchoColumn(ControlTable table) {
        statesForEchoColumn = new CellDrawState[table.getTable().getColumnCount()];
    }

    private boolean stopDrawRow() {
        for (CellDrawState state : statesForEchoColumn) {
            if (state != CellDrawState.Partially) {
                return false;
            }
        }
        return true;
    }

    private boolean canDrawCell(Cell cell) {
        for (int colIndex = cell.getListHeader().getColIndex();
             colIndex < cell.getListHeader().getColIndex() + cell.getListHeader().getColSpan();
             colIndex++) {
            if (!(statesForEchoColumn[colIndex] == null || statesForEchoColumn[colIndex] == CellDrawState.Complete)) {
                return false;
            }
        }
        return true;
    }

    private void setStatesForEchoColumn(CellResult result) {
        CellDrawState state = (result.split()) ? CellDrawState.Partially : CellDrawState.Complete;
        for (int colIndex = result.cell().getListHeader().getColIndex();
             colIndex < result.cell().getListHeader().getColIndex() + result.cell().getListHeader().getColSpan();
             colIndex++) {
            statesForEchoColumn[colIndex] = state;
        }
    }

    private CellResult drawCell(Cell cell, TextPosition fromPosition, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (cell.getParagraphList() != null) {
            long topInPage = tableOutput.cellPosition().currentCellTop(cell.getListHeader().getColIndex())
                    + tableOutput.areaWithoutOuterMargin().top();
            ListHeaderForCell lh = cell.getListHeader();

            CellOutput cellOutput = output.startCell(cell)
                    .textMargin(lh.getLeftMargin(), lh.getTopMargin(), lh.getRightMargin(), lh.getBottomMargin())
                    .verticalAlignment(lh.getProperty().getTextVerticalAlignment());

            CellResult result = new ParaListDrawer(input, output).drawForCell(cell.getParagraphList(),
                    cellOutput.textBoxArea(),
                    tableOutput.canSplitCell(),
                    topInPage,
                    lh.getBottomMargin() + tableOutput.table().getHeader().getOutterMarginBottom(),
                    fromPosition,
                    childControlsCrossingPage);

            result
                    .cell(cell)
                    .cellOutput(cellOutput);

            checkCrossPage(topInPage, lh, result);

            cellOutput.calculatedContentHeight(result.height());

            output.endCell();
            tableOutput.addCell(cellOutput);
            return result;
        } else {
            return new CellResult()
                    .cell(cell)
                    .height(cell.getListHeader().getHeight());
        }
    }

    private void checkCrossPage(long topInPage, ListHeaderForCell lh, CellResult result) {
        long tableOuterMarginBottom = tableOutput.table().getHeader().getOutterMarginBottom();
        if (lh.getHeight() + topInPage + tableOuterMarginBottom > input.pageInfo().bodyArea().bottom()) {
            long heightWithMargin = input.pageInfo().bodyArea().bottom() - tableOuterMarginBottom - topInPage;
            result
                    .height(heightWithMargin - (lh.getTopMargin() + lh.getBottomMargin()))
                    .split(true);
            result.nextPartHeight(lh.getHeight() - heightWithMargin);
            lh.setHeight(0);
        }
    }

    public TableResult drawSplit(TableResult splitTableResult) throws Exception {
        setSplitTableTop(splitTableResult);
        initializeStatesForEchoColumn(splitTableResult.table());

        tableOutput = output.startTable(splitTableResult.table(), splitTableResult.areaWithoutOuterMargin())
                .split(true);
        TableResult result = new TableResult().tableOutputForCurrentPage(tableOutput);

        int rowSize = tableOutput.table().getRowList().size();
        for (int rowIndex = splitTableResult.splitStartRowIndex(); rowIndex < rowSize; rowIndex++) {
            if (stopDrawRow()) {
                break;
            }

            boolean drawingRowCompletely = true;

            Row row = tableOutput.table().getRowList().get(rowIndex);
            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellResult drawCellResult = splitTableResult.cellResult(cell);

                    TextPosition splitPosition = null;
                    ControlOutput[] childControlsCrossingPage = ControlOutput.Zero_Array;
                    if (drawCellResult != null) {
                        if (!drawCellResult.split()) {
                            break;
                        } else {
                            splitPosition = drawCellResult.splitPosition();
                            childControlsCrossingPage = drawCellResult.cellOutput().childControlsCrossingPage();
                            cell.getListHeader().setHeight(drawCellResult.nextPartHeight());
                        }
                    }

                    CellResult cellResult = drawCell(cell, splitPosition, childControlsCrossingPage);
                    setStatesForEchoColumn(cellResult);

                    result.addCellResult(cellResult);

                    if (cellResult.split()) {
                        drawingRowCompletely = false;
                    }
                }
            }

            if (drawingRowCompletely == false) {
                result.splitStartRowIndex(rowIndex);
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());
        output.endTable();

        tableOutput.controlCharInfo(new CharInfoControl(splitTableResult.tableOutputForCurrentPage().controlCharInfo()));
        tableOutput.controlCharInfo().output(tableOutput);

        return result;
    }

    private void setSplitTableTop(TableResult splitTableDrawResult) {
        splitTableDrawResult.areaWithOuterMargin()
                .top(input.pageInfo().bodyArea().top())
                .height(0);
        splitTableDrawResult.areaWithoutOuterMargin()
                .top(input.pageInfo().bodyArea().top() + splitTableDrawResult.table().getHeader().getOutterMarginTop())
                .height(0);
    }

    public enum CellDrawState {
        Complete,
        Partially
    }
}
