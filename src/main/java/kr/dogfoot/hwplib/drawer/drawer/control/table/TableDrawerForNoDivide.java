package kr.dogfoot.hwplib.drawer.drawer.control.table;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.CellDrawInfo;
import kr.dogfoot.hwplib.drawer.drawer.control.table.info.RowDrawInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;

import java.util.Queue;

public class TableDrawerForNoDivide extends TableDrawer {
    public TableDrawerForNoDivide(DrawingInput input, InterimOutput output, CellDrawer cellDrawer) {
        super(input, output, cellDrawer);
    }

    public Queue<TableOutput> draw(CharInfoControl controlCharInfo) throws Exception {
        init(controlCharInfo);

        draw();
        addCurrentTableOutputToTableDrawInfo();

        return tableDrawInfo.tableOutputQueue();
    }

    private void draw() throws Exception {
        currentTableOutput = output.startTable(table, areaWithoutOuterMargin);
        cellDrawer.currentTableOutput(currentTableOutput);

        int rowSize = currentTableOutput.table().getRowList().size();
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            RowDrawInfo rowDrawInfo = drawRow(rowIndex);
            addCellInRow(rowDrawInfo);
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
                    false);

            rowDrawInfo.addCellDrawInfo(cellDrawInfo);

            switch (cellDrawInfo.state()) {
                case Divided:
                    rowDrawInfo.divided(true);
                    break;
            }
        }
        return rowDrawInfo;
    }

    private void addCurrentTableOutputToTableDrawInfo() {
        if (isOverPageForTable(currentTableOutput)) {
            long offsetY = output.currentPage().bodyArea().top() - currentTableOutput.areaWithOuterMargin().top();
            currentTableOutput.move(0, offsetY);
            tableDrawInfo
                    .addTableOutput(null)
                    .addTableOutput(currentTableOutput);
        } else {
            tableDrawInfo
                    .addTableOutput(currentTableOutput);
        }
    }

    private boolean isOverPageForTable(TableOutput tableOutput) {
        return tableOutput.areaWithOuterMargin().bottom() >= output.currentPage().bodyArea().bottom();
    }
}
