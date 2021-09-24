package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawerForCell;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.Queue;

public class TableDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CharInfoControl controlCharInfo;
    private ControlTable table;
    private Area areaWithoutOuterMargin;
    private TableDrawInfo tableDrawInfo;
    private ColumnStates columnStates;

    public TableDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
        columnStates = new ColumnStates();
    }

    public Queue<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        this.controlCharInfo = controlCharInfo;
        this.table = (ControlTable) controlCharInfo.control();
        this.areaWithoutOuterMargin = new Area(controlCharInfo.areaWithoutOuterMargin());

        tableDrawInfo = new TableDrawInfo();

        drawFirst();
        while (tableDrawInfo.drawContinually()) {
            setSplitTableTop();
            drawSplit();
        }

        return tableDrawInfo.tableOutputQueue();
    }

    public void drawFirst() throws Exception {
        TableOutput tableOutput = output.startTable(table, areaWithoutOuterMargin);

        tableDrawInfo
                .addTableOutput(tableOutput)
                .drawContinually(false);

        columnStates.init(table.getTable().getColumnCount());

        boolean drawingRowCompletely;
        int rowSize = tableOutput.table().getRowList().size();
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            if (columnStates.stopDraw()) {
                break;
            }

            drawingRowCompletely = true;
            Row row = tableOutput.table().getRowList().get(rowIndex);
            for (Cell cell : row.getCellList()) {
                if (columnStates.canDraw(cell)) {
                    CellDrawInfo cellDrawInfo = drawCell(cell, null, 0, ControlOutput.Zero_Array);

                    columnStates.setState(cellDrawInfo);

                    tableDrawInfo.addCellDrawInfo(cellDrawInfo);

                    if (cellDrawInfo.split()) {
                        drawingRowCompletely = false;
                    }
                }
            }

            if (drawingRowCompletely == false) {
                tableDrawInfo
                        .drawContinually(true)
                        .splitStartRowIndex(rowIndex);
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());

        output.endTable();

        tableOutput.controlCharInfo(new CharInfoControl(controlCharInfo));
        tableOutput.controlCharInfo().output(tableOutput);
    }

    private CellDrawInfo drawCell(Cell cell, TextPosition fromPosition, int startTextColumnIndex, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (cell.getParagraphList() != null) {
            long topInPage = tableDrawInfo.currentTableOutput().cellPosition().currentCellTop(cell.getListHeader().getColIndex())
                    + tableDrawInfo.currentTableOutput().areaWithoutOuterMargin().top();

            CellOutput cellOutput = output.startCell(cell);
            ListHeaderForCell lh = cell.getListHeader();
            setTextMarginAndVerticalAlignment(cellOutput, lh);

            CellDrawInfo cellDrawInfo = new ParaListDrawerForCell(input, output).draw(
                    cell.getParagraphList(),
                    cellOutput.textBoxArea(),
                    tableDrawInfo.currentTableOutput().canSplitCell(),
                    topInPage,
                    lh.getBottomMargin() + tableDrawInfo.currentTableOutput().table().getHeader().getOutterMarginBottom(),
                    fromPosition,
                    startTextColumnIndex,
                    childControlsCrossingPage);
            cellDrawInfo
                    .cell(cell)
                    .cellOutput(cellOutput);
            checkCrossPage(topInPage, lh, cellDrawInfo);

            cellOutput.calculatedContentHeight(cellDrawInfo.height());

            output.endCell();

            tableDrawInfo.currentTableOutput().addCell(cellOutput);


            return cellDrawInfo;
        } else {
            return new CellDrawInfo()
                    .cell(cell)
                    .height(cell.getListHeader().getHeight());
        }
    }

    private void setTextMarginAndVerticalAlignment(CellOutput cellOutput, ListHeaderForCell lh) {
        if (lh != null) {
            cellOutput
                    .textMargin(lh.getLeftMargin(), lh.getTopMargin(), lh.getRightMargin(), lh.getBottomMargin())
                    .verticalAlignment(lh.getProperty().getTextVerticalAlignment());
        }
    }

    private void checkCrossPage(long topInPage, ListHeaderForCell lh, CellDrawInfo cellResult) {
        long tableOuterMarginBottom = table.getHeader().getOutterMarginBottom();
        if (lh.getHeight() + topInPage + tableOuterMarginBottom > input.pageInfo().bodyArea().bottom()) {
            long heightWithMargin = input.pageInfo().bodyArea().bottom() - tableOuterMarginBottom - topInPage;
            cellResult
                    .height(heightWithMargin - (lh.getTopMargin() + lh.getBottomMargin()))
                    .split(true);
            cellResult.nextPartHeight(lh.getHeight() - heightWithMargin);
            lh.setHeight(0);
        }
    }

    public void drawSplit() throws Exception {
        TableOutput tableOutput = output.startTable(table, areaWithoutOuterMargin)
                .split(true);

        tableDrawInfo
                .addTableOutput(tableOutput)
                .drawContinually(false);

        columnStates.init(table.getTable().getColumnCount());

        int rowSize = tableOutput.table().getRowList().size();
        for (int rowIndex = tableDrawInfo.splitStartRowIndex(); rowIndex < rowSize; rowIndex++) {
            if (columnStates.stopDraw()) {
                break;
            }

            boolean drawingRowCompletely = true;

            Row row = tableOutput.table().getRowList().get(rowIndex);
            for (Cell cell : row.getCellList()) {
                if (columnStates.canDraw(cell)) {
                    CellDrawInfo cellDrawInfo = tableDrawInfo.cellCellDrawInfo(cell);

                    TextPosition splitPosition = null;
                    int startTextColumnIndex = -1;
                    ControlOutput[] childControlsCrossingPage = ControlOutput.Zero_Array;
                    if (cellDrawInfo != null) {
                        if (!cellDrawInfo.split()) {
                            break;
                        } else {
                            splitPosition = cellDrawInfo.splitPosition();
                            startTextColumnIndex = cellDrawInfo.textColumnIndex();
                            childControlsCrossingPage = cellDrawInfo.cellOutput().childControlsCrossingPage();
                            cell.getListHeader().setHeight(cellDrawInfo.nextPartHeight());
                        }
                    }

                    CellDrawInfo cellResult = drawCell(cell, splitPosition, startTextColumnIndex, childControlsCrossingPage);
                    columnStates.setState(cellResult);

                    tableDrawInfo.addCellDrawInfo(cellResult);

                    if (cellResult.split()) {
                        drawingRowCompletely = false;
                    }
                }
            }

            if (drawingRowCompletely == false) {
                tableDrawInfo
                        .drawContinually(true)
                        .splitStartRowIndex(rowIndex);
            }
        }

        tableOutput.cellPosition().calculate();
        tableOutput.areaWithoutOuterMargin().height(tableOutput.cellPosition().totalHeight());
        output.endTable();

        tableOutput.controlCharInfo(new CharInfoControl(controlCharInfo));
        tableOutput.controlCharInfo().output(tableOutput);
    }

    private void setSplitTableTop() {
        areaWithoutOuterMargin
                .top(input.pageInfo().bodyArea().top() + table.getHeader().getOutterMarginTop())
                .height(0);
    }
}
