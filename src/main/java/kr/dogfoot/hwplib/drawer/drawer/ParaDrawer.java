package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.drawer.control.table.TableResult;
import kr.dogfoot.hwplib.drawer.drawer.control.table.TableDrawer;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.drawer.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.drawer.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ParaDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;

    private final TextFlowCalculator textFlowCalculator;

    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final DividingProcessor paraDividingProcessor;

    private boolean firstLine;
    private boolean cancelNewLine;

    private Area currentTextPartArea;
    private long paraHeight;

    private TextFlowCalculationResult textFlowCalculationResult;
    private DrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;

    private int controlExtendCharIndex;

    public ParaDrawer(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;

        textFlowCalculator = new TextFlowCalculator();
        textLineDrawer = new TextLineDrawer(input, output);
        wordDrawer = new WordDrawer(input, output, this, textLineDrawer, textFlowCalculator);

        paraDividingProcessor = new DividingProcessor(input, output, this);

        currentTextPartArea = new Area();
        textFlowCalculationResult = null;
    }

    public void draw(boolean redraw) throws Exception {
        controlExtendCharIndex = 0;

        if (redraw == false) {
            startPara();
        }
        resetForNewPara();

        if (input.noChar()) {
            textLineDrawer.setEmptyLineHeight();
            saveTextLineAndNewLine();
            checkNewColumnAndPage();
        } else {
            chars();
        }

        endPara();
    }

    public void draw(boolean redraw, TextPosition startPosition) throws Exception {
        controlExtendCharIndex = 0;

        if (redraw == false) {
            startPara();
            input.gotoCharPositionInPara(startPosition);
        }
        resetForNewPara();

        if (input.noChar()) {
            textLineDrawer.setEmptyLineHeight();
            saveTextLineAndNewLine();
            checkNewColumnAndPage();
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
        input.gotoCharPositionInPara(textLineDrawer.firstCharInfo().position());

        currentTextPartArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .reset(textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextPartArea), currentTextPartArea.width());
        wordDrawer.reset();
    }

    private void startRedrawingTextLine() {
        input.gotoCharPositionInPara(textLineDrawer.firstCharInfo().position());
        textLineDrawer
                .reset(currentTextPartArea)
                .addNewTextPart(0, currentTextPartArea.width());
        wordDrawer.reset();
    }

    private boolean isOverBottom(long addingHeight) {
        return input.columnsInfo().currentColumnArea().bottom() < currentTextPartArea.top() + addingHeight;
    }

    public void nextPage() throws Exception {
        if (distributionMultiColumnRearranger().testing()) {
            throw new BreakDrawingException(new TextPosition(input.paraIndex(), input.charIndex(), input.charPosition())).forNewPage();
        }

        charInfoBuffer().clearUntilPreviousPara();
        paraListDrawer.drawHeaderFooter();

        if (input.columnsInfo().isParallelMultiColumn()) {
            int currentColumnIndex = input.columnsInfo().currentColumnIndex();
            input.nextPage();
            input.gotoColumn(currentColumnIndex);
            resetForNewPage();
            output.nextPage(input);
            output.gotoRow(0).gotoColumn(currentColumnIndex);
        } else {
            input.nextPage();
            resetForNewPage();
            output.nextPage(input);
        }

        drawSplitTables();
    }

    private void resetForNewPage() {
        resetForNewColumn();

        wordDrawer.adjustControlAreaAtNewPage();
        textFlowCalculator.resetForNewPage();

        if (output.hasControlMovedToNextPage()) {
            for (ControlCharInfo controlCharInfo : output.controlsMovedToNextPage()) {
                textFlowCalculator.add(controlCharInfo, controlCharInfo.output().areaWithOuterMargin());
            }
        }
    }

    private void drawSplitTables() throws Exception {
        TableDrawer drawer = new TableDrawer(input, output);

        ArrayList<TableResult> newSplitTableDrawResults = new ArrayList<>();
        for (TableResult splitTableDrawResult : input.splitTableDrawResults()) {
            TableResult splitTableDrawResult2 = drawer.drawSplitTable(splitTableDrawResult);
            newSplitTableDrawResults.add(splitTableDrawResult2);

            textFlowCalculator.delete(splitTableDrawResult.controlCharInfo());
        }
        input.clearSplitTableDrawResults();

        for (TableResult newSplitTableDrawResult : newSplitTableDrawResults) {
            wordDrawer.addControlOutput(newSplitTableDrawResult.controlCharInfo(), newSplitTableDrawResult.tableOutputForCurrentPage());

            if (newSplitTableDrawResult.split()) {
                input.addSplitTableDrawResult(newSplitTableDrawResult);
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

    public ParaDrawer setSectionDefine() {
        input.nextChar();
        HWPChar ch = input.currentChar();

        if (ch.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) ch).isSectionDefine()) {
            input.pageInfo().sectionDefine((ControlSectionDefine) input.currentPara().getControlList().get(0));
            controlExtendCharIndex++;
        } else {
            input.previousChar(1);
        }
        return this;
    }

    public void setColumnDefine(long startY) {
        input.nextChar();
        HWPChar ch = input.currentChar();
        if (ch.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) ch).isColumnDefine()) {
            input.newRow((ControlColumnDefine) input.currentPara().getControlList().get(controlExtendCharIndex), startY);
            controlExtendCharIndex++;
        } else {
            input.newRowWithPreviousColumnDefine(startY);
            input.previousChar(1);
        }
    }

    public void gotoFirstColumn() {
        input.gotoFirstColumn();
        output.gotoFirstColumn();
    }

    public void gotoStartCharOfCurrentRow() {
        input.currentParaListInfo().setColumnInfoWithPreviousInfo();

        if (output.currentRow().firstChar() == null) {
            input.gotoParaCharPosition(TextPosition.ParaList_Start_Position);
        } else{
            input.gotoParaCharPosition(output.currentRow().firstChar().position());
        }

        input.gotoParaCharPosition(output.currentRow().firstChar().position());

        output.currentRow().clear();
        gotoFirstColumn();
        resetForNewColumn();
    }

    public void nextColumn() {
        if (input.columnsInfo().isParallelMultiColumn()) {
            output.gotoPageAndRow(input.parallelMultiColumnInfo().startingPageNo(),
                    input.parallelMultiColumnInfo().startingRowIndex());
            input.gotoPage(input.parallelMultiColumnInfo().startingPageNo());

            int currentColumnIndex = input.columnsInfo().currentColumnIndex();
            setColumnDefine(output.currentRow().area().top());
            input.columnsInfo().currentColumnIndex(currentColumnIndex);
            output.currentRow().gotoColumn(currentColumnIndex);

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

    public void saveTextLineAndNewLine() {
        if (!textLineDrawer.justNewLine() && drawingState.canAddChar()) {
            cancelNewLine = false;
            textLineDrawer.setLineHeight();

            checkTextFlow();

            if (textLineDrawer.noDrawingChar() && input.pageInfo().checkHidingEmptyLineAfterNewPage()) {
                input.pageInfo().descendCountOfHidingEmptyLineAfterNewPage();
            } else {
                input.pageInfo().resetCountOfHidingEmptyLineAfterNewPage();
                saveTextLine();
                nextTextPartArea();
            }

            newLineAtRecalculating = false;
            newLineAtNormal = false;
        }
    }

    private void checkTextFlow() {
        if (drawingState.isNormal()) {
            currentTextPartArea
                    .height(textLineDrawer.maxCharHeight());

            textFlowCalculationResult = textFlowCalculator.calculate(currentTextPartArea);
            currentTextPartArea
                    .moveY(textFlowCalculationResult.offsetY());
            if (textFlowCalculationResult.nextState().isStartRecalculating()) {
                textFlowCalculationResult.storeTextLineArea(currentTextPartArea);
            }
            textLineDrawer.textLineArea().set(currentTextPartArea);

            if (!input.noChar()) {
                cancelNewLine = textFlowCalculationResult.cancelNewLine() && textLineDrawer.noDrawingChar();
                drawingState = textFlowCalculationResult.nextState();
            }
        }
    }

    public void checkNewColumnAndPage() throws Exception {
        if (!drawingState.isNormal()) {
            return;
        }
        if (isOverBottom(textLineDrawer.maxCharHeight())
                || input.columnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
            if (input.isBodyText()
                    && isOverPageBottom(textLineDrawer.maxCharHeight())
                    && (input.columnsInfo().lastColumn() || input.columnsInfo().isParallelMultiColumn())) {
                nextPage();
            } else {
                if (textLineDrawer.firstCharInfo() != null) {
                    output.currentColumn().nextCharPosition(textLineDrawer.firstCharInfo().position());
                } else {
                    output.currentColumn().nextCharPosition(new TextPosition(textLineDrawer.paraIndex(), 0, 0));
                }

                if (!input.columnsInfo().lastColumn()) {
                    if (shouldProcessInDistributionMultiColumn()) {
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
                    if (!input.isBodyText() && input.columnsInfo().isNormalMultiColumn()) {
                        throw new BreakDrawingException().forOverTextBoxArea();
                    }
                }
            }
        }

        if (input.isCellText() && input.currentParaListInfo().canSplitCell()) {
            if (isOverPageBottomForCell(textLineDrawer.maxCharHeight(), input.currentParaListInfo().cellTopInPage(), input.currentParaListInfo().tableOuterMarginBottom())) {
                if (textLineDrawer.firstCharInfo() != null) {
                    throw new BreakDrawingException(textLineDrawer.firstCharInfo().position()).forOverPage();
                } else {
                    throw new BreakDrawingException(new TextPosition(textLineDrawer.paraIndex(), 0, 0)).forOverPage();
                }
            }
        }
    }

    private boolean shouldProcessInDistributionMultiColumn() {
        return (distributionMultiColumnRearranger().testing() || input.columnsInfo().isDistributionMultiColumn())
                && !output.currentRow().hadRearrangedDistributionMultiColumn();
    }

    private boolean isOverPageBottom(long addingHeight) {
        return input.pageInfo().bodyArea().bottom() < currentTextPartArea.top() + addingHeight;
    }

    private boolean isOverPageBottomForCell(long addingHeight, long celTopInPage, long tableOuterMarginBottom) {
        return input.pageInfo().bodyArea().bottom() < celTopInPage + currentTextPartArea.top() + addingHeight + tableOuterMarginBottom + 150;
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
