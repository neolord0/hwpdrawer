package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private HWPDrawer drawer;

    private ArrayList<TextLinePart> parts;
    private TextLinePart currentPart;

    private long maxCharHeight;
    private long baseLine;
    private long charX;
    private long[] spaceAddings;
    private long[] charAddings;

    private CharShape drawingCharShape;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(HWPDrawer drawer) {
        this.drawer = drawer;

        parts = new ArrayList<>();
        currentPart = null;
        underLineDrawer = new UnderLineDrawer(drawer);
        strikeLineDrawer = new StrikeLineDrawer(drawer);
    }

    public TextLineDrawer initialize() {
        parts.clear();
        currentPart = null;

        spaceAddings = null;
        charAddings = null;

        maxCharHeight = 0;
        baseLine = 0;
        charX = 0;

        drawingCharShape = null;
        return this;
    }

    public void addNewPart(Area textPartArea) {
        TextLinePart textLinePart = new TextLinePart(new Area(textPartArea));
        parts.add(textLinePart);
        currentPart = textLinePart;
    }

    public CharInfo lastChar() {
        return currentPart.lastChar();
    }

    public boolean lastLine() {
        return currentPart.lastLine();
    }

    public void lastLine(boolean lastLine) {
        currentPart.lastLine(lastLine);
    }

    public void addChar(CharInfo charInfo) {
        maxCharHeight = (charInfo.charShape().getBaseSize() > maxCharHeight) ? charInfo.charShape().getBaseSize() : maxCharHeight;
        currentPart.addCharInfo(charInfo);
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public boolean noChar() {
        return currentPart.charInfos().isEmpty();
    }

    public void spaceRate(double spaceRate) {
        currentPart.spaceRate(spaceRate);
    }

    public Area area() {
        return currentPart.area();
    }

    public TextLineDrawer area(Area textLineArea) {
        currentPart.area(textLineArea);
        return this;
    }

    public void draw(DrawingInfo info) throws UnsupportedEncodingException {
        for (TextLinePart part : parts) {
            drawPart(part, info);
        }
    }

    private void drawPart(TextLinePart part, DrawingInfo info) throws UnsupportedEncodingException {
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

    private void justify(TextLinePart part) throws UnsupportedEncodingException {
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

    private long[] spaceAddings(TextLinePart part) {
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

    private long[] charAddings(TextLinePart part) {
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

    private void left(TextLinePart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void right(TextLinePart part) throws UnsupportedEncodingException {
        charX = part.area().right() - part.textWidthWithExceptingLastSpace();
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void center(TextLinePart part) throws UnsupportedEncodingException {
        charX = part.area().left() + (part.area().width() - part.textWidthWithExceptingLastSpace()) / 2;
        spaceAddings = null;
        charAddings = null;
        drawInOrder(part);
    }

    private void distribute(TextLinePart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = null;
        charAddings = charAddings(part);
        drawInOrder(part);
    }

    private void divide(TextLinePart part) throws UnsupportedEncodingException {
        charX = part.area().left();
        spaceAddings = spaceAddings(part);
        charAddings = null;
        drawInOrder(part);
    }

    private void drawInOrder(TextLinePart part) throws UnsupportedEncodingException {
        short oldRatio = 100;
        double stretchRate = 1;

        int spaceIndex = 0;
        int charIndex = 0;
        for (CharInfo charInfo : part.charInfos()) {
            if (drawingCharShape != charInfo.charShape()) {
                drawer.painter().setDrawingFont(charInfo.charShape());
                drawingCharShape = charInfo.charShape();
            }

            if (oldRatio != charInfo.charShape().getRatios().getHangul()) {
                oldRatio = charInfo.charShape().getRatios().getHangul();
                stretchRate = drawer.painter().setStretch(oldRatio);
            }

            charInfo.x(charX);
            if (charInfo.character().isSpace()) {
                charX += charInfo.widthAddingCharSpace() * part.spaceRate();
                if (spaceAddings != null && spaceIndex < spaceAddings.length) {
                    charX += spaceAddings[spaceIndex];
                    spaceIndex++;
                }
            } else {
                drawer.painter().string(charInfo.character().getCh(),
                        (long) (charInfo.x() / stretchRate),
                        getY(charInfo));
                charX += charInfo.widthAddingCharSpace();
            }

            if (charAddings != null && charIndex < charAddings.length) {
                charX += charAddings[charIndex];
                charIndex++;
            }
        }
    }

    private long getY(CharInfo charInfo) {
        return baseLine + charInfo.charShape().getBaseSize() * charInfo.charShape().getCharOffsets().getHangul() / 100;
    }

    private void drawUnder_StrikeLine(TextLinePart part) throws UnsupportedEncodingException {
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
        for (CharInfo charInfo : currentPart.charInfos()) {
            try {
                sb
                        .append(charInfo.character().getCh())
                        .append("(")
                        .append(charInfo.index())
                        .append(")");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}

