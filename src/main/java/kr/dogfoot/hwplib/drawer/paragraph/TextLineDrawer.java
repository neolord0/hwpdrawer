package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private Painter painter;
    private DrawingInfo info;

    private ArrayList<TextPart> parts;
    private TextPart currentTextPart;

    private long maxCharHeight;
    private long maxCharHeightOnlyNormalText;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    private long baseLine;
    private long charX;
    private long[] spaceAddings;
    private long[] charAddings;
    private CharShape drawingCharShape;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(Painter painter, DrawingInfo info) {
        this.painter = painter;
        this.info = info;

        parts = new ArrayList<>();
        currentTextPart = null;
        underLineDrawer = new UnderLineDrawer(painter);
        strikeLineDrawer = new StrikeLineDrawer(painter);
    }

    public TextLineDrawer initialize() {
        reset();
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = false;
        return this;
    }

    public TextLineDrawer reset() {
        parts.clear();
        currentTextPart = null;

        maxCharHeight = 0;
        maxCharHeightOnlyNormalText = 0;
        resetPart();

        baseLine = 0;
        charX = 0;
        spaceAddings = null;
        charAddings = null;
        drawingCharShape = null;

        return this;
    }

    public TextLineDrawer resetPart() {
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
        return this;
    }

    public void addNewTextPart(Area textPartArea) {
        TextPart textPart = new TextPart(new Area(textPartArea));
        parts.add(textPart);
        currentTextPart = textPart;
    }

    public boolean justNewLine() {
        return justNewLine;
    }

    public void justNewLine(boolean justNewLine) {
        this.justNewLine = justNewLine;
    }

    public boolean lastLine() {
        return currentTextPart.lastLine();
    }

    public void lastLine(boolean lastLine) {
        currentTextPart.lastLine(lastLine);
    }

    public void addChar(CharInfo charInfo) {
        maxCharHeight = (charInfo.height() > maxCharHeight) ? charInfo.height() : maxCharHeight;
        if (charInfo.type() == CharInfo.Type.Normal) {
            maxCharHeightOnlyNormalText = (charInfo.height() > maxCharHeightOnlyNormalText)
                    ? charInfo.height()
                    : maxCharHeightOnlyNormalText;
        }
        currentTextPart.addCharInfo(charInfo);
        if (charInfo.character().isSpace()) {
            spacesWidth += charInfo.widthAddingCharSpace();
        } else {
            wordsWidth += charInfo.widthAddingCharSpace();
        }
    }

    public boolean isOverRight(double width, boolean applyMinimumSpace) {
        return currentTextX(applyMinimumSpace) + width > currentTextPart.area().right();
    }

    private long currentTextX(boolean applyMinimumSpace) {
        if (applyMinimumSpace) {
            long minimumSpace = spacesWidth * (100 - info.paraShape().getProperty1().getMinimumSpace()) / 100;
            return wordsWidth + minimumSpace + currentTextPart.area().left();
        } else {
            return wordsWidth + spacesWidth + currentTextPart.area().left();
        }
    }

    public long maxCharHeight() {
        if (noNormalChar()) {
            return info.charShape().getBaseSize();
        } else {
            return maxCharHeight;
        }
    }

    public long lineHeight() {
        long lineGap = 0;
        long maxCharHeightOnlyNormalText2 = (noNormalChar())
                ? info.charShape().getBaseSize()
                : maxCharHeightOnlyNormalText;
        ParaShape paraShape = info.paraShape();
        switch (paraShape.getProperty1().getLineSpaceSort()) {
            case RatioForLetter:
                if (paraShape.getLineSpace() == paraShape.getLineSpace2()) {
                    lineGap = maxCharHeightOnlyNormalText2 * paraShape.getLineSpace() / 100 - maxCharHeightOnlyNormalText2;
                } else {
                    lineGap = Math.max(maxCharHeightOnlyNormalText, paraShape.getLineSpace2() / 2) - maxCharHeightOnlyNormalText2;
                }
                break;
            case FixedValue:
                lineGap = paraShape.getLineSpace() / 2 - maxCharHeightOnlyNormalText2;
                break;
            case OnlyMargin:
                lineGap = paraShape.getLineSpace() / 2;
                break;
        }
        return maxCharHeight() + lineGap;
    }

    public boolean noNormalChar() {
        return !currentTextPart.hasNormalChar();
    }

    public void setBestSpaceRate() {
        currentTextPart.spaceRate((double) (currentTextPart.area().width() - wordsWidth) / (double) spacesWidth);
    }

    public Area area() {
        return currentTextPart.area();
    }

    public TextLineDrawer area(Area textLineArea) {
        currentTextPart.area(new Area(textLineArea));
        return this;
    }

    public void draw() throws UnsupportedEncodingException {
        for (TextPart part : parts) {
            drawPart(part);
        }
    }

    private void drawPart(TextPart part) throws UnsupportedEncodingException {
        baseLine = part.area().top() + maxCharHeight;
        drawingCharShape = null;

        switch(info.paraShape().getProperty1().getAlignment()) {
            case Justify:
                justify(part);
                break;
            case Left:
                left(part);
                break;
            case Right:
                right(part);
                break;
            case Center:
                center(part);
                break;
            case Distribute:
                distribute(part);
                break;
            case Divide:
                divide(part);
                break;
        }
        drawUnder_StrikeLine(part);
    }

    private void justify(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        if (part.lastLine() == false) {
            if (part.spaceCountWithExceptingLastSpace() != 0) {
                spaceAddings = spaceAddings(part);
                charAddings = null;
            } else {
                spaceAddings = null;
                charAddings = charAddings(part);
            }
        } else {
            spaceAddings = null;
            charAddings = null;
        }
        drawInOrder(part);
    }

    private long[] spaceAddings(TextPart part) {
        long extra = part.area().width() - part.textWidthWithExceptingLastSpace();
        int spaceCount = part.spaceCountWithExceptingLastSpace();
        if (spaceCount == 0) {
            return null;
        }
        long extraBySpace = extra / spaceCount;
        long rest = extra % spaceCount;

        long[] spaceAddings = null;
        if (spaceCount > 0) {
            spaceAddings = new long[spaceCount];
            for (int index = 0; index < spaceCount; index++) {
                if (index <= rest) {
                    spaceAddings[index] = extraBySpace + 1;
                } else {
                    spaceAddings[index] = extraBySpace;
                }
            }
        }
        return spaceAddings;
    }

    private long[] charAddings(TextPart part) {
        long extra = part.area().width() - part.textWidthWithExceptingLastSpace();
        int charCount = part.charCountWithExceptingLastSpace() - 1;
        if (charCount <= 0) {
            return null;
        }
        long extraByChar = extra / charCount;
        long rest = extra % charCount;

        long[] charAddings = null;
        if (charCount > 0) {
            charAddings = new long[charCount];
            for (int index = 0; index < charCount; index++) {
                if (index <= rest) {
                    charAddings[index] = extraByChar + 1;
                } else {
                    charAddings[index] = extraByChar;
                }
            }
        }
        return charAddings;
    }

    private void left(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void right(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().right() - part.textWidthWithExceptingLastSpace();
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void center(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().left() + (part.area().width() - part.textWidthWithExceptingLastSpace()) / 2;
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void distribute(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = null;
        charAddings = charAddings(part);
        drawInOrder(part);
    }

    private void divide(TextPart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = spaceAddings(part);
        charAddings = null;
        drawInOrder(part);
    }

    private void drawInOrder(TextPart part) throws UnsupportedEncodingException {
        short oldRatio = 100;
        double stretchRate = 1;

        int spaceIndex = 0;
        int charIndex = 0;
        for (CharInfo charInfo : part.charInfos()) {
            if (drawingCharShape != charInfo.charShape()) {
                painter.setDrawingFont(charInfo.charShape());
                drawingCharShape = charInfo.charShape();
            }

            if (oldRatio != charInfo.charShape().getRatios().getHangul()) {
                oldRatio = charInfo.charShape().getRatios().getHangul();
                stretchRate = painter.setStretch(oldRatio);
            }

            charInfo.x(charX);
            if (charInfo.character().isSpace()) {
                charX += charInfo.widthAddingCharSpace() * part.spaceRate();
                if (spaceAddings != null && spaceIndex < spaceAddings.length) {
                    charX += spaceAddings[spaceIndex];
                    spaceIndex++;
                }
            } else {
                if (charInfo.type() == CharInfo.Type.Normal) {
                    painter.string(((NormalCharInfo) charInfo).normalCharacter().getCh(),
                            (long) (charInfo.x() / stretchRate),
                            getY(charInfo));
                } else if (charInfo.type() == CharInfo.Type.Control
                        && ((ControlCharInfo) charInfo).control() != null) {
                    Area area = new Area(charInfo.x(), part.area().bottom() - maxCharHeight,0,0)
                            .width((long) charInfo.width())
                            .height(charInfo.height())
                            .moveY(50);
                    painter.rectangle(area, false);
                }

                charX += charInfo.widthAddingCharSpace();
            }

            if (charAddings != null && charIndex < charAddings.length) {
                charX += charAddings[charIndex];
                charIndex++;
            }
        }
    }

    private long getY(CharInfo charInfo) {
        return (long) (baseLine
                - painter.textOffsetY(((NormalCharInfo) charInfo)))
                + charInfo.height() * charInfo.charShape().getCharOffsets().getHangul() / 100;

    }

    private void drawUnder_StrikeLine(TextPart part) throws UnsupportedEncodingException {
        underLineDrawer.initialize(baseLine, maxCharHeight);
        strikeLineDrawer.initialize(baseLine);

        int count = part.charInfos().size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = part.charInfos().get(index);

            underLineDrawer.draw(charInfo, (index == count - 1));
            strikeLineDrawer.draw(charInfo, (index == count - 1));
        }
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (TextPart textPart : parts) {
            for (CharInfo charInfo : textPart.charInfos()) {
                if (charInfo.type() == CharInfo.Type.Normal) {
                    NormalCharInfo normalCharInfo = (NormalCharInfo) charInfo;
                    try {
                        sb
                                .append(normalCharInfo.normalCharacter().getCh())
                                .append("(")
                                .append(charInfo.index())
                                .append(")");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;
                    if(controlCharInfo.control() == null) {
                        sb
                                .append(controlCharInfo.character().getCode())
                                .append("(")
                                .append(charInfo.index())
                                .append(")");

                    } else {
                        sb
                                .append(controlCharInfo.control().getType())
                                .append("(")
                                .append(charInfo.index())
                                .append(")");
                    }

                }
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }
}

