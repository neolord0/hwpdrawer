package kr.dogfoot.hwplib.drawer.drawinginfo.interims.table;

import kr.dogfoot.hwplib.drawer.drawinginfo.interims.Content;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.Output;
import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuffer;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;

public class CellOutput extends Output {
    private TableOutput tableOutput;
    private Cell cell;
    private Area cellArea;
    private Area textMargin;
    private TextVerticalAlignment verticalAlignment;

    private long calculatedContentHeight;

    private Content content;

    public CellOutput(TableOutput tableOutput, Cell cell) {
        this.tableOutput = tableOutput;
        this.cell = cell;
        this.cellArea = new Area(0,
                0,
                cell.getListHeader().getWidth(),
                cell.getListHeader().getHeight());

        textMargin = null;
        verticalAlignment = TextVerticalAlignment.Top;

        content = new Content();
    }

    public Cell cell() {
        return cell;
    }

    public CellOutput textMargin(int left, int top, int right, int bottom) {
        textMargin = new Area(left, top, right, bottom);
        return this;
    }

    public Area textArea() {
        if (textMargin == null) {
            return cellArea;
        } else {
            return new Area(cellArea)
                    .applyMargin(textMargin.left(),
                            textMargin.top(),
                            textMargin.right(),
                            textMargin.bottom());
        }
    }

    public void adjustTextAreaAndVerticalAlignment(Area cellArea, Area textArea) {
        long offsetY = offsetY(textArea, verticalAlignment);

        for (TextPart textPart : content.textParts()) {
            textPart.area()
                    .moveY(offsetY);
        }

        move(cellArea.left(), cellArea.top());
    }

    private long offsetY(Area textArea, TextVerticalAlignment verticalAlignment) {
        if (calculatedContentHeight < textArea.height()) {
            switch (verticalAlignment) {
                case Top:
                    return 0;
                case Center:
                    return (textArea.height() - calculatedContentHeight) / 2;
                case Bottom:
                    return textArea.height() - calculatedContentHeight;
            }
        }
        return 0;
    }

    public void move(long offsetX, long offsetY) {
        cellArea.moveX(offsetX)
                .moveY(offsetY);

        for (TextPart textPart : content.textParts()) {
            textPart.area()
                    .moveX(offsetX)
                    .moveY(offsetY);
        }

        for (ControlOutput controlOutput : content.behindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(offsetX, offsetY);
            }
        }

        for (ControlOutput controlOutput : content.nonBehindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(offsetX, offsetY);
            }
        }
    }

    public void verticalAlignment(TextVerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public long calculatedContentHeight() {
        return calculatedContentHeight;
    }

    public void calculatedContentHeight(long calculatedContentHeight) {
        this.calculatedContentHeight =
                Math.max(this.calculatedContentHeight, calculatedContentHeight);
    }

    public long calculatedHeight() {
        return calculatedContentHeight + cell.getListHeader().getTopMargin() + cell.getListHeader().getBottomMargin();
    }

    public void processAtAddingChildOutput(ControlOutput childOutput) {
        if (childOutput.vertRelTo() == VertRelTo.Para) {
            calculatedContentHeight(childOutput.controlArea().bottom() - textArea().top());
        }

    }

    @Override
    public Content content() {
        return content;
    }

    @Override
    public Type type() {
        return Type.Cell;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuffer sb = new MyStringBuffer();
        sb.tab(tabCount).append("cell - {" ).append(cellArea).append("\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append("cell - }\n");
        return sb.toString();
    }

}
