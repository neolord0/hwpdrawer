package kr.dogfoot.hwplib.drawer.paragraph.control.table;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;

public class TableDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CellDrawState[] stateForEchoColumn;

    public TableDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public TableDrawResult draw(ControlTable table, Area controlArea) throws Exception {
        TableOutput tableOutput = output.startTable(table, controlArea);

        TableDrawResult drawResult = new TableDrawResult(table, controlArea);
        drawResult.tableOutputForCurrentPage(tableOutput);

        stateForEchoColumn = new CellDrawState[table.getTable().getColumnCount()];

        int rowSize = table.getRowList().size();
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            Row row = table.getRowList().get(rowIndex);

            boolean drawingRowCompletely = true;
            boolean stopDrawRow = false;

            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellDrawResult result  = drawCell(cell, tableOutput);
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
        output.endTable();
        return drawResult;
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

    private CellDrawResult drawCell(Cell cell, TableOutput tableOutput) throws Exception {
        long cellTopInPage = tableOutput.cellPosition().currentCellTop(cell.getListHeader().getColIndex())  + tableOutput.controlArea().top();

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

            result = drawTextBoxForCell(cell.getParagraphList(), cellOutput.textBoxArea(), cellTopInPage);
            cellOutput.calculatedContentHeight(result.height());
        }

        tableOutput.addCell(cellOutput);
        output.endCell();

        result.cell(cell);
        return result;
    }



    private CellDrawResult drawTextBoxForCell(ParagraphList paragraphList, Area textBoxArea, long cellTopInPage) throws Exception {
        ParaListDrawer paragraphListDrawer = new ParaListDrawer(input, output);
        return paragraphListDrawer.drawForCell(paragraphList, textBoxArea, cellTopInPage);
    }

    public void drawSplitTables() throws Exception {
/*
        for (TableDrawResult splitTableDrawResult : input.splitTableDrawResults()) {
            drawSplitTable(splitTableDrawResult);
        }

        input.clearSplitTableDrawResults();
*/
   }

    private TableDrawResult drawSplitTable(TableDrawResult splitTableDrawResult) throws Exception {
        splitTableDrawResult.controlArea().top(0);

        ControlTable table = splitTableDrawResult.table();
        TableOutput tableOutput = output.startTable(table, splitTableDrawResult.controlArea());

        TableDrawResult drawResult = new TableDrawResult(table, splitTableDrawResult.controlArea());
        drawResult.tableOutputForCurrentPage(tableOutput);

        stateForEchoColumn = new CellDrawState[table.getTable().getColumnCount()];

        int rowSize = table.getRowList().size();
        for (int rowIndex = splitTableDrawResult.startRowIndexForNextPage(); rowIndex < rowSize; rowIndex++) {
            Row row = table.getRowList().get(rowIndex);

            boolean drawingRowCompletely = true;
            boolean stopDrawRow = false;

            for (Cell cell : row.getCellList()) {
                if (canDrawCell(cell)) {
                    CellDrawResult cellDrawResult = splitTableDrawResult.splitCellDrawResult(cell);
                    if (cellDrawResult != null) {
                        System.out.println("goto  " + cellDrawResult.splitPosition().paraIndex() + " " + cellDrawResult.splitPosition().charIndex() );
                        input.gotoParaCharPosition(cellDrawResult.splitPosition());
                    }

                    CellDrawResult result = drawCell(cell, tableOutput);
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
        output.endTable();

        System.out.println(tableOutput.test(3));
        return drawResult;
    }

    public enum CellDrawState {
        Complete,
        Partially,
        No
    }
}
