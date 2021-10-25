package kr.dogfoot.hwplib.drawer.output.control.table;

import kr.dogfoot.hwplib.drawer.output.Content;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
import kr.dogfoot.hwplib.object.bodytext.control.table.ListHeaderForCell;

public class TableOutput extends ControlOutput {
    private final ControlTable table;
    private final CellPositionCalculator cellPositionCalculator;

    private final CellOutput[][] cellOutputs;

    private boolean divided;

    public TableOutput(ControlTable table, Area areaWithoutOuterMargin) {
        this.table = table;
        this.areaWithoutOuterMargin = new Area(areaWithoutOuterMargin);

        this.cellOutputs = new CellOutput[table.getTable().getRowCount()][table.getTable().getColumnCount()];

        cellPositionCalculator = new CellPositionCalculator(table.getTable().getColumnCount(), table.getTable().getRowCount());
        divided = false;
    }

    public void addCell(CellOutput cellOutput) {
        ListHeaderForCell lh = cellOutput.cell().getListHeader();
        cellOutputs[lh.getRowIndex()][lh.getColIndex()] = cellOutput;

        cellPositionCalculator.addInfo(
                lh.getColIndex(),
                lh.getColSpan(),
                lh.getRowIndex(),
                lh.getRowSpan(),
                lh.getWidth(),
                Math.max(cellOutput.calculatedContentHeight() + lh.getTopMargin() + lh.getBottomMargin(), lh.getHeight()),
                lh.getHeight());
    }

    public CellOutput[][] cellOutputs() {
        return cellOutputs;
    }

    public CellPositionCalculator cellPosition() {
        return cellPositionCalculator;
    }

    public boolean divided() {
        return divided;
    }

    public TableOutput divided(boolean divided) {
        this.divided = divided;
        return this;
    }

    @Override
    public int zOrder() {
        if (divided == true) {
            return table.getHeader().getzOrder() - 100;
        } else {
            return table.getHeader().getzOrder();
        }
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
        areaWithoutOuterMargin.move(offsetX, offsetY);
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
    public Area areaWithOuterMargin() {
        return new Area(areaWithoutOuterMargin)
                .expand(table.getHeader().getOutterMarginLeft(),
                        table.getHeader().getOutterMarginTop(),
                        table.getHeader().getOutterMarginRight(),
                        table.getHeader().getOutterMarginBottom());
    }

    public ControlTable table() {
        return table;
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
        sb.tab(tabCount).append("table - { ").append(areaWithoutOuterMargin).append("\n");
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

    public DivideAtPageBoundary getDivideAtPageBoundary() {
        return table.getTable().getProperty().getDivideAtPageBoundary();
    }
}
