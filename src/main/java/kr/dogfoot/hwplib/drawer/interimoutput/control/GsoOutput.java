package kr.dogfoot.hwplib.drawer.interimoutput.control;

import kr.dogfoot.hwplib.drawer.interimoutput.Content;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
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

    public GsoOutput(GsoControl gso, Area controlArea) {
        this.gso = gso;
        this.controlArea = controlArea;
        textMargin = null;
        verticalAlignment = TextVerticalAlignment.Top;
        calculatedContentHeight = 0;

        content = new Content(controlArea);
    }

    public GsoControl gso() {
        return gso;
    }

    public GsoOutput textMargin(int left, int top, int right, int bottom) {
        textMargin = new Area(left, top, right, bottom);
        return this;
    }

    public Area textArea() {
        if (textMargin == null) {
            return controlArea;
        } else {
            return new Area(controlArea)
                    .applyMargin(textMargin.left(),
                            textMargin.top(),
                            textMargin.right(),
                            textMargin.bottom());
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
            calculatedContentHeight(childOutput.controlArea().bottom());
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
        controlArea.move(offsetX, offsetY);

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
    public void adjustTextAreaAndVerticalAlignment() {
        Area textArea = textArea();
        long offsetY = offsetY(textArea, verticalAlignment);

        for (TextLine line : content.textLines()) {
            line.area().move(textArea.left(), textArea.top() + offsetY);
        }

        move(textArea.left(), textArea.top() + offsetY);
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
        sb.tab(tabCount).append(gso.getGsoType().toString()).append(" - { ").append(controlArea).append("\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append(gso.getGsoType().toString()).append(" - }\n");
        return sb.toString();
    }
}
