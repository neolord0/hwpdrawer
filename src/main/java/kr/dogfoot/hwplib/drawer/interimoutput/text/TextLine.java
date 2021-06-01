package kr.dogfoot.hwplib.drawer.interimoutput.text;

import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.docinfo.parashape.Alignment;
import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class TextLine implements Iterable<TextPart> {
    public static final TextLine[] Zero_Array = new TextLine[0];

    private int paraIndex;
    private Area area;

    private ArrayList<TextPart> parts;
    private TextPart currentTextPart;

    private Alignment alignment;
    private long maxCharHeight;
    private boolean lastLine;
    private boolean hasDrawingCharacter;

    private final TreeSet<ControlOutput> behindChildOutputs;
    private final TreeSet<ControlOutput> nonBehindChildOutputs;

    public TextLine(int paraIndex, Area area) {
        this.paraIndex = paraIndex;
        this.area = area;

        parts = new ArrayList<>();
        currentTextPart = null;

        alignment = Alignment.Justify;
        maxCharHeight = -1;
        lastLine = false;
        hasDrawingCharacter = false;

        behindChildOutputs = new TreeSet<>();
        nonBehindChildOutputs = new TreeSet<>();
    }

    public int paraIndex() {
        return paraIndex;
    }

    /*
    public int firstCharIndex() {
        CharInfo firstCharInfo = firstChar();
        if (firstCharInfo != null) {
            return firstCharInfo.index();
        }
        return -1;
    }

     */

    public CharInfo firstChar() {
        if (parts.size() > 0) {
            for (TextPart part : parts) {
                if (part.charInfos().size() > 0) {
                    return part.charInfos().get(0);
                }
            }
        }
        return null;
    }

    /*
    public int firstCharPosition() {
        CharInfo firstCharInfo = firstChar();
        if (firstCharInfo != null) {
            return firstCharInfo.position();
        }
        return -1;
    }

     */

/*
    public int firstCharSize() {
        CharInfo firstCharInfo = firstChar();
        if (firstCharInfo != null) {
            return firstCharInfo.character().getCharSize();
        }
        return -1;
    }


 */

    public Area area() {
        return area;
    }

    public void area(Area area) {
        this.area = area;
    }

    public void addNewTextPart(long startX, long width) {
        TextPart textPart = new TextPart(this, startX, width);

        parts.add(textPart);
        currentTextPart = textPart;
    }

    public TextPart currentTextPart() {
        return currentTextPart;
    }

    @Override
    public Iterator<TextPart> iterator() {
        return parts.iterator();
    }

    public Alignment alignment() {
        return alignment;
    }

    public TextLine alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public TextLine maxCharHeight(long maxCharHeight) {
        this.maxCharHeight = maxCharHeight;
        return this;
    }

    public boolean lastLine() {
        return lastLine;
    }

    public TextLine lastLine(boolean lastLine) {
        this.lastLine = lastLine;
        return this;
    }

    public boolean hasDrawingCharacter() {
        return hasDrawingCharacter;
    }

    public TextLine hasDrawingCharacter(boolean hasDrawingCharacter) {
        this.hasDrawingCharacter = hasDrawingCharacter;
        return this;
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
        sb.append(String.valueOf(firstChar().index())).append(" : ");
        if (parts.size() > 0) {
            for (TextPart part : parts) {
                sb.append(part.test(tabCount)).append(", ");
            }
        }
        return sb.toString();
    }

    public void clear() {
        parts.clear();
        currentTextPart = null;
    }
}

