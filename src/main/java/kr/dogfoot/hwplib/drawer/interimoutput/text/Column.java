package kr.dogfoot.hwplib.drawer.interimoutput.text;

import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class Column {
    public final static Column[] Zero_Array = new Column[0];

    private final Area area;

    private final ArrayList<TextLine> textLines;
    private final TreeSet<ControlOutput> behindChildOutputs;
    private final TreeSet<ControlOutput> nonBehindChildOutputs;

    private int topLineIndexForHiding;
    private CharInfo nextChar;

    public Column(Area area) {
        this.area = area;
        textLines = new ArrayList<>();
        behindChildOutputs = new TreeSet<>();
        nonBehindChildOutputs = new TreeSet<>();

        topLineIndexForHiding = -1;
    }

    public void addTextLine(TextLine textLine) {
        textLine
                .column(this)
                .index(textLines.size());
        textLines.add(textLine);
    }

    public TextLine nextLine(TextLine textLine) {
        if (textLine.index() + 1 >= textLines.size()) {
            return null;
        }
        return textLines.get(textLine.index() + 1);
    }

    public TextLine firstLine() {
        if (textLines.isEmpty()) {
            return null;
        }
        return textLines.get(0);
    }

    public int textLineCount() {
        return textLines.size();
    }

    public TextLine[] textLines() {
        return textLines.toArray(TextLine.Zero_Array);
    }

    public void setLastLineInPara() {
        if (textLines.size() > 0) {
            textLines.get(textLines.size() - 1).lastInPara(true);
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

    public boolean checkRedrawingTextLine(Area area) {
        for (TextLine textLine : textLines()) {
            if (textLine.area().overlap(area)) {
                return true;
            }
        }
        return false;
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

    public TextLine hideTextLineIndex(int topLineIndex) {
        if (topLineIndex < textLineCount()) {
            topLineIndexForHiding = topLineIndex;
            return textLines.get(topLineIndex);
        }
        return null;
    }

    public void resetHideTextLineIndex() {
        topLineIndexForHiding = -1;
    }

    public int topLineIndexForHiding() {
        return topLineIndexForHiding;
    }

    public void topLineIndexForHiding(int topLineIndexForHiding) {
        this.topLineIndexForHiding = topLineIndexForHiding;
    }

    public TextLine deleteTextLineIndex(int topLineIndex) {
        if (topLineIndex < textLineCount()) {
            TextLine topLine = textLines.get(topLineIndex);

            ArrayList<TextLine> deletes = new ArrayList<>();
            for (int index = topLineIndex; index < textLines.size(); index++) {
                deletes.add(textLines.get(index));
            }

            for (TextLine textLine : deletes) {
                textLines.remove(textLine);
            }

            return topLine;
        }
        return null;
    }

    public TextLine[] paintingTextLines() {
        if (topLineIndexForHiding == -1) {
            return textLines();
        } else {
            TextLine[] arr = new TextLine[topLineIndexForHiding];
            for (int index = 0; index < topLineIndexForHiding; index++) {
                arr[index] = textLines.get(index);
            }
            return arr;
        }
    }

    public long calculateHeight() {
        if (textLines.size() == 0) {
            return -1;
        }

        if (topLineIndexForHiding == 0) {
            return 0;
        } else {
            long top;
            long bottom;

            top = textLines.get(0).area().top();

            if (topLineIndexForHiding == -1) {
                bottom = textLines.get(textLines.size() - 1).area().bottom();
            } else {
                bottom = textLines.get(topLineIndexForHiding - 1).area().bottom();
            }

            return bottom - top;
        }
    }


    public void clear() {
        textLines.clear();
        behindChildOutputs.clear();
        nonBehindChildOutputs.clear();
        topLineIndexForHiding = -1;
    }

    public Area area() {
        return area;
    }

    public void nextChar(CharInfo nextChar) {
        this.nextChar = nextChar;
    }

    public CharInfo nextChar() {
        return nextChar;
    }


    public String test(int tabCount) {
        return test(tabCount, false);
    }

    public String test(int tabCount, boolean hideLine) {
        MyStringBuilder sb = new MyStringBuilder();
        if (textLines.size() > 0) {
            sb.tab(tabCount).append("textLines - {\n");
            if (hideLine == false || topLineIndexForHiding == -1)  {
                for (TextLine line : textLines) {
                    sb.append(line.test(tabCount + 1)).append("\n");
                }
            } else {
                int count = textLines.size();
                for (int index = 0; index < topLineIndexForHiding; index++) {
                    TextLine line = textLines.get(index);
                    sb.append(line.test(tabCount + 1)).append("\n");
                }
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
