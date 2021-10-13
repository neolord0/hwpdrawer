package kr.dogfoot.hwplib.drawer.output;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.paralist.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.output.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.output.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.DivideAtPageBoundary;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public class InterimOutput {
    private Output currentOutput;

    private final Map<Integer, PageOutput> pageMap;
    private PageOutput currentPage;
    private int currentPageNo;
    private final ArrayList<ControlOutput> childControlsCrossingPage;
    private final ArrayList<DividedTableInfo> dividedTablesList;

    public InterimOutput() {
        currentOutput = null;

        currentPage = null;
        pageMap = new TreeMap<>();
        currentPageNo = 0;

        childControlsCrossingPage = new ArrayList<>();
        dividedTablesList = new ArrayList<>();
    }

    public PageOutput currentPage() {
        return currentPage;
    }

    public Output currentOutput() {
        return currentOutput;
    }

    public void gotoParentOutput() {
        this.currentOutput = currentOutput.parent();
    }

    public Output currentOutput(Output currentOutput) {
        this.currentOutput = currentOutput;
        return currentOutput;
    }

    public void nextPage(DrawingInput input) {
        currentPageNo++;
        PageOutput page = pageMap.get(currentPageNo);
        if (page == null) {
            page = new PageOutput(input.pageInfo(), input.currentColumnsInfo());
            pageMap.put(currentPageNo, page);
        }

        currentOutput = currentPage = page;
    }

    public void addEmptyPage(DrawingInput input) {
        currentPageNo++;
        PageOutput page = new PageOutput(input.pageInfo(), defaultColumnsInfo(input));
        pageMap.put(currentPageNo, page);

        currentOutput = currentPage = page;
    }

    public void addDrawingControls() {
        if (!childControlsCrossingPage.isEmpty()) {
            for (ControlOutput controlOutput : childControlsCrossingPage) {
                currentPage.content().currentRow().currentColumn().addChildOutput(controlOutput);
            }
            childControlsCrossingPage.clear();
        }

        ArrayList<TableOutput> tableOutputs = dividedTables();
        for (TableOutput tableOutput : tableOutputs) {
            currentPage.content().currentRow().currentColumn().addChildOutput(tableOutput);
        }
    }

    @NotNull
    private ColumnsInfo defaultColumnsInfo(DrawingInput input) {
        ColumnsInfo columnsInfo = new ColumnsInfo(input.pageInfo());
        columnsInfo.set(null, input.pageInfo().bodyArea());
        return columnsInfo;
    }

    public PageOutput gotoPage(PageOutput page) {
        currentPageNo = page.pageNo();

        currentOutput = currentPage = page;
        return currentPage;
    }

    public PageOutput[] pages() {
        return pageMap.values().toArray(PageOutput.Zero_Array);
    }

    public HeaderOutput startHeader() {
        HeaderOutput headerOutput = currentPage().createHeaderOutput();
        headerOutput.parent(currentOutput);
        currentOutput(headerOutput);
        return headerOutput;
    }

    public void endHeader() {
        HeaderOutput headerOutput = (HeaderOutput) currentOutput;
        headerOutput.adjustHeaderArea();

        gotoParentOutput();
    }

    public FooterOutput startFooter() {
        FooterOutput footerOutput = currentPage().createFooterOutput();
        footerOutput.parent(currentOutput);
        currentOutput(footerOutput);
        return footerOutput;
    }

    public void endFooter() {
        FooterOutput footerOutput = (FooterOutput) currentOutput;
        footerOutput.adjustFooterArea();

        gotoParentOutput();
    }

    public GsoOutput startGso(GsoControl gso, Area areaWithoutOuterMargin) {
        GsoOutput gsoOutput = new GsoOutput(gso, areaWithoutOuterMargin);
        gsoOutput.parent(currentOutput);
        currentOutput(gsoOutput);
        return gsoOutput;
    }

    public void endGso() {
        gotoParentOutput();
    }

    public TableOutput startTable(ControlTable table, Area areaWithoutOuterMargin) {
        TableOutput tableOutput = new TableOutput(table, areaWithoutOuterMargin);
        tableOutput.parent(currentOutput);
        currentOutput(tableOutput);
        return tableOutput;
    }

    public void endTable() {
        gotoParentOutput();
    }

    public CellOutput startCell(Cell cell) {
        CellOutput cellOutput = new CellOutput(cell);
        cellOutput.parent(currentOutput);
        currentOutput(cellOutput);
        return cellOutput;
    }

    public void endCell() {
        gotoParentOutput();
    }

    public Content currentContent() {
        return currentOutput().content();
    }

    public TextRow currentRow() {
        return currentContent().currentRow();
    }

    public TextColumn currentColumn() {
        return currentRow().currentColumn();
    }

    public void setLastLineInPara() {
        if (currentContent() != null) {
            currentColumn().setLastLineInPara();
        }
    }

    public boolean addChildOutput(ControlOutput childOutput) {
        if (currentOutput().type().isCell()) {
            return addChildOutputToCell(childOutput);
        } else {
            currentColumn().addChildOutput(childOutput);

            if (currentOutput().type().isGso()) {
                GsoOutput gsoOutput = (GsoOutput) currentOutput();
                gsoOutput.processAtAddingChildOutput(childOutput);
            } else if (currentOutput().type().isFooter()) {
                FooterOutput footerOutput = (FooterOutput) currentOutput();
                footerOutput.processAtAddingChildOutput(childOutput);
            }
            return true;
        }
    }

    private boolean addChildOutputToCell(ControlOutput childOutput) {
        CellOutput cellOutput = (CellOutput) currentOutput();
        if (canDivideCell(cellOutput.tableOutput().table()) && isOverPage(childOutput, cellOutput)) {
            cellOutput.addChildControlCrossingPage(childOutput);
            return false;
        } else {
            currentColumn().addChildOutput(childOutput);
            cellOutput.processAtAddingChildOutput(childOutput);
            return true;
        }
    }

    private boolean canDivideCell(ControlTable table) {
        return !table.getHeader().getProperty().isLikeWord() &&
                table.getTable().getProperty().getDivideAtPageBoundary() == DivideAtPageBoundary.Divide;
    }

    private boolean isOverPage(ControlOutput childOutput, CellOutput cellOutput) {
        long cellTopInPage = cellOutput.tableOutput().cellPosition().currentCellTop(cellOutput.cell().getListHeader().getColIndex())
                + cellOutput.tableOutput().areaWithoutOuterMargin().top();

        return currentPage().bodyArea().bottom() < childOutput.areaWithoutOuterMargin().bottom() + cellTopInPage;
    }

    public void addTextLine(TextLine line) {
        if (currentContent() != null) {
            currentColumn().addTextLine(line);
        }
    }

    public int textLineCount() {
        return currentColumn().textLineCount();
    }

    public boolean checkRedrawingTextLine(Area area) {
        return currentColumn().checkRedrawingTextLine(area);
    }

    public TextLine deleteRedrawingTextLine(Area area) {
        return currentColumn().deleteRedrawingTextLine(area);
    }

    public TextLine hideTextLine(int topLineIndex) {
        return currentColumn().hideTextLineIndex(topLineIndex);
    }

    public void resetHidingTextLineIndex() {
        currentColumn().resetHideTextLineIndex();
    }

    public TextColumn.ResultDeleteTextLineIndex deleteTextLineIndex(int topLineIndex) {
        return currentColumn().deleteTextLineIndex(topLineIndex);
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return currentRow().hadRearrangedDistributionMultiColumn();
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        currentRow().hadRearrangedDistributionMultiColumn(hadRearrangedDistributionMultiColumn);
    }

    public void nextColumn() {
        currentRow().nextColumn();
    }

    public void previousColumn() {
        currentRow().previousColumn();
    }

    public void clearColumn() {
        currentColumn().clear();
    }

    public void gotoFirstColumn() {
        currentRow().gotoColumn(0);
    }

    public void nextRow(ColumnsInfo columnsInfo) {
        currentContent().nextRow(columnsInfo);
    }

    public int currentRowIndex() {
        return currentContent().currentRowIndex();
    }

    public TextRow gotoRow(int rowIndex) {
        return currentContent().gotoRow(rowIndex);
    }

    public void gotoLastRow() {
        gotoLastPage();
        currentContent().gotoLastRow();
    }

    private void gotoLastPage() {
        PageOutput lastPage = lastPage();
        if (lastPage != null) {
            currentPageNo = lastPage.pageNo();
            currentOutput = currentPage = lastPage;
        }
    }

    private PageOutput lastPage() {
        PageOutput[] pages = pages();
        if (pages.length > 0) {
            return pages[pages.length - 1];
        }
        return null;
    }

    public long rowHeight() {
        return currentContent().rowHeight();
    }

    public long rowBottom() {
        return currentContent().rowBottom();
    }

    public void addChildControlsCrossingPage(ControlOutput output) {
        childControlsCrossingPage.add(output);
    }

    public ControlOutput[] childControlsCrossingPage() {
        return childControlsCrossingPage.toArray(ControlOutput.Zero_Array);
    }

    public void clearChildControlsCrossingPage() {
        childControlsCrossingPage.clear();
    }


    public void addDividedTables(Queue<TableOutput> dividedTables) {
        dividedTablesList.add(new DividedTableInfo(currentPageNo + 1, dividedTables));
    }

    public ArrayList<TableOutput> dividedTables() {
        ArrayList<TableOutput> tableOutputs = new ArrayList<>();

        ArrayList<DividedTableInfo> removingObjects = new ArrayList<>();
        for (DividedTableInfo dividedTableInfo : dividedTablesList) {
            if (dividedTableInfo.pageNo == currentPageNo) {
                tableOutputs.add(dividedTableInfo.tableList.poll());
                if (dividedTableInfo.tableList.isEmpty()) {
                    removingObjects.add(dividedTableInfo);
                } else {
                    dividedTableInfo.pageNo++;
                }
            }
        }

        for (DividedTableInfo removeObject : removingObjects) {
            dividedTablesList.remove(removeObject);
        }
        return tableOutputs;
    }

    public boolean hasDrawingControls() {
        return !childControlsCrossingPage.isEmpty() || hasDividedTables();
    }

    private boolean hasDividedTables() {
        for (DividedTableInfo dividedTableInfo : dividedTablesList) {
            if (dividedTableInfo.pageNo == currentPageNo + 1) {
                return true;
            }
        }
        return false;
    }

    public class DividedTableInfo {
        int pageNo;
        Queue<TableOutput> tableList;

        public DividedTableInfo(int pageNo, Queue<TableOutput> tableList) {
            this.pageNo = pageNo;
            this.tableList = tableList;
        }
    }
}
