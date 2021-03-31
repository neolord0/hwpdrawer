package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class ParagraphDrawer {
    private static final HWPCharNormal SpaceChar = new HWPCharNormal(32);

    private HWPDrawer drawer;
    private DrawingState state;

    private DrawingInfo info;
    private Area storedTextLineArea;
    private Area textLineArea;

    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    private ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    private Queue<Area> redrawTextAreas;

    private WordSplitter wordSplitter;

    public ParagraphDrawer(HWPDrawer drawer) {
        this.drawer = drawer;
        charsOfWord = new ArrayList<>();
        redrawTextAreas = new LinkedList<>();

        wordSplitter = new WordSplitter(this);
    }

    public void draw(Paragraph paragraph, DrawingInfo info) throws Exception {
        this.info = info;
        wordSplitter.info(info);

        info.startParagraph(paragraph);
        info.saveCharPosition();
        initialize();

        drawer.textLineDrawer().initialize();

        drawer.controlDrawer()
                .controlList(info.paragraph().getControlList(), info)
                .drawControlsForBehind()
                .removeControlsForBehind();

        if (info.noText()) {
            justNewLine = false;
            drawTextAndNewLine();
        } else {
            processChar();
        }

        drawer.controlDrawer()
                .drawControlsForTopBottom()
                .removeControlsForTopBottom();

        boolean newPage = info.endParagraph(paragraphHeight());
        if (newPage) {
            drawer.pageMaker().newPage(info);
        }
    }

    private void initialize() {
        state = DrawingState.Normal;
        textLineArea = new Area(info.paragraphArea()).height(0);
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
        charsOfWord.clear();
        wordWidth = 0;
    }

    private void processChar() throws Exception {
        while (info.nextChar()) {
            switch (info.character().getType()) {
                case Normal:
                    normalChar((HWPCharNormal) info.character());
                    break;
                case ControlChar:
                    controlChar((HWPCharControlChar) info.character());
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    break;
            }
            if (state == DrawingState.StartingRedraw) {
                startRedraw();
            } else if (state == DrawingState.EndingRedraw) {
                info.saveCharPosition();
                state = DrawingState.Normal;
            }
        }
    }

    private long paragraphHeight() {
        return textLineArea.top() - info.paragraphArea().top();
    }

    private void normalChar(HWPCharNormal ch) throws Exception {
        if (!ch.isSpace()) {
            storeCharOfWord(ch);
        } else {
            addWordToLine();
        }
    }

    private void storeCharOfWord(HWPCharNormal ch) throws UnsupportedEncodingException {
        CharInfo newCharInfo = new CharInfo(ch, info.charShape()).calculateWidth(drawer.painter());
        charsOfWord.add(newCharInfo);
        wordWidth += newCharInfo.width();
    }

    private void addWordToLine() throws Exception {
        if (charsOfWord.size() == 0) {
            addSpaceChar();
            return;
        }

        if (!isOverRight(wordWidth, false)) {
            addWordAllCharsToLine(charsOfWord, false, false);
            resetWord();
            addSpaceChar();
        } else {
            if (!isOverRight(wordWidth, true)) {
                addWordAllCharsToLine(charsOfWord, false, true);
                addSpaceChar();
                resetWord();

                drawer.textLineDrawer().spaceRate(bestSpaceRate());
                drawTextAndNewLine();
            } else {
                spanningWord();
            }
        }
    }

    private void addSpaceChar() throws UnsupportedEncodingException {
        CharInfo newCharInfo = new CharInfo(SpaceChar, info.charShape()).calculateWidth(drawer.painter());

        if (state.canDraw()) {
            drawer.textLineDrawer().addChar(newCharInfo);
            spacesWidth += newCharInfo.widthAddingCharSpace();
        }
    }

    boolean isOverRight(double width, boolean applyMinimumSpace) {
        return currentTextX(applyMinimumSpace) + width > textLineArea.right();
    }

    private long currentTextX(boolean applyMinimumSpace) {
        if (applyMinimumSpace) {
            long minimumSpace = spacesWidth * (100 - info.paraShape().getProperty1().getMinimumSpace()) / 100;
            return wordsWidth + minimumSpace + textLineArea.left();
        } else {
            return wordsWidth + spacesWidth + textLineArea.left();
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            addCharToLine(charInfo, checkOverRight, applyMinimumSpace);
        }
    }

    public boolean addCharToLine(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && isOverRight(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                drawer.textLineDrawer().spaceRate(bestSpaceRate());
            }
            hasNewLine = true;
            drawTextAndNewLine();
        } else {
            hasNewLine = false;
        }
        justNewLine = false;

        if (state.canDraw()) {
            drawer.textLineDrawer().addChar(charInfo);
            wordsWidth += charInfo.widthAddingCharSpace();
        }
        return hasNewLine;
    }

    private void resetWord() {
        charsOfWord.clear();
        wordWidth = 0;
    }

    private double bestSpaceRate() {
        return (double) (textLineArea.width() - wordsWidth) / (double) spacesWidth;
    }

    void drawTextAndNewLine() throws Exception {
        if (justNewLine == false) {
            long charHeight = charHeight();
            checkNewPage(charHeight);
            drawText();
            nextTextArea(charHeight);
        }
    }

    private long charHeight() {
        long charHeight;
        if (drawer.textLineDrawer().noChar()) {
            charHeight = info.charShape().getBaseSize();
        } else {
            charHeight = drawer.textLineDrawer().maxCharHeight();
        }
        return charHeight;
    }

    private void checkNewPage(long charHeight) throws IOException {
        if (isOverBottom(charHeight)) {
            if (info.isBodyText() == true) {
                drawer.controlDrawer()
                        .drawControlsForFront()
                        .removeControlsForFront();

                drawer.pageMaker().newPage(info);

                textLineArea.top(info.pageArea().top());
            }
        }
    }

    private boolean isOverBottom(long charHeight) {
        return textLineArea.top() + charHeight > info.pageArea().bottom();
    }

    private void drawText() throws UnsupportedEncodingException {
        switch (state) {
            case Normal:
                drawTextNormal();
                break;
            case Redrawing:
                redrawText();
                break;
        }
    }
    private void drawTextNormal() throws UnsupportedEncodingException {
        textLineArea
                .height(drawer.textLineDrawer().maxCharHeight())
                .moveY(drawer.controlDrawer().checkTopBottomTextFlow(textLineArea));

        Area[] dividedAreas = drawer.controlDrawer().checkSquareTextFlow(textLineArea);
        if (dividedAreas.length == 1) {
            drawer.textLineDrawer().draw(dividedAreas[0], info);
            drawer.textLineDrawer().initialize();
        } else {
            state = DrawingState.StartingRedraw;
            setRedrawTextLineArea(dividedAreas);
        }
    }

    private void setRedrawTextLineArea(Area[] dividedAreas) {
        storedTextLineArea = textLineArea;

        for (Area dividedArea : dividedAreas) {
            redrawTextAreas.offer(dividedArea);
        }
    }

    private void redrawText() throws UnsupportedEncodingException {
        drawer.textLineDrawer().draw(textLineArea, info);
        drawer.textLineDrawer().initialize();
    }

    private void nextTextArea(long charHeight) throws UnsupportedEncodingException {
        switch(state) {
            case Normal:
                newLine(charHeight);
                break;
            case Redrawing:
                nextRedrawTextArea(charHeight);
                break;
        }
    }

    private void newLine(long charHeight) {
        textLineArea.moveY(lineHeight(charHeight));

        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
    }

    private void nextRedrawTextArea(long charHeight) {
        if (redrawTextAreas.size() == 0) {
            textLineArea = storedTextLineArea;
            newLine(charHeight);

            state = DrawingState.EndingRedraw;
        } else {
            textLineArea = redrawTextAreas.poll();
            wordsWidth = 0;
            spacesWidth = 0;
            justNewLine = true;
        }
    }

    private long lineHeight(long charHeight) {
        long lineHeight = 0;
        ParaShape paraShape = info.paraShape();
        switch (paraShape.getProperty1().getLineSpaceSort()) {
            case RatioForLetter:
                if (paraShape.getLineSpace() == paraShape.getLineSpace2()) {
                    lineHeight = charHeight * paraShape.getLineSpace() / 100;
                } else {
                    lineHeight = Math.max(charHeight, paraShape.getLineSpace2() / 2);
                }
                break;
            case FixedValue:
                lineHeight = paraShape.getLineSpace() / 2;
                break;
            case OnlyMargin:
                lineHeight = charHeight + paraShape.getLineSpace() / 2;
                break;
        }
        return lineHeight;
    }

    private void spanningWord() throws Exception {
        if (isAllLineDivideByWord(info.paraShape())) {
            if (drawer.textLineDrawer().noChar() == false) {
                drawTextAndNewLine();
            }

            addWordAllCharsToLine(charsOfWord, true, false);
            addSpaceChar();
            resetWord();
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split(charsOfWord);
            if (state == DrawingState.EndingRedraw) {
                info.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
            addSpaceChar();
            resetWord();
        }
    }

    private boolean isAllLineDivideByWord(ParaShape paraShape) {
        return paraShape.getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                && paraShape.getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord;
    }


    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            addWordToLine();

            drawer.textLineDrawer().lastLine(true);
            drawTextAndNewLine();
        }
    }

    private void startRedraw() {
        initializeForRedraw();

        drawer.textLineDrawer().initialize();
        info.rollbackCharPosition();
    }

    private void initializeForRedraw() {
        state = DrawingState.Redrawing;
        textLineArea = redrawTextAreas.poll();
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
        charsOfWord.clear();
        wordWidth = 0;
    }

    private enum DrawingState {
        Normal,
        StartingRedraw,
        Redrawing,
        EndingRedraw
        ;

        public boolean canDraw() {
            return this == Normal || this == Redrawing;
        }
    }
}
