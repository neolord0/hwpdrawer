package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.PageOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.Column;
import kr.dogfoot.hwplib.drawer.interimoutput.text.MultiColumn;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class InterimOutput {
    private final Stack<Output> stack;

    private Map<Integer, PageOutput> pageMap;
    private int currentPageNo;

    private final ArrayList<ControlInfo> controlsMovedToNextPage;

    public InterimOutput() {
        stack = new Stack<>();

        pageMap = new HashMap<>();
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
                    page.content().currentMultiColumn().currentColumn().addChildOutput(controlInfo.output);
                }
                controlsMovedToNextPage.clear();
            }
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
            currentContent().currentMultiColumn().currentColumn().setLastLineInPara();
        }
    }

    public Content currentContent() {
        return currentOutput().content();
    }

    private Output currentOutput() {
        return stack.peek();
    }

    public MultiColumn currentMultiColumn() {
        return currentContent().currentMultiColumn();
    }

    public Column currentColumn() {
        return currentMultiColumn().currentColumn();
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

    public TextLine deleteTextLineIndex(int topLineIndex) {
        return currentColumn().deleteTextLineIndex(topLineIndex);
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return currentContent().hadRearrangedDistributionMultiColumn();
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        currentContent().hadRearrangedDistributionMultiColumn(hadRearrangedDistributionMultiColumn);
    }

    public void nextColumn() {
        currentMultiColumn().nextColumn();
    }

    public void previousColumn() {
        currentMultiColumn().previousColumn();
    }

    public void clearColumn() {
        currentColumn().clear();
    }

    public void nextMultiColumn(ColumnsInfo columnsInfo) {
        currentContent().nextMultiColumn(columnsInfo);
    }

    public int currentMultiColumnIndex() {
        return  currentContent().currentMultiColumnIndex();
    }

    public long multiColumnHeight() {
        return currentContent().multiColumnHeight();
    }

    public long multiColumnBottom() {
        return currentContent().multiColumnBottom();
    }

    public void gotoStartingParallelMultiColumn(ParallelMultiColumnInfo parallelMultiColumnInfo) {
        gotoPage(parallelMultiColumnInfo.startingPageNo()).content().gotoMultiColumn(parallelMultiColumnInfo.startingMultiColumnIndex());
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
