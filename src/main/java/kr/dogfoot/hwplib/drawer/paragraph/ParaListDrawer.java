package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.*;

import java.io.UnsupportedEncodingException;

public class ParaListDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;

    private final TextFlowCalculator textFlowCalculator;

    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

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

    private boolean forDistributionMultiColumn;

    public ParaListDrawer(DrawingInput input, InterimOutput output) {
        this(input, output, null);
    }

    public ParaListDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;

        textFlowCalculator = new TextFlowCalculator();

        textLineDrawer = new TextLineDrawer(input, output);
        wordDrawer = new WordDrawer(input, output,this, textLineDrawer, textFlowCalculator);
        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this);

        charInfoBuffer = new CharInfoBuffer();
        currentTextPartArea = new Area();
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

        if (!output.hadRearrangedDistributionMultiColumn()) {
            rearrangeDistributionMultiColumn();
        }

        input.endBodyTextParaList();
    }

    private void rearrangeDistributionMultiColumn() throws Exception {
        if (input.columnsInfo().isDistributionMultiColumn()
                && !input.columnsInfo().lastColumn()) {
            distributionMultiColumnRearranger.rearrange();
        }
    }

    public long drawForControl(ParagraphListInterface paraList, Area textArea) throws Exception {
        input.startControlParaList(textArea, paraList.getParagraphs());
        while (input.nextPara()) {
            paragraph(false);
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            rearrangeDistributionMultiColumn();
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

        if (!output.hadRearrangedDistributionMultiColumn()) {
            rearrangeDistributionMultiColumn();
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

        output.setLastLineInPara();
        input.endPara(endY, paraHeight);
    }

    private void resetForNewPara() {
        firstLine = true;

        currentTextPartArea.set(input.paraArea()).height(0);
        applyIndent();

        textLineDrawer
                .initialize(currentTextPartArea)
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
                    startRecalculatingTextLine();
                    drawingState = DrawingState.Recalculating;
                    break;
                case EndRecalculating:
                    drawingState = DrawingState.Normal;
                    break;
                case StartRedrawing:
                    startRedrawingTextLine();
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
        ControlCharInfo charInfo = (ControlCharInfo) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = ControlCharInfo.create((HWPCharControlExtend) input.currentChar(), control, input);
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
            controlExtendCharIndex++;
        }

        wordDrawer.addCharOfWord(charInfo);
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

    private void startRecalculatingTextLine() {
        input.gotoCharPosition(textLineDrawer.firstCharInfo());

        currentTextPartArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .reset(textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());
        wordDrawer.reset();
    }

    private void startRedrawingTextLine() {
        input.gotoCharPosition(textLineDrawer.firstCharInfo());
        textLineDrawer
                .reset(currentTextPartArea)
                .addNewTextPart(0, currentTextPartArea.width());
        wordDrawer.reset();
    }

    public void checkNewColumnAndPage() throws Exception {
        if (input.isBodyText()
                && isOverBottom(textLineDrawer.maxCharHeight())) {
            if (input.columnsInfo().lastColumn()) {
                newPage();
            } else {
                if (input.columnsInfo().isDistributionMultiColumn()) {
                    distributionMultiColumnRearranger.rearrange();
                    wordDrawer.stopAddingChar();
                } else {
                    nextColumn();
                }
            }
        } else {
            if (!input.columnsInfo().lastColumn() &&
                    input.columnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
                nextColumn();
            }
        }
    }

    private boolean isOverBottom(long height) {
        return input.columnsInfo().currentColumnArea().bottom() < currentTextPartArea.top() + height;
    }

    private void newPage() throws Exception {
        if (forDistributionMultiColumn) {
            throw new BreakingDrawException(input.paraIndex(), input.charIndex(), input.charPosition(), 0).forDistributionColumn();
        }
        pagePainter.saveCurrentPage();
        charInfoBuffer.clearUntilPreviousPara();

        input.newPage();
        resetForNewPage();
        output.newPageOutput(input);
    }

    private void resetForNewPage() {
        resetForNewColumn();

        wordDrawer.adjustControlAreaAtNewPage();
        textFlowCalculator.reset();

        if (output.hasControlMovedToNextPage()) {
            for (InterimOutput.ControlInfo controlInfo : output.controlsMovedToNextPage()) {
                textFlowCalculator.add(controlInfo.charInfo());
            }
        }
    }

    public void nextColumn() {
        input.nextColumn();
        resetForNewColumn();
        output.nextColumn();
    }

    public void previousColumn() {
        input.previousColumn();
        resetForNewColumn();
        output.previousColumn();
    }

    private void resetForNewColumn() {
        currentTextPartArea.set(input.paraArea());
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
                    output.setLastLineInPara();
                }
                break;

            case Recalculating:
                if (textFlowCalculationResult.lastTextPart() || newLineAtRecalculating) {
                    textLineDrawer.saveToOutput();
                    if (newLineAtRecalculating) {
                        output.setLastLineInPara();
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
                        .reset(currentTextPartArea)
                        .addNewTextPart(0, currentTextPartArea.width());
                break;
            case Recalculating:
                currentTextPartArea.set(textFlowCalculationResult.nextArea());

                textLineDrawer
                        .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());
                break;
            case EndRecalculating:
                currentTextPartArea.set(textFlowCalculationResult.storedTextLineArea());

                currentTextPartArea.moveY(textLineDrawer.lineHeight());
                paraHeight += textLineDrawer.lineHeight();

                restoreIndentAtFirstLine();

                textLineDrawer
                        .reset(currentTextPartArea)
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

    public void forDistributionMultiColumn(boolean forDistributionMultiColumn) {
        this.forDistributionMultiColumn = forDistributionMultiColumn;
        if (forDistributionMultiColumn == false) {
            wordDrawer.continueAddingChar();
        }
    }

    public Area currentTextPartArea() {
        return currentTextPartArea;
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
