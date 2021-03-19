package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.lineseg.LineSegItem;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import javax.swing.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ParagraphDrawer {
    private DrawingInfo info;

    private Area paragraphArea;
    private boolean pageParagraph;
    private boolean overArea;

    private long lineY;
    private long textX;
    
    private ArrayList<WordLetter> wordLetters;
    private long wordWidth;

    private TextLineDrawer textLineDrawer;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
        wordLetters = new ArrayList<>();

        textLineDrawer = new TextLineDrawer(info);
    }

    public ParagraphDrawer initialize(Paragraph paragraph, boolean pageParagraph) {
        paragraphArea = info.startParagraph(paragraph, pageParagraph);
        this.pageParagraph = pageParagraph;
        overArea = false;

        lineY = 0;
        textX = paragraphArea.left();
        wordLetters.clear();
        wordWidth = 0;
        return this;
    }

    public void draw() throws Exception {
        if (emptyPara()) {
            long charHeight = charHeight();
            checkNewPage(charHeight);
            newLine(charHeight);
        } else {
            textLineDrawer.initialize();

            while (info.nextChar() == true && overArea == false) {
                switch (info.currentChar().getType()) {
                    case Normal:
                        normalChar((HWPCharNormal) info.currentChar());
                        break;
                    case ControlChar:
                        controlChar((HWPCharControlChar) info.currentChar());
                        break;
                    case ControlInline:
                        break;
                    case ControlExtend:
                        break;
                }
            }
        }

        info.endParagraph(lineY, pageParagraph);
    }

    private boolean emptyPara() {
        return info.currentParagraph().getText() == null;
    }


    private void normalChar(HWPCharNormal ch) throws IOException {
        double charWidth = charWidth(ch);
        if (ch.isSpace()) {
            addWord();
            addCharByLetter(ch, charWidth, info.currentCharShape());
        } else if (ch.isHangul()) {
            switch (info.currentParaShape().getProperty1().getLineDivideForHangul()) {
                case ByWord:
                    storeCharByWord(ch, charWidth);
                    break;
                case ByLetter:
                    addCharByLetter(ch, charWidth, info.currentCharShape());
                    break;
            }
        } else {
            switch (info.currentParaShape().getProperty1().getLineDivideForEnglish()) {
                case ByWord:
                case ByHypen:
                    storeCharByWord(ch, charWidth);
                    break;
                case ByLetter:
                    addCharByLetter(ch, charWidth, info.currentCharShape());
                    break;
            }
        }
    }

    private void addWord() throws IOException {
        if (wordLetters.size() > 0) {
            if (isNewLine(wordWidth)) {
                long charHeight = charHeight();
                if (!textLineDrawer.noChar()) {
                    checkNewPage(charHeight);
                    drawLine();
                }
                newLine(charHeight);
            }

            for (WordLetter wordLetter : wordLetters) {
                addCharByLetter(wordLetter.ch, wordLetter.width, wordLetter.charShape);
            }

            wordLetters.clear();
            wordWidth = 0;
        }
    }

    private boolean isNewLine(double width) {
        return textX + width > paragraphArea.right();
    }

    private void storeCharByWord(HWPCharNormal ch, double charWidth) throws IOException {
        wordLetters.add(new WordLetter(ch, charWidth, info.currentCharShape()));
        wordWidth += charWidth;
    }

    private void addCharByLetter(HWPCharNormal ch, double charWidth, CharShape charShape) throws IOException {
        if (isNewLine(charWidth) && !ch.isSpace()) {
            long charHeight = charHeight();
            checkNewPage(charHeight);
            drawLine();
            newLine(charHeight);
        }

        textLineDrawer.addChar(ch, charWidth, charShape);
        textX += charWidth + (charWidth * charShape.getCharSpaces().getHangul() / 100);
    }

    private double charWidth(HWPCharNormal ch) throws UnsupportedEncodingException {
        double charWidth;
        if (ch.isSpace()) {
            charWidth = info.currentCharShape().getBaseSize() / 2;
        } else {
            if (ch.getType() == HWPCharType.Normal) {
                charWidth = info.painter().getCharWidth(ch.getCh(), info.currentCharShape());
            } else {
                charWidth = 0;
            }
        }
        if (ch.isSpace()) {
            charWidth = charWidth * (100 - info.currentParaShape().getProperty1().getMinimumSpace()) / 100;
        }

        charWidth = charWidth * info.currentCharShape().getRelativeSizes().getHangul() / 100;
        charWidth = charWidth * info.currentCharShape().getRatios().getHangul() / 100;


        return charWidth;
    }

    private void checkNewPage(long charHeight) throws IOException {
        if (isOverBottom(charHeight)) {
            if (pageParagraph == true) {
                info.newPage();

                paragraphArea = info.pageParagraphDrawArea();
                lineY = 0;
            } else {
                overArea = true;
                return;
            }
        }
    }

    private void drawLine() throws UnsupportedEncodingException {
        textLineDrawer.draw(paragraphArea.left(), lineY + paragraphArea.top());
        textLineDrawer.initialize();

    }

    private void newLine(long charHeight) {
        lineY += lineHeight(charHeight);
        textX = paragraphArea.left();
    }

    private boolean isOverBottom(long charHeight) {
        return lineY + paragraphArea.top() + charHeight > paragraphArea.bottom();
    }

    private long charHeight() {
        long charHeight;
        if (textLineDrawer.noChar()) {
            charHeight = info.currentCharShape().getBaseSize();
        } else {
            charHeight = textLineDrawer.maxCharHeight();
        }
        return charHeight;
    }


    private long lineHeight(long charHeight) {
        long lineHeight = 0;
        switch (info.currentParaShape().getProperty1().getLineSpaceSort()) {
            case RatioForLetter:
                if (info.currentParaShape().getLineSpace() == info.currentParaShape().getLineSpace2()) {
                    lineHeight = charHeight * info.currentParaShape().getLineSpace() / 100;
                } else {
                    lineHeight = Math.max(charHeight, info.currentParaShape().getLineSpace2() / 2);
                }
                break;
            case FixedValue:
                lineHeight = info.currentParaShape().getLineSpace() / 2;
                break;
            case OnlyMargin:
                lineHeight = charHeight + info.currentParaShape().getLineSpace() / 2;
                break;
        }
        return lineHeight;
    }

    private void controlChar(HWPCharControlChar ch) throws IOException {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            addWord();
            long charHeight = charHeight();
            checkNewPage(charHeight);
            drawLine();
            newLine(charHeight);
        }
    }


    public static class WordLetter {
        public HWPCharNormal ch;
        public double width;
        public CharShape charShape;

        public WordLetter(HWPCharNormal ch, double width, CharShape charShape) {
            this.ch = ch;
            this.width = width;
            this.charShape = charShape;
        }
    }
}
