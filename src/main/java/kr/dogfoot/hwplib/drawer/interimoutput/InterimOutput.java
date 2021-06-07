package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.PageOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.ArrayList;
import java.util.Stack;

public class InterimOutput {
    private PageOutput page;
    private final Stack<Output> stack;

    private final ArrayList<ControlInfo> controlsMovedToNextPage;

    public InterimOutput() {
        stack = new Stack<>();
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

    public void newPageOutput(DrawingInput input) {
        stack.clear();
        page = new PageOutput(input.pageInfo(), input.columnsInfo());
        stack.add(page);

        if (!controlsMovedToNextPage.isEmpty()) {
            for (ControlInfo controlInfo : controlsMovedToNextPage) {
                page.content().addChildOutput(controlInfo.output);
            }
            controlsMovedToNextPage.clear();
        }
    }

    public PageOutput page() {
        return page;
    }

    public HeaderOutput startHeader() {
        HeaderOutput headerOutput = page.createHeaderOutput();
        stack.push(headerOutput);
        return headerOutput;
    }

    public void endHeader() {
        HeaderOutput headerOutput = (HeaderOutput) stack.pop();
        headerOutput.adjustHeaderArea();
    }

    public FooterOutput startFooter() {
        FooterOutput footerOutput = page.createFooterOutput();
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
            currentContent().setLastLineInPara();
        }
    }
    private Content currentContent() {
        return currentOutput().content();
    }

    private Output currentOutput() {
        return stack.peek();
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (currentContent() != null) {
            currentContent().addChildOutput(childOutput);
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
            currentContent().addTextLine(line);
        }
    }

    public int textLineCount() {
        return currentContent().textLineCount();
    }

    public boolean checkRedrawingTextLine(Area area) {
        return currentContent().checkRedrawingTextLine(area);
    }

    public TextLine deleteRedrawingTextLine(Area area) {
        return currentContent().deleteRedrawingTextLine(area);
    }

    public TextLine hideTextLine(int topLineIndex) {
        return currentContent().hideTextLine(topLineIndex);
    }

    public void resetHidingTextLineIndex() {
        currentContent().resetHidingTextLineIndex();
    }

    public TextLine deleteTextLineIndex(int topLineIndex) {
        return currentContent().deleteTextLineIndex(topLineIndex);
    }

    public boolean hadRearrangedDistributionMultiColumn() {
        return currentContent().hadRearrangedDistributionMultiColumn();
    }

    public void hadRearrangedDistributionMultiColumn(boolean hadRearrangedDistributionMultiColumn) {
        currentContent().hadRearrangedDistributionMultiColumn(hadRearrangedDistributionMultiColumn);
    }

    public void nextColumn() {
        currentContent().nextColumn();
    }

    public void previousColumn() {
        currentContent().previousColumn();
    }

    public void clearColumn() {
        currentContent().clearColumn();
    }

    public long multiColumnHeight() {
        return currentContent().multiColumnHeight();
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
