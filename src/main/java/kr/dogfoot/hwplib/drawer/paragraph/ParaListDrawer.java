package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.*;

import java.io.UnsupportedEncodingException;

public class ParaListDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;

    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final ControlDrawer controlDrawer;
    private final TextFlowCalculator textFlowCalculator;
    private final DistributionColumnProcessor distributionColumnProcessor;

    private final CharInfoBuffer charInfoBuffer;

    private boolean firstLine;
    private boolean cancelNewLine;

    private Area currentTextPartArea;
    private long paraHeight;

    private TextFlowCalculationResult textFlowCalculationResult;
    private DrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;

    private int controlExtendCharIndex;

    private boolean forDistributionColumn;

    public ParaListDrawer(DrawingInput input, InterimOutput output) {
        this(input, output, null);
    }

    public ParaListDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;

        textLineDrawer = new TextLineDrawer(input, output);
        wordDrawer = new WordDrawer(input, this, textLineDrawer);
        controlDrawer = new ControlDrawer(input, output);
        textFlowCalculator = new TextFlowCalculator();
        distributionColumnProcessor = new DistributionColumnProcessor(input, output, this);

        charInfoBuffer = new CharInfoBuffer();
        textFlowCalculationResult = null;
    }

    public void drawForBodyText(ParagraphListInterface paraList) throws Exception {
        input.startBodyTextParaList(paraList.getParagraphs());

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paragraph(redraw);
                redraw = false;
            } catch (BreakingDrawException e) {
                if (e.type().isForRedrawing()) {
                    input.gotoParaCharPosition(e.paraIndex(), e.charIndex(), e.charPosition());
                    input.currentParaListInfo().resetParaStartY(e.startY());
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (input.columnsInfo().isDistributionMultiColumn()
                && !input.columnsInfo().lastColumn()
                && !output.currentOutput().content().rearrangedForDistributionColumn()) {
            distributionColumnProcessor.test();
        }

        input.endBodyTextParaList();
    }

    public long drawForControl(ParagraphListInterface paraList, Area textArea) throws Exception {
        input.startControlParaList(textArea, paraList.getParagraphs());
        while (input.nextPara()) {
            paragraph(false);
        }
        return input.endControlParaList();
    }

    public void redrawParaList() throws Exception {
        boolean redraw = true;
        while (redraw || input.nextPara()) {
            try {
                paragraph(redraw);
                redraw = false;
            } catch (BreakingDrawException e) {
                if (e.type().isForRedrawing()) {
                    redraw = true;
                } else if (e.type().isForDistributionColumn()) {
                    throw e;
                }
            }
        }

        if (input.columnsInfo().isDistributionMultiColumn()
                && !input.columnsInfo().lastColumn()
                && !output.currentOutput().content().rearrangedForDistributionColumn()) {
            distributionColumnProcessor.test();
        }
    }

    public void paragraph(boolean redraw) throws Exception {
        if (redraw == false) {
            input.startPara();
        }

        resetForNewPara();

        processDividePageOrColumn();

        if (input.noText()) {
            saveTextLineAndNewLine();
        } else {
            chars();
        }

        long endY = currentTextPartArea.top() - input.paraArea().top();
        if (input.currentPara().getHeader().isLastInList()) {
            long lineGap = textLineDrawer.lineHeight() - textLineDrawer.maxCharHeight();
            endY -= lineGap;
            paraHeight -= lineGap;
        }

        output.setLastTextPartToLastLine();
        input.endPara(endY, paraHeight);
    }

    private void resetForNewPara() {
        firstLine = true;

        currentTextPartArea = new Area(input.paraArea()).height(0);
        applyIndent();

        textLineDrawer
                .initialize(input.paraIndex(), currentTextPartArea)
                .addNewTextPart(0, currentTextPartArea.width());

        wordDrawer
                .reset();

        paraHeight = 0;

        drawingState = DrawingState.Normal;
        newLineAtRecalculating = false;
        newLineAtNormal = false;

        controlExtendCharIndex = 0;
    }

    private void applyIndent() {
        if (firstLine) {
            if (input.paraShape().getIndent() > 0) {
                currentTextPartArea.left(currentTextPartArea.left() + input.paraShape().getIndent() / 2);
            }
        } else {
            currentTextPartArea.left(currentTextPartArea.left() - input.paraShape().getIndent() / 2);
        }
    }

    private void processDividePageOrColumn() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();
        if (divideSort.isDividePage()) {
            newPage();
        } else if (divideSort.isDivideColumn()) {
            if (input.columnsInfo().lastColumn()) {
                newPage();
            } else {
                nextColumn();
            }
        }
    }

    private void chars() throws Exception {
        while (input.nextChar()) {
            switch (input.currentChar().getType()) {
                case Normal:
                    normalChar();
                    break;
                case ControlChar:
                    controlChar();
                    break;
                case ControlInline:
                    break;
                case ControlExtend:
                    controlExtend();
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

    private void normalChar() throws Exception {
        NormalCharInfo charInfo = normalCharInfo((HWPCharNormal) input.currentChar());
        if (!charInfo.character().isSpace()) {
            wordDrawer.addCharOfWord(charInfo);
        } else {
            wordDrawer.addWordToLine(charInfo);
        }
    }

    private NormalCharInfo normalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        NormalCharInfo charInfo = (NormalCharInfo) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            charInfo = new NormalCharInfo(ch, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition())
                    .calculateWidth();
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
        }
        return charInfo;
    }

    private void controlChar() throws Exception {
        if (input.currentChar().isParaBreak()
                || input.currentChar().isLineBreak()) {
            wordDrawer.addWordToLine(null);

            if (drawingState.isRecalculating()) {
                newLineAtRecalculating = true;
            } else if (drawingState.isNormal()) {
                newLineAtNormal = true;
            }
            saveTextLineAndNewLine();
        }
    }

    private void controlExtend() {
        if (input.currentChar().getCode() == 2) {
            columnDefine();
        } else if (input.currentChar().getCode() == 11) {
            tableOrGso();
        } else if (input.currentChar().getCode() == 16) {
            headerFooter();
        } else {
            controlExtendCharIndex++;
        }
    }

    private void columnDefine() {
        Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
        if (control.getType() == ControlType.ColumnDefine) {
            // todo
        }
        controlExtendCharIndex++;
    }

    private void tableOrGso() {
        ControlCharInfo charInfo = controlCharInfo((HWPCharControlExtend) input.currentChar());
        wordDrawer.addCharOfWord(charInfo);
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
        controlExtendCharIndex++;
    }

    private void startRecalculating() {
        currentTextPartArea = textFlowCalculationResult.nextArea();
        textLineDrawer
                .reset(input.paraIndex(), textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());

        wordDrawer.reset();
        input.gotoCharPosition(textLineDrawer.firstCharInfo().index(), textLineDrawer.firstCharInfo().prePosition());
    }

    private void startRedraw() {
        textLineDrawer
                .reset(input.paraIndex(), currentTextPartArea)
                .addNewTextPart(0, currentTextPartArea.width());

        wordDrawer.reset();
        input.gotoCharPosition(textLineDrawer.firstCharInfo().index(), textLineDrawer.firstCharInfo().prePosition());
    }

    public void addControlChar(ControlCharInfo controlCharInfo) throws Exception {
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
                    if ((currentTextPartArea.bottom() - input.pageInfo().bodyArea().top()) < (input.pageInfo().bodyArea().height() * 75 / 100)) {
                        textFlowCalculator.add(controlCharInfo);
                        output.addChildOutput(output2);

                        TextLine firstRedrawingTextLine = output.deleteRedrawingTextLine(controlCharInfo.areaWithOuterMargin());
                        throw new BreakingDrawException(firstRedrawingTextLine.paraIndex(),
                            firstRedrawingTextLine.firstChar().index(),
                            firstRedrawingTextLine.firstChar().position(),
                            firstRedrawingTextLine.area().top()).forRedrawing();
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

    public void checkNewColumnAndPage() throws Exception {
        if (input.isBodyText()
                && isOverBottom(textLineDrawer.maxCharHeight())) {
            if (input.columnsInfo().lastColumn()) {
                newPage();
            } else {
                if (input.columnsInfo().isDistributionMultiColumn()
                        && !input.columnsInfo().lastColumn()) {
                    distributionColumnProcessor.test();
                    wordDrawer.stopAddingChar();
                } else {
                    nextColumn();
                }
            }
        } else {
            if (!input.columnsInfo().lastColumn() &&
                    input.columnsInfo().limitedTextLineCount() != -1 &&
                    output.currentOutput().content().textLineCount() >= input.columnsInfo().limitedTextLineCount()) {
                nextColumn();
            }
        }
    }

    private void newPage() throws Exception {
        if (forDistributionColumn) {
            throw new BreakingDrawException(input.paraIndex(), input.charIndex(), input.charPosition(), 0).forDistributionColumn();
        }
        pagePainter.saveCurrentPage();
        charInfoBuffer.clearUntilPreviousPara();

        input.newPage();
        resetForNewColumnOrPage();
        output.newPageOutput(input);
    }

    public void nextColumn() {
        input.nextColumn();
        resetForNewColumnOrPage();
        output.currentOutput().content().nextColumn();
    }

    public void previousColumn() {
        input.previousColumn();
        resetForNewColumnOrPage();
        output.currentOutput().content().previousColumn();
    }

    private void resetForNewColumnOrPage() {
        currentTextPartArea = new Area(input.paraArea());
        applyIndent();

        if (textLineDrawer.noDrawingChar()) {
            textLineDrawer
                    .clearTextLine()
                    .addNewTextPart(0, currentTextPartArea.width());
        } else {
            textLineDrawer.textLineArea().set(currentTextPartArea);
            saveTextLine();
            nextTextPartArea();
        }

        wordDrawer.adjustControlAreaAtNewPage();
        textFlowCalculator.reset();

        if (output.hasControlMovedToNextPage()) {
            for (InterimOutput.ControlInfo controlInfo : output.controlsMovedToNextPage()) {
                textFlowCalculator.add(controlInfo.charInfo());
            }
        }
    }

    private boolean isOverBottom(long height) {
        return input.columnsInfo().currentColumnArea().bottom() < currentTextPartArea.top() + height;
    }


    public void saveTextLineAndNewLine() throws Exception {
        if (!textLineDrawer.justNewLine() && drawingState.canAddChar()) {
            cancelNewLine = false;
            textLineDrawer.setLineHeight();

            checkTextFlow();
            checkNewColumnAndPage();

             if (textLineDrawer.noDrawingChar() && input.checkHidingEmptyLineAfterNewPage()) {
                input.descendCountOfHidingEmptyLineAfterNewPage();
            } else {
                input.resetCountOfHidingEmptyLineAfterNewPage();
                saveTextLine();
                nextTextPartArea();
            }

            newLineAtRecalculating = false;
            newLineAtNormal = false;
        }
    }

    private void checkTextFlow() {
        if (drawingState.isNormal() && !input.noText()) {
            currentTextPartArea
                    .height(textLineDrawer.maxCharHeight());

            textFlowCalculationResult = textFlowCalculator.calculate(currentTextPartArea);

            currentTextPartArea
                    .moveY(textFlowCalculationResult.offsetY());
            if (textFlowCalculationResult.nextState().isStartRecalculating()) {
                textFlowCalculationResult.storeTextLineArea(currentTextPartArea);
            }
            textLineDrawer.textLineArea().set(currentTextPartArea);

            cancelNewLine = textFlowCalculationResult.cancelNewLine() && textLineDrawer.noDrawingChar();
            drawingState = textFlowCalculationResult.nextState();

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
                if (textFlowCalculationResult.lastTextPart() || newLineAtRecalculating) {
                    textLineDrawer.saveToOutput();

                    if (newLineAtRecalculating) {
                        output.setLastTextPartToLastLine();
                    }

                    drawingState = DrawingState.EndRecalculating;
                }
                break;
        }
    }

    private void nextTextPartArea() {
        switch (drawingState) {
            case Normal:
            case StartRedrawing:
                if (!cancelNewLine) {
                    currentTextPartArea.moveY(textLineDrawer.lineHeight());
                    paraHeight += textLineDrawer.lineHeight();
                }
                restoreIndentAtFirstLine();

                textLineDrawer
                        .reset(input.paraIndex(), currentTextPartArea)
                        .addNewTextPart(0, currentTextPartArea.width());
                break;
            case Recalculating:
                currentTextPartArea = textFlowCalculationResult.nextArea();

                textLineDrawer
                        .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());
                break;
            case EndRecalculating:
                currentTextPartArea.set(textFlowCalculationResult.storedTextLineArea());

                currentTextPartArea.moveY(textLineDrawer.lineHeight());
                paraHeight += textLineDrawer.lineHeight();

                restoreIndentAtFirstLine();

                textLineDrawer
                        .reset(input.paraIndex(), currentTextPartArea)
                        .addNewTextPart(0, currentTextPartArea.width());

                break;
        }
    }

    private void restoreIndentAtFirstLine() {
        if (firstLine) {
            currentTextPartArea.left(currentTextPartArea.left() - input.paraShape().getIndent() / 2);
            firstLine = false;
        }
    }

    public DrawingState drawingState() {
        return drawingState;
    }

    public void forDistributionColumn(boolean forDistributionColumn) {
        this.forDistributionColumn = forDistributionColumn;
        if (forDistributionColumn == false) {
            wordDrawer.continueAddingChar();
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

        public boolean isNormal() {
            return this == Normal;
        }

        public boolean isEndRecalculating() {
            return this == EndRecalculating;
        }

        public boolean isStartRecalculating() {
            return this == StartRecalculating;
        }

        public boolean isRecalculating() {
            return this == Recalculating;
        }
    }
}
