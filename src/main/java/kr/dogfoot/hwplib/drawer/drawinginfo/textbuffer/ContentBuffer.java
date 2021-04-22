package kr.dogfoot.hwplib.drawer.drawinginfo.textbuffer;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;

import java.util.ArrayList;
import java.util.TreeSet;

public class ContentBuffer {
    protected ArrayList<TextPart> textParts;
    protected TreeSet<ControlCharInfo> behindControls;
    protected TreeSet<ControlCharInfo> notBehindControls;

    private long height;

    public ContentBuffer() {
        textParts = new ArrayList<>();
        behindControls = new TreeSet<>();
        notBehindControls = new TreeSet<>();
        height = 0;
    }

    public void addTextPart(TextPart textPart) {
        textParts.add(textPart);
    }

    public ArrayList<TextPart> textParts() {
        return textParts;
    }

    public void setLastTextPartToLastLine() {
        if (textParts.size() > 0) {
            textParts.get(textParts.size() - 1).lastLine(true);
        }
    }

    public void addBehindControl(ControlCharInfo controlCharInfo) {
        behindControls.add(controlCharInfo);
    }

    public TreeSet<ControlCharInfo> behindControls() {
        return behindControls;
    }

    public void addNotBehindControl(ControlCharInfo controlCharInfo) {
        notBehindControls.add(controlCharInfo);
    }

    public TreeSet<ControlCharInfo> notBehindControls() {
        return notBehindControls;
    }

    public long height() {
        return height;
    }

    public void height(long height) {
        this.height = height;
    }
}
