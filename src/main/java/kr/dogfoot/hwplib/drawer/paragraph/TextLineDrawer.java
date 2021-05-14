package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;


public class TextLineDrawer {
    private final DrawingInfo info;

    private TextLine textLine;

    private long maxCharHeight;
    private long maxBaseSize;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    public TextLineDrawer(DrawingInfo info) {
        this.info = info;
    }

    public TextLineDrawer initialize(Area area) {
        reset(area);
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = false;
        return this;
    }

    public TextLineDrawer reset(Area area) {
        textLine = new TextLine(new Area(area));

        maxCharHeight = 0;
        maxBaseSize = 0;
        justNewLine = true;

        return this;
    }

    public void addNewTextPart(long startX, long width) {
        textLine.addNewTextPart(startX, width);

        wordsWidth = 0;
        spacesWidth = 0;
    }

    public TextLineDrawer clearTextLine() {
        textLine.clear();
        return this;
    }

    public void textLineArea(Area area) {
        textLine.area(area);
    }

    public boolean justNewLine() {
        return justNewLine;
    }

    public void justNewLine(boolean justNewLine) {
        this.justNewLine = justNewLine;
    }

    public void addChar(CharInfo charInfo) {
        maxCharHeight = Math.max(charInfo.height(), maxCharHeight);
        maxBaseSize = Math.max(charInfo.charShape().getBaseSize(), maxBaseSize);

        textLine.currentTextPart().addCharInfo(charInfo);
        if (charInfo.character().isSpace()) {
            spacesWidth += charInfo.widthAddingCharSpace();
        } else {
            wordsWidth += charInfo.widthAddingCharSpace();
        }
    }

    public boolean isOverWidth(double width, boolean applyMinimumSpace) {
        return currentTextX(applyMinimumSpace) + width > textLine.currentTextPart().width();
    }

    private long currentTextX(boolean applyMinimumSpace) {
        if (applyMinimumSpace) {
            long minimumSpace = spacesWidth * (100 - info.paraShape().getProperty1().getMinimumSpace()) / 100;
            return wordsWidth + minimumSpace;
        } else {
            return wordsWidth + spacesWidth;
        }
    }

    public long maxCharHeight() {
        if (noDrawingCharacter()) {
            return info.charShape().getBaseSize();
        } else {
            return maxCharHeight;
        }
    }

    public long lineHeight() {
        long lineGap = 0;
        long maxBasSize2 = (noDrawingCharacter())
                ? info.charShape().getBaseSize()
                : maxBaseSize;
        ParaShape paraShape = info.paraShape();
        switch (paraShape.getProperty1().getLineSpaceSort()) {
            case RatioForLetter:
                if (paraShape.getLineSpace() == paraShape.getLineSpace2()) {
                    lineGap = maxBasSize2 * paraShape.getLineSpace() / 100 - maxBasSize2;
                } else {
                    lineGap = Math.max(maxBasSize2, paraShape.getLineSpace2() / 2) - maxBasSize2;
                }
                break;
            case FixedValue:
                lineGap = paraShape.getLineSpace() / 2 - maxBasSize2;
                break;
            case OnlyMargin:
                lineGap = paraShape.getLineSpace() / 2;
                break;
        }
        return maxCharHeight() + lineGap;
    }

    public boolean noDrawingCharacter() {
        return !textLine.hasDrawingCharacter();
    }

    public void setBestSpaceRate() {
        textLine.currentTextPart().spaceRate((double) (textLine.currentTextPart().width() - wordsWidth) / (double) spacesWidth);
    }

    public boolean saveToOutput() {
        if (textLine.hasDrawingCharacter()) {
            textLine.maxCharHeight(maxCharHeight)
                    .alignment(info.paraShape().getProperty1().getAlignment());
            info.output().addTextLine(textLine);
            return true;
        }
        return false;
    }
}

