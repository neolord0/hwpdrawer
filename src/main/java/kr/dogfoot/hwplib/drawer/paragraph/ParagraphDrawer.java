package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
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
    private HWPDrawer drawer;
    private WordSplitter wordSplitter;

    private DrawingState state;
    private DrawingInfo info;

    private Area storedTextLineArea;
    private Area textLineArea;
    private long charHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;

    private Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(HWPDrawer drawer) {
        this.drawer = drawer;
        wordSplitter = new WordSplitter(drawer);

        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(Paragraph paragraph, DrawingInfo info) throws Exception {
        info.startParagraph(paragraph);
        initialize(info);
        wordSplitter.resetWord();

        state = DrawingState.Normal;

        drawer.textLineDrawer()
                .initialize(info)
                .addNewPart(textLineArea);

        drawer.controlDrawer()
                .controlList(info.paragraph().getControlList(), info)
                .drawControlsForBehind()
                .removeControlsForBehind();

        if (info.noText()) {
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
    }

    private void initialize(DrawingInfo info) {
        this.info = info;
        wordSplitter.info(info);
        textLineArea = new Area(info.paragraphArea()).height(0);
        recalculatingTextAreas.clear();
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
            switch (state) {
                case StartRecalculating:
                    startRecalculating();
                    state = DrawingState.Recalculating;
                    break;
                case EndRecalculating:
                    endRecalculating();
                    state = DrawingState.Normal;
                    break;
                case StartRedrawing:
                    startRedraw();
                    state = DrawingState.Normal;
                    break;
            }
        }
    }


    private CharInfo newCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        return new CharInfo(ch, info.charShape(), info.charIndex(), info.charPosition()).calculateWidth(drawer.painter());
    }

    private void startRecalculating() {
        textLineArea = recalculatingTextAreas.poll();
        wordSplitter.resetWord();

        drawer.textLineDrawer()
                .reset()
                .addNewPart(textLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void endRecalculating() {
        textLineArea = storedTextLineArea;
        textLineArea.moveY(lineHeight());

        drawer.textLineDrawer()
                .reset()
                .addNewPart(textLineArea);
    }

    private void startRedraw() {
        wordSplitter.resetWord();

        drawer.textLineDrawer()
                .reset()
                .addNewPart(textLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }


    private void normalChar(CharInfo charInfo) throws Exception {
        if (!charInfo.character().isSpace()) {
            wordSplitter.addCharOfWord(charInfo);
        } else {
            addWordToLine(charInfo);
        }
    }

    private void addWordToLine(CharInfo spaceCharInfo) throws Exception {
        if (wordSplitter.noChar()) {
            addSpaceChar(spaceCharInfo);
            return;
        }

        if (!drawer.textLineDrawer().isOverRight(wordSplitter.wordWidth(), false)) {
            addWordAllCharsToLine(wordSplitter.charsOfWord(), false, false);
            addSpaceChar(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            if (!drawer.textLineDrawer().isOverRight(wordSplitter.wordWidth(), true)) {
                addWordAllCharsToLine(wordSplitter.charsOfWord(), false, true);
                wordSplitter.resetWord();

                drawer.textLineDrawer().setBestSpaceRate();
                drawTextAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
    }

    private void addSpaceChar(CharInfo spaceCharInfo) {
        if (state.canAddCharInfo() && spaceCharInfo != null) {
            drawer.textLineDrawer().addChar(spaceCharInfo);
        }
    }


    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            addCharToLine(charInfo, checkOverRight, applyMinimumSpace);
        }
    }

    public boolean addCharToLine(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && drawer.textLineDrawer().isOverRight(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                drawer.textLineDrawer().setBestSpaceRate();
            }
            hasNewLine = true;
            drawTextAndNewLine();
        } else {
            hasNewLine = false;
        }
        drawer.textLineDrawer().justNewLine(false);
        addChar(charInfo);
        return hasNewLine;
    }

    private void addChar(CharInfo charInfo) {
        if (state.canAddCharInfo()) {
            if (drawer.textLineDrawer().noChar() && state == DrawingState.Normal) {
                setLineFirst((charInfo.index() - 1),  (charInfo.position() - charInfo.character().getCharSize()));
            }

            drawer.textLineDrawer().addChar(charInfo);
        }
    }

    private void setLineFirst(int index, int position) {
        lineFirstCharIndex = index;
        lineFirstCharPosition = position;
    }


    public void drawTextAndNewLine() throws Exception {
        if (!drawer.textLineDrawer().justNewLine()) {
            saveCharHeight();
            checkNewPage();
            checkTextFlow();
            drawTextLine();
            nextArea();
        }
    }

    private void saveCharHeight() {
        if (drawer.textLineDrawer().noChar()) {
            charHeight = info.charShape().getBaseSize();
        } else {
            charHeight = drawer.textLineDrawer().maxCharHeight();
        }
    }

    private void checkNewPage() throws IOException {
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

    private boolean isOverBottom(long height) {
        return textLineArea.top() + height > info.pageArea().bottom();
    }

    private void checkTextFlow() {
        if (state == DrawingState.Normal) {
            textLineArea
                    .height(drawer.textLineDrawer().maxCharHeight());
            ControlDrawer.TextFlowCheckResult result = drawer.controlDrawer().checkTextFlow(textLineArea);
            textLineArea
                    .moveY(result.offsetY());

            state = result.nextState();

            if (state == DrawingState.StartRecalculating) {
                storedTextLineArea = textLineArea;
                for (Area dividedArea : result.dividedAreas()) {
                    recalculatingTextAreas.offer(dividedArea);
                }
            }
        }
    }


    private void drawTextLine() throws UnsupportedEncodingException {
        switch (state) {
            case Normal:
                // todo : area() 없애기
                drawer.textLineDrawer()
                        .area(textLineArea)
                        .draw();
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0
                    || drawer.textLineDrawer().lastLine()) {
                    drawer.textLineDrawer().draw();
                }
                break;
        }
    }

    private void nextArea() {
        switch(state) {
            case Normal:
            case StartRedrawing:
                newLine();
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0 ||
                        drawer.textLineDrawer().lastLine()) {
                    state = DrawingState.EndRecalculating;
                } else {
                    nextTextPart();
                }
                break;
        }
    }


    private void newLine() {
        textLineArea.moveY(lineHeight());
        drawer.textLineDrawer()
                .reset()
                .addNewPart(textLineArea);
    }

    private void nextTextPart() {
        textLineArea = recalculatingTextAreas.poll();
        drawer.textLineDrawer()
                .resetPart()
                .addNewPart(textLineArea);
    }

    private long lineHeight() {
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
        ArrayList<CharInfo> charsOfWord = wordSplitter.charsOfWord();

        if (isAllLineDivideByWord(info.paraShape())) {
            if (drawer.textLineDrawer().noChar() == false) {
                drawTextAndNewLine();
            }

            if (state == DrawingState.EndRecalculating) {
                if (state == DrawingState.EndRecalculating) {
                    info.beforeChar(charsOfWord.size() + 1);
                }
            }
            addWordAllCharsToLine(charsOfWord, true, false);
            addSpaceChar(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split();
            if (state == DrawingState.EndRecalculating) {
                info.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
            addSpaceChar(spaceCharInfo);
            wordSplitter.resetWord();
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

    public enum DrawingState {
        Normal,
        StartRecalculating,
        Recalculating,
        EndRecalculating,
        StartRedrawing;

        public boolean canAddCharInfo() {
            return this == Normal || this == Recalculating;
        }
    }
}
