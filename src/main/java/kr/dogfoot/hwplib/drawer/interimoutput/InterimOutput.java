package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.input.PageInfo;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.PageOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.Stack;

public class InterimOutput {
    private PageOutput page;
    private final Stack<Output> stack;

    public InterimOutput() {
        stack = new Stack<>();
    }

    public void newPageOutput(PageInfo pageInfo) {
        stack.clear();

        page = new PageOutput(pageInfo);
        stack.add(page);
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

    public void setLastTextPartToLastLine() {
        if (currentOutput().content() != null) {
            currentOutput().content().setLastTextPartToLastLine();
        }
    }

    private Output currentOutput() {
        return stack.peek();
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (currentOutput().content() != null) {
            currentOutput().content().addChildOutput(childOutput);
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
        if (currentOutput().content() != null) {
            currentOutput().content().addTextLine(line);
        }
    }

    public TextLine checkAndDeleteRedrawingTextLine(Area area) {
        return currentOutput().content().deleteRedrawingTextLine(area);
    }
}
