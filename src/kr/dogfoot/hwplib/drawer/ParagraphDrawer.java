package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ParagraphDrawer {
    private DrawingInfo info;

    private static final HWPCharNormal spaceChar = new HWPCharNormal(32);

    private Area paragraphArea;
    private boolean pageParagraph;
    private boolean overArea;

    private long lineY;
    private long wordsWidth;
    private long spacesWidth;

    private double currentCharWidth;
    private ArrayList<WordChar> wordChars;
    private long wordWidth;

    private TextLineDrawer textLineDrawer;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
        wordChars = new ArrayList<>();

        textLineDrawer = new TextLineDrawer(info);
    }

    public void draw(Paragraph paragraph, boolean pageParagraph) throws Exception {
        paragraphArea = info.startParagraph(paragraph, pageParagraph);
        System.out.println("{");
        
        initialize(pageParagraph);

        if (noParaText()) {
            drawTextAndNewLine();
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
        System.out.println("}");
    }

    private void initialize(boolean pageParagraph) {
        this.pageParagraph = pageParagraph;
        overArea = false;

        lineY = 0;
        wordsWidth = 0;
        spacesWidth = 0;

        currentCharWidth = 0;
        wordChars.clear();
        wordWidth = 0;
    }

    private boolean noParaText() {
        return info.currentParagraph().getText() == null;
    }

    private void normalChar(HWPCharNormal ch) throws IOException {
        currentCharWidth = charWidth(ch);
        if (!ch.isSpace()) {
            storeCharByWord(ch);
        } else {
            addWordToLine();
        }
    }

    private void storeCharByWord(HWPCharNormal ch) throws IOException {
        wordChars.add(new WordChar(ch, currentCharWidth, info.currentCharShape()));
        wordWidth += currentCharWidth;
    }

    private void addWordToLine() throws IOException {
        if (wordChars.size() > 0) {
            if (!isOverRight(wordWidth)) {
                addWordAllCharsToLine(wordChars, false);
                addSpaceChar();
                resetWord();
            } else {
                spanningWord();
            }
        } else {
            addSpaceChar();
        }
    }

    private boolean isOverRight(double width) {
        return currentTextX() + width > paragraphArea.right();
    }

    private long currentTextX() {
        return wordsWidth + spacesWidth + paragraphArea.left();
    }

    private void addWordAllCharsToLine(ArrayList<WordChar> wordChars, boolean checkOverRight) throws IOException {
        for (WordChar wc : wordChars) {
            addCharToLine(wc.ch, wc.width, wc.charShape, checkOverRight);
        }
    }

    private void addCharToLine(HWPCharNormal ch, double charWidth, CharShape charShape, boolean checkOverRight) throws IOException {
        if (checkOverRight && isOverRight(charWidth)) {
            drawTextAndNewLine();
        }

        textLineDrawer.addChar(ch, charWidth, charShape);
        wordsWidth += charWidth + (charWidth * charShape.getCharSpaces().getHangul() / 100);
    }

    private void resetWord() {
        wordChars.clear();
        wordWidth = 0;
    }

    private void spanningWord() throws IOException {
        if (!isOverRightApplyMinimumSpace(wordWidth)) {
            addWordAllCharsToLine(wordChars, false);
            addSpaceChar();
            resetWord();

            textLineDrawer.spaceRate(bestSpaceRate());
            drawTextAndNewLine();
        } else {
            if (info.currentParaShape().getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                    && info.currentParaShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord) {
                drawTextAndNewLine();

                addWordAllCharsToLine(wordChars, true);
                addSpaceChar();
                resetWord();
            } else {
                splitWords();
                addSpaceChar();
                resetWord();
            }
        }
    }

    private boolean isOverRightApplyMinimumSpace(long width) {
        return info.currentParaShape().getProperty1().getMinimumSpace() == 0
                || currentTextXApplyMinimumSpace() + width > paragraphArea.right();
    }

    private long currentTextXApplyMinimumSpace() {
        long minimumSpace = spacesWidth * (100 - info.currentParaShape().getProperty1().getMinimumSpace()) / 100;
        return wordsWidth + minimumSpace + paragraphArea.left();
    }

    private double bestSpaceRate() {
        return (double) (paragraphArea.width() - wordsWidth) / (double) spacesWidth;
    }

    private void drawTextAndNewLine() throws IOException {
        long charHeight = charHeight();
        checkNewPage(charHeight);
        drawText();
        newLine(charHeight);
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

    private boolean isOverBottom(long charHeight) {
        return lineY + paragraphArea.top() + charHeight > paragraphArea.bottom();
    }

    private void drawText() throws UnsupportedEncodingException {
        textLineDrawer.draw(paragraphArea.left(), lineY + paragraphArea.top());
        textLineDrawer.initialize();
    }

    private void newLine(long charHeight) {
        lineY += lineHeight(charHeight);
        wordsWidth = 0;
        spacesWidth = 0;
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

    private void splitWords() throws IOException {
        boolean splitByEnglishLetter = info.currentParaShape().getProperty1().getLineDivideForEnglish() != LineDivideForEnglish.ByWord;
        boolean splitByHangulLetter = info.currentParaShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByLetter;

        boolean previousHangul = false;
        ArrayList<WordChar> wordCharsByLanguage = new ArrayList<>();
        long wordWidthByLanguage = 0;

        int count = wordChars.size();
        for (int index = 0; index < count; index++) {
            WordChar wc = wordChars.get(index);

            if ((index > 0 && previousHangul != wc.ch.isHangul()) || index + 1 == count) {
                if (index + 1 == count) {
                    wordCharsByLanguage.add(wc);
                    wordWidthByLanguage += wc.width;
                }
                System.out.print("\t");
                for (WordChar wc2 : wordCharsByLanguage) {
                    System.out.print(wc2.ch.getCh());
                }
                System.out.println("");

                if (previousHangul) {
                    if (splitByHangulLetter) {
                        addWordAllCharsToLine(wordCharsByLanguage, true);
                    } else {
                        addEachLanguageWordToLine(wordCharsByLanguage, wordWidthByLanguage);
                    }
                } else {
                    if (splitByEnglishLetter) {
                        addWordAllCharsToLine(wordCharsByLanguage, true);
                    } else {
                        addEachLanguageWordToLine(wordCharsByLanguage, wordWidthByLanguage);
                    }
                }

                wordCharsByLanguage.clear();
                wordWidthByLanguage = 0;
            }

            wordCharsByLanguage.add(wc);
            wordWidthByLanguage += wc.width;
            previousHangul = wc.ch.isHangul();
        }
    }

    private void addEachLanguageWordToLine(ArrayList<WordChar> wordChars, long wordWidth) throws IOException {
        if (wordChars.size() > 0) {
            if (!isOverRight(wordWidth)) {
                addWordAllCharsToLine(wordChars, false);
            } else {
                drawTextAndNewLine();
                addWordAllCharsToLine(wordChars,true);
            }
        }
    }

    private void addSpaceChar() {
        textLineDrawer.addChar(spaceChar, currentCharWidth, info.currentCharShape());
        spacesWidth += currentCharWidth + (currentCharWidth * info.currentCharShape().getCharSpaces().getHangul() / 100);
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
        charWidth = charWidth * info.currentCharShape().getRelativeSizes().getHangul() / 100;
        charWidth = charWidth * info.currentCharShape().getRatios().getHangul() / 100;
        return charWidth;
    }

    private void controlChar(HWPCharControlChar ch) throws IOException {
        if (ch.isParaBreak() || ch.isLineBreak()) {

            addWordToLine();

            drawTextAndNewLine();
        }
    }

    private static class WordChar {
        public HWPCharNormal ch;
        public double width;
        public CharShape charShape;

        public WordChar(HWPCharNormal ch, double width, CharShape charShape) {
            this.ch = ch;
            this.width = width;
            this.charShape = charShape;
        }
    }
}
