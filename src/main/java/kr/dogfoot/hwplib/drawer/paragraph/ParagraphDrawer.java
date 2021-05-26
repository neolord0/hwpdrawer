package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.*;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class ParagraphDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;

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

    private final CharInfoBuffer charInfoBuffer;

    private Area currentTextLineArea;
    private long lineHeight;
    private int lineFirstCharIndex;
    private int lineFirstCharPosition;
    private Area storedTextLineArea;

    private int controlExtendCharIndex;

    private final Queue<Area> recalculatingTextAreas;

    public ParagraphDrawer(DrawingInput input, InterimOutput output) {
        this(input, output, null);
    }

    public ParagraphDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;

        textLineDrawer = new TextLineDrawer(input, output);
        controlDrawer = new ControlDrawer(input, output);
        wordSplitter = new WordSplitter(input, this, textLineDrawer);

        textFlowCalculator = new TextFlowCalculator();

        charInfoBuffer = new CharInfoBuffer();
        recalculatingTextAreas = new LinkedList<>();
    }

    public void draw(boolean redraw) throws Exception {
        if (redraw == false) {
            input.startPara();
        }

        initialize();

        drawingState = DrawingState.Normal;
        newLineAtRecalculating = false;
        newLineAtNormal = false;

        processDividePageOrColumn();

        if (input.noText()) {
            saveTextLineAndNewLine();
        } else {
            processChar();
        }


        long endY = currentTextLineArea.top() - input.paraArea().top();
        if (input.currentPara().getHeader().isLastInList()) {
            long lineGap = lineHeight - textLineDrawer.maxCharHeight();
            endY -= lineGap;
            height -= lineGap;
        }

        output.setLastTextPartToLastLine();
        input.endPara(endY, height);
    }

    private void initialize() {
        charInfoBuffer.clear();

        wordSplitter
                .resetWord();

        currentTextLineArea = new Area(input.paraArea()).height(0);

        if (input.paraShape().getIndent() > 0) {
            currentTextLineArea.left(currentTextLineArea.left() + input.paraShape().getIndent() / 2);
        }

        textLineDrawer
                .initialize(input.paraIndex(), currentTextLineArea)
                .addNewTextPart(0, currentTextLineArea.width());
        firstLine = true;
        height = 0;

        controlExtendCharIndex = 0;

        recalculatingTextAreas.clear();
    }

    private void processDividePageOrColumn() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();
        if (divideSort.isDividePage()) {
            newPage();
        } else if (divideSort.isDivideColumn()) {
            if (input.pageInfo().lastColumn()) {
                newPage();
            } else {
                newColumn();
            }
        }
    }

    private void processChar() throws Exception {
        while (input.nextChar()) {
            switch (input.currentChar().getType()) {
                case Normal:
                    normalChar(normalCharInfo((HWPCharNormal) input.currentChar()));
                    break;
                case ControlChar:
                    controlChar((HWPCharControlChar) input.currentChar());
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    controlExtend(input.currentChar());
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
        NormalCharInfo charInfo = (NormalCharInfo) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            charInfo = new NormalCharInfo(ch, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition())
                    .calculateWidth();
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
        }
        return charInfo;
    }

    private void controlExtend(HWPChar character) {
        if (character.getCode() == 2) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            if (control.getType() == ControlType.ColumnDefine) {
                // todo : 단 정의
            }
            controlExtendCharIndex++;
        } else if (character.getCode() == 11) {
            controlExtend(controlCharInfo((HWPCharControlExtend) character));
        } else if (character.getCode() == 16) {
            headerFooter();
            controlExtendCharIndex++;
        } else {
            controlExtendCharIndex++;
        }
    }

    private ControlCharInfo controlCharInfo(HWPCharControlExtend ch) {
        ControlCharInfo charInfo = (ControlCharInfo) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = ControlCharInfo.create(ch, control, input);
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
            controlExtendCharIndex++;
        }
        return charInfo;
    }

    private void headerFooter() {
        Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
        if (control.getType() == ControlType.Header) {
            ControlHeader header = (ControlHeader) control;
            switch (header.getHeader().getApplyPage()) {
                case BothPage:
                    input.pageInfo().bothHeader(header);
                    break;
                case EvenPage:
                    input.pageInfo().evenHeader(header);
                    break;
                case OddPage:
                    input.pageInfo().oddHeader(header);
                    break;
            }
        } else if (control.getType() == ControlType.Footer) {
            ControlFooter footer = (ControlFooter) control;
            switch (footer.getHeader().getApplyPage()) {
                case BothPage:
                    input.pageInfo().bothFooter(footer);
                    break;
                case EvenPage:
                    input.pageInfo().evenFooter(footer);
                    break;
                case OddPage:
                    input.pageInfo().oddFooter(footer);
                    break;
            }
        }
    }


    private void startRecalculating() {
        wordSplitter.resetWord();

        currentTextLineArea = recalculatingTextAreas.poll();

        long partStartX = currentTextLineArea.left() - storedTextLineArea.left();
        textLineDrawer
                .reset(input.paraIndex(), storedTextLineArea)
                .addNewTextPart(partStartX, currentTextLineArea.width());

        input.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }

    private void startRedraw() {
        wordSplitter.resetWord();

        textLineDrawer
                .reset(input.paraIndex(), currentTextLineArea)
                .addNewTextPart(0, currentTextLineArea.width());

        input.gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
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
        if (!textLineDrawer.isOverWidth(wordSplitter.wordWidth(), false)) {
            addWordAllCharsToLine(wordSplitter.charsOfWord(), false, false);
            addSpaceCharToLine(spaceCharInfo);
            wordSplitter.resetWord();
        } else {
            if (!textLineDrawer.isOverWidth(wordSplitter.wordWidth(), true)) {
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
        if (checkOverRight && textLineDrawer.isOverWidth(charInfo.width(), applyMinimumSpace)) {
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
                checkNewColumnAndPage();
                setLineFirst(charInfo.index(), (charInfo.position() - charInfo.character().getCharSize()));
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
        ControlOutput output2 = controlDrawer.draw(controlCharInfo);
        controlCharInfo.output(output2);

        if (controlCharInfo.isLikeLetter()) {
            textLineDrawer.addChar(controlCharInfo);
            return;
        }

        if (controlCharInfo.textFlowMethod() == TextFlowMethod.FitWithText
                || controlCharInfo.textFlowMethod() == TextFlowMethod.TakePlace) {
            if (!textFlowCalculator.alreadyAdded(controlCharInfo)) {
                if (output.checkRedrawingTextLine(controlCharInfo.areaWithOuterMargin())) {
                    if ((currentTextLineArea.bottom() - input.pageInfo().bodyArea().top()) < (input.pageInfo().bodyArea().height() * 75 / 100)) {
                        textFlowCalculator.add(controlCharInfo);
                        output.addChildOutput(output2);

                        TextLine firstRedrawingTextLine = output.deleteRedrawingTextLine(controlCharInfo.areaWithOuterMargin());
                        throw new RedrawException(firstRedrawingTextLine.paraIndex(),
                            firstRedrawingTextLine.firstCharIndex(),
                            firstRedrawingTextLine.firstCharPosition(),
                            firstRedrawingTextLine.area().top());
                    } else {
                        output.addControlMovedToNextPage(output2, controlCharInfo);
                    }
                } else {
                    textFlowCalculator.add(controlCharInfo);
                    output.addChildOutput(output2);
                }
            }
        } else {
            output.addChildOutput(output2);
        }
    }

    private void checkNewColumnAndPage() throws Exception {
        if (input.isBodyText()
                && isOverBottom(textLineDrawer.maxCharHeight())) {
            if (input.pageInfo().lastColumn()) {
                newPage();
            } else {
                newColumn();
            }
        }
    }

    private void newPage() throws Exception {
        pagePainter.saveCurrentPage();

        input.newPage();
        processAtNewColumnOrPage();

        charInfoBuffer.clearUntilPreviousPara();

        output.newPageOutput(input.pageInfo());
    }

    private void newColumn() {
        input.newColumn();
        processAtNewColumnOrPage();
    }

    private void processAtNewColumnOrPage() {
        currentTextLineArea = new Area(input.paraArea());
        if (firstLine == true) {
            if (input.paraShape().getIndent() > 0) {
                currentTextLineArea.left(currentTextLineArea.left() + input.paraShape().getIndent() / 2);
            }
        } else {
            currentTextLineArea.left(currentTextLineArea.left() - input.paraShape().getIndent() / 2);
        }

        if (textLineDrawer.noDrawingCharacter()) {
            textLineDrawer
                    .clearTextLine()
                    .addNewTextPart(0, currentTextLineArea.width());
        } else {
            textLineDrawer.textLineArea(new Area(currentTextLineArea));
            saveTextLine();
            nextArea();
        }

        wordSplitter.adjustControlAreaAtNewPage();
        textFlowCalculator.reset();

        if (output.hasControlMovedToNextPage()) {
            for (InterimOutput.ControlInfo controlInfo : output.controlsMovedToNextPage())
            textFlowCalculator.add(controlInfo.charInfo());
        }
    }

    private boolean isOverBottom(long height) {
        return input.pageInfo().columnArea().bottom() - (currentTextLineArea.top() + height) < 0;
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
            checkNewColumnAndPage();

            if (textLineDrawer.noDrawingCharacter() && input.checkHidingEmptyLineAfterNewPage()) {
                input.descendCountOfHidingEmptyLineAfterNewPage();
            } else {
                input.resetCountOfHidingEmptyLineAfterNewPage();

                saveTextLine();
                nextArea();
            }

            newLineAtRecalculating = false;
            newLineAtNormal = false;
        }
    }

    private void checkTextFlow() {
        if (drawingState == DrawingState.Normal && !input.noText()) {
            currentTextLineArea
                    .height(textLineDrawer.maxCharHeight());
            TextFlowCalculator.Result result = textFlowCalculator.calculate(currentTextLineArea);
            currentTextLineArea
                    .moveY(result.offsetY());
            cancelNewLine = result.cancelNewLine() && textLineDrawer.noDrawingCharacter();

            textLineDrawer.textLineArea(new Area(currentTextLineArea));
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
                    output.setLastTextPartToLastLine();
                }
                break;
            case Recalculating:
                if (recalculatingTextAreas.size() == 0 || newLineAtRecalculating) {
                    textLineDrawer.saveToOutput();
                    if (newLineAtRecalculating) {
                        output.setLastTextPartToLastLine();
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

                    currentTextLineArea.left(currentTextLineArea.left() - input.paraShape().getIndent() / 2);
                    firstLine = false;
                }
                if (!cancelNewLine) {
                    currentTextLineArea.moveY(lineHeight);
                    height += lineHeight;
                }
                textLineDrawer
                        .reset(input.paraIndex(), currentTextLineArea)
                        .addNewTextPart(0, currentTextLineArea.width());
                break;
            case Recalculating:
                currentTextLineArea = recalculatingTextAreas.poll();
                long partStartX = currentTextLineArea.left() - storedTextLineArea.left();
                textLineDrawer
                        .addNewTextPart(partStartX, currentTextLineArea.width());
                break;
            case EndRecalculating:
                currentTextLineArea = storedTextLineArea.moveY(lineHeight);
                height += lineHeight;

                if (firstLine) {
                    currentTextLineArea.left(currentTextLineArea.left() - input.paraShape().getIndent() / 2);
                    firstLine = false;
                }

                textLineDrawer
                        .reset(input.paraIndex(), currentTextLineArea)
                        .addNewTextPart(0, currentTextLineArea.width());
                break;
        }
    }

    private void spanningWord(NormalCharInfo spaceCharInfo) throws Exception {
        ArrayList<CharInfo> charsOfWord = wordSplitter.charsOfWord();

        if (isAllLineDivideByWord(input.paraShape())) {
            if (!textLineDrawer.noDrawingCharacter()) {
                saveTextLineAndNewLine();
            }

            if (drawingState == DrawingState.EndRecalculating) {
                input.beforeChar(charsOfWord.size() + 1);
            }
            addWordAllCharsToLine(charsOfWord, true, false);
        } else {
            int countOfAddingBeforeNewLine = wordSplitter.split();
            if (drawingState == DrawingState.EndRecalculating) {
                input.beforeChar(charsOfWord.size() - countOfAddingBeforeNewLine + 1);
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
