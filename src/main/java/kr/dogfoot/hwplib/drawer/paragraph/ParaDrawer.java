package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextRow;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.paragraph.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;

import java.io.UnsupportedEncodingException;

public class ParaDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;
    private final ParaListDrawer paraListDrawer;

    private final TextFlowCalculator textFlowCalculator;

    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final ParaDividingProcessor paraDividingProcessor;

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

    public ParaDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter, ParaListDrawer paraListDrawer) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;
        this.paraListDrawer = paraListDrawer;

        textFlowCalculator = new TextFlowCalculator();
        textLineDrawer = new TextLineDrawer(input, output);
        wordDrawer = new WordDrawer(input, output, this, textLineDrawer, textFlowCalculator);

        paraDividingProcessor = new ParaDividingProcessor(input, output, this);

        currentTextPartArea = new Area();
        textFlowCalculationResult = null;
    }

    public void draw(boolean redraw) throws Exception {
        controlExtendCharIndex = 0;

        if (redraw == false) {
            startPara();
        }
        resetForNewPara();

        if (input.noText()) {
            textLineDrawer.setEmptyLineHeight();
            checkNewColumnAndPage();
            saveTextLineAndNewLine();
        } else {
            chars();
        }

        endPara();
    }


    private void startPara() throws Exception {
        input.startPara();
        paraDividingProcessor.process();
    }

    private void endPara() {
        output.setLastLineInPara();

        long endY = currentTextPartArea.top() - input.paraArea().top();
        if (input.currentPara().getHeader().isLastInList()) {
            long lineGap = textLineDrawer.lineGap();
            endY -= lineGap;
            paraHeight -= lineGap;
        }
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

    private void chars() throws Exception {
        while (input.nextChar()) {
            wordDrawer.continueAddingChar();
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
        NormalCharInfo charInfo = (NormalCharInfo) charInfoBuffer().get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            charInfo = new NormalCharInfo(ch, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition())
                    .calculateWidth();
            charInfoBuffer().add(input.paraIndex(), input.charIndex(), charInfo);
        }
        return charInfo;
    }

    private CharInfoBuffer charInfoBuffer() {
        return paraListDrawer.charInfoBuffer();
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
        if (input.currentChar().getCode() == 11) {
            tableOrGso();
        } else if (input.currentChar().getCode() == 16) {
            headerFooter();
        } else {
            controlExtendCharIndex++;
        }
    }

    private void tableOrGso() {
        ControlCharInfo charInfo = (ControlCharInfo) charInfoBuffer().get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = ControlCharInfo.create((HWPCharControlExtend) input.currentChar(), control, input);
            charInfoBuffer().add(input.paraIndex(), input.charIndex(), charInfo);
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
        input.gotoCharInPara(textLineDrawer.firstCharInfo());

        currentTextPartArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .reset(textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());
        wordDrawer.reset();
    }

    private void startRedrawingTextLine() {
        input.gotoCharInPara(textLineDrawer.firstCharInfo());
        textLineDrawer
                .reset(currentTextPartArea)
                .addNewTextPart(0, currentTextPartArea.width());
        wordDrawer.reset();
    }

    private boolean isOverBottom(long addingHeight) {
        return input.columnsInfo().currentColumnArea().bottom() < currentTextPartArea.top() + addingHeight;
    }

    public void nextPage() throws Exception {
        if (forDistributionMultiColumn) {
            throw new BreakDrawingException(input.paraIndex(), input.charIndex(), input.charPosition()).forNewPage();
        }

        charInfoBuffer().clearUntilPreviousPara();
        paraListDrawer.drawHeaderFooter();

        if (input.columnsInfo().isParallelMultiColumn()) {
            int currentColumnIndex = input.columnsInfo().currentColumnIndex();
            input.nextPage();
            input.gotoColumnIndex(currentColumnIndex);
            resetForNewPage();
            output.nextPage(input);
            output.gotoRow(0).gotoColumnIndex(currentColumnIndex);
        } else {
            input.nextPage();
            resetForNewPage();
            output.nextPage(input);
        }
    }

    private void resetForNewPage() {
        resetForNewColumn();

        wordDrawer.adjustControlAreaAtNewPage();
        textFlowCalculator.resetForNewPage();

        if (output.hasControlMovedToNextPage()) {
            for (InterimOutput.ControlInfo controlInfo : output.controlsMovedToNextPage()) {
                textFlowCalculator.add(controlInfo.charInfo());
            }
        }
    }

    public void nextRow() {
        distributionMultiColumnRearranger().resetEndingParaIndex();

        if (input.isBodyText()) {
            output.gotoLastRow();
            input.gotoPage(output.currentPage().pageNo());
            setColumnDefine(output.rowBottom() + TextRow.Gsp);
        } else {
            long rowBottom = output.rowBottom();
            if (rowBottom == -1) {
                setColumnDefine(0);
            } else {
                setColumnDefine(rowBottom + TextRow.Gsp);
            }
       }
        output.nextRow(input.columnsInfo());

        resetForNewColumn();
    }

    public DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return paraListDrawer.distributionMultiColumnRearranger();
    }

    public void setSectionDefine() {
        input.nextChar();
        HWPChar firstChar = input.currentChar();

        if (firstChar.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) firstChar).isSectionDefine()) {
            input.sectionDefine((ControlSectionDefine) input.currentPara().getControlList().get(0));
            controlExtendCharIndex++;
        } else {
            input.previousChar(1);
        }
    }

    public void setColumnDefine(long startY) {
        input.nextChar();
        HWPChar firstChar = input.currentChar();
        if (firstChar.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) firstChar).isColumnDefine()) {
            input.newRow((ControlColumnDefine) input.currentPara().getControlList().get(controlExtendCharIndex), startY);
            controlExtendCharIndex++;
        } else {
            input.newRowWithPreviousColumnDefine(startY);
            input.previousChar(1);
        }
    }

    public void gotoFirstColumn() {
        while (input.columnsInfo().currentColumnIndex() > 0) {
            input.previousColumn();
        }
        while (output.currentRow().currentColumnIndex() > 0) {
            output.previousColumn();
        }
    }

    public void gotoStartCharOfCurrentRow() {
        input.columnsInfo().setWithPreviousInfo();
        input.currentParaListInfo().setTextBoxAreaToColumnArea();
        input.gotoFirstCharOfCurrentRow(output);

        output.currentRow().clear();
        gotoFirstColumn();
        resetForNewColumn();
    }

    public void nextColumn() {
        if (input.columnsInfo().isParallelMultiColumn()) {
            output.gotoStartingParallelMultiColumn(input.parallelMultiColumnInfo());
            input.gotoPage(input.parallelMultiColumnInfo().startingPageNo());

            int currentColumnIndex = input.columnsInfo().currentColumnIndex();
            setColumnDefine(output.currentRow().area().top());
            input.columnsInfo().currentColumnIndex(currentColumnIndex);
            output.currentRow().gotoColumnIndex(currentColumnIndex);

            input.nextColumn();
            output.nextColumn();
            resetForNewColumn();
        } else {
            input.nextColumn();
            output.nextColumn();
            resetForNewColumn();
        }
    }

    public void previousColumn() {
        input.previousColumn();
        output.previousColumn();
        resetForNewColumn();
    }

    public void resetForNewColumn() {
        currentTextPartArea.set(input.paraArea());
        applyIndent();
        textFlowCalculator.resetForNewColumn();
        if (textLineDrawer.notInitialized()) {
            textLineDrawer
                    .initialize(currentTextPartArea)
                    .addNewTextPart(0, currentTextPartArea.width());
        } else {
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
    }

    public void saveTextLineAndNewLine() throws Exception {
        if (!textLineDrawer.justNewLine() && drawingState.canAddChar()) {
            cancelNewLine = false;
            textLineDrawer.setLineHeight();

            checkTextFlow();

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

    public void checkNewColumnAndPage() throws Exception {
        System.out.println(input.columnsInfo().currentColumnArea().bottom() + " " + input.columnsInfo().textBoxArea().bottom());
        if (drawingState.isNormal()
                && (isOverBottom(textLineDrawer.maxCharHeight())
                || input.columnsInfo().isOverLimitedTextLineCount(output.textLineCount()))) {
            if (input.isBodyText()
                    && isOverBottom(textLineDrawer.maxCharHeight())
                    && (input.columnsInfo().lastColumn() || input.columnsInfo().isParallelMultiColumn())) {
                nextPage();
            } else {
                output.currentColumn().nextChar(textLineDrawer.firstCharInfo());

                if (!input.columnsInfo().lastColumn()) {
                    if (!output.currentRow().hadRearrangedDistributionMultiColumn()
                            && (distributionMultiColumnRearranger().testing()
                            || input.columnsInfo().isDistributionMultiColumn())) {
                        distributionMultiColumnRearranger().rearrangeFromCurrentColumn();

                        if (distributionMultiColumnRearranger().testing()) {
                            throw new BreakDrawingException().forEndingTest();
                        } else {
                            wordDrawer.stopAddingChar();
                        }
                    } else {
                        nextColumn();
                    }
                } else {
                    if (!input.isBodyText()
                            && input.columnsInfo().isNormalMultiColumn()) {

                        throw new BreakDrawingException().forOverTextBoxArea();
                    }
                }
            }
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

    public TextFlowCalculator textFlowCalculator() {
        return textFlowCalculator;
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
