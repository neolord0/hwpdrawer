package kr.dogfoot.hwplib.drawer.drawer.para;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.RedrawException;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculationResult;
import kr.dogfoot.hwplib.drawer.drawer.para.textflow.TextFlowCalculator;
import kr.dogfoot.hwplib.drawer.drawer.paralist.DistributionMultiColumnRearranger;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.input.paralist.ParagraphListInfo;
import kr.dogfoot.hwplib.drawer.input.paralist.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.control.table.TableOutput;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;
import kr.dogfoot.hwplib.drawer.output.text.TextRow;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.CharPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;

import java.util.ArrayList;

public class ParaDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    private final TextFlowCalculator textFlowCalculator;

    private final CharAdder charAdder;
    private final TextLineDrawer textLineDrawer;
    private final WordDrawer wordDrawer;
    private final NewPageColumnChecker newPageColumnChecker;

    private final DividingProcessor dividingProcessor;

    private boolean firstLine;
    private boolean cancelNewLine;

    private Area currentTextArea;
    private long paraHeight;

    private TextFlowCalculationResult textFlowCalculationResult;
    private ParaDrawingState drawingState;
    private boolean newLineAtRecalculating;
    private boolean newLineAtNormal;

    private CharPosition startPositionOfRecalculatingTextLine;

    public ParaDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        textFlowCalculator = new TextFlowCalculator();

        textLineDrawer = new TextLineDrawer(input, output);

        wordDrawer = new WordDrawer(input, output, this, textLineDrawer, textFlowCalculator);
        charAdder = new CharAdder(input, output, this, wordDrawer);
        newPageColumnChecker = new NewPageColumnChecker(input, output, textLineDrawer);
        dividingProcessor = new DividingProcessor(input, output, this, charAdder);

        currentTextArea = new Area();
        textFlowCalculationResult = null;
    }

    public void distributionMultiColumnRearranger(DistributionMultiColumnRearranger distributionMultiColumnRearranger) {
        this.distributionMultiColumnRearranger = distributionMultiColumnRearranger;
        dividingProcessor.distributionMultiColumnRearranger(distributionMultiColumnRearranger);
        newPageColumnChecker.distributionMultiColumnRearranger(distributionMultiColumnRearranger);
        distributionMultiColumnRearranger.textFlowCalculator(textFlowCalculator);
    }

    public void draw(boolean redraw) throws Exception {
        draw(redraw, null, null);
    }

    public void draw(boolean redraw, CharPosition startPosition, ControlOutput[] childControlsCrossingPage) throws Exception {
        if (redraw == false) {
            start(startPosition);
        }
        resetForNewPara();
        addChildControlsCrossingPage(childControlsCrossingPage);
        processChars();
        end();
    }

    private void start(CharPosition startPosition) throws Exception {
        input.startPara();
        charAdder.resetAtStartingPara();

        if (startPosition != null && startPosition.charIndex() > 0) {
            input.gotoCharPositionInPara(startPosition);
        } else {
            dividingProcessor.process();
        }
    }

    private void end() {
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

    private void addChildControlsCrossingPage(ControlOutput[] childControlsCrossingPage) throws RedrawException {
        wordDrawer.addChildControls(childControlsCrossingPage, input.sortOfText().isForCell());
    }

    private void processChars() throws Exception {
        if (input.noChar()) {
            setEmptyLineHeight();
            saveTextLineAndNewLine();
            checkNewColumnAndPage();
        } else {
            chars();
        }
    }


    private void setEmptyLineHeight() {
        Area area = new Area(textLineDrawer.textLineArea())
                .resizeY(input.charShape().getBaseSize());
        textLineDrawer.textLineArea(area);
    }

    private void chars() throws Exception {
        while (input.nextChar()) {
            charAdder.addChar(input.currentChar(), currentTextArea);

            switch (drawingState) {
                case StartRecalculating:
                    onStartRecalculatingTextLine();
                    drawingState = ParaDrawingState.Recalculating;
                    break;
                case EndRecalculating:
                    onEndRecalculating();
                    drawingState = ParaDrawingState.Normal;
                    break;
                case StartRedrawing:
                    onStartRedrawingTextLine();
                    drawingState = ParaDrawingState.Normal;
                    break;
                case ETC:
                    drawingState = ParaDrawingState.Normal;
                    break;
            }
        }
    }

    private void onStartRecalculatingTextLine() {
        input.gotoCharPositionInPara(textLineDrawer.firstDrawingCharPosition());

        currentTextArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .reset(textFlowCalculationResult.storedTextLineArea())
                .addNewTextPart(textFlowCalculationResult.startX(currentTextArea), currentTextArea.width());
        wordDrawer.reset();

        startPositionOfRecalculatingTextLine = input.currentCharPosition();
    }

    private void onEndRecalculating() {
        if (startPositionOfRecalculatingTextLine.equals(input.currentCharPosition()) && textFlowCalculationResult.nextStartY() > 0) {
            long oldHeight = currentTextArea.height();
            currentTextArea
                    .top(textFlowCalculationResult.nextStartY() + 1)
                    .height(oldHeight);
        }
    }

    private void onStartRedrawingTextLine() {
        input.gotoCharPositionInPara(textLineDrawer.firstDrawingCharPosition());
        textLineDrawer
                .reset(currentTextArea)
                .addNewTextPart(0, currentTextArea.width());
        wordDrawer.reset();
    }

    public void setNewLine() {
        if (drawingState.isRecalculating()) {
            newLineAtRecalculating = true;
        } else if (drawingState.isNormal()) {
            newLineAtNormal = true;
        }
    }

    public void nextPage() throws Exception {
        if (distributionMultiColumnRearranger.testing()) {
            throw new BreakDrawingException(new CharPosition(input.paraIndex(), input.charIndex(), input.charPosition())).forNewPage();
        }

        ParaListDrawer.drawHeaderFooter(input, output);
        charAdder.clearBufferUntilPreviousPara();

        gotoNextPage();

        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            input.parallelMultiColumnInfo()
                    .addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());
        }

        addDividedTablesAndChildControlsCrossingPage();
    }

    private void gotoNextPage() {
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

                ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
                output.gotoPage((PageOutput) parentInfo.output()).content().gotoRow(0).gotoColumn(currentColumnIndex);
            }
        } else {
            input.nextPage();
            resetForNewPage();
            output.nextPage(input);
        }
    }

    private void resetForNewPage() {
        resetForNewColumn();

        wordDrawer.adjustControlAreaAtNewPage(currentTextArea);
        textFlowCalculator.resetForNewPage();
    }

    private void addDividedTablesAndChildControlsCrossingPage() throws Exception {
        ArrayList<TableOutput> tableOutputs = output.dividedTables();
        wordDrawer.addDividedTables(tableOutputs);

        wordDrawer.addChildControls(output.childControlsCrossingPage(), false);
        output.clearChildControlsCrossingPage();
    }

    public void nextRow() {
        distributionMultiColumnRearranger.resetEndingParaIndex();

        if (input.sortOfText().isForBody()) {
            output.gotoLastRow();
            input.gotoPage(output.currentPage().pageNo());
        }

        gotoNextRow();
        resetForNewColumn();
    }

    private void gotoNextRow() {
        ControlColumnDefine columnDefine = charAdder.columnDefine();
        long rowBottom = output.rowBottom();

        long startY;
        if (input.sortOfText().isForBody()) {
            startY = rowBottom + TextRow.Gsp;
        } else {
            if (rowBottom == -1) {
                startY = input.currentParaListInfo().textBoxArea().top();
            } else {
                startY = rowBottom + TextRow.Gsp;
            }
        }

        input.columnsInfo(columnDefine, startY);
        output.nextRow(input.currentColumnsInfo());
    }

    public void gotoFirstColumn() {
        input.gotoFirstColumn();
        output.gotoFirstColumn();
    }

    public void gotoStartCharOfCurrentRow() {
        input.currentParaListInfo().setColumnInfoWithPreviousInfo();

        if (output.currentRow().firstChar() == null) {
            input.gotoParaCharPosition(CharPosition.ParaList_Start_Position);
        } else {
            input.gotoParaCharPosition(output.currentRow().firstChar().position());
        }

        output.currentRow().clear();
        gotoFirstColumn();
        resetForNewColumn();
    }

    public void nextColumn() {
        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            gotoNextParentForParallelMultiColumn();
        }

        input.nextColumn();
        output.nextColumn();
        resetForNewColumn();
    }

    private void gotoNextParentForParallelMultiColumn() {
        input.parallelMultiColumnInfo().nextColumn();

        ParallelMultiColumnInfo.ParentInfo parentInfo = input.parallelMultiColumnInfo().nextParentInfo();
        if (input.sortOfText().isForBody()) {
            output.gotoPage((PageOutput) parentInfo.output())
                    .content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
        } else if (input.sortOfText().isForCell()) {
            output.currentOutput(parentInfo.output())
                    .content().gotoRow(input.parallelMultiColumnInfo().startingRowIndex());
            input.currentParaListInfo().cellInfo(parentInfo.cellInfo());
        }

        int columnIndex = input.currentColumnsInfo().currentColumnIndex();

        input.columnsInfo(charAdder.columnDefine(), output.currentRow().area().top());
        input.currentColumnsInfo().currentColumnIndex(columnIndex);
        output.currentRow().gotoColumn(columnIndex);
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
            }
        }
    }

    public void saveTextLineAndNewLine() throws Exception {
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

    private void checkTextFlow() throws Exception {
        if (drawingState.isNormal()) {
            currentTextArea
                    .height(textLineDrawer.maxCharHeight());
            textFlowCalculationResult = textFlowCalculator.calculate(currentTextArea);

            long offsetY = textFlowCalculationResult.offsetY();
            if (offsetY > 0) {
                currentTextArea
                        .moveY(textFlowCalculationResult.offsetY());
                if (input.sortOfText() == ParagraphListInfo.Sort.ForBody
                        && input.pageInfo().bodyArea().bottom() < currentTextArea.bottom()) {

                    ControlOutput[] controlOutputs = textLineDrawer.controlOutputs();
                    if (controlOutputs.length > 0) {
                        for (ControlOutput controlOutput : controlOutputs) {
                            output.currentPage().content().removeControl(controlOutput);
                        }
                    }
                    input.gotoParaCharPosition(textLineDrawer.firstCharPosition());

                    textLineDrawer.clearTextLine();
                    drawingState = ParaDrawingState.ETC;
                    nextPage();
                    return;
                }
            }

            if (textFlowCalculationResult.nextState().isStartRecalculating()) {
                textFlowCalculationResult.storeTextLineArea(currentTextArea);
            }
            textLineDrawer.textLineArea(currentTextArea);

            if (!input.noChar()) {
                cancelNewLine = textFlowCalculationResult.cancelNewLine() && textLineDrawer.noDrawingChar();
                drawingState = textFlowCalculationResult.nextState();
            }
        }
    }

    private void saveTextLine() throws Exception {
        switch (drawingState) {
            case Normal:
                saveTextLineAtNormal();
                break;
            case Recalculating:
                saveTextLineAtRecalculating();
                break;
        }
    }

    private void saveTextLineAtNormal() throws Exception {
        if (textLineDrawer.isOverPageHeight()
                && textLineDrawer.hasDividedTable()
                && textLineDrawer.hasDrawingChar()) {
            nextPage();
            input.gotoParaCharPosition(textLineDrawer.firstCharPosition());
        } else {
            textLineDrawer.saveToOutput();
            if (newLineAtNormal) {
                output.setLastLineInPara();
            }
        }
    }

    private void saveTextLineAtRecalculating() {
        if (textFlowCalculationResult.lastTextPart() || newLineAtRecalculating) {
            textLineDrawer.saveToOutput();
            if (newLineAtRecalculating) {
                output.setLastLineInPara();
            }
            drawingState = ParaDrawingState.EndRecalculating;
        }
    }

    private void nextTextPartArea() {
        switch (drawingState) {
            case Normal:
            case StartRedrawing:
                nextTextPartAreaAtNormal();
                break;
            case Recalculating:
                nextTextPartAreaAtRecalculating();
                break;
            case EndRecalculating:
                nextTextPartAreaAtEndRecalculating();
                break;
        }
    }

    private void nextTextPartAreaAtNormal() {
        if (!cancelNewLine) {
            currentTextArea.moveY(textLineDrawer.lineHeight());
            paraHeight += textLineDrawer.lineHeight();
        }
        restoreIndentAtFirstLine();
        textLineDrawer
                .reset(currentTextArea)
                .addNewTextPart(0, currentTextArea.width());
    }

    private void nextTextPartAreaAtRecalculating() {
        currentTextArea.set(textFlowCalculationResult.nextArea());
        textLineDrawer
                .addNewTextPart(textFlowCalculationResult.startX(currentTextArea), currentTextArea.width());
    }

    private void nextTextPartAreaAtEndRecalculating() {
        currentTextArea.set(textFlowCalculationResult.storedTextLineArea());

        currentTextArea.moveY(textLineDrawer.lineHeight());
        paraHeight += textLineDrawer.lineHeight();

        restoreIndentAtFirstLine();
        textLineDrawer
                .reset(currentTextArea)
                .addNewTextPart(0, currentTextArea.width());
    }

    private void restoreIndentAtFirstLine() {
        if (firstLine) {
            currentTextArea.left(currentTextArea.left() - input.paraShape().getIndent() / 2);
            firstLine = false;
        }
    }

    public void checkNewColumnAndPage() throws Exception {
        switch (newPageColumnChecker.check(drawingState, currentTextArea)) {
            case NextPage:
                nextPage();
                break;
            case NextColumn:
                nextColumn();
                break;
            case ResetForNewColumn:
                resetForNewColumn();
                break;
            case StopAddingChar:
                wordDrawer.stopAddingChar();
                break;
        }
    }

    public ParaDrawingState drawingState() {
        return drawingState;
    }

    public Area currentTextArea() {
        return currentTextArea;
    }
}
