package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ParagraphDrawer {
    private final PagePainter pagePainter;
    private final DrawingInfo info;

    private final TextLineDrawer textLineDrawer;
    private final ControlDrawer controlDrawer;
    private final WordSplitter wordSplitter;
    private final TextFlowCalculator textFlowCalculator;
    private boolean cancelNewLine;

    private DrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;
    private boolean firstLine;
    private long height;

    private final Map<Integer, CharInfo> charInfoBuffer;

    private Area currentTextLineArea;
    private long lineHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;
    private Area storedTextLineArea;

    private int controlExtendCharIndex;

    private final Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(DrawingInfo info) {
        this(null, info);
    }

    public ParagraphDrawer(PagePainter pagePainter, DrawingInfo info) {
        this.pagePainter = pagePainter;
        this.info = info;

        textLineDrawer = new TextLineDrawer(info);
        controlDrawer = new ControlDrawer(info);
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
            long lineGap = lineHeight - textLineDrawer.maxCharHeight();
            endY -= lineGap;
            height -= lineGap;
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
                    controlExtend(info.character());
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

    private void controlExtend(HWPChar character) {
        if (character.getCode() == 2) {
            Control control = info.paragraph().getControlList().get(controlExtendCharIndex);
            if (control.getType() == ControlType.ColumnDefine) {
                // todo : 단 정의
            }
            controlExtendCharIndex++;
        } else if (character.getCode() == 11) {
            controlExtend(controlCharInfo((HWPCharControlExtend) character));
        } else if (character.getCode() == 16) {
            Control control = info.paragraph().getControlList().get(controlExtendCharIndex);
            if (control.getType() == ControlType.Header) {
                ControlHeader header = (ControlHeader) control;
                switch (header.getHeader().getApplyPage()) {
                    case BothPage:
                        info.pageInfo().bothHeader(header);
                        break;
                    case EvenPage:
                        info.pageInfo().evenHeader(header);
                        break;
                    case OddPage:
                        info.pageInfo().oddHeader(header);
                        break;
                }
            } else if (control.getType() == ControlType.Footer) {
                ControlFooter footer = (ControlFooter) control;
                switch (footer.getHeader().getApplyPage()) {
                    case BothPage:
                        info.pageInfo().bothFooter(footer);
                        break;
                    case EvenPage:
                        info.pageInfo().evenFooter(footer);
                        break;
                    case OddPage:
                        info.pageInfo().oddFooter(footer);
                        break;
                }
            }
            controlExtendCharIndex++;
        } else {
            controlExtendCharIndex++;
        }
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

    private void addControlChar(ControlCharInfo controlCharInfo) throws Exception {
        ControlOutput output = controlDrawer.draw(controlCharInfo);
        controlCharInfo.output(output);

        if (!controlCharInfo.isLikeLetter()) {
            if (controlCharInfo.textFlowMethod() == 0/*어울림*/) {
                textFlowCalculator.addForSquare(controlCharInfo);
            } else if (controlCharInfo.textFlowMethod() == 1/*자리차지*/) {
                textFlowCalculator.addForTopBottom(controlCharInfo);
            }
            info.output().addChildOutput(output);
        } else {
            textLineDrawer.addChar(controlCharInfo);
        }
    }

    private void checkNewPage() throws Exception {
        if (info.isBodyText()
                && isOverBottom(textLineDrawer.maxCharHeight())) {
            pagePainter.saveCurrentPage();
            info.newPage();

            currentTextLineArea.top(info.pageInfo().bodyArea().top());

            if (textLineDrawer.noDrawingCharacter()) {
                textLineDrawer
                        .addNewTextPart(currentTextLineArea);
            } else {
                textLineDrawer.area(currentTextLineArea);
                saveTextLine();
                nextArea();
            }

            wordSplitter.adjustControlAreaAtNewPage();
            textFlowCalculator.reset();
        }
    }

    private boolean isOverBottom(long height) {
        return info.pageInfo().bodyArea().bottom() - (currentTextLineArea.top() + height) < 0;
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
        if (drawingState == DrawingState.Normal && !info.noText()) {
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
                textLineDrawer.saveToOutput();
                if (newLineAtNormal) {
                    info.output().setLastTextPartToLastLine();
                }
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0 || newLineAtRecalculating) {
                    textLineDrawer.saveToOutput();
                    if (newLineAtRecalculating) {
                        info.output().setLastTextPartToLastLine();
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
                if (firstLine) {
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

                if (firstLine) {
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
