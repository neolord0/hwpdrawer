package kr.dogfoot.hwplib.drawer.interimoutput.control.table;

import kr.dogfoot.hwplib.drawer.interimoutput.Content;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

public class TableOutput extends ControlOutput {
    private final ControlTable table;
    private final CellPositionCalculator cellPositionCalculator;

    private final CellOutput[][] cellOutputs;

    public TableOutput(ControlTable table, Area controlArea) {
        this.table = table;
        this.controlArea = controlArea;

        this.cellOutputs = new CellOutput[table.getTable().getColumnCount()][table.getTable().getRowCount()];

        cellPositionCalculator = new CellPositionCalculator(table.getTable().getColumnCount(), table.getTable().getRowCount());
    }

    public void addCell(CellOutput cellOutput) {
        ListHeaderForCell lh = cellOutput.cell().getListHeader();
        cellOutputs[lh.getColIndex()][lh.getRowIndex()] = cellOutput;

        cellPositionCalculator.addInfo(
                lh.getColIndex(),
                lh.getColSpan(),
                lh.getRowIndex(),
                lh.getRowSpan(),
                lh.getWidth(),
                Math.max(cellOutput.calculatedHeight(), lh.getHeight()));
    }

    public CellOutput[][] cellOutputs() {
        return cellOutputs;
    }

    public CellPositionCalculator cellPosition() {
        return cellPositionCalculator;
    }

    @Override
    public int zOrder() {
        return table.getHeader().getzOrder();
    }

    @Override
    public TextFlowMethod textFlowMethod() {
        return table.getHeader().getProperty().getTextFlowMethod();
    }

    @Override
    public VertRelTo vertRelTo() {
        return table.getHeader().getProperty().getVertRelTo();
    }

    @Override
    public void move(long offsetX, long offsetY) {
        controlArea.move(offsetX, offsetY);
        for (CellOutput[] cellOutputs2 : cellOutputs) {
            for (CellOutput cellOutput : cellOutputs2) {
                if (cellOutput != null) {
                    cellOutput.move(offsetX, offsetY);
                }
            }
        }
    }

    @Override
    public void adjustTextBoxAreaAndVerticalAlignment() {
        // nothing
    }

    @Override
    public Content content() {
        return null;
    }

    @Override
    public Type type() {
        return Type.Table;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount).append("table - { ").append(controlArea).append("\n");
        for (CellOutput[] cellOutputs2 : cellOutputs) {
            for (CellOutput cellOutput : cellOutputs2) {
                if (cellOutput != null) {
                    sb.append(cellOutput.test(tabCount + 1));
                }
            }
        }
        sb.tab(tabCount).append("table - }\n");
        return sb.toString();

    }
}
