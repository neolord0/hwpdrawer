package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawerForCell;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.CharPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.Queue;

public class TableDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CharInfoControl controlCharInfo;

    private ControlTable table;
    private Area areaWithoutOuterMargin;

    private TableOutput currentTableOutput;
    private TableDrawInfo tableDrawInfo;
    private ColumnStates columnStates;
    private RowDrawInfoList rowDrawInfoList;

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

        switch(table.getTable().getProperty().getDivideAtPageBoundary()) {
            case NoDivide:
                drawInEachPage(false);
                break;
            case Divide:
                drawInEachPage(false);
                while (currentTableOutput.split()) {
                    setSplitTableTop();
                    drawInEachPage(true);
                }
                break;

            case DivideByCell:
                rowDrawInfoList = new RowDrawInfoList(table);
                drawInEachPageWithDividingByCell(false);
                while (currentTableOutput.split()) {
                    setSplitTableTop();
                    drawInEachPageWithDividingByCell(true);
                }
                break;
        }
        return tableDrawInfo.tableOutputQueue();
    }

    private void drawInEachPage(boolean split) throws Exception {
        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);

        columnStates.init(table.getTable().getColumnCount());

        int rowSize = currentTableOutput.table().getRowList().size();
        int rowStart = (split) ? tableDrawInfo.splitStartRowIndex() : 0;
        for (int rowIndex = rowStart; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo = drawRow(rowIndex, split);
            columnStates.setStates(rowDrawInfo);

            addCellInRow(rowDrawInfo);

            if (rowDrawInfo.split()) {
                currentTableOutput.split(true);
                tableDrawInfo.splitStartRowIndex(rowIndex);
            }

            if (columnStates.stopDraw()) {
                break;
            }
        }

        setHeightAndCharInfo();

        output.endTable();
        tableDrawInfo
                .addTableOutput(currentTableOutput);
    }

    private void setHeightAndCharInfo() {
        currentTableOutput.cellPosition().calculate();
        currentTableOutput.areaWithoutOuterMargin().height(currentTableOutput.cellPosition().totalHeight());
        currentTableOutput.controlCharInfo(new CharInfoControl(controlCharInfo));
        currentTableOutput.controlCharInfo().output(currentTableOutput);
    }


    private RowDrawInfo drawRow(int rowIndex, boolean splitTable) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);
        CellDrawInfo oldCellDrawInfo;
        CellDrawInfo cellDrawInfo;

        for (Cell cell : row.getCellList()) {
            if (columnStates.canDraw(cell)) {
                if (splitTable) {
                    oldCellDrawInfo = tableDrawInfo.cellCellDrawInfo(cell);
                    if (oldCellDrawInfo != null) {
                        if (oldCellDrawInfo.state().isNormal()) {
                           break;
                        } else {
                            cell.getListHeader().setHeight(oldCellDrawInfo.nextPartHeight());
                        }
                    }
                } else {
                    oldCellDrawInfo = null;
                }

                if (oldCellDrawInfo != null) {
                    cellDrawInfo = drawCell(oldCellDrawInfo.cell(),
                            oldCellDrawInfo.splitPosition(),
                            oldCellDrawInfo.startTextColumnIndex(),
                            oldCellDrawInfo.cellOutput().childControlsCrossingPage());
                } else {
                    cellDrawInfo = drawCell(cell, null, -1, ControlOutput.Zero_Array);
                }
                rowDrawInfo.addCellDrawInfo(cellDrawInfo);

                switch(cellDrawInfo.state()) {
                    case Split:
                        rowDrawInfo.split(true);
                        break;
                }
            }
        }
        return rowDrawInfo;
    }


    private void drawInEachPageWithDividingByCell(boolean split) throws Exception {
        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);

        columnStates.init(table.getTable().getColumnCount());
        rowDrawInfoList.clear();

        int rowSize = currentTableOutput.table().getRowList().size();
        int rowStart = (split) ? tableDrawInfo.splitStartRowIndex() : 0;
        for (int rowIndex = rowStart; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo = drawRowForDivideByCell(rowIndex, split);
            rowDrawInfoList.add(rowDrawInfo);

            columnStates.setStates(rowDrawInfo);

            if (columnStates.canSplitRow(rowIndex)) {
                if (!rowDrawInfoList.overPage()) {
                    addCellInRows(rowDrawInfoList.rowDrawInfos());
                } else {
                    currentTableOutput.split(true);
                    tableDrawInfo.splitStartRowIndex(rowDrawInfoList.startRowIndex());
                    break;
                }
                rowDrawInfoList.clear();
            }
        }

        setHeightAndCharInfo();

        output.endTable();
        tableDrawInfo
                .addTableOutput(currentTableOutput);
    }

    private RowDrawInfo drawRowForDivideByCell(int rowIndex, boolean splitTable) throws Exception {
        Row row = currentTableOutput.table().getRowList().get(rowIndex);
        RowDrawInfo rowDrawInfo = new RowDrawInfo(rowIndex);
        CellDrawInfo cellDrawInfo;

        for (Cell cell : row.getCellList()) {
            cellDrawInfo = drawCell(cell, null, -1, ControlOutput.Zero_Array);
            rowDrawInfo.addCellDrawInfo(cellDrawInfo);

            if (cellDrawInfo.state() == CellDrawInfo.State.OverPage) {
                rowDrawInfo.overPage(true);
            }
        }
        return rowDrawInfo;
    }

    private void addCellInRow(RowDrawInfo rowDrawInfo) {
        for (CellDrawInfo cellDrawInfoInRow : rowDrawInfo.cellDrawInfos())  {
            tableDrawInfo.addCellDrawInfo(cellDrawInfoInRow);
            if (cellDrawInfoInRow.cellOutput() != null) {
                currentTableOutput.addCell(cellDrawInfoInRow.cellOutput());
            }
        }
    }

    private void addCellInRows(RowDrawInfo[] rowDrawInfos) {
        for (RowDrawInfo rowDrawInfo : rowDrawInfos)  {
            addCellInRow(rowDrawInfo);
        }
    }

    private CellDrawInfo drawCell(Cell cell, CharPosition fromPosition, int startTextColumnIndex, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (cell.getParagraphList() != null) {
            CellOutput cellOutput = output.startCell(cell);
            ListHeaderForCell lh = cell.getListHeader();
            setTextMarginAndVerticalAlignment(cellOutput, lh);
            long topInPage = currentTableOutput.cellPosition().currentCellTop(cell.getListHeader().getColIndex())
                    + currentTableOutput.areaWithoutOuterMargin().top();

            if (table.getTable().getProperty().getDivideAtPageBoundary() == DivideAtPageBoundary.DivideByCell) {
                topInPage += rowDrawInfoList.cellPosition().currentCellTop(cell.getListHeader().getColIndex());
            }

            CellDrawInfo cellDrawInfo = new ParaListDrawerForCell(input, output).draw(
                    cell.getParagraphList(),
                    cellOutput.textBoxArea(),
                    currentTableOutput.canSplitCell(),
                    topInPage,
                    lh.getBottomMargin() + currentTableOutput.table().getHeader().getOutterMarginBottom(),
                    fromPosition,
                    startTextColumnIndex,
                    childControlsCrossingPage);

            checkCrossPageAndOverPage(cellDrawInfo, topInPage, lh);
            cellOutput.calculatedContentHeight(cellDrawInfo.height());

            output.endCell();

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

    private void checkCrossPageAndOverPage(CellDrawInfo cellDrawInfo, long topInPage, ListHeaderForCell lh) {
        if (topInPage + cellDrawInfo.height() > input.pageInfo().bodyArea().bottom()) {
            cellDrawInfo.state(CellDrawInfo.State.OverPage);

            cellDrawInfo.splitPosition(null);
            cellDrawInfo.startTextColumnIndex(0);
            cellDrawInfo.cellOutput().clearChildControlsCrossingPage();
        } else {
            long tableOuterMarginBottom = table.getHeader().getOutterMarginBottom();
            if (lh.getHeight() + topInPage + tableOuterMarginBottom > input.pageInfo().bodyArea().bottom()) {
                long heightWithMargin = input.pageInfo().bodyArea().bottom() - tableOuterMarginBottom - topInPage;
                cellDrawInfo
                        .height(heightWithMargin - (lh.getTopMargin() + lh.getBottomMargin()))
                        .state(CellDrawInfo.State.Split);
                cellDrawInfo.nextPartHeight(lh.getHeight() - heightWithMargin);
                lh.setHeight(0);
            }
        }
    }

    private void setSplitTableTop() {
        areaWithoutOuterMargin
                .top(input.pageInfo().bodyArea().top() + table.getHeader().getOutterMarginTop())
                .height(0);
    }
}
