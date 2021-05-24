package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;


public class TextLineDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private TextLine textLine;

    private long maxCharHeight;
    private long maxBaseSize;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    public TextLineDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public TextLineDrawer initialize(int paragraphIndex, Area area) {
        reset(paragraphIndex, area);
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = false;
        return this;
    }

    public TextLineDrawer reset(int paragraphIndex, Area area) {
        textLine = new TextLine(paragraphIndex, new Area(area));

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
            long minimumSpace = spacesWidth * (100 - input.paraShape().getProperty1().getMinimumSpace()) / 100;
            return wordsWidth + minimumSpace;
        } else {
            return wordsWidth + spacesWidth;
        }
    }

    public long maxCharHeight() {
        if (noDrawingCharacter()) {
            return input.charShape().getBaseSize();
        } else {
            return maxCharHeight;
        }
    }

    public long lineHeight() {
        long lineGap = 0;
        long maxBasSize2 = (noDrawingCharacter())
                ? input.charShape().getBaseSize()
                : maxBaseSize;
        ParaShape paraShape = input.paraShape();
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
                    .alignment(input.paraShape().getProperty1().getAlignment());
            output.addTextLine(textLine);
            return true;
        }
        return false;
    }
}

