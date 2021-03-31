package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private HWPDrawer drawer;

    private ArrayList<CharInfo> drawingCharInfos;
    private boolean lastLine;
    private int spaceCount;

    private Area textLineArea;
    private long maxCharHeight;
    private long baseLine;
    private long charX;
    private long[] spaceAddings;
    private long[] charAddings;

    private double spaceRate;
    private CharShape drawingCharShape;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(HWPDrawer drawer) {
        this.drawer = drawer;

        drawingCharInfos = new ArrayList<>();
        underLineDrawer = new UnderLineDrawer(drawer);
        strikeLineDrawer = new StrikeLineDrawer(drawer);
    }

    public void initialize() {
        drawingCharInfos.clear();
        lastLine = false;
        spaceCount = 0;
        spaceAddings = null;
        charAddings = null;

        maxCharHeight = 0;
        baseLine = 0;
        charX = 0;
        spaceRate = 1.0;
        drawingCharShape = null;
    }

    public void lastLine(boolean lastLine) {
        this.lastLine = lastLine;
    }

    public void addChar(CharInfo charInfo) {
        maxCharHeight = (charInfo.charShape().getBaseSize() > maxCharHeight) ? charInfo.charShape().getBaseSize() : maxCharHeight;
        drawingCharInfos.add(charInfo);
        if (charInfo.character().isSpace()) {
            spaceCount++;
        }
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public void draw(Area textLineArea, DrawingInfo info) throws UnsupportedEncodingException {
        this.textLineArea = textLineArea;
        baseLine = textLineArea.top() + maxCharHeight;
        drawingCharShape = null;

        switch(info.paraShape().getProperty1().getAlignment()) {
            case Justify:
                justify();
                break;
            case Left:
                left();
                break;
            case Right:
                right();
                break;
            case Center:
                center();
                break;
            case Distribute:
                distribute();
                break;
            case Divide:
                divide();
                break;
        }
        drawUnder_StrikeLine();
    }

    private void justify() throws UnsupportedEncodingException {
        charX = textLineArea.left();
        if (lastLine == false) {
            if (spaceCountWithExceptingLastSpace() != 0) {
                spaceAddings = spaceAddings();
                charAddings = null;
            } else {
                spaceAddings = null;
                charAddings = charAddings();
            }
        } else {
            spaceAddings = null;
            charAddings = null;
        }
        drawInOrder();
    }

    private long[] spaceAddings() {
        long extra = textLineArea.width() - textWidthWithExceptingLastSpace();
        int spaceCount = spaceCountWithExceptingLastSpace();
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

    private long textWidthWithExceptingLastSpace() {
        long width = 0;
        int count = drawingCharInfos.size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = drawingCharInfos.get(index);
            if (charInfo.character().isSpace()) {
                if (index < count - 1) {
                    width += charInfo.widthAddingCharSpace() * spaceRate;
                }
            } else {
                width += charInfo.widthAddingCharSpace();
            }
        }
        return width;
    }

    private int spaceCountWithExceptingLastSpace() {
        if (drawingCharInfos.size() > 0) {
            CharInfo lastCharInfo = drawingCharInfos.get(drawingCharInfos.size() - 1);
            if (lastCharInfo.character().isSpace()) {
                return spaceCount - 1;
            }
        }
        return spaceCount;
    }

    private void left() throws UnsupportedEncodingException {
        charX = textLineArea.left();
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }

    private void right() throws UnsupportedEncodingException {
        charX = textLineArea.right() - textWidthWithExceptingLastSpace();
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }


    private void center() throws UnsupportedEncodingException {
        charX = textLineArea.left() + (textLineArea.width() - textWidthWithExceptingLastSpace()) / 2;
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }

    private void distribute() throws UnsupportedEncodingException {
        charX = textLineArea.left();
        spaceAddings = null;
        charAddings = charAddings();
        drawInOrder();
    }

    private long[] charAddings() {
        long extra = textLineArea.width() - textWidthWithExceptingLastSpace();
        int charCount = charCountWithExceptingLastSpace() - 1;
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

    private int charCountWithExceptingLastSpace() {
        int textCount = drawingCharInfos.size();
        if (textCount > 0) {
            CharInfo lastCharInfo = drawingCharInfos.get(textCount - 1);
            if (lastCharInfo.character().isSpace()) {
                return textCount - 1;
            }
        }
        return textCount;
    }


    private void divide() throws UnsupportedEncodingException {
        charX = textLineArea.left();
        spaceAddings = spaceAddings();
        charAddings = null;
        drawInOrder();
    }

    private void drawInOrder() throws UnsupportedEncodingException {
        short oldRatio = 100;
        double stretchRate = 1;

        int spaceIndex = 0;
        int charIndex = 0;
        for (CharInfo charInfo : drawingCharInfos) {
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
                charX += charInfo.widthAddingCharSpace() * spaceRate;
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

    private void drawUnder_StrikeLine() throws UnsupportedEncodingException {
        underLineDrawer.initialize(baseLine, maxCharHeight);
        strikeLineDrawer.initialize(baseLine);

        int count = drawingCharInfos.size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = drawingCharInfos.get(index);

            underLineDrawer.draw(charInfo, (index == count - 1));
            strikeLineDrawer.draw(charInfo, (index == count - 1));
        }
    }

    public boolean noChar() {
        return drawingCharInfos.size() == 0;
    }

    public String text() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (CharInfo charInfo : drawingCharInfos) {
            sb.append(charInfo.character().getCh());
        }
        return sb.toString();
    }

    public void spaceRate(double spaceRate) {
        this.spaceRate = spaceRate;
    }

    public CharInfo[] charInfos() {
        return drawingCharInfos.toArray(CharInfo.Zero_Array);
    }
}
