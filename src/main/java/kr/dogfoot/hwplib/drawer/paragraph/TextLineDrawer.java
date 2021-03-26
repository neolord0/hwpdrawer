package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.DrawingInfo;
import kr.dogfoot.hwplib.drawer.StrikeLineDrawer;
import kr.dogfoot.hwplib.drawer.UnderLineDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private DrawingInfo info;

    private ArrayList<CharDrawInfo> charDrawInfos;
    private boolean lastLine;
    private int spaceCount;

    private long maxCharHeight;
    private long baseLine;
    private long charX;
    private long[] spaceAddings;
    private long[] charAddings;

    private double spaceRate;
    private CharShape drawingCharShape;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(DrawingInfo info) {
        this.info = info;

        charDrawInfos = new ArrayList<>();
        underLineDrawer = new UnderLineDrawer(info);
        strikeLineDrawer = new StrikeLineDrawer(info);
    }

    public void initialize() {
        charDrawInfos.clear();
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

    public void addChar(HWPCharNormal ch, double width, CharShape charShape) {
        maxCharHeight = (charShape.getBaseSize() > maxCharHeight) ? charShape.getBaseSize() : maxCharHeight;
        charDrawInfos.add(new CharDrawInfo(ch, width, charShape));
        if (ch.isSpace()) {
            spaceCount++;
        }
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public void draw(long y) throws UnsupportedEncodingException {
        baseLine = y + maxCharHeight;
        drawingCharShape = null;
        switch(info.currentParaShape().getProperty1().getAlignment()) {
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
        charX = info.paragraphArea().left();
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
        long extra = info.paragraphArea().width() - textWidthWithExceptingLastSpace();
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
        int count = charDrawInfos.size();
        for (int index = 0; index < count; index++) {
            CharDrawInfo cdi = charDrawInfos.get(index);
            if (cdi.ch.isSpace()) {
                if (index < count - 1) {
                    width += (cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100)) * spaceRate;
                }
            } else {
                width += cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100);
            }
        }
        return width;
    }

    private int spaceCountWithExceptingLastSpace() {
        if (charDrawInfos.size() > 0) {
            CharDrawInfo lastCDI = charDrawInfos.get(charDrawInfos.size() - 1);
            if (lastCDI.ch.isSpace()) {
                return spaceCount - 1;
            }
        }
        return spaceCount;
    }

    private void left() throws UnsupportedEncodingException {
        charX = info.paragraphArea().left();
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }

    private void right() throws UnsupportedEncodingException {
        charX = info.paragraphArea().right() - textWidthWithExceptingLastSpace();
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }


    private void center() throws UnsupportedEncodingException {
        charX = info.paragraphArea().left() + (info.paragraphArea().width() - textWidthWithExceptingLastSpace()) / 2;
        spaceAddings = null;
        charAddings = null;
        drawInOrder();
    }

    private void distribute() throws UnsupportedEncodingException {
        charX = info.paragraphArea().left();
        spaceAddings = null;
        charAddings = charAddings();
        drawInOrder();
    }

    private long[] charAddings() {
        long extra = info.paragraphArea().width() - textWidthWithExceptingLastSpace();
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
        int textCount = charDrawInfos.size();
        if (textCount > 0) {
            CharDrawInfo lastCDI = charDrawInfos.get(textCount - 1);
            if (lastCDI.ch.isSpace()) {
                return textCount - 1;
            }
        }
        return textCount;
    }


    private void divide() throws UnsupportedEncodingException {
        charX = info.paragraphArea().left();
        spaceAddings = spaceAddings();
        charAddings = null;
        drawInOrder();
    }

    private void drawInOrder() throws UnsupportedEncodingException {
        short oldRatio = 100;
        double stretchRate = 1;

        int spaceIndex = 0;
        int charIndex = 0;
        for (CharDrawInfo cdi : charDrawInfos) {
            if (drawingCharShape != cdi.charShape) {
                info.painter().setDrawingFont(cdi.charShape);
                drawingCharShape = cdi.charShape;
            }

            if (oldRatio != cdi.charShape.getRatios().getHangul()) {
                oldRatio = cdi.charShape.getRatios().getHangul();
                stretchRate = info.painter().setStretch(oldRatio);
            }

            cdi.x(charX);
            if (cdi.ch.isSpace()) {
                charX += (cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100)) * spaceRate;
                if (spaceAddings != null && spaceIndex < spaceAddings.length) {
                    charX += spaceAddings[spaceIndex];
                    spaceIndex++;
                }
            } else {
                info.painter().string(cdi.ch.getCh(),
                        (long) (cdi.x / stretchRate),
                        getY(cdi));
                charX += cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100);
            }

            if (charAddings != null && charIndex < charAddings.length) {
                charX += charAddings[charIndex];
                charIndex++;
            }
        }
    }

    private long getY(CharDrawInfo cdi) {
        return baseLine + cdi.charShape.getBaseSize() * cdi.charShape.getCharOffsets().getHangul() / 100;
    }

    private void drawUnder_StrikeLine() throws UnsupportedEncodingException {
        underLineDrawer.initialize(baseLine, maxCharHeight);
        strikeLineDrawer.initialize(baseLine);

        int count = charDrawInfos.size();
        for (int index = 0; index < count; index++) {
            CharDrawInfo cdi = charDrawInfos.get(index);

            underLineDrawer.draw(cdi, (index == count - 1));
            strikeLineDrawer.draw(cdi, (index == count - 1));
        }
    }

    public boolean noChar() {
        return charDrawInfos.size() == 0;
    }

    public String text() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        for (CharDrawInfo cdi : charDrawInfos) {
            sb.append(cdi.ch.getCh());
        }
        return sb.toString();
    }

    public void spaceRate(double spaceRate) {
        this.spaceRate = spaceRate;
    }


    public static class CharDrawInfo {
        public HWPCharNormal ch;
        public double width;
        public CharShape charShape;
        public long x;

        public CharDrawInfo(HWPCharNormal ch, double width, CharShape charShape) {
            this.ch = ch;
            this.width = width;
            this.charShape = charShape;
        }

        public void x(long x) {
            this.x = x;
        }
    }
}
