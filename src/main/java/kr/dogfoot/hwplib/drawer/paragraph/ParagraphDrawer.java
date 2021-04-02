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
    private WordSplitter wordSplitter;

    private DrawingState state;

    private DrawingInfo info;
    private Area storedTextLineArea;
    private Area textLineArea;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;

    private long wordsWidth;
    private long spacesWidth;
    private boolean justNewLine;

    private ArrayList<CharInfo> charsOfWord;
    private long wordWidth;

    private Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(HWPDrawer drawer) {
        this.drawer = drawer;
        wordSplitter = new WordSplitter(this);

        charsOfWord = new ArrayList<>();
        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(Paragraph paragraph, DrawingInfo info) throws Exception {
        System.out.println("{");
        this.info = info;
        wordSplitter.info(info);

        info.startParagraph(paragraph);
        initialize();
        resetWord();

        state = DrawingState.Normal;

        drawer.textLineDrawer()
                .initialize()
                .addNewPart(textLineArea);

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
        long paragraphHeight =  textLineArea.top() - info.paragraphArea().top();
        boolean newPage = info.endParagraph(paragraphHeight);
        if (newPage) {
            drawer.pageMaker().newPage(info);
        }
        System.out.println("}");
    }

    private void initialize() {
        textLineArea = new Area(info.paragraphArea()).height(0);
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
        recalculatingTextAreas.clear();
    }

    private void resetWord() {
        charsOfWord.clear();
        wordWidth = 0;
    }

    private void setLineFirst(int index, int position) {
        lineFirstCharIndex = index;
        lineFirstCharPosition = position;
    }

    private void processChar() throws Exception {
        while (info.nextChar()) {
            switch (info.character().getType()) {
                case Normal:
                    normalChar(newCharInfo((HWPCharNormal) info.character()));
                    break;
                case ControlChar:
                    controlChar((HWPCharControlChar) info.character());
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    break;
            }
            if (state == DrawingState.StartingRecalculating) {
                startRecalculating();
                state = DrawingState.Recalculating;
            } else if (state == DrawingState.EndingRecalculating) {
                endRecalculating();
                state = DrawingState.Normal;
            }
        }
    }


    private CharInfo newCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        return new CharInfo(ch, info.charShape(), info.charIndex(), info.charPosition()).calculateWidth(drawer.painter());
    }

    private void startRecalculating() {
        initialize(recalculatingTextAreas.poll());
        resetWord();

        drawer.textLineDrawer()
                .initialize()
                .addNewPart(textLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
//        System.out.println("start recal : " + lineFirstCharIndex + " " + lineFirstCharPosition + " ");
    }

    private void endRecalculating() {
        drawer.textLineDrawer()
                .initialize()
                .addNewPart(textLineArea);
    }

    private void initialize(Area area) {
        textLineArea = area;
        wordsWidth = 0;
        spacesWidth = 0;
        justNewLine = true;
    }

    private void normalChar(CharInfo charInfo) throws Exception {
        if (!charInfo.character().isSpace()) {
            storeCharOfWord(charInfo);
        } else {
            addWordToLine(charInfo);
        }
    }

    private void storeCharOfWord(CharInfo charInfo) throws UnsupportedEncodingException {
        charsOfWord.add(charInfo);
        wordWidth += charInfo.width();
    }

    private void addWordToLine(CharInfo spaceCharInfo) throws Exception {
        if (charsOfWord.size() == 0) {
            addSpaceChar(spaceCharInfo);
            return;
        }

        if (!isOverRight(wordWidth, false)) {
            addWordAllCharsToLine(charsOfWord, false, false);
            addSpaceChar(spaceCharInfo);
            resetWord();
        } else {
            if (!isOverRight(wordWidth, true)) {
                addWordAllCharsToLine(charsOfWord, false, true);
                // addSpaceChar(spaceCharInfo);
                resetWord();

                drawer.textLineDrawer().spaceRate(bestSpaceRate());
                drawTextAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
    }

    private void addSpaceChar(CharInfo spaceCharInfo) throws UnsupportedEncodingException {
        if (state.canAddCharInfo() && spaceCharInfo != null) {
            drawer.textLineDrawer().addChar(spaceCharInfo);
            spacesWidth += spaceCharInfo.widthAddingCharSpace();
        }
    }

    public boolean isOverRight(double width, boolean applyMinimumSpace) {
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
        addChar(charInfo);
        return hasNewLine;
    }

    private void addChar(CharInfo charInfo) throws UnsupportedEncodingException {
        if (state.canAddCharInfo()) {
            if (drawer.textLineDrawer().noChar() && state == DrawingState.Normal) {
/*
                System.out.println("linefirst : " +charInfo.character().getCh() + " " +
                        (charInfo.index() - 1) + " " +
                        (charInfo.position() - charInfo.character().getCharSize()));

 */
               setLineFirst((charInfo.index() - 1),  (charInfo.position() - charInfo.character().getCharSize()));
            }
            drawer.textLineDrawer().addChar(charInfo);
            wordsWidth += charInfo.widthAddingCharSpace();
        }
    }


    private double bestSpaceRate() {
        return (double) (textLineArea.width() - wordsWidth) / (double) spacesWidth;
    }

    public void drawTextAndNewLine() throws Exception {
        if (justNewLine == false) {
            long charHeight = charHeight();
            checkNewPage(charHeight);
            drawTextLine();
            nextArea(charHeight);
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

    private void drawTextLine() throws UnsupportedEncodingException {
        switch (state) {
            case Normal:
                drawTextNormal();
                break;
            case Recalculating:
    //             System.out.println(drawer.textLineDrawer().lastLine() + " " + drawer.textLineDrawer().area() + drawer.textLineDrawer().text());
                if (recalculatingTextAreas.size() == 0
                    || drawer.textLineDrawer().lastLine()) {
                    drawer.textLineDrawer().draw(info);
                }
                break;
        }
    }


    private void drawTextNormal() throws UnsupportedEncodingException {
//        System.out.println("before draw : " + drawer.textLineDrawer().text());

        textLineArea
                .height(drawer.textLineDrawer().maxCharHeight())
                .moveY(drawer.controlDrawer().checkTopBottomTextFlow(textLineArea));

        Area[] dividedAreas = drawer.controlDrawer().checkSquareTextFlow(textLineArea);
        if (dividedAreas.length == 1) {
            System.out.println("{");
            System.out.println(dividedAreas[0] + " " +drawer.textLineDrawer().text());
            drawer.textLineDrawer()
                    .area(new Area(dividedAreas[0]))
                    .draw(info);
            System.out.println("}");
        } else {
            setRedrawTextLineArea(dividedAreas);

            state = DrawingState.StartingRecalculating;
        }
    }

    private void setRedrawTextLineArea(Area[] dividedAreas) {
        storedTextLineArea = textLineArea;

        for (Area dividedArea : dividedAreas) {
            recalculatingTextAreas.offer(dividedArea);
        }
    }

    private void nextArea(long charHeight) throws UnsupportedEncodingException {
        switch(state) {
            case Normal:
                newLine(charHeight);
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0 ||
                        drawer.textLineDrawer().lastLine()) {
                    textLineArea = storedTextLineArea;
                    newLine(charHeight);

                    state = DrawingState.EndingRecalculating;
                } else {
                    initialize(recalculatingTextAreas.poll());

                    drawer.textLineDrawer().addNewPart(textLineArea);
                }
                break;
        }
    }

    private void newLine(long charHeight) {
        initialize(textLineArea.moveY(lineHeight(charHeight)));

        drawer.textLineDrawer()
                .initialize()
                .addNewPart(textLineArea);
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

    private void spanningWord(CharInfo spaceCharInfo) throws Exception {
        if (isAllLineDivideByWord(info.paraShape())) {
            if (drawer.textLineDrawer().noChar() == false) {
                drawTextAndNewLine();
            }

            if (state == DrawingState.EndingRecalculating) {
                if (state == DrawingState.EndingRecalculating) {
                    info.beforeChar(charsOfWord.size() + 1);
                }
            }
            addWordAllCharsToLine(charsOfWord, true, false);
            addSpaceChar(spaceCharInfo);
            resetWord();
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split(charsOfWord);
            if (state == DrawingState.EndingRecalculating) {
                info.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
            addSpaceChar(spaceCharInfo);
            resetWord();
        }
    }

    private boolean isAllLineDivideByWord(ParaShape paraShape) {
        return paraShape.getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                && paraShape.getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord;
    }

    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            addWordToLine(null);

            drawer.textLineDrawer().lastLine(true);
            drawTextAndNewLine();
        }
    }

    private enum DrawingState {
        Normal,
        StartingRecalculating,
        Recalculating,
        EndingRecalculating;

        public boolean canAddCharInfo() {
            return this == Normal || this == Recalculating;
        }
    }
}
