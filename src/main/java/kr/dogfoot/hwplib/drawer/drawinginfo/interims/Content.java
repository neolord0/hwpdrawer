package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextLine;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextPart;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class Content {
    private final ArrayList<TextLine> lines;
    private final TreeSet<ControlOutput> behindChildOutputs;
    private final TreeSet<ControlOutput> nonBehindChildOutputs;

    public Content() {
        lines = new ArrayList<>();
        behindChildOutputs = new TreeSet<>();
        nonBehindChildOutputs = new TreeSet<>();
    }

    public void addTextLine(TextLine line) {
        lines.add(line);
    }

    public TextLine[] textLines() {
        return lines.toArray(TextLine.Zero_Array);
    }

    public void setLastTextPartToLastLine() {
        if (lines.size() > 0) {
            lines.get(lines.size() - 1).lastLine(true);
        }
    }

    public void addChildOutput(ControlOutput childOutput) {
        if (childOutput.textFlowMethod() == 2/*뒤로*/) {
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
        if (lines.size() > 0) {
            sb.tab(tabCount).append("textLines - {\n");
            for (TextLine line : lines) {
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
}

