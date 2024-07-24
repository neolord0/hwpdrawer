package kr.dogfoot.hwpdrawer.drawer.control.table;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.drawer.control.table.info.CellDrawInfo;
import kr.dogfoot.hwpdrawer.drawer.control.table.info.ColumnStates;
import kr.dogfoot.hwpdrawer.drawer.control.table.info.RowDrawInfo;
import kr.dogfoot.hwpdrawer.drawer.control.table.info.TableDrawInfo;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;

public abstract class TableDrawer {
    protected final DrawingInput input;
    protected final InterimOutput output;
    protected final CellDrawer cellDrawer;

    protected CharInfoControl controlCharInfo;
    protected ControlTable table;
    protected Area areaWithoutOuterMargin;

    protected TableDrawInfo tableDrawInfo;

    protected TableOutput currentTableOutput;
    protected ColumnStates columnStates;

    protected TableDrawer(DrawingInput input, InterimOutput output, CellDrawer cellDrawer) {
        this.input = input;
        this.output = output;
        this.cellDrawer = cellDrawer;
    }

    protected void init(CharInfoControl controlCharInfo) {
        this.controlCharInfo = controlCharInfo;
        this.table = (ControlTable) controlCharInfo.control();
        this.areaWithoutOuterMargin = new Area(controlCharInfo.areaWithoutOuterMargin());

        tableDrawInfo = new TableDrawInfo();
    }

    protected void setTableAreaToPageTop() {
        areaWithoutOuterMargin
                .top(input.pageInfo().bodyArea().top() + table.getHeader().getOutterMarginTop())
                .height(0);
    }

    protected void setHeightAndCharInfo() {
        currentTableOutput.cellPosition().calculate();
        currentTableOutput.areaWithoutOuterMargin().height(currentTableOutput.cellPosition().totalHeight());
        currentTableOutput.controlCharInfo(new CharInfoControl(controlCharInfo));
        currentTableOutput.controlCharInfo().output(currentTableOutput);
    }

    protected void addCellInRow(RowDrawInfo rowDrawInfo) {
        for (CellDrawInfo cellDrawInfoInRow : rowDrawInfo.cellDrawInfos()) {
            tableDrawInfo.addCellDrawInfo(cellDrawInfoInRow);
            if (cellDrawInfoInRow.cellOutput() != null) {
                currentTableOutput.addCell(cellDrawInfoInRow.cellOutput());
            }
        }
    }
}
