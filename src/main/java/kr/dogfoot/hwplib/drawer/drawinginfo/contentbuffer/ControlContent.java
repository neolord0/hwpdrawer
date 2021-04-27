package kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;


public class ControlContent extends ContentBuffer {
    private Area textArea;
    private long height;

    public ControlContent(Area textArea) {
        this.textArea = textArea;
        height = 0;
    }

    public void adjustVerticalAlignment(TextVerticalAlignment verticalAlignment) {
        long offsetY = offsetY(verticalAlignment);
        if (offsetY != 0) {
            for (TextPart textPart : textParts) {
                textPart.area().moveY(offsetY);
            }
        }
    }

    private long offsetY(TextVerticalAlignment verticalAlignment) {
        if (height < textArea.height()) {
            switch (verticalAlignment) {
                case Top:
                    return 0;
                case Center:
                    return (textArea.height() - height) / 2;
                case Bottom:
                    return textArea.height() - height;
            }
        }
        return 0;
    }

    public ControlContent adjustArea(Area textArea) {
        this.textArea = textArea;
        for (TextPart textPart : textParts) {
            textPart.area()
                    .moveX(textArea.left())
                    .moveY(textArea.top());
        }
        return this;
    }

    public long height() {
        return height;
    }

    public void height(long height) {
        this.height = height;
    }
}
