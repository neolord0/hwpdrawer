package kr.dogfoot.hwplib.drawer.drawinginfo.interims.page;

import kr.dogfoot.hwplib.drawer.drawinginfo.interims.Content;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.Output;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextLine;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class FooterOutput extends Output {
    private final Area footerArea;
    private long calculatedContentHeight;

    private final Content content;

    public FooterOutput(Area footerArea) {
        this.footerArea = footerArea;
        calculatedContentHeight = 0;

        content = new Content();
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

    public void adjustFooterArea() {
        long offsetY = offsetY();
        for (TextLine line : content.textLines()) {
            for (TextPart part : line) {
                part.area().move(footerArea.left(), footerArea.top() + offsetY);
            }
        }

        for (ControlOutput controlOutput : content.behindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(footerArea.left(), footerArea.top() + offsetY);
            }
        }

        for (ControlOutput controlOutput : content.nonBehindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(footerArea.left(), footerArea.top() + offsetY);
            }
        }
    }

    private long offsetY() {
        if (calculatedContentHeight < footerArea.height()) {
            return footerArea.height() - calculatedContentHeight;
        }
        return 0;
    }

    @Override
    public Content content() {
        return content;
    }

    @Override
    public Type type() {
        return Type.Footer;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount).append("footer - {\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append("footer - }\n");
        return sb.toString();
    }

}
