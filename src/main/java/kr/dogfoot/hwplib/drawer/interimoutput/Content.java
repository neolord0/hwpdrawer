package kr.dogfoot.hwplib.drawer.interimoutput;

import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class Content {
    private final ArrayList<TextLine> textLines;
    private final TreeSet<ControlOutput> behindChildOutputs;
    private final TreeSet<ControlOutput> nonBehindChildOutputs;

    public Content() {
        textLines = new ArrayList<>();
        behindChildOutputs = new TreeSet<>();
        nonBehindChildOutputs = new TreeSet<>();
    }

    public void addTextLine(TextLine line) {
        textLines.add(line);
    }

    public TextLine[] textLines() {
        return textLines.toArray(TextLine.Zero_Array);
    }

    public void setLastTextPartToLastLine() {
        if (textLines.size() > 0) {
            textLines.get(textLines.size() - 1).lastLine(true);
        }
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (childOutput.textFlowMethod() == TextFlowMethod.BehindText) {
            behindChildOutputs.add(childOutput);
        } else {
            nonBehindChildOutputs.add(childOutput);
        }
    }

    public Set<ControlOutput> behindChildOutputs() {
        return behindChildOutputs;
    }

    public Set<ControlOutput> nonBehindChildOutputs() {
        return nonBehindChildOutputs;
    }

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        if (textLines.size() > 0) {
            sb.tab(tabCount).append("textLines - {\n");
            for (TextLine line : textLines) {
                sb.append(line.test(tabCount + 1)).append("\n");
            }
            sb.tab(tabCount).append("textLines - }\n");
        }

        if (behindChildOutputs.size() > 0) {
            sb.tab(tabCount).append("b-controls - {\n");
            for (ControlOutput controlOutput : behindChildOutputs) {
                sb.append(controlOutput.test(tabCount + 1));
            }
            sb.tab(tabCount).append("b-controls - }\n");
        }

        if (nonBehindChildOutputs.size() > 0) {
            sb.tab(tabCount).append("n-controls - {\n");
            for (ControlOutput controlOutput : nonBehindChildOutputs) {
                sb.append(controlOutput.test(tabCount + 1));
            }
            sb.tab(tabCount).append("n-controls - }\n");
        }

        return sb.toString();
    }

    public TextLine deleteRedrawingTextLine(Area area) {
        TextLine firstTextLine = null;
        boolean overlapped = false;
        ArrayList<TextLine> deletings = new ArrayList<>();
        for (TextLine textLine : textLines()) {
            if (overlapped == false && textLine.area().overlap(area)) {
                firstTextLine = textLine;
                overlapped = true;
            }
            if (overlapped == true) {
                deletings.add(textLine);
            }
        }

        for (TextLine deleting : deletings) {
            textLines.remove(deleting);
        }
        return firstTextLine;
    }
}

