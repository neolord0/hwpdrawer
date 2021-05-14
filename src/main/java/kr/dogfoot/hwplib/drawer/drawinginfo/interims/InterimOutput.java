package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.drawinginfo.PageInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.GsoOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.page.HeaderOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.page.PageOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextLine;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextPart;
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


    public Output current() {
        return stack.peek();
    }

    public void setLastTextPartToLastLine() {
        if (current().content() != null) {
            current().content().setLastTextPartToLastLine();
        }
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (current().content() != null) {
            current().content().addChildOutput(childOutput);
        }

        if (current().type() == Output.Type.Gso) {
            GsoOutput gsoOutput = (GsoOutput) current();
            gsoOutput.processAtAddingChildOutput(childOutput);
        } else if (current().type() == Output.Type.Cell) {
            CellOutput cellOutput = (CellOutput) current();
            cellOutput.processAtAddingChildOutput(childOutput);
        } else if (current().type() == Output.Type.Footer) {
            FooterOutput footerOutput = (FooterOutput) current();
            footerOutput.processAtAddingChildOutput(childOutput);
        }
    }

    public void addTextLine(TextLine line) {
        if (current().content() != null) {
            current().content().addTextLine(line);
        }
    }
}
