package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class Content {
    private final ArrayList<TextPart> textParts;
    private final TreeSet<ControlOutput> behindChildOutputs;
    private final TreeSet<ControlOutput> nonBehindChildOutputs;

    public Content() {
        textParts = new ArrayList<>();
        behindChildOutputs = new TreeSet<>();
        nonBehindChildOutputs = new TreeSet<>();
    }

    public void addTextPart(TextPart textPart) {
        textParts.add(textPart);
    }

    public TextPart[] textParts() {
        return textParts.toArray(TextPart.Zero_Array);
    }

    public void setLastTextPartToLastLine() {
        if (textParts.size() > 0) {
            textParts.get(textParts.size() - 1).lastLine(true);
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
        if (textParts.size() > 0) {
            sb.tab(tabCount).append("textParts - {\n");
            for (TextPart part : textParts) {
                sb.append(part.test(tabCount + 1));
            }
            sb.tab(tabCount).append("textParts - }\n");
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
