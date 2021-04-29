package kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ControlBookmark;

import java.util.ArrayList;
import java.util.TreeSet;

public abstract class OutputContent {
    protected TreeSet<ControlCharInfo> behindControls;
    protected TreeSet<ControlCharInfo> nonBehindControls;

    protected ArrayList<TextPart> textParts;
    protected ArrayList<ControlContent> childControlContents;

    public OutputContent() {
        behindControls = new TreeSet<>();
        nonBehindControls = new TreeSet<>();

        textParts = new ArrayList<>();
        childControlContents = new ArrayList<>();
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

    public void addControl(ControlCharInfo controlCharInfo) {
        if (controlCharInfo.textFlowMethod() == 2/*뒤로*/) {
            behindControls.add(controlCharInfo);
        } else {
            nonBehindControls.add(controlCharInfo);
        }
    }

    public TreeSet<ControlCharInfo> behindControls() {
        return behindControls;
    }

    public TreeSet<ControlCharInfo> nonBehindControls() {
        return nonBehindControls;
    }

    public void addChildControlContents(ControlContent childControlContent) {
        childControlContents.add(childControlContent);
    }

    public abstract Type type();

    public enum Type {
        Page,
        Control
    }
}
