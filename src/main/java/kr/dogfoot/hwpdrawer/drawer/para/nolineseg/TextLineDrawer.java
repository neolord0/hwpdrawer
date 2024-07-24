package kr.dogfoot.hwpdrawer.drawer.para.nolineseg;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfo;
import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.output.text.TextLine;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.CharPosition;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import java.util.ArrayList;


public class TextLineDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private CharInfo firstCharInfo;
    private CharInfo firstDrawingCharInfo;

    private TextLine textLine;

    private long lineHeight;
    private long maxCharHeight;
    private long maxBaseSize;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;
    private boolean hasDividedTable;
    private ArrayList<ControlOutput> controlOutputs;

    public TextLineDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
        controlOutputs = new ArrayList<>();
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
        hasDividedTable = false;

        controlOutputs.clear();
        return this;
    }

    public boolean notInitialized() {
        return textLine == null;
    }

    public int paraIndex() {
        return textLine.paraIndex();
    }

    public void firstCharInfo(CharInfo firstCharInfo) {
        this.firstCharInfo = firstCharInfo;
    }

    public CharPosition firstCharPosition() {
         if (firstCharInfo != null) {
            return firstCharInfo.position();
        } else {
            return new CharPosition(paraIndex(), 0, 0);
        }
    }

    public void firstDrawingCharInfo(CharInfo firstDrawingCharInfo) {
        this.firstDrawingCharInfo = firstDrawingCharInfo;
    }

    public CharPosition firstDrawingCharPosition() {
        if (firstDrawingCharInfo != null) {
            return firstDrawingCharInfo.position();
        } else {
            return new CharPosition(paraIndex(), 0, 0);
        }
    }

    public void addNewTextPart(long startX, long width) {
        textLine.addNewTextPart(startX, width);

        wordsWidth = 0;
        spacesWidth = 0;
    }

    public TextLineDrawer clearTextLine() {
        textLine.clear();
        controlOutputs.clear();
        return this;
    }

    public Area textLineArea() {
        return textLine.area();
    }

    public void textLineArea(Area area) {
        textLine.area().set(area);
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

    public boolean hasDrawingChar() {
        return textLine.hasDrawingChar();
    }

    public void setBestSpaceRate() {
        textLine.currentTextPart().spaceRate((double) (textLine.currentTextPart().width() - wordsWidth) / (double) spacesWidth);
    }

    public boolean isOverPageHeight() {
        if (textLineArea().top() + lineHeight > input.pageInfo().bodyArea().bottom()) {
            return true;
        }
        return false;
    }

    public boolean hasDividedTable() {
        return hasDividedTable;
    }

    public void hasDividedTable(boolean hasDividedTable) {
        this.hasDividedTable = hasDividedTable;
    }

    public void saveToOutput() {
        textLine.maxCharHeight(maxCharHeight)
                .alignment(input.paraShape().getProperty1().getAlignment());
        output.addTextLine(textLine);
    }

    public long lineHeight() {
        return lineHeight;
    }

    public long lineGap() {
        return lineHeight - maxCharHeight();
    }


    public void addControlCharInfo(CharInfoControl controlCharInfo) {
        textLine.addControlCharInfo(controlCharInfo);
    }

    public String test() {
        return textLine.test(0);
    }

    public int controlOutputCount() {
        return controlOutputs.size();
    }

    public void addControlOutput(ControlOutput controlOutput) {
        controlOutputs.add(controlOutput);
    }

    public ControlOutput[] controlOutputs() {
        return controlOutputs.toArray(ControlOutput.Zero_Array);
    }
}
