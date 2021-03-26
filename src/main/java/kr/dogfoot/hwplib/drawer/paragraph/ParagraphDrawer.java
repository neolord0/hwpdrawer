package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.DrawingInfo;
import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
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

    private boolean pageParagraph;
    private boolean overArea;

    private long lineY;
    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    private double currentCharWidth;
    private ArrayList<WordChar> wordChars;
    private long wordWidth;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
        wordChars = new ArrayList<>();
    }

    public void draw(Paragraph paragraph, boolean bodyTextParapgraph) throws Exception {
        info.startParagraph(paragraph, bodyTextParapgraph);
        initialize(pageParagraph);

        info.textLineDrawer().initialize();

        info.controlDrawer()
                .controlList(paragraph.getControlList())
                .drawControlsForBehind()
                .removeControlsForBehind();

        if (noParaText()) {
            drawTextAndNewLine();
        } else {

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

        info.controlDrawer()
                .drawControlsForTopBottom()
                .removeControlsForTopBottom();

        System.out.println(info.controlDrawer().toTest());

        info.endParagraph(lineY, pageParagraph);
    }

    private void initialize(boolean pageParagraph) {
        this.pageParagraph = pageParagraph;
        overArea = false;

        lineY = 0;
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;

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
            if (!isOverRight(wordWidth, false)) {
                addWordAllCharsToLine(wordChars, false, false);
                addSpaceChar();
                resetWord();
            } else {
                spanningWord();
            }
        } else {
            addSpaceChar();
        }
    }

    private boolean isOverRight(double width, boolean applyMinimumSpace) {
        if (applyMinimumSpace) {
            return currentTextXApplyMinimumSpace() + width > info.paragraphArea().right();
        } else {
            return currentTextX() + width > info.paragraphArea().right();
        }
    }

    private long currentTextXApplyMinimumSpace() {
        long minimumSpace = spacesWidth * (100 - info.currentParaShape().getProperty1().getMinimumSpace()) / 100;
        return wordsWidth + minimumSpace + info.paragraphArea().left();
    }

    private long currentTextX() {
        return wordsWidth + spacesWidth + info.paragraphArea().left();
    }

    private void addWordAllCharsToLine(ArrayList<WordChar> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws IOException {
        for (WordChar wc : wordChars) {
            addCharToLine(wc.ch, wc.width, wc.charShape, checkOverRight, applyMinimumSpace);
        }
    }

    private void addCharToLine(HWPCharNormal ch, double charWidth, CharShape charShape, boolean checkOverRight, boolean applyMinimumSpace) throws IOException {
        if (checkOverRight && isOverRight(charWidth, applyMinimumSpace)) {
            if (applyMinimumSpace) {
                info.textLineDrawer().spaceRate(bestSpaceRate());
            }
            drawTextAndNewLine();
        }
        justNewLine = false;
        info.textLineDrawer().addChar(ch, charWidth, charShape);
        wordsWidth += charWidth + (charWidth * charShape.getCharSpaces().getHangul() / 100);
    }

    private void resetWord() {
        wordChars.clear();
        wordWidth = 0;
    }

    private void spanningWord() throws IOException {
        if (!isOverRight(wordWidth, true)) {
            addWordAllCharsToLine(wordChars, false, false);
            addSpaceChar();
            resetWord();

            info.textLineDrawer().spaceRate(bestSpaceRate());
            drawTextAndNewLine();
        } else {
            if (info.currentParaShape().getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                    && info.currentParaShape().getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord) {
                if (info.textLineDrawer().noChar() == false) {
                    drawTextAndNewLine();
                }

                addWordAllCharsToLine(wordChars, true, false);
                addSpaceChar();
                resetWord();
            } else {
                splitWords();
                addSpaceChar();
                resetWord();
            }
        }
    }

    private double bestSpaceRate() {
        return (double) (info.paragraphArea().width() - wordsWidth) / (double) spacesWidth;
    }

    private void drawTextAndNewLine() throws IOException {
        if (justNewLine == false) {
            long charHeight = charHeight();
            checkNewPage(charHeight);
            drawText();
            newLine(charHeight);
        }
    }

    private long charHeight() {
        long charHeight;
        if (info.textLineDrawer().noChar()) {
            charHeight = info.currentCharShape().getBaseSize();
        } else {
            charHeight = info.textLineDrawer().maxCharHeight();
        }
        return charHeight;
    }

    private void checkNewPage(long charHeight) throws IOException {
        if (isOverBottom(charHeight)) {
            if (pageParagraph == true) {
                info.controlDrawer()
                        .drawControlsForFront()
                        .removeControlsForFront();

                info.newPage();
                lineY = 0;
            } else {
                overArea = true;
                return;
            }
        }
    }

    private boolean isOverBottom(long charHeight) {
        return lineY + info.paragraphArea().top() + charHeight > info.paragraphArea().bottom();
    }

    private void drawText() throws UnsupportedEncodingException {
        lineY += info.controlDrawer().checkTopBottomTextFlow(textLineArea());
        info.textLineDrawer().draw(lineY + info.paragraphArea().top());
        Area textLineArea = textLineArea();
        Area[] dividedAreas =  info.controlDrawer().checkSquareTextFlow(textLineArea);

        System.out.println(textLineArea.toString() + "{");
        for (Area dividedArea : dividedAreas) {
            System.out.println("\t" + dividedArea);
        }
        System.out.println("}");

        info.textLineDrawer().initialize();
    }

    private Area textLineArea() {
        return new Area(info.paragraphArea().left(),
                info.paragraphArea().top() + lineY,
                info.paragraphArea().right(),
                info.paragraphArea().top() + lineY + info.textLineDrawer().maxCharHeight());
    }

    private void newLine(long charHeight) {
        lineY += lineHeight(charHeight);

        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
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

        ArrayList<WordsCharByLanguage> wordCharsByLanguages = splitByLanguage(wordChars);
        for (WordsCharByLanguage wcl: wordCharsByLanguages) {
            boolean splitByLetter = (wcl.hangul) ? splitByHangulLetter : splitByEnglishLetter;
            if (splitByLetter) {
                addWordAllCharsToLine(wcl.wordChars, true, true);
            } else {
                addEachLanguageWordToLine(wcl.wordChars, wcl.wordWidth);
            }
        }
    }

    private ArrayList<WordsCharByLanguage> splitByLanguage(ArrayList<WordChar> wordChars) throws UnsupportedEncodingException {
        if (wordChars == null || wordChars.size() == 0) {
            return null;
        }

        ArrayList<WordsCharByLanguage> list = new ArrayList<>();
        WordsCharByLanguage item = new WordsCharByLanguage();
        boolean previousHangul = false;
        int count = wordChars.size();
        for (int index = 0; index < count; index++) {
            WordChar wc = wordChars.get(index);
            if (index > 0 && previousHangul != wc.ch.isHangul()) {
                item.hangul = previousHangul;
                list.add(item);
                item = new WordsCharByLanguage();
            }

            item.wordWidth += wc.width;
            item.wordChars.add(wc);

            previousHangul = wc.ch.isHangul();
        }
        if (item.wordChars.size() > 0) {
            item.hangul = previousHangul;
            list.add(item);
        }

        return list;
    }

    private void addEachLanguageWordToLine(ArrayList<WordChar> wordChars, long wordWidth) throws IOException {
        if (wordChars.size() > 0) {
            if (!isOverRight(wordWidth, true)) {
                addWordAllCharsToLine(wordChars, false, false);
            } else {
                drawTextAndNewLine();
                addWordAllCharsToLine(wordChars,true, true);
            }
        }
    }

    private void addSpaceChar() {
        info.textLineDrawer().addChar(spaceChar, currentCharWidth, info.currentCharShape());
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

            justNewLine = false;
            info.textLineDrawer().lastLine(true);
            drawTextAndNewLine();
        }
    }

    private static class WordsCharByLanguage {
        public boolean hangul;
        public long wordWidth;
        public ArrayList<WordChar> wordChars;

        public WordsCharByLanguage() {
            wordChars = new ArrayList<>();
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
