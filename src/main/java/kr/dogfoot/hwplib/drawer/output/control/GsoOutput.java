package kr.dogfoot.hwplib.drawer.output.control;

import kr.dogfoot.hwplib.drawer.output.Content;
import kr.dogfoot.hwplib.drawer.output.text.TextColumn;
import kr.dogfoot.hwplib.drawer.output.text.TextLine;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;

public class GsoOutput extends ControlOutput {
    private final GsoControl gso;
    private Area textMargin;
    private TextVerticalAlignment verticalAlignment;

    private long calculatedContentHeight;

    private final Content content;

    public GsoOutput(GsoControl gso, Area areaWithoutOuterMargin) {
        this.gso = gso;
        this.areaWithoutOuterMargin = areaWithoutOuterMargin;
        textMargin = null;
        verticalAlignment = TextVerticalAlignment.Top;
        calculatedContentHeight = 0;

        content = new Content(areaWithoutOuterMargin);
    }

    public GsoControl gso() {
        return gso;
    }

    public GsoOutput textMargin(int left, int top, int right, int bottom) {
        textMargin = new Area(left, top, right, bottom);
        return this;
    }

    public Area textBoxArea() {
        if (textMargin == null) {
            return areaWithoutOuterMargin;
        } else {
            return new Area(areaWithoutOuterMargin)
                    .applyMargin(textMargin.left(),
                            textMargin.top(),
                            textMargin.right(),
                            textMargin.bottom());
        }
    }

    public void applyCalculatedContentHeight() {
        if (calculatedContentHeight > textBoxArea().height()) {
            areaWithoutOuterMargin.bottom(areaWithoutOuterMargin.top() + textMargin.top() + calculatedContentHeight + textMargin.bottom());
        }
    }

    public void verticalAlignment(TextVerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public void calculatedContentHeight(long calculatedContentHeight) {
        this.calculatedContentHeight =
                Math.max(this.calculatedContentHeight, calculatedContentHeight);
    }

    public void processAtAddingChildOutput(ControlOutput childOutput) {
        if (childOutput.vertRelTo() == VertRelTo.Para) {
            calculatedContentHeight(childOutput.areaWithoutOuterMargin().bottom());
        }
    }

    @Override
    public int zOrder() {
        return gso.getHeader().getzOrder();
    }

    @Override
    public TextFlowMethod textFlowMethod() {
        return gso.getHeader().getProperty().getTextFlowMethod();
    }

    @Override
    public VertRelTo vertRelTo() {
        return gso.getHeader().getProperty().getVertRelTo();
    }

    @Override
    public void move(long offsetX, long offsetY) {
        areaWithoutOuterMargin.move(offsetX, offsetY);

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

    @Override
    public void adjustTextBoxAreaAndVerticalAlignment() {
        Area textBoxArea = textBoxArea();
        long offsetY = offsetY(textBoxArea, verticalAlignment);

        for (TextRow multiColumn : content.rows()) {
            for (TextColumn column : multiColumn.columns()) {
                for (TextLine line : column.textLines()) {
                    line.area().move(textBoxArea.left(), textBoxArea.top() + offsetY);;
                }
            }
        }

        move(textBoxArea.left(), textBoxArea.top() + offsetY);
    }

    @Override
    public Area areaWithOuterMargin() {
        return new Area(areaWithoutOuterMargin)
                .expand(gso.getHeader().getOutterMarginLeft(),
                        gso.getHeader().getOutterMarginTop(),
                        gso.getHeader().getOutterMarginRight(),
                        gso.getHeader().getOutterMarginBottom());
    }

    private long offsetY(Area textBoxArea, TextVerticalAlignment verticalAlignment) {
        if (calculatedContentHeight < textBoxArea.height()) {
            switch (verticalAlignment) {
                case Top:
                    return 0;
                case Center:
                    return (textBoxArea.height() - calculatedContentHeight) / 2;
                case Bottom:
                    return textBoxArea.height() - calculatedContentHeight;
            }
        }
        return 0;
    }

    @Override
    public Content content() {
        return content;
    }

    @Override
    public Type type() {
        return Type.Gso;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount).append(gso.getGsoType().toString()).append(" - { ").append(areaWithoutOuterMargin).append("\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append(gso.getGsoType().toString()).append(" - }\n");
        return sb.toString();
    }
}
