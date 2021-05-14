package kr.dogfoot.hwplib.drawer.drawinginfo.interims.text;

import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.docinfo.parashape.Alignment;
import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;
import java.util.Iterator;

public class TextLine implements Iterable<TextPart> {
    public static final TextLine[] Zero_Array = new TextLine[0];

    private Area area;

    private ArrayList<TextPart> parts;
    private TextPart currentTextPart;

    private Alignment alignment;
    private long maxCharHeight;
    private boolean lastLine;
    private boolean hasDrawingCharacter;

    public TextLine(Area area) {
        this.area = area;

        parts = new ArrayList<>();
        currentTextPart = null;

        alignment = Alignment.Justify;
        maxCharHeight = -1;
        lastLine = false;
        hasDrawingCharacter = false;
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

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
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

