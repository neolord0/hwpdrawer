package kr.dogfoot.hwplib.drawer.drawer.para;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.RedrawException;
import kr.dogfoot.hwplib.drawer.drawer.paralist.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.drawer.paralist.DistributionMultiColumnRearranger;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.paralist.ParagraphListInfo;
import kr.dogfoot.hwplib.drawer.input.paralist.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.Output;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.CellOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwplib.drawer.drawer.charInfo.CharInfoNormal;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class ParaDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final CharInfoBuffer charInfoBuffer;
    private DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    private final TextFlowCalculator textFlowCalculator;

    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final DividingProcessor dividingProcessor;

    private boolean firstLine;
    private boolean cancelNewLine;

    private Area currentTextArea;
    private long paraHeight;

    private TextFlowCalculationResult textFlowCalculationResult;
    private ParaDrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;

    private int controlExtendCharIndex;

    public ParaDrawer(DrawingInput input, InterimOutput output, CharInfoBuffer charInfoBuffer) {
        this.input = input;
        this.output = output;
        this.charInfoBuffer = charInfoBuffer;

        textFlowCalculator = new TextFlowCalculator();
        textLineDrawer = new TextLineDrawer(input, output);
        wordDrawer = new WordDrawer(input, output, this, textLineDrawer, textFlowCalculator);

        dividingProcessor = new DividingProcessor(input, output, this);

        currentTextArea = new Area();
        textFlowCalculationResult = null;
    }

    public void distributionMultiColumnRearranger(DistributionMultiColumnRearranger distributionMultiColumnRearranger) {
        this.distributionMultiColumnRearranger = distributionMultiColumnRearranger;
        dividingProcessor.distributionMultiColumnRearranger(distributionMultiColumnRearranger);
    }

    public void draw(boolean redraw) throws Exception {
        draw(redraw, null, null);
    }

    public void draw(boolean redraw, TextPosition startPosition, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (redraw == false) {
            startPara(startPosition);
        }

        resetForNewPara();
        addChildControlsCrossingPage(childControlsCrossingPage, output.currentOutput().type() == Output.Type.Cell);

        if (input.noChar()) {
            textLineDrawer.setEmptyLineHeight();
            saveTextLineAndNewLine();
            checkNewColumnAndPage();
        } else {
            chars();
        }

        endPara();
    }

    private void startPara(TextPosition startPosition) throws Exception {
        input.startPara();
        controlExtendCharIndex = 0;

        if (startPosition != null && startPosition.charIndex() > 0) {
            input.gotoCharPositionInPara(startPosition);
        } else {
            dividingProcessor.process();
        }
    }

    private void endPara() {
        output.setLastLineInPara();
        input.endPara(endY(), paraHeight);
    }

    private long endY() {
        long endY = currentTextArea.top() - input.paraArea().top();
        if (input.currentPara().getHeader().isLastInList()) {
            long lineGap = textLineDrawer.lineGap();
            endY -= lineGap;
            paraHeight -= lineGap;
        }
        return endY;
    }

    private void resetForNewPara() {
        firstLine = true;
        resetCurrentTextArea();

        textLineDrawer
                .initialize(currentTextArea)
                .addNewTextPart(0, currentTextArea.width());

        wordDrawer
                .reset();

        paraHeight = 0;

        drawingState = ParaDrawingState.Normal;
        newLineAtRecalculating = false;
        newLineAtNormal = false;
    }

    private void resetCurrentTextArea() {
        currentTextArea.set(input.paraArea()).height(0);
        if (firstLine) {
            if (input.paraShape().getIndent() > 0) {
                currentTextArea.left(currentTextArea.left() + input.paraShape().getIndent() / 2);
            }
        } else {
            currentTextArea.left(currentTextArea.left() - input.paraShape().getIndent() / 2);
        }
    }

    private void addChildControlsCrossingPage(ControlOutput[] childControlsCrossingPage, boolean inCell) throws RedrawException {
        if (childControlsCrossingPage == null) {
            return;
        }

        for (ControlOutput childOutput : childControlsCrossingPage) {
            if (inCell) {
                CellOutput cellOutput = (CellOutput) output.currentOutput();
                childOutput.areaWithoutOuterMargin()
                        .moveY(cellOutput.cell().getListHeader().getTopMargin() - childOutput.areaWithoutOuterMargin().top());
            }
            wordDrawer.addControlOutput(childOutput);
        }
    }

    private void chars() throws Exception {
        while (input.nextChar()) {
            wordDrawer.continueAddingChar();

            processChar(input.currentChar());
            processByDrawingState();
        }
    }

    private void processChar(HWPChar ch) throws Exception {
        switch (ch.getType()) {
            case Normal:
                normalChar((HWPCharNormal) ch);
                break;
            case ControlChar:
                controlChar((HWPCharControlChar) ch);
                break;
            case ControlInline:
                break;
            case ControlExtend:
                controlExtend((HWPCharControlExtend) ch);
                break;
        }
    }

    private void processByDrawingState() {
        switch (drawingState) {
            case StartRecalculating:
                startRecalculatingTextLine();
                drawingState = ParaDrawingState.Recalculating;
                break;
            case EndRecalculating:
                drawingState = ParaDrawingState.Normal;
                break;
            case StartRedrawing:
                startRedrawingTextLine();
                drawingState = ParaDrawingState.Normal;
                break;
        }
    }

    private void normalChar(HWPCharNormal ch) throws Exception {
        CharInfoNormal charInfo = normalCharInfo(ch);
        if (!charInfo.character().isSpace()) {
            wordDrawer.addCharOfWord(charInfo);
        } else {
            wordDrawer.addWordToLine(charInfo);
        }
    }

    private CharInfoNormal normalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        CharInfoNormal charInfo = (CharInfoNormal) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            charInfo = new CharInfoNormal(ch, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition())
                    .calculateWidth();
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
        }
        return charInfo;
    }

    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            wordDrawer.addWordToLine(null);
            if (drawingState.isRecalculating()) {
                newLineAtRecalculating = true;
            } else if (drawingState.isNormal()) {
                newLineAtNormal = true;
            }

            saveTextLineAndNewLine();
        }
    }

    private void controlExtend(HWPCharControlExtend ch) {
        if (ch.getCode() == 11) {
            tableOrGso(ch);
        } else if (ch.getCode() == 16) {
            headerFooter(ch);
        } else {
            controlExtendCharIndex++;
        }
    }

    private void tableOrGso(HWPCharControlExtend ch) {
        CharInfoControl charInfo = (CharInfoControl) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = CharInfoControl.create(ch, control, input, currentTextArea);
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
            controlExtendCharIndex++;
        }

        wordDrawer.addCharOfWord(charInfo);
    }

    private void headerFooter(HWPCharControlExtend ch) {
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

        currentTextArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .reset(textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextArea), currentTextArea.width());
        wordDrawer.reset();
    }

    private void startRedrawingTextLine() {
        input.gotoCharPositionInPara(textLineDrawer.firstCharInfo().position());
        textLineDrawer
                .reset(currentTextArea)
                .addNewTextPart(0, currentTextArea.width());
        wordDrawer.reset();
    }

    public void nextPage() throws Exception {
        if (distributionMultiColumnRearranger.testing()) {
            throw new BreakDrawingException(new TextPosition(input.paraIndex(), input.charIndex(), input.charPosition())).forNewPage();
        }

        charInfoBuffer.clearUntilPreviousPara();

        ParaListDrawer.drawHeaderFooter(input, output);

        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            if (input.currentColumnsInfo().isFirstColumn()) {
                int currentColumnIndex = input.currentColumnsInfo().currentColumnIndex();
                input.nextPage();
                input.gotoColumn(currentColumnIndex);
                resetForNewPage();
                output.nextPage(input);
                output.gotoRow(0).gotoColumn(currentColumnIndex);
            } else {
                int currentColumnIndex = input.currentColumnsInfo().currentColumnIndex();
                input.nextPage();
                input.gotoColumn(currentColumnIndex);
                resetForNewPage();

                gotoNextSplitPageForParallelMultiColumn(currentColumnIndex);
            }

            input.parallelMultiColumnInfo()
                    .addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());
        } else {
            input.nextPage();
            resetForNewPage();
            output.nextPage(input);
        }

        addSplitTables();
        addChildControlsCrossingPage(output.childControlsCrossingPage(), false);
        output.clearChildControlsCrossingPage();
    }

    private void resetForNewPage() {
        resetForNewColumn();

        wordDrawer.adjustControlAreaAtNewPage();
        textFlowCalculator.resetForNewPage();
    }

    private void gotoNextSplitPageForParallelMultiColumn(int currentColumnIndex) {
        ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
        output.gotoPage((PageOutput) parentInfo.output()).content().gotoRow(0).gotoColumn(currentColumnIndex);
    }

    private void addSplitTables() throws Exception {
        ArrayList<TableOutput> tableOutputs = output.splitTables();
        for (TableOutput output : tableOutputs) {
            wordDrawer.addControlOutput(output);
        }
    }

    public void nextRow() {
        distributionMultiColumnRearranger.resetEndingParaIndex();

        if (input.sortOfText() == ParagraphListInfo.Sort.ForBody) {
            output.gotoLastRow();
            input.gotoPage(output.currentPage().pageNo());
            setColumnDefine(output.rowBottom() + TextRow.Gsp);
        } else {
            long rowBottom = output.rowBottom();
            if (rowBottom == -1) {
                setColumnDefine(input.currentParaListInfo().textBoxArea().top());
            } else {
                setColumnDefine(rowBottom + TextRow.Gsp);
            }
        }
        output.nextRow(input.currentColumnsInfo());
        resetForNewColumn();
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
        if (ch != null && ch.getType() == HWPCharType.ControlExtend &&
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

        output.currentRow().clear();
        gotoFirstColumn();
        resetForNewColumn();
    }

    public void nextColumn() {
        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            nextColumnForParallelMultiColumn();
        } else {
            input.nextColumn();
            output.nextColumn();
            resetForNewColumn();
        }
    }

    private void nextColumnForParallelMultiColumn(){
        input.parallelMultiColumnInfo().nextColumn();

        ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
        switch (input.sortOfText()) {
            case ForBody:
                PageOutput pageOutput = (PageOutput) parentInfo.output();
                output.gotoPage(pageOutput).content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
                break;

            case ForCell:
                output.currentOutput(parentInfo.output()).content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
                input.currentParaListInfo().cellInfo(parentInfo.cellInfo());
                break;
        }

        int currentColumnIndex = input.currentColumnsInfo().currentColumnIndex();
        setColumnDefine(output.currentRow().area().top());
        input.currentColumnsInfo().currentColumnIndex(currentColumnIndex);
        output.currentRow().gotoColumn(currentColumnIndex);

        input.nextColumn();
        output.nextColumn();
        resetForNewColumn();
    }

    public void previousColumn() {
        input.previousColumn();
        output.previousColumn();
        resetForNewColumn();
    }

    private void resetForNewColumn() {
        resetCurrentTextArea();

        textFlowCalculator.resetForNewColumn();
        if (textLineDrawer.notInitialized()) {
            textLineDrawer
                    .initialize(currentTextArea)
                    .addNewTextPart(0, currentTextArea.width());
        } else {
            if (textLineDrawer.noDrawingChar()) {
                textLineDrawer
                        .clearTextLine()
                        .addNewTextPart(0, currentTextArea.width());
            } else {
                textLineDrawer.textLineArea().set(currentTextArea);
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
            currentTextArea
                    .height(textLineDrawer.maxCharHeight());

            textFlowCalculationResult = textFlowCalculator.calculate(currentTextArea);
            currentTextArea
                    .moveY(textFlowCalculationResult.offsetY());
            if (textFlowCalculationResult.nextState().isStartRecalculating()) {
                textFlowCalculationResult.storeTextLineArea(currentTextArea);
            }
            textLineDrawer.textLineArea().set(currentTextArea);

            if (!input.noChar()) {
                cancelNewLine = textFlowCalculationResult.cancelNewLine() && textLineDrawer.noDrawingChar();
                drawingState = textFlowCalculationResult.nextState();
            }
        }
    }

    public void checkNewColumnAndPage() throws Exception {
        if (drawingState.isNormal()) {
            switch (input.sortOfText()) {
                case ForBody:
                    checkNewColumnAndPageForBodyText();
                    break;
                case ForControl:
                    checkNewColumnAndPageForControl();
                    break;
                case ForCell:
                    checkNewColumnAndPageForCell();
                    break;
            }
        }
    }

    private void checkNewColumnAndPageForBodyText() throws Exception {
        if (!isOverColumnBottom(textLineDrawer.maxCharHeight())
                && !input.currentColumnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
            return;
        }

        if (isOverPageBottom(textLineDrawer.maxCharHeight())
                && (input.currentColumnsInfo().isLastColumn() || input.currentColumnsInfo().isParallelMultiColumn())) {
            nextPage();
        } else {
            output.currentColumn().nextCharPosition(textLineDrawer.firstCharPosition());

            if (!input.currentColumnsInfo().isLastColumn()) {
                if (shouldProcessInDistributionMultiColumn()) {
                    distributionMultiColumnRearranger.rearrangeFromCurrentColumn();

                    if (distributionMultiColumnRearranger.testing()) {
                        throw new BreakDrawingException().forEndingTest();
                    } else {
                        wordDrawer.stopAddingChar();
                    }
                } else {
                    nextColumn();
                }
            }
        }
    }

    private void checkNewColumnAndPageForControl() throws Exception {
        if (!isOverColumnBottom(textLineDrawer.maxCharHeight())
                && !input.currentColumnsInfo().isOverLimitedTextLineCount(output.textLineCount())) {
            return;
        }

        output.currentColumn().nextCharPosition(textLineDrawer.firstCharPosition());

        if (!input.currentColumnsInfo().isLastColumn()) {
            if (shouldProcessInDistributionMultiColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();

                if (distributionMultiColumnRearranger.testing()) {
                    throw new BreakDrawingException().forEndingTest();
                } else {
                    wordDrawer.stopAddingChar();
                }
            } else {
                nextColumn();
            }
        } else {
            if (input.currentColumnsInfo().isNormalMultiColumn()) {
                throw new BreakDrawingException().forOverTextBoxArea();
            }
        }
    }

    private void checkNewColumnAndPageForCell() throws Exception {
        if (input.currentParaListInfo().cellInfo().canSplit()) {
             if (isOverPageBottomForCell(textLineDrawer.maxCharHeight())) {
                if (input.currentColumnsInfo().isParallelMultiColumn()) {
                    if (input.currentColumnsInfo().isFirstColumn()) {
                        throw new BreakDrawingException(textLineDrawer.firstCharPosition())
                                .forOverPage()
                                .columnIndex(0);
                    } else {
                        gotoSplitNextCell();
                    }
                } else {
                    if (input.currentColumnsInfo().isLastColumn()) {
                        throw new BreakDrawingException(textLineDrawer.firstCharPosition()).forOverPage();
                    } else {
                        nextColumn();
                    }
                }
                return;
             }
        }
        checkNewColumnAndPageForControl();
    }

    private void gotoSplitNextCell() throws BreakDrawingException {
        ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
        if (parentInfo != null) {
            output.currentOutput(parentInfo.output()).content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
            input.currentParaListInfo().cellInfo(parentInfo.cellInfo());
            input.currentParaListInfo().resetParaStartY(input.currentParaListInfo().textBoxArea().top());
        } else {
            throw new BreakDrawingException(textLineDrawer.firstCharPosition())
                    .forOverPage()
                    .columnIndex(input.currentColumnsInfo().currentColumnIndex());
        }
        resetForNewColumn();
    }

    private boolean isOverColumnBottom(long addingHeight) {
        return input.currentColumnsInfo().currentColumnArea().bottom() < currentTextArea.top() + addingHeight;
    }

    private boolean isOverPageBottom(long addingHeight) {
        return input.pageInfo().bodyArea().bottom() < currentTextArea.top() + addingHeight;
    }

    private boolean shouldProcessInDistributionMultiColumn() {
        return (distributionMultiColumnRearranger.testing()
                || input.currentColumnsInfo().isDistributionMultiColumn()
                || input.currentColumnsInfo().processLikeDistributionMultiColumn())
                && !output.currentRow().hadRearrangedDistributionMultiColumn();
    }

    private boolean isOverPageBottomForCell(long addingHeight) {
        return input.pageInfo().bodyArea().bottom() - input.currentParaListInfo().cellInfo().bottomMargin()
                < input.currentParaListInfo().cellInfo().topInPage() + currentTextArea.top() + addingHeight;
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
                    drawingState = ParaDrawingState.EndRecalculating;
                }
                break;
        }
    }

    private void nextTextPartArea() {
        switch (drawingState) {
            case Normal:
            case StartRedrawing:
                if (!cancelNewLine) {
                    currentTextArea.moveY(textLineDrawer.lineHeight());
                    paraHeight += textLineDrawer.lineHeight();
                }
                restoreIndentAtFirstLine();

                textLineDrawer
                        .reset(currentTextArea)
                        .addNewTextPart(0, currentTextArea.width());
                break;
            case Recalculating:
                currentTextArea.set(textFlowCalculationResult.nextArea());

                textLineDrawer
                        .addNewTextPart(textFlowCalculationResult.startX(currentTextArea), currentTextArea.width());
                break;
            case EndRecalculating:
                currentTextArea.set(textFlowCalculationResult.storedTextLineArea());

                currentTextArea.moveY(textLineDrawer.lineHeight());
                paraHeight += textLineDrawer.lineHeight();

                restoreIndentAtFirstLine();

                textLineDrawer
                        .reset(currentTextArea)
                        .addNewTextPart(0, currentTextArea.width());
                break;
        }
    }

    private void restoreIndentAtFirstLine() {
        if (firstLine) {
            currentTextArea.left(currentTextArea.left() - input.paraShape().getIndent() / 2);
            firstLine = false;
        }
    }

    public ParaDrawingState drawingState() {
        return drawingState;
    }

    public Area currentTextPartArea() {
        return currentTextArea;
    }

    public TextFlowCalculator textFlowCalculator() {
        return textFlowCalculator;
    }
}
