package kr.dogfoot.hwplib.drawer.output;

import jdk.internal.org.objectweb.asm.util.ASMifiable;
import kr.dogfoot.hwplib.drawer.input.paralist.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.output.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.output.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import org.jetbrains.annotations.NotNull;

import javax.xml.soap.SAAJMetaFactory;
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

    public void nextPage(DrawingInput input) {
        stack.clear();

        currentPageNo++;
        PageOutput page = pageMap.get(currentPageNo);
        if (page == null) {
            page = new PageOutput(input.pageInfo(), input.columnsInfo());
            pageMap.put(currentPageNo, page);

            addMovedControls(page);
        }

        stack.add(page);
    }

    private void addMovedControls(PageOutput page) {
        if (!controlsMovedToNextPage.isEmpty()) {
            for (ControlInfo controlInfo : controlsMovedToNextPage) {
                page.content().currentRow().currentColumn().addChildOutput(controlInfo.output);
            }
            controlsMovedToNextPage.clear();
        }
    }

    public void addEmptyPage(DrawingInput input) {
        stack.clear();

        currentPageNo++;
        PageOutput page = new PageOutput(input.pageInfo(), defaultColumnsInfo(input));
        pageMap.put(currentPageNo, page);

        addMovedControls(page);

        stack.add(page);
    }

    @NotNull
    private ColumnsInfo defaultColumnsInfo(DrawingInput input) {
        ColumnsInfo columnsInfo = new ColumnsInfo(input.pageInfo());
        columnsInfo.set(null, input.pageInfo().bodyArea());
        return columnsInfo;
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

    public GsoOutput startGso(GsoControl gso, Area areaWithoutOuterMargin) {
        GsoOutput gsoOutput = new GsoOutput(gso, areaWithoutOuterMargin);
        stack.push(gsoOutput);
        return gsoOutput;
    }

    public void endGso() {
        stack.pop();
    }

    public TableOutput startTable(ControlTable table, Area areaWithoutOuterMargin) {
        TableOutput tableOutput = new TableOutput(table, areaWithoutOuterMargin);
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

    public Output currentOutput() {
        return stack.peek();
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
        if (currentOutput().type() == Output.Type.Cell) {
            return addChildOutputToCell(childOutput);
        } else {
            currentColumn().addChildOutput(childOutput);

            if (currentOutput().type() == Output.Type.Gso) {
                GsoOutput gsoOutput = (GsoOutput) currentOutput();
                gsoOutput.processAtAddingChildOutput(childOutput);
            } else if (currentOutput().type() == Output.Type.Footer) {
                FooterOutput footerOutput = (FooterOutput) currentOutput();
                footerOutput.processAtAddingChildOutput(childOutput);
            }
            return true;
        }
    }

    private boolean addChildOutputToCell(ControlOutput childOutput) {
        CellOutput cellOutput = (CellOutput) currentOutput();
        long cellTopInPage = cellOutput.tableOutput().cellPosition().currentCellTop(cellOutput.cell().getListHeader().getColIndex())  + cellOutput.tableOutput().areaWithoutOuterMargin().top();
        if (cellOutput.tableOutput().canSplitCell() && currentPage().bodyArea().bottom() < (childOutput.areaWithoutOuterMargin().bottom() + cellTopInPage)) {
            cellOutput.addChildControlCrossingPage(childOutput);
            return false;
        } else {
            currentColumn().addChildOutput(childOutput);
            cellOutput.processAtAddingChildOutput(childOutput);
            return true;
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
            stack.clear();
            currentPageNo = lastPage.pageNo();
            stack.add(lastPage);
        }
    }

    private PageOutput lastPage() {
        PageOutput[] pages = pages();
        if (pages.length > 0) {
            return pages[pages.length - 1];
        }
        return null;
    }

    public void gotoPageAndRow(int pageNo, int rowIndex) {
        gotoPage(pageNo).gotoRow(rowIndex);
    }

    public long rowHeight() {
        return currentContent().rowHeight();
    }

    public long rowBottom() {
        return currentContent().rowBottom();
    }

    public void addControlMovedToNextPage(ControlOutput output, ControlCharInfo charInfo) {
        controlsMovedToNextPage.add(new ControlInfo(output, charInfo));
    }

    public boolean hasControlMovedToNextPage() {
        return !controlsMovedToNextPage.isEmpty();
    }

    public ControlCharInfo[] controlsMovedToNextPage() {
        int count = controlsMovedToNextPage.size();
        ControlCharInfo[] controls = new ControlCharInfo[count];
        for (int index = 0; index < count; index++) {
            controls[index] = controlsMovedToNextPage.get(index).charInfo;
        }
        return controls;
    }

    private static final class ControlInfo {
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
