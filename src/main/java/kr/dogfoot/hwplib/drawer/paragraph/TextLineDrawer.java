package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;


public class TextLineDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CharInfo firstCharInfo;

    private TextLine textLine;

    private long lineHeight;
    private long maxCharHeight;
    private long maxBaseSize;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    public TextLineDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public TextLineDrawer initialize(Area area) {
        reset(area);
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = false;
        return this;
    }

    public TextLineDrawer reset(Area area) {
        textLine = new TextLine(input.paraIndex(), new Area(area));
        maxCharHeight = 0;
        maxBaseSize = 0;
        justNewLine = true;
        return this;
    }

    public boolean notInitialized() {
        return textLine == null;
    }

    public CharInfo firstCharInfo() {
        return firstCharInfo;
    }

    public void firstCharInfo(CharInfo firstCharInfo) {
        this.firstCharInfo = firstCharInfo;
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

    public Area textLineArea() {
        return textLine.area();
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
        if (noDrawingChar()) {
            return input.charShape().getBaseSize();
        } else {
            return maxCharHeight;
        }
    }

    public void setLineHeight() {
        long lineGap = 0;
        long maxBaseSize2 = (noDrawingChar())
                ? input.charShape().getBaseSize()
                : maxBaseSize;
        ParaShape paraShape = input.paraShape();
        switch (paraShape.getProperty1().getLineSpaceSort()) {
            case RatioForLetter:
                if (paraShape.getLineSpace() == paraShape.getLineSpace2()) {
                    lineGap = maxBaseSize2 * paraShape.getLineSpace() / 100 - maxBaseSize2;
                } else {
                    lineGap = Math.max(maxBaseSize2, paraShape.getLineSpace2() / 2) - maxBaseSize2;
                }
                break;
            case FixedValue:
                lineGap = paraShape.getLineSpace() / 2 - maxBaseSize2;
                break;
            case OnlyMargin:
                lineGap = paraShape.getLineSpace() / 2;
                break;
        }
        lineHeight = maxCharHeight() + lineGap;
    }

    public boolean noDrawingChar() {
        return !textLine.hasDrawingChar();
    }

    public void setBestSpaceRate() {
        textLine.currentTextPart().spaceRate((double) (textLine.currentTextPart().width() - wordsWidth) / (double) spacesWidth);
    }

    public void saveToOutput() {
        textLine.maxCharHeight(maxCharHeight)
                .alignment(input.paraShape().getProperty1().getAlignment());
        output.addTextLine(textLine);
    }

    public String test() {
        return textLine.test(0);
    }

    public long lineHeight() {
        return lineHeight;
    }

    public long lineGap() {
        return lineHeight - maxCharHeight();
    }


    public void setEmptyLineHeight() {
        textLineArea().bottom(textLineArea().bottom() + input.charShape().getBaseSize());
    }

    public void addControlCharInfo(ControlCharInfo controlCharInfo) {
        textLine.addControlCharInfo(controlCharInfo);
    }
}

