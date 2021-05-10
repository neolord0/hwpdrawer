package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.drawinginfo.interims.table.CellOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.table.TableOutput;
import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

import java.util.Stack;

public class InterimOutput {
    private PageOutput page;
    private Stack<Output> stack;

    public InterimOutput() {
        stack = new Stack<>();
    }

    public void newPageOutput(int pageNo, Area paperArea, Area pageArea) {
        stack.clear();

        page = new PageOutput(pageNo, paperArea, pageArea);
        stack.add(page);
    }

    public PageOutput page() {
        return page;
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
            current().content().setLastTextPartToLastLine();;
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
        }



    }

    public void addTextPart(TextPart part) {
        if (current().content() != null) {
            current().content().addTextPart(part);
        }
    }

}
