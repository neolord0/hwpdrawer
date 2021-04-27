package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ParagraphDrawer {
    private PagePainter pagePainter;
    private DrawingInfo info;

    private TextLineDrawer textLineDrawer;
    private WordSplitter wordSplitter;
    private TextFlowCalculator textFlowCalculator;
    private boolean cancelNewLine;

    private DrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;
    private boolean firstLine;
    private long height;

    private Map<Integer, CharInfo> charInfoBuffer;

    private Area currentTextLineArea;
    private long lineHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;
    private Area storedTextLineArea;

    private int controlExtendCharIndex;

    private Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(DrawingInfo info) {
        this(null, info);
    }

    public ParagraphDrawer(PagePainter pagePainter, DrawingInfo info) {
        this.pagePainter = pagePainter;
        this.info = info;

        textLineDrawer = new TextLineDrawer(info);
        wordSplitter = new WordSplitter(this, textLineDrawer, info);

        textFlowCalculator = new TextFlowCalculator();

        charInfoBuffer = new HashMap<>();
        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(Paragraph paragraph) throws Exception {
        info.startParagraph(paragraph);

        initialize();

        drawingState = DrawingState.Normal;
        newLineAtRecalculating = false;
        newLineAtNormal = false;

        if (info.noText()) {
            saveTextLineAndNewLine();
        } else {
            processChar();
        }

        long endY = currentTextLineArea.top() - info.paragraphArea().top();
        if (paragraph.getHeader().isLastInList()) {
            endY -= (lineHeight - textLineDrawer.maxCharHeight());
            height -= (lineHeight - textLineDrawer.maxCharHeight());
        }

        info.endParagraph(endY, height);
    }

    private void initialize() {
        charInfoBuffer.clear();

        wordSplitter
                .resetWord();

        currentTextLineArea = new Area(info.paragraphArea()).height(0);

        if (info.paraShape().getIndent() > 0) {
            currentTextLineArea.left(currentTextLineArea.left() + info.paraShape().getIndent() / 2);
        }

        textLineDrawer
                .initialize()
                .addNewTextPart(currentTextLineArea);
        firstLine = true;
        height = 0;

        controlExtendCharIndex = 0;

        recalculatingTextAreas.clear();
    }

    private void processChar() throws Exception {
        while (info.nextChar()) {
            switch (info.character().getType()) {
                case Normal:
                    normalChar(normalCharInfo((HWPCharNormal) info.character()));
                    break;
                case ControlChar:
                    controlChar((HWPCharControlChar) info.character());
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    controlExtend(controlCharInfo((HWPCharControlExtend) info.character()));
                    break;
            }

            switch (drawingState) {
                case StartRecalculating:
                    startRecalculating();
                    drawingState = DrawingState.Recalculating;
                    break;
                case EndRecalculating:
                    drawingState = DrawingState.Normal;
                    break;
                case StartRedrawing:
                    startRedraw();
                    drawingState = DrawingState.Normal;
                    break;
            }
        }
    }

    public NormalCharInfo normalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        NormalCharInfo charInfo = (NormalCharInfo) charInfoBuffer.get(info.charIndex());
        if (charInfo == null) {
            charInfo = new NormalCharInfo(ch, info.charShape(), info.charIndex(), info.charPosition())
                    .calculateWidth();
            charInfoBuffer.put(info.charIndex(), charInfo);
        }
        return charInfo;
    }

    private ControlCharInfo controlCharInfo(HWPCharControlExtend ch) {
        ControlCharInfo charInfo = (ControlCharInfo) charInfoBuffer.get(info.charIndex());
        if (charInfo == null) {
            Control control = info.paragraph().getControlList().get(controlExtendCharIndex);

            charInfo = ControlCharInfo.create(ch, control, info);
            charInfoBuffer.put(info.charIndex(), charInfo);
            controlExtendCharIndex++;
        }
        return charInfo;
    }

    private void startRecalculating() {
        wordSplitter.resetWord();

        currentTextLineArea = recalculatingTextAreas.poll();
        textLineDrawer
                .reset()
                .addNewTextPart(currentTextLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void startRedraw() {
        wordSplitter.resetWord();

        textLineDrawer
                .reset()
                .addNewTextPart(currentTextLineArea);

        info.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void normalChar(NormalCharInfo charInfo) throws Exception {
        if (!charInfo.character().isSpace()) {
            wordSplitter.addCharOfWord(charInfo);
        } else {
            addWordToLine(charInfo);
        }
    }

    private void addWordToLine(NormalCharInfo spaceCharInfo) throws Exception {
        if (wordSplitter.noChar()) {
            addSpaceCharToLine(spaceCharInfo);
            return;
        }
        if (!textLineDrawer.isOverRight(wordSplitter.wordWidth(), false)) {
            addWordAllCharsToLine(wordSplitter.charsOfWord(), false, false);
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            if (!textLineDrawer.isOverRight(wordSplitter.wordWidth(), true)) {
                addWordAllCharsToLine(wordSplitter.charsOfWord(), false, true);
                wordSplitter.resetWord();

                textLineDrawer.setBestSpaceRate();
                saveTextLineAndNewLine();
            } else {
                spanningWord(spaceCharInfo);
            }
        }
    }

    public void addSpaceCharToLine(NormalCharInfo spaceCharInfo) {
        if (drawingState.canAddChar() && spaceCharInfo != null) {
            textLineDrawer.addChar(spaceCharInfo);
        }
    }

    private void addWordAllCharsToLine(ArrayList<CharInfo> wordChars, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        for (CharInfo charInfo : wordChars) {
            addCharToLine(charInfo, checkOverRight, applyMinimumSpace);
        }
    }

    public boolean addCharToLine(CharInfo charInfo, boolean checkOverRight, boolean applyMinimumSpace) throws Exception {
        boolean hasNewLine;
        if (checkOverRight && textLineDrawer.isOverRight(charInfo.width(), applyMinimumSpace)) {
            if (applyMinimumSpace) {
                textLineDrawer.setBestSpaceRate();
            }

            saveTextLineAndNewLine();

            if (drawingState == DrawingState.Normal || drawingState == DrawingState.EndRecalculating) {
                hasNewLine = true;
            } else {
                hasNewLine = false;
            }
        } else {
            hasNewLine = false;
        }
        textLineDrawer.justNewLine(false);

        if (drawingState.canAddChar()) {
            if (textLineDrawer.noDrawingCharacter() && drawingState == DrawingState.Normal) {
                checkNewPage();
                setLineFirst((charInfo.index() - 1), (charInfo.position() - charInfo.character().getCharSize()));
            }

            if (drawingState == DrawingState.Normal && charInfo.type() == CharInfo.Type.Control) {
                addControlChar((ControlCharInfo) charInfo);
            } else {
                textLineDrawer.addChar(charInfo);
            }
        }

        return hasNewLine;
    }

    private void addControlChar(ControlCharInfo controlCharInfo) {
        if (controlCharInfo.isLikeLetter() == false) {
            if (controlCharInfo.textFlowMethod() == 0/*어울림*/) {
                textFlowCalculator.addForSquare(controlCharInfo);
            } else if (controlCharInfo.textFlowMethod() == 1/*자리차지*/) {
                textFlowCalculator.addForTopBottom(controlCharInfo);
            }

            if (controlCharInfo.textFlowMethod() == 2/*뒤로*/) {
                info.contentBuffer().addBehindControl(controlCharInfo);
            } else {
                info.contentBuffer().addNotBehindControl(controlCharInfo);
            }
        } else {
            textLineDrawer.addChar(controlCharInfo);
        }
    }

    private void checkNewPage() throws Exception {
        if (info.isBodyText()
                && isOverBottom(textLineDrawer.maxCharHeight())) {
            pagePainter.saveCurrentPage();
            info.newPage();

            currentTextLineArea.top(info.pageArea().top());

            textLineDrawer
                    .addNewTextPart(currentTextLineArea);
            wordSplitter.adjustControlAreaAtNewPage();
            textFlowCalculator.reset();
        }
    }

    private boolean isOverBottom(long height) {
        return info.pageArea().bottom() - (currentTextLineArea.top() + height) < 0;
    }

    private void setLineFirst(int index, int position) {
        lineFirstCharIndex = index;
        lineFirstCharPosition = position;
    }

    public void saveTextLineAndNewLine() throws Exception {
        if (!textLineDrawer.justNewLine() && drawingState.canAddChar()) {
            cancelNewLine = false;
            lineHeight = textLineDrawer.lineHeight();

            checkTextFlow();
            checkNewPage();

            if (textLineDrawer.noDrawingCharacter() && info.checkHidingEmptyLineAfterNewPage()) {
                info.descendCountOfHidingEmptyLineAfterNewPage();
            } else {
                info.resetCountOfHidingEmptyLineAfterNewPage();

                saveTextLine();
                nextArea();
            }

            newLineAtRecalculating = false;
            newLineAtNormal = false;
        }
    }

    private void checkTextFlow() {
        if (drawingState == DrawingState.Normal && info.noText() == false) {
            currentTextLineArea
                    .height(textLineDrawer.maxCharHeight());
            TextFlowCalculator.Result result = textFlowCalculator.calculate(currentTextLineArea);
            currentTextLineArea
                    .moveY(result.offsetY());
            cancelNewLine = result.cancelNewLine() && textLineDrawer.noDrawingCharacter();

            textLineDrawer.area(currentTextLineArea);
            drawingState = result.nextState();

            if (drawingState == DrawingState.StartRecalculating) {
                storedTextLineArea = currentTextLineArea;
                for (Area dividedArea : result.dividedAreas()) {
                    recalculatingTextAreas.offer(dividedArea);
                }
            }
        }
    }

    private void saveTextLine() {
        switch (drawingState) {
            case Normal:
                textLineDrawer.saveToContentBuffer();
                if (newLineAtNormal) {
                    info.contentBuffer().setLastTextPartToLastLine();
                }
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0 || newLineAtRecalculating) {
                    textLineDrawer.saveToContentBuffer();
                    if (newLineAtRecalculating) {
                        info.contentBuffer().setLastTextPartToLastLine();
                    }
                    drawingState = DrawingState.EndRecalculating;
                }
                break;
        }
    }

    private void nextArea() {
        switch (drawingState) {
            case Normal:
            case StartRedrawing:
                if (firstLine == true) {
                    currentTextLineArea.left(currentTextLineArea.left() - info.paraShape().getIndent() / 2);
                    firstLine = false;
                }
                if (!cancelNewLine) {
                    currentTextLineArea.moveY(lineHeight);
                    height += lineHeight;
                }
                textLineDrawer
                        .reset()
                        .addNewTextPart(currentTextLineArea);
                break;
            case Recalculating:
                currentTextLineArea = recalculatingTextAreas.poll();
                textLineDrawer
                        .resetPart()
                        .addNewTextPart(currentTextLineArea);
                break;
            case EndRecalculating:
                currentTextLineArea = storedTextLineArea.moveY(lineHeight);
                height += lineHeight;

                if (firstLine == true) {
                    currentTextLineArea.left(currentTextLineArea.left() - info.paraShape().getIndent() / 2);
                    firstLine = false;
                }

                textLineDrawer
                        .reset()
                        .addNewTextPart(currentTextLineArea);
                break;
        }
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        ArrayList<CharInfo> charsOfWord = wordSplitter.charsOfWord();

        if (isAllLineDivideByWord(info.paraShape())) {
            if (!textLineDrawer.noDrawingCharacter()) {
                saveTextLineAndNewLine();
            }

            if (drawingState == DrawingState.EndRecalculating) {
                info.beforeChar(charsOfWord.size() + 1);
            }
            addWordAllCharsToLine(charsOfWord, true, false);
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split();
            if (drawingState == DrawingState.EndRecalculating) {
                info.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
            }
        }
        addSpaceCharToLine(spaceCharInfo);
        wordSplitter.resetWord();
    }

    private boolean isAllLineDivideByWord(ParaShape paraShape) {
        return paraShape.getProperty1().getLineDivideForEnglish() == LineDivideForEnglish.ByWord
                && paraShape.getProperty1().getLineDivideForHangul() == LineDivideForHangul.ByWord;
    }

    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            addWordToLine(null);
            if (drawingState == DrawingState.Recalculating) {
                newLineAtRecalculating = true;
            } else if (drawingState == DrawingState.Normal) {
                newLineAtNormal = true;
            }
            saveTextLineAndNewLine();
        }
    }

    private void controlExtend(ControlCharInfo charInfo) {
        if (charInfo.character().getCode() == 11) {
            wordSplitter.addCharOfWord(charInfo);
        }
    }

    public enum DrawingState {
        Normal,
        StartRecalculating,
        Recalculating,
        EndRecalculating,
        StartRedrawing;

        public boolean canAddChar() {
            return this == Normal || this == Recalculating;
        }
    }
}
