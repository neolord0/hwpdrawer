package kr.dogfoot.hwpdrawer.output.page;

import kr.dogfoot.hwpdrawer.output.Content;
import kr.dogfoot.hwpdrawer.output.Output;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.text.TextLine;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class HeaderOutput extends Output {
    private final Area headerArea;

    private final Content content;

    public HeaderOutput(Area headerArea) {
        this.headerArea = headerArea;
        content = new Content(headerArea);
    }

    public void adjustHeaderArea() {
        for (TextLine line : content.textLines()) {
            line.area().move(headerArea.left(), headerArea.top());
        }

        for (ControlOutput controlOutput : content.behindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(headerArea.left(), headerArea.top());
            }
        }

        for (ControlOutput controlOutput : content.nonBehindChildOutputs()) {
            if (controlOutput.vertRelTo() == VertRelTo.Para) {
                controlOutput.move(headerArea.left(), headerArea.top());
            }
        }
    }

    @Override
    public Content content() {
        return content;
    }

    @Override
    public Type type() {
        return Type.Header;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount).append("header - {\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append("header - }\n");
        return sb.toString();
    }
}
