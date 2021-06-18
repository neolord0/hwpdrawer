package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;

    private final ParaDrawer paraDrawer;
    private final CharInfoBuffer charInfoBuffer;
    private final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    public ParaListDrawer(DrawingInput input, InterimOutput output) {
        this(input, output, null);
    }

    public ParaListDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;

        paraDrawer = new ParaDrawer(input, output, pagePainter, this);

        charInfoBuffer = new CharInfoBuffer();
        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this, paraDrawer);
    }

    public void drawForBodyText(ParagraphListInterface paraList) throws Exception {
        input.startBodyTextParaList(paraList.getParagraphs());

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                input.gotoParaCharPosition(e.paraIndex(), e.charIndex(), e.charPosition());
                input.currentParaListInfo().resetParaStartY(e.startY());
                redraw = true;
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (input.columnsInfo().isDistributionMultiColumn()
                    && !input.columnsInfo().lastColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        input.endBodyTextParaList();
        drawHeaderFooter();
    }

    public static void drawHeaderFooter(DrawingInput input, InterimOutput output) throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    public void drawHeaderFooter() throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    private static void drawHeader(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().header(output.currentPage()) != null) {
            output.startHeader();

            ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
            paraListDrawer.drawForControl(input.pageInfo().header(output.currentPage()).getParagraphList(), input.pageInfo().headerArea().widthHeight());

            output.endHeader();
        }
    }

    private static void drawFooter(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().footer(output.currentPage()) != null) {
            FooterOutput footerOutput = output.startFooter();

            ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
            long calculatedContentHeight = paraListDrawer.drawForControl(input.pageInfo().footer(output.currentPage()).getParagraphList(), input.pageInfo().footerArea().widthHeight());

            footerOutput.calculatedContentHeight(calculatedContentHeight);
            output.endFooter();
        }
    }

    public long drawForControl(ParagraphListInterface paraList, Area textArea) throws Exception {
        input.startControlParaList(textArea, paraList.getParagraphs());

        while (input.nextPara()) {
            paraDrawer.draw(false);
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (input.columnsInfo().isDistributionMultiColumn()
                    && !input.columnsInfo().lastColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        return input.endControlParaList();
    }

    public void redraw(int endParaIndex) throws Exception {
        boolean redraw = true;
        boolean endingPara = false;

        while (redraw || (endingPara = !input.nextPara()) == false) {
            if (endParaIndex != -1 && input.paraIndex() > endParaIndex) {
                endingPara = true;
                break;
            }
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                switch (e.type()) {
                    case ForNewPage:
                    case ForEndingTest:
                    case ForDividingColumn:
                        throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (endingPara == true
                    && !input.columnsInfo().lastColumn()
                    && distributionMultiColumnRearranger.hasEmptyColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        if (endingPara || input.nextPara() == false) {
            throw new BreakDrawingException().forEndingPara();
        }
    }

    public CharInfoBuffer charInfoBuffer() {
        return charInfoBuffer;
    }

    public DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return distributionMultiColumnRearranger;
    }

    /*
    private void paragraph(boolean redraw) throws Exception {
        controlExtendCharIndex = 0;

        if (redraw == false) {
            startPara();
        }

        resetForNewPara();
        if (input.noText()) {
            checkNewColumnAndPage();
            saveTextLineAndNewLine();
        } else {
            chars();
        }

        output.setLastLineInPara();

        endPara();
    }

    private void startPara() throws Exception {
        input.startPara();
        paraDividingProcessor.process();
    }

    private void endPara() {
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
        if (input.currentChar().getCode() == 11) {
            tableOrGso();
        } else if (input.currentChar().getCode() == 16) {
            headerFooter();
        } else {
            increaseControlExtendCharIndex();
        }
    }

    public void increaseControlExtendCharIndex() {
        controlExtendCharIndex++;
    }


    public int controlExtendCharIndex() {
        return controlExtendCharIndex;
    }

    private void tableOrGso() {
        ControlCharInfo charInfo = (ControlCharInfo) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = ControlCharInfo.create((HWPCharControlExtend) input.currentChar(), control, input);
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
            increaseControlExtendCharIndex();
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
        increaseControlExtendCharIndex();
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


    private boolean isOverBottom(long height) {
        return input.columnsInfo().currentColumnArea().bottom() < currentTextPartArea.top() + height;
    }

    public void newPage() throws Exception {
        if (forDistributionMultiColumn) {
            throw new BreakDrawingException(input.paraIndex(), input.charIndex(), input.charPosition()).forNewPage();
        }

        pagePainter.saveCurrentPage();
        charInfoBuffer.clearUntilPreviousPara();

        input.newPage();
        resetForNewPage();
        output.newPage(input);
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

    public void nextMultiColumn() {
        distributionMultiColumnRearranger.endParaIndex(-1);
        setColumnDefine(output.multiColumnBottom());
        output.newMultiColumn(input.columnsInfo());
        resetForNewColumn();
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
            input.newMultiColumn((ControlColumnDefine) input.currentPara().getControlList().get(controlExtendCharIndex),  startY + MultiColumnGsp);
            controlExtendCharIndex++;
        } else {
            input.newMultiColumnWithSameColumnDefine(startY + MultiColumnGsp);
            input.previousChar(1);
        }
    }

    public void nextColumn() {
        input.nextColumn();
        output.nextColumn();
        resetForNewColumn();
    }

    public void previousColumn() {
        input.previousColumn();
        output.previousColumn();
        resetForNewColumn();
    }

    public void resetForNewColumn() {
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
        if (drawingState.isNormal()
                && (isOverBottom(textLineDrawer.maxCharHeight())
                        || input.columnsInfo().isOverLimitedTextLineCount(output.textLineCount()))) {
            if (isOverBottom(textLineDrawer.maxCharHeight()) && input.columnsInfo().lastColumn() && input.isBodyText()) {
                newPage();
            } else {
                output.currentColumn().nextChar(textLineDrawer.firstCharInfo());

                if (!input.columnsInfo().lastColumn()) {
                    if (!output.currentMultiColumn().hadRearrangedDistributionMultiColumn()
                            && (distributionMultiColumnRearranger.testing()
                            || input.columnsInfo().isDistributionMultiColumn())) {

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

     */
}
