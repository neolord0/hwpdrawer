package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.PageOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextRow;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class InterimOutput {
    private final Stack<Output> stack;

    private Map<Integer, PageOutput> pageMap;
    private int currentPageNo;

    private final ArrayList<ControlInfo> controlsMovedToNextPage;

    public InterimOutput() {
        stack = new Stack<>();

        pageMap = new TreeMap<>();
        currentPageNo = 0;

        controlsMovedToNextPage = new ArrayList<>();
    }

    public void addControlMovedToNextPage(ControlOutput output, ControlCharInfo charInfo) {
        controlsMovedToNextPage.add(new ControlInfo(output, charInfo));
    }

    public boolean hasControlMovedToNextPage() {
        return !controlsMovedToNextPage.isEmpty();
    }

    public ControlInfo[] controlsMovedToNextPage() {
        return controlsMovedToNextPage.toArray(ControlInfo.Zero_Array);
    }

    public void nextPage(DrawingInput input) throws Exception {
        stack.clear();

        currentPageNo++;
        PageOutput page = pageMap.get(currentPageNo);
        if (page == null) {
            page = new PageOutput(input.pageInfo(), input.columnsInfo());
            pageMap.put(currentPageNo, page);

            if (!controlsMovedToNextPage.isEmpty()) {
                for (ControlInfo controlInfo : controlsMovedToNextPage) {
                    page.content().currentRow().currentColumn().addChildOutput(controlInfo.output);
                }
                controlsMovedToNextPage.clear();
            }
        }

        stack.add(page);
    }


    public void addEmptyPage(DrawingInput input) {
        stack.clear();

        currentPageNo++;
        ColumnsInfo columnsInfo = new ColumnsInfo(input.pageInfo());
        columnsInfo.set(null, input.pageInfo().bodyArea());
        PageOutput page = new PageOutput(input.pageInfo(), columnsInfo);
        pageMap.put(currentPageNo, page);

        if (!controlsMovedToNextPage.isEmpty()) {
            for (ControlInfo controlInfo : controlsMovedToNextPage) {
                page.content().currentRow().currentColumn().addChildOutput(controlInfo.output);
            }
            controlsMovedToNextPage.clear();
        }

        stack.add(page);
    }

    public PageOutput gotoPage(int pageNo) {
        stack.clear();
        currentPageNo = pageNo;
        PageOutput page = pageMap.get(currentPageNo);
        stack.add(page);
        return page;
    }

    public PageOutput[] pages() {
        return pageMap.values().toArray(PageOutput.Zero_Array);
    }

    public PageOutput currentPage() {
        return pageMap.get(currentPageNo);
    }

    public void clearPages() {
        pageMap.clear();
    }

    public HeaderOutput startHeader() {
        HeaderOutput headerOutput = currentPage().createHeaderOutput();
        stack.push(headerOutput);
        return headerOutput;
    }

    public void endHeader() {
        HeaderOutput headerOutput = (HeaderOutput) stack.pop();
        headerOutput.adjustHeaderArea();
    }

    public FooterOutput startFooter() {
        FooterOutput footerOutput = currentPage().createFooterOutput();
        stack.push(footerOutput);
        return footerOutput;
    }

    public void endFooter() {
        FooterOutput footerOutput = (FooterOutput) stack.pop();
        footerOutput.adjustFooterArea();
    }

    public GsoOutput startGso(GsoControl gso, Area controlArea) {
        GsoOutput gsoOutput = new GsoOutput(gso, controlArea);
        stack.push(gsoOutput);
        return gsoOutput;
    }

    public void endGso() {
        stack.pop();
    }

    public TableOutput startTable(ControlTable table, Area controlArea) {
        TableOutput tableOutput = new TableOutput(table, controlArea);
        stack.push(tableOutput);
        return tableOutput;
    }

    public void endTable() {
        stack.pop();
    }

    public CellOutput startCell(Cell cell, TableOutput tableOutput) {
        CellOutput cellOutput = new CellOutput(tableOutput, cell);
        stack.push(cellOutput);
        return cellOutput;
    }

    public void endCell() {
        stack.pop();
    }

    public void setLastLineInPara() {
        if (currentContent() != null) {
            currentContent().currentRow().currentColumn().setLastLineInPara();
        }
    }

    public Content currentContent() {
        return currentOutput().content();
    }

    public Output currentOutput() {
        return stack.peek();
    }

    public TextRow currentRow() {
        return currentContent().currentRow();
    }

    public TextColumn currentColumn() {
        return currentRow().currentColumn();
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (currentContent() != null) {
            currentColumn().addChildOutput(childOutput);
        }

        if (currentOutput().type() == Output.Type.Gso) {
            GsoOutput gsoOutput = (GsoOutput) currentOutput();
            gsoOutput.processAtAddingChildOutput(childOutput);
        } else if (currentOutput().type() == Output.Type.Cell) {
            CellOutput cellOutput = (CellOutput) currentOutput();
            cellOutput.processAtAddingChildOutput(childOutput);
        } else if (currentOutput().type() == Output.Type.Footer) {
            FooterOutput footerOutput = (FooterOutput) currentOutput();
            footerOutput.processAtAddingChildOutput(childOutput);
        }
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
            gotoPage(lastPage);
        }
    }

    private void gotoPage(PageOutput lastPage) {
        stack.clear();
        currentPageNo = lastPage.pageNo();
        stack.add(lastPage);
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

    public void gotoStartingParallelMultiColumn(ParallelMultiColumnInfo parallelMultiColumnInfo) {
        gotoPage(parallelMultiColumnInfo.startingPageNo()).content().gotoRow(parallelMultiColumnInfo.startingRowIndex());
    }

    public static final class ControlInfo {
        public static final ControlInfo[] Zero_Array = new ControlInfo[0];

        private final ControlOutput output;
        private final ControlCharInfo charInfo;

        public ControlInfo(ControlOutput output, ControlCharInfo charInfo) {
            this.output = output;
            this.charInfo = charInfo;
        }

        public ControlOutput output() {
            return output;
        }

        public ControlCharInfo charInfo() {
            return charInfo;
        }
    }
}
