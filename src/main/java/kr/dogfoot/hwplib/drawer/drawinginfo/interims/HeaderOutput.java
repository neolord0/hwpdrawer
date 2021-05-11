package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuffer;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

public class HeaderOutput extends Output {
    private Area headerArea;

    private Content content;

    public HeaderOutput(Area headerArea) {
        this.headerArea = headerArea;

        content = new Content();
    }


    public void adjustHeaderArea() {
        for (TextPart textPart : content.textParts()) {
            textPart.area().move(headerArea.left(), headerArea.top());
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
        MyStringBuffer sb = new MyStringBuffer();
        sb.tab(tabCount).append("header - {\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append("header - }\n");
        return sb.toString();
    }

}
