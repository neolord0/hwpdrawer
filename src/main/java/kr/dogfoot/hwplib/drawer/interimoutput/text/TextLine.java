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

    private Column column;
    private int index;

    private int paraIndex;
    private Area area;

    private ArrayList<TextPart> parts;
    private TextPart currentTextPart;

    private Alignment alignment;
    private long maxCharHeight;
    private boolean lastInPara;
    private boolean hasDrawingChar;

    public TextLine(int paraIndex, Area area) {
        this.paraIndex = paraIndex;
        this.area = area;

        parts = new ArrayList<>();
        currentTextPart = null;

        alignment = Alignment.Justify;
        maxCharHeight = -1;
        lastInPara = false;
        hasDrawingChar = false;
    }

    public void clear() {
        parts.clear();
        currentTextPart = null;
    }

    public int paraIndex() {
        return paraIndex;
    }

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

    public Column column() {
        return column;
    }

    public TextLine column(Column column) {
        this.column = column;
        return this;
    }

    public int index() {
        return index;
    }

    public void index(int index) {
        this.index = index;
    }

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

    public boolean lastInPara() {
        return lastInPara;
    }

    public TextLine lastInPara(boolean lastLine) {
        this.lastInPara = lastLine;
        return this;
    }

    public boolean hasDrawingChar() {
        return hasDrawingChar;
    }

    public TextLine hasDrawingChar(boolean hasDrawingCharacter) {
        this.hasDrawingChar = hasDrawingCharacter;
        return this;
    }

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount);
        sb.append(area).append("-");
        if (firstChar() != null) {
            sb.append(String.valueOf(firstChar().paraIndex())).append(":").append(String.valueOf(firstChar().index())) .append(" = ");
        } else {
            sb.append("-:- = ");
        }
        if (parts.size() > 0) {
            for (TextPart part : parts) {
                sb.append(part.test(0)).append(", ");
            }
        }
        return sb.toString();
    }
}

