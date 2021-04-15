package kr.dogfoot.hwplib.drawer.drawinginfo.textbuffer;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.TextVerticalAlignment;


public class ControlContent extends ContentBuffer {
    private Area textArea;
    private TextVerticalAlignment verticalAlignment;
    private long top;
    private long bottom;

    public ControlContent(Area textArea, TextVerticalAlignment verticalAlignment) {
        this.textArea = textArea;
        this.verticalAlignment = verticalAlignment;
        top = -1;
        bottom = -1;
    }

    public void addTextPart(TextPart textPart) {
        super.addTextPart(textPart);
        if(textPart.hasNormalChar()) {
            top = (top == -1 || textPart.area().top() < top) ? textPart.area().top() : top;
            bottom = (bottom == -1 || textPart.area().bottom() > bottom) ? textPart.area().bottom() : bottom;
        }
    }

    public void adjustVerticalAlignment() {
        long offsetY = offsetY();
        if (offsetY != 0) {
            for (TextPart textPart : textParts) {
                textPart.area().moveY(offsetY);
            }
        }
    }

    private long offsetY() {
        long textHeight  = bottom - top;
        if (textHeight < textArea.height()) {
            switch (verticalAlignment) {
                case Top:
                    return 0;
                case Center:
                    return (textArea.height() - textHeight) / 2;
                case Bottom:
                    return textArea.height() - textHeight;
            }
        }
        return 0;
    }
}
