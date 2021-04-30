package kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;

import java.util.ArrayList;
import java.util.TreeSet;

public abstract class OutputContent {
    protected ArrayList<TextPart> textParts;
    protected TreeSet<ControlContent> behindChildContents;
    protected TreeSet<ControlContent> nonBehindChildContents;

    public OutputContent() {
        textParts = new ArrayList<>();
        behindChildContents = new TreeSet<>();
        nonBehindChildContents = new TreeSet<>();
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

    public void addChildContent(ControlContent childContent) {
        if (childContent.textFlowMethod() == 2/*뒤로*/) {
            behindChildContents.add(childContent);
        } else {
            nonBehindChildContents.add(childContent);
        }
    }

    public TreeSet<ControlContent> behindChildContents() {
        return behindChildContents;
    }

    public TreeSet<ControlContent> nonBehindChildContents() {
        return nonBehindChildContents;
    }

    public abstract Type type();

    public String test(int tabCount) {
        StringBuffer sb = new StringBuffer();
        tab(tabCount, sb);
        sb.append("{ - textPart\n");
        for (TextPart part : textParts) {
            tab(tabCount + 1, sb);
            sb.append(part.test()).append("\n");
        }
        tab(tabCount, sb);
        sb.append("} - textPart\n");

        for (ControlContent controlContent : behindChildContents) {
            tab(tabCount, sb);
            sb.append("{ - b-control ").append(controlContent.controlArea()).append("\n");
            sb.append(controlContent.test(tabCount + 1));
            tab(tabCount, sb);
            sb.append("} - b-control\n");
        }
        for (ControlContent controlContent : nonBehindChildContents) {
            tab(tabCount, sb);
            sb.append("{ - n-control ").append(controlContent.controlArea()).append("\n");
            sb.append(controlContent.test(tabCount + 1));
            tab(tabCount, sb);
            sb.append("} - n-control\n");
        }

        return sb.toString();
    }

    protected void tab(int count, StringBuffer sb) {
        for (int index = 0; index < count; index++) {
            sb.append('\t');
        }
    }

    public enum Type {
        Page,
        Gso,
        Table
    }
}
