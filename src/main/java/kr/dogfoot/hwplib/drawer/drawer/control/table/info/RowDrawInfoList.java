package kr.dogfoot.hwplib.drawer.drawer.control.table.info;

import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellPositionCalculator;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

import java.util.ArrayList;

public class RowDrawInfoList {
    private final ArrayList<RowDrawInfo> list;
    private final CellPositionCalculator cellPositionCalculator;

    public RowDrawInfoList(ControlTable table) {
        list = new ArrayList<>();
        cellPositionCalculator = new CellPositionCalculator(table.getTable().getColumnCount(), table.getTable().getRowCount());
    }

    public void add(RowDrawInfo rowDrawInfo) {
        list.add(rowDrawInfo);

        for (CellDrawInfo cellDrawInfo : rowDrawInfo.cellDrawInfos()) {
            CellOutput cellOutput = cellDrawInfo.cellOutput();
            ListHeaderForCell lh = cellOutput.cell().getListHeader();

            cellPositionCalculator.addInfo(
                    lh.getColIndex(),
                    lh.getColSpan(),
                    lh.getRowIndex(),
                    lh.getRowSpan(),
                    lh.getWidth(),
                    Math.max(cellOutput.calculatedContentHeight() + lh.getTopMargin() + lh.getBottomMargin(), lh.getHeight()),
                    lh.getHeight());
        }
    }

    public RowDrawInfo[] rowDrawInfos() {
        return list.toArray(RowDrawInfo.Zero_Array);
    }

    public boolean overPage() {
        for (RowDrawInfo rowDrawInfo : list) {
            if (rowDrawInfo.overPage()) {
                return true;
            }
        }
        return false;
    }

    public int startRowIndex() {
        return list.get(0).rowIndex();
    }

    public int endRowIndex() {
        return list.get(list.size() - 1).rowIndex();
    }

    public void clear() {
        list.clear();
        cellPositionCalculator.reset();
    }

    public CellPositionCalculator cellPosition() {
        return cellPositionCalculator;
    }

    public boolean divided() {
        for (RowDrawInfo rowDrawInfo : list) {
            if (rowDrawInfo.divided()) {
                return true;
            }
        }
        return false;
    }
}
